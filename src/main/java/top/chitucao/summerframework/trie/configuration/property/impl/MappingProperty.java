package top.chitucao.summerframework.trie.configuration.property.impl;

import java.util.function.Function;

import top.chitucao.summerframework.trie.configuration.property.AbstractProperty;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 自定义映射关系的树节点属性
 * -1.这里的自定义指的是可以手动指定字典key和字段值的映射关系，合理的映射关系将支持范围和比较查询；
 * -2.比如金额，数字，日期等可以转换成一个对应的的数字；
 * -3.范围和比较查询时推荐和TreeMapNode结合使用，查询时间复杂度会降低到logn；
 *
 * @author chitucao
 */
public class MappingProperty<T, R, K> extends AbstractProperty<T, R, K> {

    /** 指定如何将字段值映射成节点值 */
    private Function<R, K> field2NodeKeyMapper;

    /** 指定如何将节点值映射成字段值 */
    private Function<K, R> nodeKey2FieldMapper;

    public MappingProperty(String name) {
        super(name, NodeType.HASH_MAP.name(), false);
    }

    public MappingProperty(String name, String nodeType) {
        super(name, nodeType, false);
    }

    public MappingProperty(String name, NodeType nodeType) {
        super(name, nodeType.name(), false);
    }

    @Override
    public K mappingNodeKey(R field) {
        return field2NodeKeyMapper.apply(field);
    }

    @Override
    public K mappingOrCreateNodeKey(R field) {
        return field2NodeKeyMapper.apply(field);
    }

    @Override
    public R nodeKey2FieldValue(K nodeKey) {
        return nodeKey2FieldMapper.apply(nodeKey);
    }

    @Override
    public Dict<R, K> getDict() {
        throw new IllegalStateException("MappingProperty is not a dictProperty, propertyName: " + name);
    }

    /**
     * 设置字段值映射成树节点值的映射函数
     * @param field2NodeKeyMapper   字段值映射成树节点值的映射函数
     */
    public void setField2NodeKeyMapper(Function<R, K> field2NodeKeyMapper) {
        this.field2NodeKeyMapper = field2NodeKeyMapper;
    }

    /**
     * 设置树节点值映射成字段值的映射函数
     * @param nodeKey2FieldMapper   树节点值映射成字段值的映射函数
     */
    public void setNodeKey2FieldMapper(Function<K, R> nodeKey2FieldMapper) {
        this.nodeKey2FieldMapper = nodeKey2FieldMapper;
    }
}