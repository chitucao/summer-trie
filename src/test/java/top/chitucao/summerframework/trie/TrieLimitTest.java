package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
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
import top.chitucao.summerframework.trie.query.ResultBuilder;

/**
 * limit 下推的特征(characterization)测试。
 * <p>
 * 验证 {@link Criteria#limit(int)} 在各查询方法上：
 * <ul>
 *   <li>精确把结果裁剪到 n 条（dataSearch / propertySearch / listSearch / treeSearch）；</li>
 *   <li>limit ≥ 总量时返回全部；</li>
 *   <li>不设置 limit 时行为与原先一致（回归护栏，每个用例都先断言全量）；</li>
 *   <li>有序层(skiplist)下 limit 取到的是确定的“最小前 n 个”；</li>
 *   <li>limit 设在链式任意节点都生效；</li>
 *   <li>limit 非法值（≤0）抛异常。</li>
 * </ul>
 * 结构：city(HASH_MAP) → seq(CONCURRENT_SKIP_LIST_MAP，有序) → data(AutoMapping，叶子存数据)。
 * 数据：BJ 的 seq 1..10、SH 的 seq 1..5，共 15 条，每个 (city,seq) 恰好 1 条。
 *
 * @author 资深全栈架构师
 */
public class TrieLimitTest {

    public static class Item {
        private final String city;
        private final int    seq;
        private final long   id;

        public Item(String city, int seq, long id) {
            this.city = city;
            this.seq = seq;
            this.id = id;
        }

        public String getCity() {
            return city;
        }

        public int getSeq() {
            return seq;
        }

        public long getId() {
            return id;
        }
    }

    /** listSearch 的可变结果类（需要 setter） */
    public static class Row {
        private String  city;
        private Integer seq;

        public void setCity(String city) {
            this.city = city;
        }

        public void setSeq(Integer seq) {
            this.seq = seq;
        }

        public String getCity() {
            return city;
        }

        public Integer getSeq() {
            return seq;
        }
    }

    private Trie<Item> trie;

    @Before
    public void setup() {
        Configuration cfg = new Configuration();

        OriginProperty<Item, String> cityProp = new OriginProperty<>("city", NodeType.HASH_MAP);
        cityProp.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(cityProp);

        // seq 用有序跳表节点，使 limit 取到“最小的前 n 个”是确定的
        OriginProperty<Item, Integer> seqProp = new OriginProperty<>("seq", NodeType.CONCURRENT_SKIP_LIST_MAP);
        seqProp.setObject2FieldMapper(Item::getSeq);
        cfg.addProperty(seqProp);

        AutoMappingProperty<Item, Item> dataProp = new AutoMappingProperty<>("data");
        dataProp.setObject2FieldMapper(Function.identity());
        cfg.addProperty(dataProp);

        trie = new MapTrie<>(cfg);

        long id = 1;
        for (int s = 1; s <= 10; s++) {
            trie.insert(new Item("BJ", s, id++));
        }
        for (int s = 1; s <= 5; s++) {
            trie.insert(new Item("SH", s, id++));
        }
    }

    /**
     * dataSearch 的 limit 精确裁剪结果条数。
     */
    @Test
    public void testDataSearch_limit_capsSize() {
        assertEquals("全量应为15条", 15, trie.dataSearch(new Criteria()).size());
        assertEquals("limit=5 应裁剪到5条", 5, trie.dataSearch(new Criteria().limit(5)).size());
        assertEquals("limit=1 应裁剪到1条", 1, trie.dataSearch(new Criteria().limit(1)).size());
    }

    /**
     * limit 超过总量时返回全部（不会越界、不会少返回）。
     */
    @Test
    public void testDataSearch_limit_largerThanTotal_returnsAll() {
        assertEquals("limit 超过总量应返回全部", 15, trie.dataSearch(new Criteria().limit(100)).size());
        // 单城市 SH 共 5 条
        assertEquals(5, trie.dataSearch(new Criteria().and("city").eq("SH").limit(100)).size());
    }

    /**
     * 有序(skiplist)层下，limit 取到的是确定的“最小前 n 个”——city=BJ 下 seq 最小的 1,2,3。
     */
    @Test
    public void testDataSearch_limit_withCriteria_deterministicBySortedSeq() {
        List<Item> top3 = trie.dataSearch(new Criteria().and("city").eq("BJ").limit(3));
        assertEquals(3, top3.size());
        Set<Integer> seqs = new HashSet<>();
        for (Item it : top3) {
            seqs.add(it.getSeq());
        }
        assertEquals("应为 seq 最小的前3个", new HashSet<>(Arrays.asList(1, 2, 3)), seqs);
    }

    /**
     * propertySearch 的 limit：城市去重后裁剪；seq 去重后取最小前 n（两城市都从 1 开始，确定为 1..n）。
     */
    @Test
    public void testPropertySearch_limit() {
        assertEquals("去重城市共2个", 2, trie.<String> propertySearch(new Criteria(), "city").size());
        assertEquals("limit=1 只取1个城市", 1, trie.<String> propertySearch(new Criteria().limit(1), "city").size());

        // seq 跨城市去重共 10 个(1..10)；limit=4 取最小的 1..4
        List<Integer> seqs = trie.propertySearch(new Criteria().limit(4), "seq");
        assertEquals(4, seqs.size());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4)), new HashSet<>(seqs));
    }

    /**
     * treeSearch 的 limit：把平铺后的叶子行数裁剪到 n。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTreeSearch_limit_capsFlattenedRows() {
        Map<String, List<Integer>> all = (Map<String, List<Integer>>) trie.treeSearch(new Criteria(), new Aggregations(), "city", "seq");
        assertEquals("不限制时平铺总行数应为15", 15, flatten(all));

        Map<String, List<Integer>> limited = (Map<String, List<Integer>>) trie.treeSearch(new Criteria().limit(4), new Aggregations(), "city", "seq");
        assertEquals("treeSearch limit 应把平铺行数裁剪到4", 4, flatten(limited));
    }

    /**
     * listSearch 的 limit：精确裁剪结果列表大小。
     */
    @Test
    public void testListSearch_limit() {
        assertEquals("不限制应为15行", 15, buildListSearch(new Criteria()).size());
        assertEquals("limit=6 应裁剪到6行", 6, buildListSearch(new Criteria().limit(6)).size());
    }

    /**
     * limit 设在链式前段、后面再 .and() 追加条件，仍应生效（getLimit 会扫描整条 criteriaChain）。
     */
    @Test
    public void testLimit_setInChain_isHonored() {
        List<Item> r = trie.dataSearch(Criteria.where("city").eq("BJ").limit(3).and("seq").gte(1));
        assertEquals("limit 设在链式前段仍生效", 3, r.size());
    }

    /**
     * limit 非法值（≤0）应抛异常。
     */
    @Test
    public void testLimit_invalidValue_throws() {
        try {
            new Criteria().limit(0);
            fail("limit(0) 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 预期
        }
        try {
            new Criteria().limit(-3);
            fail("limit(负数) 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 预期
        }
    }

    private int flatten(Map<String, List<Integer>> grouped) {
        int total = 0;
        for (List<Integer> v : grouped.values()) {
            total += v.size();
        }
        return total;
    }

    private List<Row> buildListSearch(Criteria criteria) {
        ResultBuilder<Row> rb = new ResultBuilder<>(Row::new);
        rb.addSetter("city", Row::setCity);
        rb.addSetter("seq", Row::setSeq);
        return trie.listSearch(criteria, new Aggregations(), rb);
    }
}
