package top.chitucao.summerframework.trie.node.extra;

import java.io.Serializable;
import java.util.function.Supplier;

import top.chitucao.summerframework.trie.node.AbstractMapNode;
import top.chitucao.summerframework.trie.node.Node;

/**
 * AbstractNamedMapNode
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: AbstractNamedMapNode.java, v 0.1 2025-08-20 16:15 chitucao Exp $$
 */
public abstract class AbstractNamedMapNode<K> extends AbstractMapNode<K> implements Serializable {

    /**
     * 节点名称
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 添加子节点
     *
     * @param key   子节点key
     * @param child 子节点
     */
    @Override
    public Node<K> putChild(K key, Node<K> child) {
        Node<K> node = super.putChild(key, child);
        //noinspection rawtypes
        ((AbstractNamedMapNode) node).setName(String.valueOf(key));
        return node;
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
        Node<K> node = super.putChild(key, childSupplier);
        //noinspection rawtypes
        ((AbstractNamedMapNode) node).setName(String.valueOf(key));
        return node;
    }
}