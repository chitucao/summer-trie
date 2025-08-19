package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 节点属性
 * @param <T>   对象实体
 * @param <R>   字段值
 * @param <K>   树节点关键字
 *
 * @author chitucao
 */
public interface Property<T, R, K> {
    /**
     * 名称，全局唯一
     * 
     * @return  名称
     */
    String name();

    /**
     * 节点类型
     *
     * @return  节点类型
     */
    NodeType nodeType();

    /**
     * 层级
     * 
     * @return 层级
     */
    int level();

    /**
     * 设置层级
     *
     * @param level 层级
     */
    void setLevel(int level);

    /**
     * 从对象实体获取字段值
     * -1.通常情况是指定对象的原始字段，也可以返回合并或者处理过的字段，比如两个日期组成一个日期范围，两个价格组成一个价格区间
     *
     * @param object    对象实体
     * @return          字段值
     */
    R mappingFieldValue(T object);

    /**
     * 将字段值转换为树节点关键字
     *
     * @param field     字段值
     * @return          树节点关键字
     */
    K mappingNodeKey(R field);

    /**
     * 将字段值转换为树节点关键字
     * -1.如果当前字典没有该关键字，则新增一个返回，适用于{@link AutoMappingProperty}
     *
     * @param field     字段值
     * @return          树节点关键字
     */
    K mappingOrCreateNodeKey(R field);

    /**
     * 将树节点关键字转换为字段值
     *
     * @param nodeKey       树节点关键字
     * @return              字段值
     */
    R nodeKey2FieldValue(K nodeKey);

    /**
     * 是否是字典属性
     *
     * @return  是否是字典属性
     */
    boolean isDictProperty();

    /**
     * 获取字典
     * -1.仅节点是字典属性的节点时可用
     *
     * @return  字典
     */
    Dict<R, K> getDict();
}