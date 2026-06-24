package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.dict.HashMapDict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 包含字典的节点属性
 *
 * @author chitucao
 */
public abstract class AbstractDictProperty<T, R, K> extends AbstractProperty<T, R, K> {

    /** 字段字典（非 final：紧凑只读模式下 MapTrie 构建前可替换为更省内存的实现，如 ArrayDict） */
    protected Dict<R, K>       dict;

    public AbstractDictProperty(String name) {
        super(name, NodeType.HASH_MAP.name(), true);
        this.dict = new HashMapDict<>();
    }

    public AbstractDictProperty(String name, String nodeType) {
        super(name, nodeType, true);
        this.dict = new HashMapDict<>();
    }

    public AbstractDictProperty(String name, String nodeType, Dict<R, K> dict) {
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

    /**
     * 替换字典实现（仅应在构建前、字典为空时调用，如紧凑只读模式切换为 ArrayDict）
     *
     * @param dict 新字典
     */
    public void setDict(Dict<R, K> dict) {
        this.dict = dict;
    }

}