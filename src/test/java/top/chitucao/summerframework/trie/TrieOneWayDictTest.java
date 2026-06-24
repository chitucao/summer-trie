package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.OneWayMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * OneWayMapping 关闭 indexer 后的正确性测试：正向(函数)、反向(dict)、dictValues、erase 均不依赖 indexer。
 *
 * @author 资深全栈架构师
 */
public class TrieOneWayDictTest {

    public static class Item {
        private final String region;
        private final long   id;

        public Item(String region, long id) {
            this.region = region;
            this.id = id;
        }

        public String getRegion() {
            return region;
        }

        public long getId() {
            return id;
        }
    }

    private Trie<Item> build(boolean fastErase) {
        Configuration cfg = new Configuration();
        cfg.setUseFastErase(fastErase);

        OriginProperty<Item, String> region = new OriginProperty<>("region", NodeType.HASH_MAP);
        region.setObject2FieldMapper(Item::getRegion);
        cfg.addProperty(region);

        // data 用 OneWayMapping（默认无 indexer）：nodeKey = id（函数），反查用 dict
        OneWayMappingProperty<Item, Item, Long> data = new OneWayMappingProperty<>("data", NodeType.HASH_MAP);
        data.setObject2FieldMapper(Function.identity());
        data.setField2NodeKeyMapper(Item::getId);
        cfg.addProperty(data);

        return new MapTrie<>(cfg);
    }

    @Test
    public void testOneWay_noIndexer_query() {
        Trie<Item> trie = build(false);
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 4; k++) {
                trie.insert(new Item("R" + r, r * 10L + k));
            }
        }
        assertEquals(20, trie.getSize());
        assertEquals(20, trie.count(new Criteria()));
        assertEquals(4, trie.count(new Criteria().and("region").eq("R0")));
        assertEquals(4, trie.dataSearch(new Criteria().and("region").eq("R0")).size());

        // dictValues 走 dict（nodeKey->值），不依赖 indexer
        Set<Item> byId = trie.dictValues("data", 11L); // R1 下 id=11
        assertEquals(1, byId.size());
        assertEquals(11L, byId.iterator().next().getId());
    }

    @Test
    public void testOneWay_noIndexer_erase() {
        Trie<Item> trie = build(false);
        List<Item> items = new ArrayList<>();
        for (int k = 0; k < 10; k++) {
            Item it = new Item("R", k);
            items.add(it);
            trie.insert(it);
        }
        assertEquals(10, trie.getSize());

        // erase(T)：mappingDictKey 走函数(getId)，不依赖 indexer
        trie.erase(items.get(0));
        assertEquals(9, trie.getSize());
        assertEquals(9, trie.dataSearch(new Criteria().and("region").eq("R")).size());

        // erase(criteria)
        trie.erase(new Criteria().and("region").eq("R"));
        assertEquals(0, trie.getSize());
    }
}
