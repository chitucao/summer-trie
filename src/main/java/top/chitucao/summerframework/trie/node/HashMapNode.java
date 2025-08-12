package top.chitucao.summerframework.trie.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * HashMapNode
 *
 * @author chitucao
 */
public class HashMapNode implements Node {

    private HashMap<Number, Node> child;

    public HashMapNode() {
        this.child = new HashMap<>();
    }

    public HashMapNode(Map<Number, Node> child) {
        this.child = new HashMap<>(child);
    }

    public HashMapNode(Stream<Number> keys) {
        this.child = new HashMap<>();
        keys.forEach(key -> {
            child.put(key, EmptyNodeHolder.EMPTY_NODE);
        });
    }

    @Override
    public int getSize() {
        return child.size();
    }

    @Override
    public Node addChild(Number key, Node childNode) {
        Node exChildNode = child.get(key);
        if (Objects.nonNull(exChildNode)) {
            return exChildNode;
        }
        child.put(key, childNode);
        return childNode;
    }

    @Override
    public Node addChild(Number key, Supplier<Node> childSupplier) {
        Node exChildNode = child.get(key);
        if (Objects.nonNull(exChildNode)) {
            return exChildNode;
        }
        Node childNode = childSupplier.get();
        child.put(key, childNode);
        return childNode;
    }

    /**
     * 设置子节点
     *
     * @param childMap 子节点映射
     */
    @Override
    public void setChild(Map<Number, Node> childMap) {
        this.child = new HashMap<>(childMap);
    }

    @Override
    public Set<Number> keys() {
        return child.keySet();
    }

    @Override
    public Map<Number, Node> childMap() {
        return child;
    }

    @Override
    public Node getChild(Number key) {
        return child.get(key);
    }

    @Override
    public void removeChild(Number key) {
        child.remove(key);
    }

    public static class EmptyNodeHolder {
        public static final HashMapNode EMPTY_NODE = new HashMapNode();
    }
}