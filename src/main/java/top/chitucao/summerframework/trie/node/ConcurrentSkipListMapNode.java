package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

/**
 * 跳表实现的线程安全的trie节点
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: ConcurrentSkipListMapNode.java, v 0.1 2025-09-22 13:55 chitucao Exp $$
 */
public class ConcurrentSkipListMapNode<K> extends AbstractMapNode<K> {

    public ConcurrentSkipListMapNode() {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new ConcurrentSkipListMap<>();
    }

    public ConcurrentSkipListMapNode(Stream<K> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        children = new ConcurrentSkipListMap<>();
        keys.forEach(key -> {
            //noinspection unchecked
            children.put(key, EmptyNodeHolder.EMPTY_NODE);
        });
    }

    /**
     * 设置子节点映射表
     *
     * @param children 子节点映射表
     */
    @Override
    public void setChildren(Map<K, Node<K>> children) {
        this.children = new ConcurrentSkipListMap<>(children);
    }

    /**
    * 单例实现的空节点
    */
    public static class EmptyNodeHolder {
        @SuppressWarnings("rawtypes")
        public static final ConcurrentSkipListMapNode EMPTY_NODE = new ConcurrentSkipListMapNode();
    }
}