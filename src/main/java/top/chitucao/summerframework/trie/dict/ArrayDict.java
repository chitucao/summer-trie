package top.chitucao.summerframework.trie.dict;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 面向"密集自增整数 key"的字典实现，专为 {@link top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty} 优化。
 * <p>
 * AutoMapping 的 nodeKey 是 {@code NumberAdder} 产生的密集自增 id（1..N）。本实现用一条<b>可增长数组</b>按 {@code id-1}
 * 下标存 {@code id -> fieldValue}，相比 {@code HashMap<Long, R>} 省掉每条 {@code HashMap$Node}（约 32B）与装箱的 Long key，
 * 仅保留 {@code indexer}（fieldValue -> id）用于插入去重与按值反查。
 * <p>
 * <b>适用场景：构建期单线程写 + 只读/定期重建（与 AdaptiveNode 一致）。</b>
 * 注意：删除采用 tombstone（置 null），id 不复用；在"长期高频 erase+insert 增量更新"下数组会持续增长，
 * 这种场景请用 {@link HashMapDict}。
 *
 * @param <R> 字段值类型
 * @author 资深全栈架构师
 */
public class ArrayDict<R> implements Dict<R, Number> {

    /** id-1 -> fieldValue（懒分配、可增长） */
    private Object[]               values;

    /** 存活条目数 */
    private int                    count;

    /** fieldValue -> id，用于插入去重与反查 */
    private final Map<R, Number>   indexer = new ConcurrentHashMap<>();

    private static int index(Number nodeKey) {
        return nodeKey.intValue() - 1;
    }

    private void ensureCapacity(int index) {
        if (values == null) {
            values = new Object[Math.max(16, index + 1)];
        } else if (index >= values.length) {
            int newCap = values.length;
            while (newCap <= index) {
                newCap <<= 1;
            }
            values = Arrays.copyOf(values, newCap);
        }
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public Set<R> fieldValues() {
        return indexer.keySet();
    }

    @Override
    public void put(Number nodeKey, R fieldValue) {
        int i = index(nodeKey);
        if (i < 0) {
            throw new IllegalArgumentException("ArrayDict requires dense positive ids, got: " + nodeKey);
        }
        ensureCapacity(i);
        if (values[i] == null) {
            count++;
        }
        values[i] = fieldValue;
        indexer.put(fieldValue, nodeKey);
    }

    @Override
    public Number getOrCreateNodeKey(R fieldValue, Supplier<Number> keySupplier) {
        return indexer.computeIfAbsent(fieldValue, f -> {
            Number nodeKey = keySupplier.get();
            int i = index(nodeKey);
            if (i < 0) {
                throw new IllegalArgumentException("ArrayDict requires dense positive ids, got: " + nodeKey);
            }
            ensureCapacity(i);
            if (values[i] == null) {
                count++;
            }
            values[i] = f;
            return nodeKey;
        });
    }

    @Override
    public Number getNodeKey(R fieldValue) {
        return indexer.get(fieldValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R getFieldValue(Number nodeKey) {
        int i = index(nodeKey);
        if (values == null || i < 0 || i >= values.length) {
            return null;
        }
        return (R) values[i];
    }

    @Override
    public boolean containsFieldValue(R fieldValue) {
        return indexer.containsKey(fieldValue);
    }

    @Override
    public void decrNodeKeyCount(Number nodeKey, int count) {
        // ArrayDict 不维护计数（size 由节点 subtreeSize 维护），no-op
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeNodeKey(Number nodeKey) {
        int i = index(nodeKey);
        if (values == null || i < 0 || i >= values.length || values[i] == null) {
            return;
        }
        R fieldValue = (R) values[i];
        values[i] = null;
        count--;
        indexer.remove(fieldValue);
    }

    /**
     * 字典键值视图（id -> fieldValue）。由 indexer 反转得到，key 类型与 AutoMapping 实际 key 一致；仅 dictValues 等少量场景调用。
     */
    @Override
    public Map<Number, R> dict() {
        return new AbstractMap<Number, R>() {
            @Override
            public R get(Object key) {
                return key instanceof Number ? getFieldValue((Number) key) : null;
            }

            @Override
            public int size() {
                return count;
            }

            @Override
            public Set<Entry<Number, R>> entrySet() {
                return new AbstractSet<Entry<Number, R>>() {
                    @Override
                    public int size() {
                        return indexer.size();
                    }

                    @Override
                    public Iterator<Entry<Number, R>> iterator() {
                        Iterator<Entry<R, Number>> it = indexer.entrySet().iterator();
                        return new Iterator<Entry<Number, R>>() {
                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<Number, R> next() {
                                if (!it.hasNext()) {
                                    throw new NoSuchElementException();
                                }
                                Entry<R, Number> e = it.next();
                                return new SimpleImmutableEntry<>(e.getValue(), e.getKey());
                            }
                        };
                    }
                };
            }
        };
    }
}
