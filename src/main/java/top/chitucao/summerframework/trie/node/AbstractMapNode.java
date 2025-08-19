package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * map实现的trie节点抽象
 * @param <K>   节点值类型
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: AbstractMapNode.java, v 0.1 2025-08-18 10:22 chitucao Exp $$
 */
public abstract class AbstractMapNode<K> implements Node<K> {

    /**
     * 子节点映射表
     */
    protected Map<K, Node<K>> children;

    /**
     * 子节点映射表
     *
     * @return 子节点映射表
     */
    @Override
    public Map<K, Node<K>> children() {
        return children;
    }

    /**
     * 子节点数量
     *
     * @return 子节点数量
     */
    @Override
    public int childSize() {
        return children.size();
    }

    /**
     * 子节点key集合
     *
     * @return 子节点key集合
     */
    @Override
    public Set<K> childKeySet() {
        return children.keySet();
    }

    /**
     * 子节点
     *
     * @return  子节点key
     */
    @Override
    public Node<K> child(K key) {
        return children.get(key);
    }

    /**
     * 添加子节点
     *
     * @param key   子节点key
     * @param child 子节点
     */
    @Override
    public Node<K> putChild(K key, Node<K> child) {
        Node<K> existChild = children.get(key);
        if (Objects.nonNull(existChild)) {
            return existChild;
        }
        children.put(key, child);
        return child;
    }

    /**
     * 添加子节点
     *
     * @param key           子节点key
     * @param childSupplier 子节点提供者
     * @return 添加子节点
     */
    @Override
    public Node<K> putChild(K key, Supplier<Node<K>> childSupplier) {
        Node<K> existChild = children.get(key);
        if (Objects.nonNull(existChild)) {
            return existChild;
        }
        Node<K> child = childSupplier.get();
        children.put(key, child);
        return child;
    }

    /**
     * 删除子节点
     *
     * @param key 字典key
     */
    @SuppressWarnings("TypeParameterHidesVisibleType")
    @Override
    public <K> void removeChild(K key) {
        //noinspection SuspiciousMethodCalls
        children.remove(key);
    }
}