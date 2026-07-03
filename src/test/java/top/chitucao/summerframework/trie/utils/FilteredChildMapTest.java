package top.chitucao.summerframework.trie.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Test;

import top.chitucao.summerframework.trie.node.HashMapNode;
import top.chitucao.summerframework.trie.node.Node;

/**
 * FilteredChildMap 契约测试：
 * 1. size / isEmpty / get / containsKey 在空 source / 无匹配 / 部分匹配下行为正确；
 * 2. 三个 view（entrySet / values / keySet）返回懒遍历，结果与 predicate 一致；
 * 3. 所有写方法抛 UnsupportedOperationException；
 * 4. 迭代器严格懒求值（不一次性物化）。
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FilteredChildMapTest {

    private Map<Integer, Node> sampleSource() {
        Map<Integer, Node> m = new LinkedHashMap<>();
        m.put(1, new HashMapNode<>());
        m.put(2, new HashMapNode<>());
        m.put(3, new HashMapNode<>());
        m.put(4, new HashMapNode<>());
        m.put(5, new HashMapNode<>());
        return m;
    }

    @Test
    public void testBasicFilter() {
        Map source = sampleSource();
        Predicate<Map.Entry> odd = e -> ((Integer) e.getKey()) % 2 == 1;
        FilteredChildMap view = new FilteredChildMap(source, odd);
        assertEquals(3, view.size());
        assertFalse(view.isEmpty());
        Set keys = new HashSet<>(view.keySet());
        assertEquals(new HashSet<>(Arrays.asList(1, 3, 5)), keys);
    }

    @Test
    public void testEmptyWhenNoMatch() {
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> false);
        assertTrue(view.isEmpty());
        assertEquals(0, view.size());
        assertFalse(view.keySet().iterator().hasNext());
        assertFalse(view.values().iterator().hasNext());
        assertFalse(view.entrySet().iterator().hasNext());
    }

    @Test
    public void testGetAndContainsKey() {
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> ((Integer) e.getKey()) >= 3);
        assertNull(view.get(1));
        assertNull(view.get(2));
        assertTrue(view.containsKey(3));
        assertTrue(view.containsKey(5));
        assertFalse(view.containsKey(1));
        assertFalse(view.containsKey(999));
    }

    @Test
    public void testIteratorOrderPreservesSource() {
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> ((Integer) e.getKey()) != 3);
        Iterator<Map.Entry> it = view.entrySet().iterator();
        assertEquals(Integer.valueOf(1), it.next().getKey());
        assertEquals(Integer.valueOf(2), it.next().getKey());
        assertEquals(Integer.valueOf(4), it.next().getKey());
        assertEquals(Integer.valueOf(5), it.next().getKey());
        assertFalse(it.hasNext());
    }

    @Test
    public void testSizeCachedAndConsistentWithIteration() {
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> ((Integer) e.getKey()) <= 2);
        int first = view.size();
        int second = view.size();
        assertEquals(2, first);
        assertEquals(first, second);
        int iterCount = 0;
        for (Object ignored : view.entrySet()) {
            iterCount++;
        }
        assertEquals(first, iterCount);
    }

    @Test
    public void testWriteMethodsUnsupported() {
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> true);
        try {
            view.put(99, new HashMapNode<>());
            fail("put should throw");
        } catch (UnsupportedOperationException expected) {
            /* ok */
        }
        try {
            view.remove(1);
            fail("remove should throw");
        } catch (UnsupportedOperationException expected) {
            /* ok */
        }
        try {
            view.clear();
            fail("clear should throw");
        } catch (UnsupportedOperationException expected) {
            /* ok */
        }
        try {
            view.putAll(source);
            fail("putAll should throw");
        } catch (UnsupportedOperationException expected) {
            /* ok */
        }
    }

    @Test
    public void testLazinessAvoidsFullScan() {
        // 用计数 predicate 验证：iterator.hasNext + next 一次只推进到第一个命中，不会一次性物化全部
        int[] evalCount = { 0 };
        Map source = sampleSource();
        FilteredChildMap view = new FilteredChildMap(source, e -> {
            evalCount[0]++;
            return ((Integer) e.getKey()) >= 3;
        });
        Iterator<Map.Entry> it = view.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(3), it.next().getKey());
        // 到此只应该 evaluate 了 1~3 三个元素
        assertEquals(3, evalCount[0]);
    }
}
