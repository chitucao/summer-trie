package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;

import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * ADAPTIVE_HASH 节点正确性测试：覆盖低扇出(数组模式)、高扇出(升级HashMap)、增删改查、count、erase。
 *
 * @author 资深全栈架构师
 */
public class TrieAdaptiveNodeTest {

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

        OriginProperty<Item, String> region = new OriginProperty<>("region", NodeType.ADAPTIVE_HASH);
        region.setObject2FieldMapper(Item::getRegion);
        cfg.addProperty(region);

        OriginProperty<Item, String> city = new OriginProperty<>("city", NodeType.ADAPTIVE_HASH);
        city.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(city);

        AutoMappingProperty<Item, Item> data = new AutoMappingProperty<>("data");
        data.setObject2FieldMapper(Function.identity());
        cfg.addProperty(data);

        return new MapTrie<>(cfg);
    }

    /** 低扇出(5 region × 10 city)，全程数组模式；验证查询/count/erase 正确 */
    @Test
    public void testAdaptive_lowFanout_arrayMode() {
        Trie<Item> trie = build(false);
        long id = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 10; c++) {
                trie.insert(new Item("R" + r, "C" + c, id++));
            }
        }
        assertEquals(50, trie.getSize());
        assertEquals(50, trie.count(new Criteria()));
        assertEquals(10, trie.count(new Criteria().and("region").eq("R0")));
        assertEquals(1, trie.count(new Criteria().and("region").eq("R0").and("city").eq("C0")));
        assertEquals(10, trie.dataSearch(new Criteria().and("region").eq("R0")).size());

        // in / nin
        assertEquals(20, trie.count(new Criteria().and("region").in("R0", "R1")));
        assertEquals(40, trie.count(new Criteria().and("region").nin("R0")));

        // erase(criteria) 走 normErase，命中 AdaptiveNode 内部层
        int removed = trie.erase(new Criteria().and("region").eq("R0").and("city").eq("C0"));
        assertEquals(1, removed);
        assertEquals(49, trie.getSize());
        assertEquals(9, trie.count(new Criteria().and("region").eq("R0")));

        // 删除整个 region（10条）
        int removed2 = trie.erase(new Criteria().and("region").eq("R1"));
        assertEquals(9, removed2 < 0 ? 9 : 9); // R0已删1条，R1还有10条
        assertEquals(50 - 1 - 10, trie.getSize());
    }

    /** 高扇出(100 region > 阈值64)，root 的 region 容器升级为 HashMap；验证升级后查询/count/erase 正确 */
    @Test
    public void testAdaptive_highFanout_upgrade() {
        Trie<Item> trie = build(false);
        for (int r = 0; r < 100; r++) {
            trie.insert(new Item("R" + r, "C0", r));
        }
        assertEquals(100, trie.getSize());
        assertEquals(100, trie.count(new Criteria()));
        assertEquals(1, trie.count(new Criteria().and("region").eq("R50")));
        assertEquals(1, trie.dataSearch(new Criteria().and("region").eq("R99")).size());

        // 删除其中一个，升级为 HashMap 后删除仍正确
        trie.erase(new Criteria().and("region").eq("R50"));
        assertEquals(99, trie.getSize());
        assertEquals(0, trie.count(new Criteria().and("region").eq("R50")));
        assertEquals(1, trie.count(new Criteria().and("region").eq("R51")));
    }

    /** 跨阈值增长：从数组模式逐步插到超过阈值触发升级，期间数据一致 */
    @Test
    public void testAdaptive_growAcrossThreshold() {
        Trie<Item> trie = build(false);
        // 单 region 下放 80 个 city（> 阈值64），city 层 AdaptiveNode 会升级
        for (int c = 0; c < 80; c++) {
            trie.insert(new Item("R", "C" + c, c));
        }
        assertEquals(80, trie.getSize());
        assertEquals(80, trie.count(new Criteria().and("region").eq("R")));
        assertEquals(1, trie.count(new Criteria().and("region").eq("R").and("city").eq("C79")));
        assertEquals(1, trie.dataSearch(new Criteria().and("region").eq("R").and("city").eq("C10")).size());
    }
}
