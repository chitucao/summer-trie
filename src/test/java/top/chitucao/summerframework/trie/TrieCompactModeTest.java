package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.AdaptiveNode;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * Configuration.compactReadOnly 一键紧凑只读模式测试：
 * 用普通 CONCURRENT_HASH_MAP 配置 + 开关，应透明切换为 AdaptiveNode(节点) + ArrayDict(AutoMapping字典)，且结果与普通模式一致。
 *
 * @author 资深全栈架构师
 */
public class TrieCompactModeTest {

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

    private Trie<Item> build(boolean compact) {
        Configuration cfg = new Configuration();
        cfg.setCompactReadOnly(compact);

        OriginProperty<Item, String> region = new OriginProperty<>("region", NodeType.CONCURRENT_HASH_MAP);
        region.setObject2FieldMapper(Item::getRegion);
        cfg.addProperty(region);

        OriginProperty<Item, String> city = new OriginProperty<>("city", NodeType.CONCURRENT_HASH_MAP);
        city.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(city);

        AutoMappingProperty<Item, Item> data = new AutoMappingProperty<>("data", DictKeyType.LONG, NodeType.CONCURRENT_HASH_MAP);
        data.setObject2FieldMapper(Function.identity());
        cfg.addProperty(data);

        return new MapTrie<>(cfg);
    }

    private List<Item> seed(Trie<Item> trie) {
        List<Item> items = new ArrayList<>();
        long id = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 10; c++) {
                Item it = new Item("R" + r, "C" + c, id++);
                items.add(it);
                trie.insert(it);
            }
        }
        return items;
    }

    /** 开关打开后，哈希层节点应被透明替换为 AdaptiveNode */
    @Test
    public void testCompact_usesAdaptiveNode() {
        Trie<Item> trie = build(true);
        seed(trie);
        assertTrue("紧凑模式下哈希层根节点应为 AdaptiveNode", trie.getRoot() instanceof AdaptiveNode);
        assertEquals(50, trie.getSize());
    }

    /** 紧凑模式查询/计数/删除均正确 */
    @Test
    public void testCompact_correctness() {
        Trie<Item> trie = build(true);
        List<Item> items = seed(trie);

        assertEquals(50, trie.count(new Criteria()));
        assertEquals(10, trie.count(new Criteria().and("region").eq("R0")));
        assertEquals(10, trie.dataSearch(new Criteria().and("region").eq("R0")).size());

        trie.erase(items.get(0)); // 删 R0-C0
        assertEquals(49, trie.getSize());
        assertEquals(9, trie.dataSearch(new Criteria().and("region").eq("R0")).size());
    }

    /** 紧凑模式与普通模式结果一致 */
    @Test
    public void testCompact_matchesPlain() {
        Trie<Item> plain = build(false);
        Trie<Item> compact = build(true);
        seed(plain);
        seed(compact);

        assertEquals(plain.getSize(), compact.getSize());
        assertEquals(plain.count(new Criteria().and("region").eq("R2")), compact.count(new Criteria().and("region").eq("R2")));

        Set<Long> plainIds = new HashSet<>();
        for (Item it : plain.dataSearch(new Criteria().and("region").eq("R2"))) {
            plainIds.add(it.getId());
        }
        Set<Long> compactIds = new HashSet<>();
        for (Item it : compact.dataSearch(new Criteria().and("region").eq("R2"))) {
            compactIds.add(it.getId());
        }
        assertEquals(plainIds, compactIds);
    }
}
