package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Aggregations;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * 多层查询(dfsSearch/multiLevelSearch)的特征(characterization)测试。
 * <p>
 * 用于验证 T2 把 dfsSearch 的“每节点复制路径”改为“回溯(add/removeLast)”后，分组/平铺语义保持不变：
 * 覆盖分支、同名city跨region复用、同一路径下多条数据的去重等场景。
 *
 * @author 资深全栈架构师
 */
public class TrieMultiLevelSearchTest {

    public static class Item {
        private final String region;
        private final String city;
        private final long   id;

        public Item(String region, String city, long id) {
            this.region = region;
            this.city = city;
            this.id = id;
        }

        public String getRegion() {
            return region;
        }

        public String getCity() {
            return city;
        }

        public long getId() {
            return id;
        }
    }

    private Trie<Item> trie;

    @Before
    public void setup() {
        Configuration cfg = new Configuration();

        OriginProperty<Item, String> regionProp = new OriginProperty<>("region", NodeType.HASH_MAP);
        regionProp.setObject2FieldMapper(Item::getRegion);
        cfg.addProperty(regionProp);

        OriginProperty<Item, String> cityProp = new OriginProperty<>("city", NodeType.HASH_MAP);
        cityProp.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(cityProp);

        AutoMappingProperty<Item, Item> dataProp = new AutoMappingProperty<>("data");
        dataProp.setObject2FieldMapper(Function.identity());
        cfg.addProperty(dataProp);

        trie = new MapTrie<>(cfg);

        // CN: BJ(2条，测同一路径下多数据的去重), SH, X
        trie.insert(new Item("CN", "BJ", 1));
        trie.insert(new Item("CN", "BJ", 2));
        trie.insert(new Item("CN", "SH", 3));
        trie.insert(new Item("CN", "X", 4));
        // US: NY, X(同名city跨region，测回溯弹栈是否干净)
        trie.insert(new Item("US", "NY", 5));
        trie.insert(new Item("US", "X", 6));
    }

    /**
     * 两层分组：treeSearch(region, city) 应得到 {CN:[BJ,SH,X], US:[NY,X]}，
     * 其中 CN-BJ 虽有2条数据也只出现一次（数据层终止前快照，天然去重）。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTreeSearch_groupByRegionCity() {
        Map<String, List<String>> grouped = (Map<String, List<String>>) trie.treeSearch(new Criteria(), new Aggregations(), "region", "city");

        assertEquals("region 分组数应为2", 2, grouped.size());
        assertEquals("CN 下的城市集合", new HashSet<>(java.util.Arrays.asList("BJ", "SH", "X")), new HashSet<>(grouped.get("CN")));
        assertEquals("US 下的城市集合", new HashSet<>(java.util.Arrays.asList("NY", "X")), new HashSet<>(grouped.get("US")));
        // 去重校验：CN-BJ 两条数据，但分组里 BJ 只应出现一次（路径回溯/快照正确，无重复）
        assertEquals("CN 城市列表不应有重复", 3, grouped.get("CN").size());
    }

    /**
     * 带条件的两层分组：region=CN 时只应返回 CN 组，且回溯不串味到其它分支。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTreeSearch_withCriteria() {
        Map<String, List<String>> grouped = (Map<String, List<String>>) trie.treeSearch(new Criteria().and("region").eq("CN"), new Aggregations(), "region", "city");

        assertEquals("只应有 CN 一个分组", 1, grouped.size());
        assertEquals(new HashSet<>(java.util.Arrays.asList("BJ", "SH", "X")), new HashSet<>(grouped.get("CN")));
    }

    /**
     * 单层 city 查询(propertySearch 路径)：所有 region 下的 city 去重并集，验证 X 跨 region 只算一次。
     */
    @Test
    public void testPropertySearch_distinctCities() {
        List<String> cities = trie.propertySearch(new Criteria(), "city");
        assertEquals("去重后的城市集合", new HashSet<>(java.util.Arrays.asList("BJ", "SH", "X", "NY")), new HashSet<>(cities));
    }

    /**
     * 叶子数据查询：全量 dataSearch 应返回全部6条（含 CN-BJ 的2条）。
     */
    @Test
    public void testDataSearch_all() {
        List<Item> all = trie.dataSearch(new Criteria());
        assertEquals(6, all.size());
    }

    /**
     * 按城市过滤的叶子查询：CN 下 BJ 有2条数据。
     */
    @Test
    public void testDataSearch_byRegionCity() {
        List<Item> bj = trie.dataSearch(new Criteria().and("region").eq("CN").and("city").eq("BJ"));
        assertEquals(2, bj.size());
        Set<Long> ids = new HashSet<>();
        for (Item it : bj) {
            ids.add(it.getId());
        }
        assertEquals(new HashSet<>(java.util.Arrays.asList(1L, 2L)), ids);
    }

    /**
     * 尾层漏数据修复的判别性测试：treeSearch(region, city, data) 中 data 是展示字段，
     * CN-BJ 同一路径下有2条数据，修复前只会返回1条，修复后应返回全部2条。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTreeSearch_dataAsDisplay_multipleLeavesPerPath() {
        Map<String, Map<String, List<Item>>> res = (Map<String, Map<String, List<Item>>>) trie.treeSearch(new Criteria(), new Aggregations(), "region", "city", "data");

        // CN-BJ 同路径2条数据：修复前=1（漏），修复后=2
        assertEquals("CN-BJ 应返回全部2条数据（尾层漏数据已修复）", 2, res.get("CN").get("BJ").size());
        assertEquals("CN-SH 1条", 1, res.get("CN").get("SH").size());
        assertEquals("US-NY 1条", 1, res.get("US").get("NY").size());

        // 全量展开后总行数应等于全部6条数据（修复前会少于6）
        int total = 0;
        for (Map<String, List<Item>> cityMap : res.values()) {
            for (List<Item> dataList : cityMap.values()) {
                total += dataList.size();
            }
        }
        assertEquals("展开后数据总行数应为6", 6, total);
    }
}
