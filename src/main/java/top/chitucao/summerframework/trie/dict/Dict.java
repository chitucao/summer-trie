package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Set;

/**
 * 字段值字典
 * @param <R>   字段值
 * @param <K>   树节点值
 *
 * @author chitucao
 */
public interface Dict<R, K> {

    /**
     * 字典数量
     *
     * @return 字典数量
     */
    int size();

    /**
     * 字典键值表
     *
     * @return 字典键值表
     */
    Map<K, R> dict();

    /**
     * 所有字段值
     *
     * @return 所有字段值
     */
    Set<R> fieldValues();

    /**
     * 新增一个字典
     *
     * @param nodeKey       树节点值
     * @param fieldValue    字段值
     */
    void put(K nodeKey, R fieldValue);

    /**
     * 根据字段值查询对应的树节点值
     *
     * @param fieldValue    字段值
     * @return              树节点值
     */
    K getNodeKey(R fieldValue);

    /**
     * 根据树节点值查询字段值
     *
     * @param nodeKey   树节点值
     * @return          字段值
     */
    R getFieldValue(K nodeKey);

    /**
     * 是否包含某个字段值
     *
     * @param fieldValue    字段值
     * @return              是否包含某个字段值
     */
    boolean containsFieldValue(R fieldValue);

    /**
     * 减少一个字典key对应的子节点数量
     * 
     * @param nodeKey   树节点值
     * @param count     减少的数量
     */
    void decrNodeKeyCount(K nodeKey, int count);

    /**
     * 删除一个字典key
     *
     * @param nodeKey   字典key
     */
    void removeNodeKey(K nodeKey);

}