package top.chitucao.summerframework.trie.node.extra;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import top.chitucao.summerframework.trie.node.Node;

/**
 * NamedHashMapNode
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: NamedHashMapNode.java, v 0.1 2025-08-20 16:21 chitucao Exp $$
 */
public class NamedHashMapNode<K> extends AbstractNamedMapNode<K> implements Serializable{

    public NamedHashMapNode() {
        children = new HashMap<>();
    }

    public NamedHashMapNode(Stream<K> keys) {
        children = new HashMap<>();
        keys.forEach(key -> {
            //noinspection unchecked
            children.put(key, NamedHashMapNode.EmptyNodeHolder.EMPTY_NODE);
        });
    }

    /**
     * 设置子节点映射表
     *
     * @param children  子节点映射表
     */
    @Override
    public void setChildren(Map<K, Node<K>> children) {
        this.children = new HashMap<>(children);
    }

    /**
     * 单例实现的空节点
     */
    public static class EmptyNodeHolder {
        @SuppressWarnings("rawtypes")
        public static final NamedHashMapNode EMPTY_NODE = new NamedHashMapNode<>();
    }
}