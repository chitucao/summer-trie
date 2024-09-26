package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Setter;

/**
 * 基于两个HashMap的字典实现
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: HashMapDict.java, v 0.1 2024-09-05 15:35 chitucao Exp $$
 */
public class HashMapDict<R> implements Dict<R> {

    protected Map<Number, R>       dict;

    protected Map<R, Number>       indexer;

    protected Map<Number, Integer> counter;

    /** 在字典值数量为0的时候是否删除该字典，如果是定时重建，并且快速删除，则没必要开启这个选项，开启了可以稍微节省点空间 */
    @Setter
    protected boolean              removeDictIfNonCount;

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

    @Override
    public int getSize() {
        return dict.size();
    }

    @Override
    public Set<R> dictValues() {
        return indexer.keySet();
    }

    @Override
    public Map<Number, R> dictAll() {
        return dict;
    }

    @Override
    public Number getDictKey(R r) {
        return indexer.get(r);
    }

    @Override
    public R getDictValue(Number dictKey) {
        return dict.get(dictKey);
    }

    @Override
    public void putDict(Number dictKey, R dictValue) {
        indexer.put(dictValue, dictKey);
        dict.put(dictKey, dictValue);
        counter.put(dictKey, counter.getOrDefault(dictKey, 0) + 1);
    }

    @Override
    public void putDictObj(Number dictKey, Object dictValue) {
        @SuppressWarnings("unchecked")
        R val = (R) dictValue;
        indexer.put(val, dictKey);
        dict.put(dictKey, val);
        counter.put(dictKey, counter.getOrDefault(dictKey, 0) + 1);
    }

    @Override
    public boolean containsDictKey(Number dictKey) {
        return dict.containsKey(dictKey);
    }

    @Override
    public boolean containsDictValue(R r) {
        return indexer.containsKey(r);
    }

    @Override
    public void decrDictCount(Number dictKey, int count) {
        counter.put(dictKey, counter.get(dictKey) - count);
        if (removeDictIfNonCount && Objects.equals(counter.get(dictKey), 0)) {
            removeDictKey(dictKey);
        }
    }

    @Override
    public void removeDictKey(Number dictKey) {
        indexer.remove(dict.get(dictKey));
        dict.remove(dictKey);
        counter.remove(dictKey);
    }
}