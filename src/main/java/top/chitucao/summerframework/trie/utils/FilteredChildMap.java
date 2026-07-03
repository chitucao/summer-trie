package top.chitucao.summerframework.trie.utils;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import top.chitucao.summerframework.trie.node.Node;

/**
 * 只读的懒视图子节点 Map。
 *
 * <p>构造时只持有源 {@code childMap} 的引用和一个过滤谓词；{@link #entrySet()} / {@link #values()} / {@link #keySet()}
 * 都返回懒遍历 view，不会一次性物化。{@link #size()} 和 {@link #isEmpty()} 延迟计算并缓存。</p>
 *
 * <p>用途：替代 {@code BasicOperates} 里 HashMap 分支 O(n) 操作每次 {@code new HashMap()} + for put 的中间结果分配，
 * 把分配量从 O(深度 × 命中数) 降到 O(深度)。</p>
 *
 * <p>本类出于与既有 raw {@code Map<?, Node<?>>} 接口对齐的考虑，内部使用 raw Map，和项目其他代码风格一致；
 * 所有写方法都抛 {@link UnsupportedOperationException}，需要写入的调用方（{@code slice} / {@code remove} / {@code normErase}）需自行快照。</p>
 *
 * @author chitucao
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class FilteredChildMap extends AbstractMap implements Map {

    /** 源子节点 Map，只做只读遍历。 */
    private final Map                        source;

    /** 过滤谓词；返回 true 的 entry 才会出现在本视图中。 */
    private final Predicate<Map.Entry>       predicate;

    /** 惰性缓存的 size；-1 表示未计算。 */
    private int                              cachedSize = -1;

    private transient Set<Map.Entry>         entrySetView;
    private transient Collection<Node>       valuesView;
    private transient Set                    keySetView;

    public FilteredChildMap(Map source, Predicate<Map.Entry> predicate) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (predicate == null) {
            throw new IllegalArgumentException("predicate must not be null");
        }
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public int size() {
        if (cachedSize < 0) {
            int n = 0;
            for (Object obj : source.entrySet()) {
                if (predicate.test((Map.Entry) obj)) {
                    n++;
                }
            }
            cachedSize = n;
        }
        return cachedSize;
    }

    @Override
    public boolean isEmpty() {
        if (cachedSize >= 0) {
            return cachedSize == 0;
        }
        for (Object obj : source.entrySet()) {
            if (predicate.test((Map.Entry) obj)) {
                return false;
            }
        }
        cachedSize = 0;
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        Object val = source.get(key);
        if (val == null) {
            return false;
        }
        return predicate.test(new SimpleImmutableEntry(key, val));
    }

    @Override
    public Object get(Object key) {
        Object val = source.get(key);
        if (val == null) {
            return null;
        }
        return predicate.test(new SimpleImmutableEntry(key, val)) ? val : null;
    }

    @Override
    public Set<Map.Entry> entrySet() {
        Set<Map.Entry> view = entrySetView;
        if (view == null) {
            view = new AbstractSet<Map.Entry>() {
                @Override
                public Iterator<Map.Entry> iterator() {
                    return new FilteringIterator(source.entrySet().iterator(), predicate);
                }

                @Override
                public int size() {
                    return FilteredChildMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return FilteredChildMap.this.isEmpty();
                }
            };
            entrySetView = view;
        }
        return view;
    }

    @Override
    public Collection<Node> values() {
        Collection<Node> view = valuesView;
        if (view == null) {
            view = new AbstractCollection<Node>() {
                @Override
                public Iterator<Node> iterator() {
                    final Iterator<Map.Entry> src = new FilteringIterator(source.entrySet().iterator(), predicate);
                    return new Iterator<Node>() {
                        @Override
                        public boolean hasNext() {
                            return src.hasNext();
                        }

                        @Override
                        public Node next() {
                            return (Node) src.next().getValue();
                        }
                    };
                }

                @Override
                public int size() {
                    return FilteredChildMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return FilteredChildMap.this.isEmpty();
                }
            };
            valuesView = view;
        }
        return view;
    }

    @Override
    public Set keySet() {
        Set view = keySetView;
        if (view == null) {
            view = new AbstractSet() {
                @Override
                public Iterator iterator() {
                    final Iterator<Map.Entry> src = new FilteringIterator(source.entrySet().iterator(), predicate);
                    return new Iterator() {
                        @Override
                        public boolean hasNext() {
                            return src.hasNext();
                        }

                        @Override
                        public Object next() {
                            return src.next().getKey();
                        }
                    };
                }

                @Override
                public int size() {
                    return FilteredChildMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return FilteredChildMap.this.isEmpty();
                }

                @Override
                public boolean contains(Object o) {
                    return FilteredChildMap.this.containsKey(o);
                }
            };
            keySetView = view;
        }
        return view;
    }

    // ---- 写操作一律拒绝，强制调用方先做快照 ----

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException("FilteredChildMap is read-only");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("FilteredChildMap is read-only");
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException("FilteredChildMap is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("FilteredChildMap is read-only");
    }

    /**
     * 懒过滤的 Entry 迭代器：只在 {@link #hasNext()} 时向前推进源迭代器直到找到第一个满足谓词的 entry。
     */
    private static final class FilteringIterator implements Iterator<Map.Entry> {

        private final Iterator<Map.Entry> src;

        private final Predicate<Map.Entry> filter;

        private Map.Entry                 nextEntry;

        private boolean                   hasNextComputed;

        FilteringIterator(Iterator src, Predicate<Map.Entry> filter) {
            this.src = (Iterator<Map.Entry>) src;
            this.filter = filter;
        }

        @Override
        public boolean hasNext() {
            if (hasNextComputed) {
                return nextEntry != null;
            }
            while (src.hasNext()) {
                Map.Entry e = src.next();
                if (filter.test(e)) {
                    nextEntry = e;
                    hasNextComputed = true;
                    return true;
                }
            }
            nextEntry = null;
            hasNextComputed = true;
            return false;
        }

        @Override
        public Map.Entry next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Map.Entry e = nextEntry;
            nextEntry = null;
            hasNextComputed = false;
            return e;
        }
    }
}
