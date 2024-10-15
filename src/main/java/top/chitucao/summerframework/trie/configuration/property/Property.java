package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 节点属性
 * @param <T>   实体
 * @param <R>   字段值（字段值是实际存在节点中的数据）
 *
 * @author chitucao
 */
public interface Property<T, R> {
    /**
     * 属性唯一名称
     * 
     * @return  属性唯一名称
     */
    String name();

    /**
     * 层级
     * 
     * @return 层级
     */
    int level();

    void setLevel(int level);

    /**
     * 节点类型
     * 
     * @return  节点类型
     */
    NodeType nodeType();

    /**
     * 指定将实体转换成字段值的映射关系
     * 这里既可以指定一个具体字段，也可以返回处理后的字段，比如两个日期组成一个日期范围，两个价格组成一个价格区间
     *
     * @param t     实体
     * @return      字段值
     */
    R mappingValue(T t);

    /**
     * 指定将字段值转成成字典key的映射关系
     *
     * @param r     字段值
     * @return      字典key
     */
    Number mappingDictKey(R r);

    /**
     * 根据字段值查询字典key
     * @param r     字段值
     * @return      字典key
     */
    Number getDictKey(R r);

    /**
     * 获取字段字典
     *
     * @return  字段字典
     */
    Dict<R> dict();
}