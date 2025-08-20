package top.chitucao.summerframework.trie.configuration.property.impl;

import java.util.function.Function;

import top.chitucao.summerframework.trie.configuration.property.AbstractDictProperty;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 单向建立映射关系的节点属性
 * -1.这里只需要指定字段值如何映射成节点值就行了，不需要指定节点值如何映射成字段值
 *
 * @author chitucao
 */
public class OneWayMappingProperty<T, R, K> extends AbstractDictProperty<T, R, K> {

    /** 指定如何将字段值映射成节点值 */
    private Function<R, K> field2NodeKeyMapper;

    public OneWayMappingProperty(String name) {
        super(name);
    }

    public OneWayMappingProperty(String name, NodeType nodeType) {
        super(name, nodeType);
    }

    public OneWayMappingProperty(String name, NodeType nodeType, Dict<R, K> dict) {
        super(name, nodeType, dict);
    }

    @Override
    public K mappingNodeKey(R field) {
        return field2NodeKeyMapper.apply(field);
    }

    @Override
    public K mappingOrCreateNodeKey(R field) {
        return field2NodeKeyMapper.apply(field);
    }

    /**
     * 设置字段值映射成树节点值的映射函数
     * @param field2NodeKeyMapper   字段值映射成树节点值的映射函数
     */
    public void setField2NodeKeyMapper(Function<R, K> field2NodeKeyMapper) {
        this.field2NodeKeyMapper = field2NodeKeyMapper;
    }
}