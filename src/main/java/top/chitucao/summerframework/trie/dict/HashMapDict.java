package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于两个HashMap的字典实现
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: HashMapDict.java, v 0.1 2024-09-05 15:35 chitucao Exp $$
 */
public class HashMapDict<R> implements Dict<R> {

    protected Map<Number, R> dict;

    protected Map<R, Number> indexer;

    public HashMapDict() {
        this.dict = new ConcurrentHashMap<>();
        this.indexer = new ConcurrentHashMap<>();
    }

    /**
     * 适用于字典大小确定的情况下，饥饿初始化，避免扩容，同时提高hash的效率
     */
    public HashMapDict(Integer capacity) {
        this.dict = new ConcurrentHashMap<>((int) (capacity / 0.75f) + 1, 1);
        this.indexer = new ConcurrentHashMap<>((int) (capacity / 0.75f) + 1, 1);
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
    }

    @Override
    public void putDictObj(Number dictKey, Object dictValue) {
        @SuppressWarnings("unchecked")
        R val = (R) dictValue;
        indexer.put(val, dictKey);
        dict.put(dictKey, val);
    }

    @Override
    public boolean containsDictKey(Number dictKey) {
        return dict.containsKey(dictKey);
    }

    @Override
    public boolean containsDictValue(R r) {
        return indexer.containsKey(r);
    }
}