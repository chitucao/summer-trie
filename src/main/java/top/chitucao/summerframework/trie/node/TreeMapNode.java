package top.chitucao.summerframework.trie.node;

import lombok.Getter;

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
@Getter
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
        if (child.containsKey(key)) {
            return child.get(key);
        }
        child.put(key, childNode);
        return child.get(key);
    }

    @Override
    public Node addChild(Number key, Supplier<Node> childSupplier) {
        if (child.containsKey(key)) {
            return child.get(key);
        }
        child.put(key, childSupplier.get());
        return child.get(key);
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

    @Override
    public Map<Number, Node> eq(Number key) {
        //noinspection SortedCollectionWithNonComparableKeys
        Map<Number, Node> result = new TreeMap<>();
        if (Objects.isNull(key)) {
            return result;
        }
        Node childNode = child.get(key);
        if (Objects.nonNull(childNode)) {
            result.put(key, childNode);
        }
        return result;
    }

    @Override
    public Map<Number, Node> between(Number left, Number right) {
        if (Objects.isNull(left) && Objects.isNull(right)) {
            return new TreeMap<>(child);
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.longValue() > right.longValue()) {
                //noinspection SortedCollectionWithNonComparableKeys
                return new TreeMap<>();
            }
            return new TreeMap<>(child.subMap(left, true, right, true));
        } else if (Objects.nonNull(left)) {
            return new TreeMap<>(child.tailMap(left, true));
        } else {
            return new TreeMap<>(child.headMap(right, true));
        }
    }

    @Override
    public Map<Number, Node> in(Set<Number> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        TreeMap<Number, Node> result = new TreeMap<>();
        keys.forEach(k -> {
            Node v = child.get(k);
            if (Objects.nonNull(v)) {
                result.put(k, v);
            }
        });
        return result;
    }

    @Override
    public Map<Number, Node> notIn(Set<Number> keys) {
        //noinspection SortedCollectionWithNonComparableKeys
        TreeMap<Number, Node> result = new TreeMap<>();
        child.forEach((k, v) -> {
            if (!keys.contains(k)) {
                result.put(k, v);
            }
        });
        return result;
    }

    @Override
    public boolean containsEq(Number key) {
        return child.containsKey(key);
    }

    @Override
    public boolean containsBetween(Number left, Number right) {
        if (Objects.isNull(left) && Objects.isNull(right)) {
            return true;
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.longValue() > right.longValue()) {
                return false;
            }
            return !child.subMap(left, true, right, true).isEmpty();
        } else if (Objects.nonNull(left)) {
            return !child.tailMap(left, true).isEmpty();
        } else {
            return !child.headMap(right, true).isEmpty();
        }
    }

    @Override
    public boolean containsIn(Set<Number> keys) {
        if (keys == null || keys.isEmpty()) {
            return !child.isEmpty();
        }
        return keys.stream().anyMatch(child::containsKey);
    }

    @Override
    public boolean containsNotIn(Set<Number> keys) {
        if (keys == null || keys.isEmpty()) {
            return !child.isEmpty();
        }
        for (Number number : child.keySet()) {
            if (!keys.contains(number)) {
                return true;
            }
        }
        return false;
    }

    public static class EmptyNodeHolder {
        public static final TreeMapNode EMPTY_NODE = new TreeMapNode();
    }
}