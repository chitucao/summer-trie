package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OneWayMappingProperty;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.nodemanager.DefaultNodeManager;
import top.chitucao.summerframework.trie.nodemanager.NodeManager;
import top.chitucao.summerframework.trie.query.Criteria;
import top.chitucao.summerframework.trie.query.Criterion;
import top.chitucao.summerframework.trie.train.TrainSourceDO;
import top.chitucao.summerframework.trie.utils.FilteredChildMap;

/**
 * 阶段一优化的回归测试：
 * - P1-1: NAVIGABLE_MAP 范围查询返回 view 后，配套快照保证 normErase / fastErase 不 CME、结果与快慢路径一致。
 * - P1-2: HASH_MAP 分支返回 FilteredChildMap，多条件链式和聚合/存在性检查结果与原地物化一致。
 * - P1-3: levelSearch / fastErase 替换 Stream 后，多条件组合结果与原实现语义一致。
 */
public class TrieRegressionTest {

    // 资源目录：解析到测试 classpath 根，随项目位置自动生效（避免硬编码绝对路径，工程移动后仍可用）
    private static final String RESOURCE_FOLDER = new File(TrieRegressionTest.class.getResource("/").getFile()).getAbsolutePath() + File.separator;

    private static final String DATA_FILE       = "train_resource_3000.json";

    /** 价格用 TreeMap、其他字段用 HashMap 的组合，能同时覆盖 P1-1 (TreeMap view) 和 P1-2 (HashMap FilteredChildMap)。 */
    private Configuration buildMixedConfiguration() {
        Configuration configuration = new Configuration();
        // 价格（范围字段，TreeMap）
        OneWayMappingProperty<TrainSourceDO, Double, Integer> price = new OneWayMappingProperty<>("minRealPrice", NodeType.TREE_MAP);
        price.setObject2FieldMapper(TrainSourceDO::getMinRealPrice);
        price.setField2NodeKeyMapper(r -> (int) (r * 100));
        configuration.addProperty(price);
        // 出发城市（等值 / IN，HashMap）
        OneWayMappingProperty<TrainSourceDO, Integer, Integer> depCity = new OneWayMappingProperty<>("depCityId", NodeType.HASH_MAP);
        depCity.setObject2FieldMapper(TrainSourceDO::getDepartureCityId);
        depCity.setField2NodeKeyMapper(Function.identity());
        configuration.addProperty(depCity);
        // 车次类型（等值，HashMap + 字典压缩）
        AutoMappingProperty<TrainSourceDO, String> trainType = new AutoMappingProperty<>("trainType", DictKeyType.BYTE);
        trainType.setObject2FieldMapper(TrainSourceDO::getTrainType);
        configuration.addProperty(trainType);
        // 叶子：数据本身
        AutoMappingProperty<TrainSourceDO, TrainSourceDO> data = new AutoMappingProperty<>("data");
        data.setObject2FieldMapper(Function.identity());
        configuration.addProperty(data);
        return configuration;
    }

    private List<TrainSourceDO> loadData() {
        File file = FileUtil.newFile(RESOURCE_FOLDER + DATA_FILE);
        String json = FileUtil.readString(file, StandardCharsets.UTF_8);
        TypeReference<List<TrainSourceDO>> ref = new TypeReference<List<TrainSourceDO>>() {
        };
        return JSONUtil.toBean(json, ref, true);
    }

    private MapTrie<TrainSourceDO> buildTrie(boolean useFastErase) {
        Configuration cfg = buildMixedConfiguration();
        cfg.setUseFastErase(useFastErase);
        MapTrie<TrainSourceDO> trie = new MapTrie<>(cfg);
        for (TrainSourceDO d : loadData()) {
            trie.insert(d);
        }
        return trie;
    }

    // ---------------- P1-1: NAVIGABLE_MAP view 安全性 ----------------

    @Test
    public void testRangeQueryReturnsNavigableView() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        // 取 root（价格层）
        Node<?> root = trie.getRoot();
        NodeManager head = getHeadNodeManager(trie);
        Criteria c = Criteria.where("minRealPrice").between(0.0, 100.0);
        Criterion criterion = c.getAllCriterion().get("minRealPrice");
        Map<?, ? extends Node<?>> result = ((DefaultNodeManager) head).search((Node) root, criterion);
        // P1-1 的语义：范围查询直接返回 NavigableMap 子视图，不是新 TreeMap
        assertTrue("range op should return NavigableMap sub-view, actual: " + result.getClass().getName(),
            result instanceof NavigableMap);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testNormEraseWithRangeNoCME() {
        // 关键回归：normErase 走 search().keySet() 路径，subMap view 若不快照会 CME
        MapTrie<TrainSourceDO> trie = buildTrie(false);
        int before = trie.getSize();
        assertTrue(before > 0);

        Criteria c = Criteria.where("minRealPrice").between(50.0, 150.0);
        int removed = trie.erase(c);
        assertTrue("normErase should return removed count >= 0, got " + removed, removed >= 0);
        int after = trie.getSize();
        assertEquals(before - removed, after);

        // 剩余数据的价格必须全部 < 50 或 > 150
        List<TrainSourceDO> remain = trie.dataSearch(new Criteria());
        for (TrainSourceDO d : remain) {
            boolean inRange = d.getMinRealPrice() >= 50.0 && d.getMinRealPrice() <= 150.0;
            assertFalse("data in erased range still present: " + d.getMinRealPrice(), inRange);
        }
    }

    @Test
    public void testFastEraseWithRangeNoCME() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        int before = trie.getSize();

        Criteria c = Criteria.where("minRealPrice").between(50.0, 150.0);
        trie.erase(c);
        // fastErase 不维护 size，这里直接走 dataSearch 验证残留
        List<TrainSourceDO> remain = trie.dataSearch(new Criteria());
        assertTrue(remain.size() < before);
        for (TrainSourceDO d : remain) {
            boolean inRange = d.getMinRealPrice() >= 50.0 && d.getMinRealPrice() <= 150.0;
            assertFalse("data in erased range still present: " + d.getMinRealPrice(), inRange);
        }
    }

    @Test
    public void testFastAndNormEraseEquivalentResult() {
        // fastErase 和 normErase 同样条件后剩余数据集合应相同
        MapTrie<TrainSourceDO> trieFast = buildTrie(true);
        MapTrie<TrainSourceDO> trieNorm = buildTrie(false);

        Criteria c = Criteria.where("minRealPrice").between(50.0, 150.0).and("depCityId").ne(0);
        trieFast.erase(c);
        trieNorm.erase(c);

        List<TrainSourceDO> a = trieFast.dataSearch(new Criteria());
        List<TrainSourceDO> b = trieNorm.dataSearch(new Criteria());
        a.sort(Comparator.comparing(TrainSourceDO::getId));
        b.sort(Comparator.comparing(TrainSourceDO::getId));
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).getId(), b.get(i).getId());
        }
    }

    // ---------------- P1-2: HASH_MAP FilteredChildMap 行为一致性 ----------------

    @Test
    public void testHashMapNinReturnsFilteredChildMap() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        NodeManager depCityManager = getNodeManagerByName(trie, "depCityId");
        Node<?> cityRootPrice = trie.getRoot().children().values().iterator().next();

        Criteria c = Criteria.where("depCityId").nin(Arrays.asList(100, 200, 300));
        Criterion criterion = c.getAllCriterion().get("depCityId");
        Map<?, ?> result = ((DefaultNodeManager) depCityManager).search((Node) cityRootPrice, criterion);
        assertTrue("HashMap NIN should return FilteredChildMap, actual: " + result.getClass().getName(),
            result instanceof FilteredChildMap);
    }

    @Test
    public void testMultiCondConsistencyWithStreamFilter() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        List<TrainSourceDO> data = loadData();

        Criteria criteria = Criteria.where("minRealPrice").between(50.0, 200.0)//
            .and("depCityId").in(Arrays.asList(data.get(0).getDepartureCityId(), data.get(1).getDepartureCityId()))//
            .and("trainType").ne("UNKNOWN");
        List<TrainSourceDO> actual = trie.dataSearch(criteria);

        List<Long> expectedIds = data.stream()
            .filter(e -> e.getMinRealPrice() >= 50.0 && e.getMinRealPrice() <= 200.0)
            .filter(e -> e.getDepartureCityId() == data.get(0).getDepartureCityId() || e.getDepartureCityId() == data.get(1).getDepartureCityId())
            .filter(e -> !Objects.equals(e.getTrainType(), "UNKNOWN"))
            .map(TrainSourceDO::getId)
            .sorted()
            .collect(Collectors.toList());
        List<Long> actualIds = actual.stream().map(TrainSourceDO::getId).sorted().collect(Collectors.toList());
        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void testInSingleValueStillReturnsMaterializedMap() {
        // IN 单值走原短路分支，返回物化 HashMap（非 FilteredChildMap），避免多余开销
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        List<TrainSourceDO> data = loadData();
        int cityId = data.get(0).getDepartureCityId();

        NodeManager depCityManager = getNodeManagerByName(trie, "depCityId");
        Node<?> cityRootPrice = trie.getRoot().children().values().iterator().next();

        Criteria c = Criteria.where("depCityId").in(Arrays.asList(cityId));
        Criterion criterion = c.getAllCriterion().get("depCityId");
        Map<?, ?> result = ((DefaultNodeManager) depCityManager).search((Node) cityRootPrice, criterion);
        assertFalse("IN single value should not return FilteredChildMap, actual: " + result.getClass().getName(),
            result instanceof FilteredChildMap);
    }

    // ---------------- P1-3: Stream→迭代 后行为不变 ----------------

    @Test
    public void testDataSearchWithoutAnyCriterionReturnsAll() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        int all = trie.getSize();
        List<TrainSourceDO> result = trie.dataSearch(new Criteria());
        assertEquals(all, result.size());
    }

    @Test
    public void testListSearchFlattenPathsConsistent() {
        MapTrie<TrainSourceDO> trie = buildTrie(true);
        // 聚合到 depCityId 维度，验证路径展平和上一版行为一致（条数 ≤ 数据总数，且每个 city 都在原数据里）
        List<Integer> result = trie.propertySearch(new Criteria(), "depCityId");
        assertNotNull(result);
        List<Integer> expected = loadData().stream().map(TrainSourceDO::getDepartureCityId).distinct().sorted().collect(Collectors.toList());
        List<Integer> actualSorted = result.stream().sorted().collect(Collectors.toList());
        assertEquals(expected, actualSorted);
    }

    // ---------------- Helper：通过反射取 MapTrie 内部 NodeManager，便于直接断言底层返回类型 ----------------

    private NodeManager getHeadNodeManager(MapTrie<?> trie) {
        return getNodeManagers(trie).get(0);
    }

    private NodeManager getNodeManagerByName(MapTrie<?> trie, String name) {
        for (NodeManager nm : getNodeManagers(trie)) {
            if (nm.property().name().equals(name)) {
                return nm;
            }
        }
        throw new IllegalStateException("nodeManager " + name + " not found");
    }

    @SuppressWarnings("unchecked")
    private List<NodeManager> getNodeManagers(MapTrie<?> trie) {
        try {
            java.lang.reflect.Field f = MapTrie.class.getDeclaredField("nodeManagers");
            f.setAccessible(true);
            return (List<NodeManager>) f.get(trie);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
