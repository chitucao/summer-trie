package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 红黑树实现的trie节点
 *
 * @author chitucao
 */
public class TreeMapNode<K> extends AbstractMapNode<K> {

    public TreeMapNode() {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new TreeMap<>();
    }

    public TreeMapNode(Stream<K> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new TreeMap<>();
        keys.forEach(key -> {
            //noinspection unchecked
            children.put(key, EmptyNodeHolder.EMPTY_NODE);
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
        public static final TreeMapNode EMPTY_NODE = new TreeMapNode();
    }
}