package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.dict.HashMapDict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 包含字典的节点属性
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: AbstractDictProperty.java, v 0.1 2025-08-19 17:51 chitucao Exp $$
 */
public abstract class AbstractDictProperty<T, R, K> extends AbstractProperty<T, R, K> {

    /** 字段字典 */
    protected final Dict<R, K> dict;

    public AbstractDictProperty(String name) {
        super(name, NodeType.HASH_MAP, true);
        this.dict = new HashMapDict<>();
    }

    public AbstractDictProperty(String name, NodeType nodeType) {
        super(name, nodeType, true);
        this.dict = new HashMapDict<>();
    }

    public AbstractDictProperty(String name, NodeType nodeType, Dict<R, K> dict) {
        super(name, nodeType, true);
        this.dict = dict;
    }

    @Override
    public K mappingNodeKey(R field) {
        return dict.getNodeKey(field);
    }

    @Override
    public R nodeKey2FieldValue(K nodeKey) {
        return dict.getFieldValue(nodeKey);
    }

    @Override
    public Dict<R, K> getDict() {
        return dict;
    }

}