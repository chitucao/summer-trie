package top.chitucao.summerframework.trie.node;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

/**
 * 自适应哈希节点（借鉴 ART 的"按扇出自适应"思想，适配到 summer-trie 的"一层一字段、按字段值分叉"结构）。
 * <p>
 * - 低扇出（{@code <= threshold}）：用两条并行数组 {@code keys[]/children[]} 紧凑存储，线性查找；
 *   省掉 HashMap 的桶数组与每条 {@code HashMap$Node}（每条约 32B）的对象开销。
 * - 高扇出（{@code > threshold}）：自动升级为 {@link java.util.HashMap}，避免线性查找退化。
 * - {@link #children()} 返回一个轻量 {@code Map} 视图，使现有查询算子（面向 Map）零改动即可工作；
 *   节省的是"常驻内存"（节点只存两条小数组），视图仅在查询时临时创建。
 * <p>
 * <b>线程安全性：与 {@link HashMapNode} 一致——非线程安全，适用于"构建期单线程写 + 运行期只读"。</b>
 * 需要并发写请用 {@code CONCURRENT_HASH_MAP}。
 *
 * @param <K> 节点值类型
 * @author 资深全栈架构师
 */
public class AdaptiveNode<K> implements Node<K> {

    /** 数组转 HashMap 的扇出阈值（内存 vs 线性查找速度的权衡点） */
    static final int                                                  DEFAULT_THRESHOLD = 64;

    private final int                                                 threshold;

    /** 低扇出存储：并行数组（懒分配） */
    private Object[]                                                   keys;
    private Node<K>[]                                                  vals;
    private int                                                       size;

    /** 高扇出存储：升级后非空 */
    private Map<K, Node<K>>                                           map;

    /** 子树记录数（与其它节点一致，原子维护） */
    private volatile int                                              subtreeSize;

    private static final AtomicIntegerFieldUpdater<AdaptiveNode>     SUBTREE_SIZE_UPDATER = AtomicIntegerFieldUpdater
        .newUpdater(AdaptiveNode.class, "subtreeSize");

    public AdaptiveNode() {
        this(DEFAULT_THRESHOLD);
    }

    public AdaptiveNode(int threshold) {
        this.threshold = threshold;
    }

    private int indexOf(Object key) {
        Object[] ks = keys;
        int n = size;
        for (int i = 0; i < n; i++) {
            if (Objects.equals(ks[i], key)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity() {
        if (keys == null) {
            keys = new Object[4];
            vals = (Node<K>[]) new Node[4];
        } else if (size == keys.length) {
            int newCap = Math.min(keys.length * 2, threshold);
            keys = Arrays.copyOf(keys, newCap);
            vals = Arrays.copyOf(vals, newCap);
        }
    }

    @SuppressWarnings("unchecked")
    private void upgradeToMap() {
        Map<K, Node<K>> m = new java.util.HashMap<>((int) (size / 0.75f) + 1);
        for (int i = 0; i < size; i++) {
            m.put((K) keys[i], vals[i]);
        }
        this.map = m;
        this.keys = null;
        this.vals = null;
        this.size = 0;
    }

    @Override
    public Node<K> child(K key) {
        if (map != null) {
            return map.get(key);
        }
        int i = indexOf(key);
        return i >= 0 ? vals[i] : null;
    }

    @Override
    public Node<K> putChild(K key, Node<K> child) {
        if (map != null) {
            return map.computeIfAbsent(key, k -> child);
        }
        int i = indexOf(key);
        if (i >= 0) {
            return vals[i];
        }
        if (size >= threshold) {
            upgradeToMap();
            return map.computeIfAbsent(key, k -> child);
        }
        ensureCapacity();
        keys[size] = key;
        vals[size] = child;
        size++;
        return child;
    }

    @Override
    public Node<K> putChild(K key, Supplier<Node<K>> childSupplier) {
        if (map != null) {
            return map.computeIfAbsent(key, k -> childSupplier.get());
        }
        int i = indexOf(key);
        if (i >= 0) {
            return vals[i];
        }
        if (size >= threshold) {
            upgradeToMap();
            return map.computeIfAbsent(key, k -> childSupplier.get());
        }
        ensureCapacity();
        Node<K> child = childSupplier.get();
        keys[size] = key;
        vals[size] = child;
        size++;
        return child;
    }

    @Override
    public void removeChild(K key) {
        if (map != null) {
            map.remove(key);
            return;
        }
        int i = indexOf(key);
        if (i < 0) {
            return;
        }
        int last = size - 1;
        System.arraycopy(keys, i + 1, keys, i, last - i);
        System.arraycopy(vals, i + 1, vals, i, last - i);
        keys[last] = null;
        vals[last] = null;
        size--;
    }

    @Override
    public int childSize() {
        return map != null ? map.size() : size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<K> childKeySet() {
        if (map != null) {
            return map.keySet();
        }
        Set<K> set = new LinkedHashSet<>((int) (size / 0.75f) + 1);
        for (int i = 0; i < size; i++) {
            set.add((K) keys[i]);
        }
        return set;
    }

    @Override
    public Map<K, Node<K>> children() {
        if (map != null) {
            return map;
        }
        return new ArrayView();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setChildren(Map<K, Node<K>> children) {
        this.map = null;
        this.keys = null;
        this.vals = null;
        this.size = 0;
        if (children == null || children.isEmpty()) {
            return;
        }
        if (children.size() > threshold) {
            this.map = new java.util.HashMap<>(children);
            return;
        }
        int cap = Math.max(4, children.size());
        this.keys = new Object[cap];
        this.vals = (Node<K>[]) new Node[cap];
        for (Map.Entry<K, Node<K>> e : children.entrySet()) {
            keys[size] = e.getKey();
            vals[size] = e.getValue();
            size++;
        }
    }

    @Override
    public int subtreeSize() {
        return subtreeSize;
    }

    @Override
    public void addSubtreeSize(int delta) {
        SUBTREE_SIZE_UPDATER.addAndGet(this, delta);
    }

    /**
     * 数组存储之上的轻量、可写 Map 视图：读委托到数组/节点，写(remove / iterator.remove)回写到节点，
     * 使现有面向 Map 的查询/删除算子无需感知底层是数组。
     */
    private final class ArrayView extends AbstractMap<K, Node<K>> {

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<K> get(Object key) {
            return child((K) key);
        }

        @Override
        public boolean containsKey(Object key) {
            return indexOf(key) >= 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<K> remove(Object key) {
            int i = indexOf(key);
            if (i < 0) {
                return null;
            }
            Node<K> old = vals[i];
            removeChild((K) key);
            return old;
        }

        @Override
        public Set<Entry<K, Node<K>>> entrySet() {
            return new AbstractSet<Entry<K, Node<K>>>() {
                @Override
                public int size() {
                    return size;
                }

                @Override
                public Iterator<Entry<K, Node<K>>> iterator() {
                    return new Iterator<Entry<K, Node<K>>>() {
                        private int cursor  = 0;
                        private int lastRet = -1;

                        @Override
                        public boolean hasNext() {
                            return cursor < size;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public Entry<K, Node<K>> next() {
                            if (cursor >= size) {
                                throw new NoSuchElementException();
                            }
                            lastRet = cursor;
                            Entry<K, Node<K>> e = new SimpleImmutableEntry<>((K) keys[cursor], vals[cursor]);
                            cursor++;
                            return e;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public void remove() {
                            if (lastRet < 0) {
                                throw new IllegalStateException();
                            }
                            removeChild((K) keys[lastRet]);
                            // 数组左移后当前位置应重新检查
                            cursor = lastRet;
                            lastRet = -1;
                        }
                    };
                }
            };
        }
    }
}
