package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.dict.ArrayDict;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * ArrayDict 正确性测试：AutoMapping 数据字典换成 ArrayDict 后，增删查/count/dataSearch 仍正确。
 *
 * @author 资深全栈架构师
 */
public class TrieArrayDictTest {

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

        // 数据字典用 ArrayDict（opt-in，走现有 Dict 构造函数）
        AutoMappingProperty<Item, Item> data = new AutoMappingProperty<>("data", DictKeyType.LONG, NodeType.HASH_MAP, new ArrayDict<>());
        data.setObject2FieldMapper(Function.identity());
        cfg.addProperty(data);

        return new MapTrie<>(cfg);
    }

    @Test
    public void testArrayDict_crudAndQuery() {
        Trie<Item> trie = build(false);
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 20; k++) {
                trie.insert(new Item("R" + r, r * 100L + k));
            }
        }
        assertEquals(100, trie.getSize());
        assertEquals(100, trie.count(new Criteria()));
        assertEquals(20, trie.count(new Criteria().and("region").eq("R0")));

        List<Item> r0 = trie.dataSearch(new Criteria().and("region").eq("R0"));
        assertEquals(20, r0.size());
        // 通过 ArrayDict 反查回来的对象 id 落在 R0 区间
        for (Item it : r0) {
            assertTrue(it.getId() >= 0 && it.getId() < 100);
            assertEquals("R0", it.getRegion());
        }

        // 全量
        assertEquals(100, trie.dataSearch(new Criteria()).size());
    }

    @Test
    public void testArrayDict_erase() {
        Trie<Item> trie = build(false);
        List<Item> items = new ArrayList<>();
        for (int k = 0; k < 30; k++) {
            Item it = new Item("R", k);
            items.add(it);
            trie.insert(it);
        }
        assertEquals(30, trie.getSize());

        // erase(T) 逐条精确删除：会调用 ArrayDict.removeNodeKey(tombstone) 并清理树节点
        for (Item it : items) {
            trie.erase(it);
        }
        assertEquals(0, trie.getSize());
        assertEquals(0, trie.count(new Criteria()));
        assertEquals(0, trie.dataSearch(new Criteria()).size());

        // 删除后还能继续插入（id 继续自增，不复用 tombstone 槽）
        trie.insert(new Item("R", 999));
        assertEquals(1, trie.getSize());
        assertEquals(1, trie.dataSearch(new Criteria().and("region").eq("R")).size());
    }
}
