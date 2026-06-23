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
        super(name, NodeType.HASH_MAP.name());
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public AutoMappingProperty(String name, DictKeyType dictKeyType, String nodeType) {
        super(name, nodeType);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public AutoMappingProperty(String name, DictKeyType dictKeyType, String nodeType, Dict<R, Number> dict) {
        super(name, nodeType, dict);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public AutoMappingProperty(String name, DictKeyType dictKeyType, NodeType nodeType) {
        super(name, nodeType.name());
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public AutoMappingProperty(String name, DictKeyType dictKeyType, NodeType nodeType, Dict<R, Number> dict) {
        super(name, nodeType.name(), dict);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    @Override
    public Number mappingOrCreateNodeKey(R field) {
        // 原子地“获取或创建”自增key：杜绝原先 containsFieldValue 后再 nextKey 的 check-then-act 竞态
        // （并发下同一字段值会被分配两个不同的key，导致 indexer/dict/counter 错乱、按值查询/删除查不全）
        return dict.getOrCreateNodeKey(field, dictKeyAdder::nextKey);
    }

}