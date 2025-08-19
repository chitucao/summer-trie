package top.chitucao.summerframework.trie.node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 哈希表实现的trie节点
 *
 * @author chitucao
 */
public class HashMapNode<K> extends AbstractMapNode<K> {

    public HashMapNode() {
        children = new HashMap<>();
    }

    public HashMapNode(Stream<K> keys) {
        children = new HashMap<>();
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
        this.children = new HashMap<>(children);
    }

    /**
     * 单例实现的空节点
     */
    public static class EmptyNodeHolder {
        @SuppressWarnings("rawtypes")
        public static final HashMapNode EMPTY_NODE = new HashMapNode();
    }
}