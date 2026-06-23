package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
     * 原子地获取或创建字段值对应的树节点key
     * -1.若字段值已存在，返回已有的nodeKey；否则用keySupplier生成一个新key并登记后返回；
     * -2.并发下保证同一个字段值只会分配一个nodeKey（适用于{@link top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty}的自增key分配）；
     *
     * @param fieldValue    字段值
     * @param keySupplier   新key生成器（仅在该字段值首次出现时调用）
     * @return              树节点key
     */
    K getOrCreateNodeKey(R fieldValue, Supplier<K> keySupplier);

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