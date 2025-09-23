package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 线程安全的HashMap实现的trie节点
 *
 * @author chitucao
 */
public class ConcurrentHashMapNode<K> extends AbstractMapNode<K> {

    public ConcurrentHashMapNode() {
        children = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMapNode(Stream<K> keys) {
        children = new ConcurrentHashMap<>();
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
        this.children = new ConcurrentHashMap<>(children);
    }

    /**
     * 单例实现的空节点
     */
    public static class EmptyNodeHolder {
        @SuppressWarnings("rawtypes")
        public static final ConcurrentHashMapNode EMPTY_NODE = new ConcurrentHashMapNode();
    }

}