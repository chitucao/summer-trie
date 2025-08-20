package top.chitucao.summerframework.trie.configuration.property.impl;

import top.chitucao.summerframework.trie.configuration.property.AbstractDictProperty;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.NumberAdder;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 自动建立映射关系的节点属性
 * -将字段值通过自增的方式映射为一个数字，将该数字存在树节点上；
 * -适用于字段值占用空间比较大，重复度比较高，需要进行数据压缩的场景；
 * @author chitucao
 */
public class AutoMappingProperty<T, R> extends AbstractDictProperty<T, R, Number> {

    protected NumberAdder dictKeyAdder;

    public AutoMappingProperty(String name) {
        super(name);
        this.dictKeyAdder = new NumberAdder(DictKeyType.LONG);
    }

    public AutoMappingProperty(String name, DictKeyType dictKeyType) {
        super(name, NodeType.HASH_MAP);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public AutoMappingProperty(String name, Dict<R, Number> dict, DictKeyType dictKeyType) {
        super(name, NodeType.HASH_MAP, dict);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    @Override
    public Number mappingOrCreateNodeKey(R field) {
        if (dict.containsFieldValue(field)) {
            return dict.getNodeKey(field);
        }
        return dictKeyAdder.nextKey();
    }

}