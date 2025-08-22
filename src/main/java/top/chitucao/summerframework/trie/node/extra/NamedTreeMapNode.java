package top.chitucao.summerframework.trie.node.extra;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import top.chitucao.summerframework.trie.node.Node;

/**
 * NamedTreeMapNode
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: NamedTreeMapNode.java, v 0.1 2025-08-20 16:23 chitucao Exp $$
 */
public class NamedTreeMapNode<K> extends AbstractNamedMapNode<K> implements Serializable {

    public NamedTreeMapNode() {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new TreeMap<>();
    }

    public NamedTreeMapNode(Stream<K> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new TreeMap<>();
        keys.forEach(key -> {
            //noinspection unchecked
            children.put(key, NamedTreeMapNode.EmptyNodeHolder.EMPTY_NODE);
        });
    }

    /**
     * 设置子节点映射表
     *
     * @param children  子节点映射表
     */
    @Override
    public void setChildren(Map<K, Node<K>> children) {
        this.children = new TreeMap<>(children);
    }

    /**
     * 单例实现的空节点
     */
    public static class EmptyNodeHolder {
        @SuppressWarnings("rawtypes")
        public static final NamedTreeMapNode EMPTY_NODE = new NamedTreeMapNode();
    }
}