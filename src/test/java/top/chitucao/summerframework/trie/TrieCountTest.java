package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * T4：subtreeSize + getSize O(1) + count(Criteria) 的测试。
 * 覆盖：纯插入后 getSize/count、精确删除 erase(T) 维护、normErase 维护(可靠路径)、fastErase 退化兜底(仍正确)。
 *
 * @author 资深全栈架构师
 */
public class TrieCountTest {

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

    private Trie<Item> build(boolean fastErase) {
        Configuration cfg = new Configuration();
        cfg.setUseFastErase(fastErase);

        OriginProperty<Item, String> regionProp = new OriginProperty<>("region", NodeType.HASH_MAP);
        regionProp.setObject2FieldMapper(Item::getRegion);
        cfg.addProperty(regionProp);

        OriginProperty<Item, String> cityProp = new OriginProperty<>("city", NodeType.HASH_MAP);
        cityProp.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(cityProp);

        AutoMappingProperty<Item, Item> dataProp = new AutoMappingProperty<>("data");
        dataProp.setObject2FieldMapper(Function.identity());
        cfg.addProperty(dataProp);

        return new MapTrie<>(cfg);
    }

    /** CN: BJ,BJ,SH,X ; US: NY,X  共6条；返回插入的对象引用列表(供 erase(T) 使用) */
    private List<Item> seed(Trie<Item> trie) {
        List<Item> items = new ArrayList<>();
        items.add(new Item("CN", "BJ", 1));
        items.add(new Item("CN", "BJ", 2));
        items.add(new Item("CN", "SH", 3));
        items.add(new Item("CN", "X", 4));
        items.add(new Item("US", "NY", 5));
        items.add(new Item("US", "X", 6));
        for (Item it : items) {
            trie.insert(it);
        }
        return items;
    }

    @Test
    public void testGetSize_afterInserts_O1Path() {
        Trie<Item> trie = build(false);
        seed(trie);
        // 纯插入后 size 可靠，getSize 走 subtreeSize O(1)
        assertEquals(6, trie.getSize());
    }

    @Test
    public void testCount_matchesDataSearch() {
        Trie<Item> trie = build(false);
        seed(trie);

        assertEquals("无条件 count == 全量", 6, trie.count(new Criteria()));
        assertEquals("region=CN", 4, trie.count(new Criteria().and("region").eq("CN")));
        assertEquals("region=US", 2, trie.count(new Criteria().and("region").eq("US")));
        assertEquals("region=CN & city=BJ", 2, trie.count(new Criteria().and("region").eq("CN").and("city").eq("BJ")));
        assertEquals("city=X 跨region", 2, trie.count(new Criteria().and("city").eq("X")));

        // 与 dataSearch().size() 对账
        assertEquals(trie.dataSearch(new Criteria().and("region").eq("CN")).size(), trie.count(new Criteria().and("region").eq("CN")));
    }

    @Test
    public void testEraseObject_maintainsSize() {
        Trie<Item> trie = build(false);
        List<Item> items = seed(trie);
        assertEquals(6, trie.getSize());

        // 精确删除一条(CN-SH)
        trie.erase(items.get(2));
        assertEquals("erase(T) 后 getSize -1", 5, trie.getSize());
        assertEquals("count 同步", 5, trie.count(new Criteria()));
        assertEquals("CN 由4变3", 3, trie.count(new Criteria().and("region").eq("CN")));
    }

    @Test
    public void testNormErase_maintainsSize_reliable() {
        Trie<Item> trie = build(false); // 普通删除，维护 size，getSize 仍走 O(1)
        seed(trie);
        int removed = trie.erase(new Criteria().and("region").eq("CN").and("city").eq("BJ"));
        assertEquals("normErase 返回删除条数", 2, removed);
        assertEquals("getSize 6-2=4", 4, trie.getSize());
        assertEquals("count 同步", 4, trie.count(new Criteria()));
        assertEquals("CN 4-2=2", 2, trie.count(new Criteria().and("region").eq("CN")));
    }

    /** 回归：normErase 用非叶层条件删除后，被删空的树节点应真正移除，dataSearch 不得返回残留 null */
    @Test
    public void testNormErase_internalCriterion_removesTreeNodes() {
        Trie<Item> trie = build(false);
        seed(trie); // CN: BJ,BJ,SH,X ; US: NY,X  共6条

        // 非叶层条件：删除 CN-BJ（2条）
        trie.erase(new Criteria().and("region").eq("CN").and("city").eq("BJ"));

        assertEquals(4, trie.getSize());
        // CN 现在只剩 SH、X 各1条；dataSearch 不应有残留 null
        List<Item> cn = trie.dataSearch(new Criteria().and("region").eq("CN"));
        assertEquals("CN 应剩2条且无残留null", 2, cn.size());
        for (Item it : cn) {
            assertNotNull("dataSearch 不应返回残留 null", it);
        }
        // 被删的 CN-BJ 应彻底不可见
        assertEquals(0, trie.dataSearch(new Criteria().and("region").eq("CN").and("city").eq("BJ")).size());
    }

    @Test
    public void testFastErase_fallbackStillCorrect() {
        Trie<Item> trie = build(true); // 快速删除：不维护 size，置不可靠，getSize/count 退化遍历但结果正确
        seed(trie);
        assertEquals("删除前 6", 6, trie.getSize());

        int ret = trie.erase(new Criteria().and("region").eq("CN").and("city").eq("BJ"));
        assertEquals("fastErase 返回 -1", -1, ret);

        // size 不可靠 → 走遍历兜底，结果应正确
        assertEquals("getSize 兜底=4", 4, trie.getSize());
        assertEquals("count 无条件兜底=4", 4, trie.count(new Criteria()));
        assertEquals("count region=CN 兜底=2", 2, trie.count(new Criteria().and("region").eq("CN")));
        assertEquals("count region=US 兜底=2", 2, trie.count(new Criteria().and("region").eq("US")));
    }
}
