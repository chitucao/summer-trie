package top.chitucao.summerframework.trie.configuration.property.impl;

import top.chitucao.summerframework.trie.configuration.property.AbstractProperty;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 节点值和字段值保持一致的节点属性
 * -1.树节点值存的就是字段值本身
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: OriginProperty.java, v 0.1 2025-08-19 15:46 chitucao Exp $$
 */
public class OriginProperty<T, R> extends AbstractProperty<T, R, R> {

    public OriginProperty(String name) {
        super(name, NodeType.HASH_MAP, false);
    }

    public OriginProperty(String name, NodeType nodeType) {
        super(name, nodeType, false);
    }

    @Override
    public R mappingNodeKey(R field) {
        return field;
    }

    @Override
    public R mappingOrCreateNodeKey(R field) {
        return field;
    }

    @Override
    public R nodeKey2FieldValue(R nodeKey) {
        return nodeKey;
    }

    @Override
    public Dict<R, R> getDict() {
        throw new IllegalStateException("OriginProperty is not a dictProperty, propertyName: " + name);
    }
}