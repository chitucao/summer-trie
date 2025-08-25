package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于两个HashMap的字典实现
 *
 * @param <R>   字段值
 * @param <K>   树节点值
 */
public class HashMapDict<R, K> implements Dict<R, K> {

    protected Map<K, R>       dict;

    protected Map<R, K>       indexer;

    protected Map<K, Integer> counter;

    /** 在字典值数量为0的时候是否删除该字典，如果是定时重建，并且快速删除，则没必要开启这个选项，开启了可以稍微节省点空间 */
    protected boolean         removeDictIfNonCount;

    public HashMapDict() {
        this.dict = new ConcurrentHashMap<>();
        this.indexer = new ConcurrentHashMap<>();
        this.counter = new ConcurrentHashMap<>();
        this.removeDictIfNonCount = false;
    }

    /**
     * 适用于字典大小确定的情况下，饥饿初始化，避免扩容，同时提高hash的效率
     */
    public HashMapDict(Integer capacity) {
        this.dict = new ConcurrentHashMap<>((int) (capacity / 0.75f) + 1, 1);
        this.indexer = new ConcurrentHashMap<>((int) (capacity / 0.75f) + 1, 1);
        this.counter = new ConcurrentHashMap<>((int) (capacity / 0.75f) + 1, 1);
        this.removeDictIfNonCount = false;
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
    public Set<R> fieldValues() {
        return indexer.keySet();
    }

    /**
     * 新增一个字典
     *
     * @param nodeKey       树节点值
     * @param fieldValue    字段值
     */
    public void put(K nodeKey, R fieldValue) {
        indexer.put(fieldValue, nodeKey);
        dict.put(nodeKey, fieldValue);
        counter.put(nodeKey, counter.getOrDefault(nodeKey, 0) + 1);
    }

    /**
     * 根据字段值查询对应的树节点值
     *
     * @param fieldValue    字段值
     * @return              树节点值
     */
    public K getNodeKey(R fieldValue) {
        return indexer.get(fieldValue);
    }

    /**
     * 根据树节点值查询字段值
     *
     * @param nodeKey   树节点值
     * @return          字段值
     */
    public R getFieldValue(K nodeKey) {
        return dict.get(nodeKey);
    }

    /**
     * 是否包含某个字段值
     *
     * @param fieldValue    字段值
     * @return              是否包含某个字段值
     */
    public boolean containsFieldValue(R fieldValue) {
        return indexer.containsKey(fieldValue);
    }

    /**
     * 减少一个字典key的数量
     *
     * @param nodeKey   树节点值
     * @param count     减少的数量
     */
    public void decrNodeKeyCount(K nodeKey, int count) {
        Integer nodeKeyCount = counter.get(nodeKey);
        if (Objects.isNull(nodeKeyCount)) {
            return;
        }
        nodeKeyCount -= count;
        counter.put(nodeKey, nodeKeyCount);
        if (removeDictIfNonCount && Objects.equals(nodeKeyCount, 0)) {
            removeNodeKey(nodeKey);
        }
    }

    /**
     * 删除一个字典key
     *
     * @param nodeKey   字典key
     */
    public void removeNodeKey(K nodeKey) {
        R fieldValue = dict.remove(nodeKey);
        if (Objects.nonNull(fieldValue)) {
            indexer.remove(fieldValue);
        }
        counter.remove(nodeKey);
    }
}