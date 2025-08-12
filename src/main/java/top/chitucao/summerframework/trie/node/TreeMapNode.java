package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * TreeMapNode
 *
 * @author chitucao
 */
public class TreeMapNode implements Node {

    private TreeMap<Number, Node> child;

    public TreeMapNode() {
        //noinspection SortedCollectionWithNonComparableKeys
        this.child = new TreeMap<>();
    }

    public TreeMapNode(Map<Number, Node> child) {
        this.child = new TreeMap<>(child);
    }

    public TreeMapNode(Stream<Number> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        this.child = new TreeMap<>();
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
    public void setChild(Map<Number, Node> childMap) {
        this.child = new TreeMap<>(childMap);
    }

    @Override
    public void removeChild(Number key) {
        child.remove(key);
    }

    public static class EmptyNodeHolder {
        public static final TreeMapNode EMPTY_NODE = new TreeMapNode();
    }
}