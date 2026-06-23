package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 基于两个HashMap的字典实现
 *
 * @param <R>   字段值
 * @param <K>   树节点值
 */
public class HashMapDict<R, K> implements Dict<R, K> {

    protected Map<K, R>       dict;

    protected Map<R, K>       indexer;

    /** 计数器：仅在 removeDictIfNonCount=true 时才分配并维护；否则为 null（计数不再被使用，size 由节点 subtreeSize 维护），省一份 map */
    protected Map<K, Integer> counter;

    /** 在字典值数量为0的时候是否删除该字典，如果是定时重建，并且快速删除，则没必要开启这个选项，开启了可以稍微节省点空间 */
    protected boolean         removeDictIfNonCount;

    public HashMapDict() {
        this(false);
    }

    public HashMapDict(boolean removeDictIfNonCount) {
        this.dict = new ConcurrentHashMap<>();
        this.indexer = new ConcurrentHashMap<>();
        this.removeDictIfNonCount = removeDictIfNonCount;
        this.counter = removeDictIfNonCount ? new ConcurrentHashMap<>() : null;
    }

    /**
     * 适用于字典大小确定的情况下，饥饿初始化，避免扩容，同时提高hash的效率
     */
    public HashMapDict(Integer capacity) {
        this(capacity, false);
    }

    public HashMapDict(Integer capacity, boolean removeDictIfNonCount) {
        int initCap = (int) (capacity / 0.75f) + 1;
        this.dict = new ConcurrentHashMap<>(initCap, 1);
        this.indexer = new ConcurrentHashMap<>(initCap, 1);
        this.removeDictIfNonCount = removeDictIfNonCount;
        this.counter = removeDictIfNonCount ? new ConcurrentHashMap<>(initCap, 1) : null;
    }

    /**
     * 字典数量
     *
     * @return 字典数量
     */
    @Override
    public int size() {
        return dict.size();
    }

    /**
     * 字典键值表
     *
     * @return 字典键值表
     */
    @Override
    public Map<K, R> dict() {
        return dict;
    }

    /**
     * 所有字段值
     *
     * @return 所有字段值
     */
    @Override
    public Set<R> fieldValues() {
        return indexer.keySet();
    }

    /**
     * 新增一个字典
     *
     * @param nodeKey       树节点值
     * @param fieldValue    字段值
     */
    @Override
    public void put(K nodeKey, R fieldValue) {
        indexer.put(fieldValue, nodeKey);
        dict.put(nodeKey, fieldValue);
        // 仅在启用 removeDictIfNonCount 时才维护计数（原子自增）；默认关闭时 counter 为 null，省去一份 map
        if (counter != null) {
            counter.merge(nodeKey, 1, Integer::sum);
        }
    }

    /**
     * 原子地获取或创建字段值对应的nodeKey
     * -1.indexer.computeIfAbsent 保证并发下同一字段值只分配一个nodeKey，并在首次出现时同步登记 dict；
     * -2.计数(counter)仍由 {@link #put} 在每次插入时累加，因此这里不触碰 counter；
     *
     * @param fieldValue    字段值
     * @param keySupplier   新key生成器（仅首次出现该字段值时调用）
     * @return              树节点key
     */
    @Override
    public K getOrCreateNodeKey(R fieldValue, Supplier<K> keySupplier) {
        return indexer.computeIfAbsent(fieldValue, f -> {
            K nodeKey = keySupplier.get();
            dict.put(nodeKey, f);
            return nodeKey;
        });
    }

    /**
     * 根据字段值查询对应的树节点值
     *
     * @param fieldValue    字段值
     * @return              树节点值
     */
    @Override
    public K getNodeKey(R fieldValue) {
        return indexer.get(fieldValue);
    }

    /**
     * 根据树节点值查询字段值
     *
     * @param nodeKey   树节点值
     * @return          字段值
     */
    @Override
    public R getFieldValue(K nodeKey) {
        return dict.get(nodeKey);
    }

    /**
     * 是否包含某个字段值
     *
     * @param fieldValue    字段值
     * @return              是否包含某个字段值
     */
    @Override
    public boolean containsFieldValue(R fieldValue) {
        return indexer.containsKey(fieldValue);
    }

    /**
     * 减少一个字典key的数量
     *
     * @param nodeKey   树节点值
     * @param count     减少的数量
     */
    @Override
    public void decrNodeKeyCount(K nodeKey, int count) {
        // 未启用 removeDictIfNonCount 时不维护计数，直接返回（size 由节点 subtreeSize 维护，字典清理走 removeNodeKey）
        if (counter == null) {
            return;
        }
        // 原子读改写，避免并发下计数丢更新；key不存在时computeIfPresent直接返回null不做处理
        Integer nodeKeyCount = counter.computeIfPresent(nodeKey, (k, v) -> v - count);
        if (removeDictIfNonCount && Objects.nonNull(nodeKeyCount) && nodeKeyCount <= 0) {
            removeNodeKey(nodeKey);
        }
    }

    /**
     * 删除一个字典key
     *
     * @param nodeKey   字典key
     */
    @Override
    public void removeNodeKey(K nodeKey) {
        R fieldValue = dict.remove(nodeKey);
        if (Objects.nonNull(fieldValue)) {
            indexer.remove(fieldValue);
        }
        if (counter != null) {
            counter.remove(nodeKey);
        }
    }
}