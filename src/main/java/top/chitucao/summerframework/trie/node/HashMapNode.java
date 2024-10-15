package top.chitucao.summerframework.trie.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.Maps;

import lombok.Getter;

/**
 * HashMapNode
 *
 * @author chitucao
 */
@Getter
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
        if (child.containsKey(key)) {
            return child.get(key);
        }
        child.put(key, childNode);
        return child.get(key);
    }

    @Override
    public Node addChild(Number key, Supplier<Node> childNodeSupplier) {
        if (child.containsKey(key)) {
            return child.get(key);
        }
        child.put(key, childNodeSupplier.get());
        return child.get(key);
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

    @Override
    public Map<Number, Node> eq(Number key) {
        Map<Number, Node> result = Maps.newHashMap();
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
        Map<Number, Node> result = Maps.newHashMap();
        if (Objects.isNull(left) && Objects.isNull(right)) {
            return Maps.newHashMap(child);
        }
        if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.longValue() > right.longValue()) {
                return result;
            }
            child.forEach((k, v) -> {
                if (k.longValue() >= left.longValue() && k.longValue() <= right.longValue()) {
                    result.put(k, v);
                }
            });
        } else if (Objects.nonNull(left)) {
            child.forEach((k, v) -> {
                if (k.longValue() >= left.longValue()) {
                    result.put(k, v);
                }
            });
        } else {
            child.forEach((k, v) -> {
                if (k.longValue() <= right.longValue()) {
                    result.put(k, v);
                }
            });
        }
        return result;
    }

    @Override
    public Map<Number, Node> in(Set<Number> keys) {
        Map<Number, Node> result = Maps.newHashMap();
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
        Map<Number, Node> result = Maps.newHashMap();
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
            return child.keySet().stream().anyMatch(k -> k.longValue() >= left.longValue() && k.longValue() <= right.longValue());
        } else if (Objects.nonNull(left)) {
            return child.keySet().stream().anyMatch(k -> k.longValue() >= left.longValue());
        } else {
            return child.keySet().stream().anyMatch(k -> k.longValue() <= right.longValue());
        }
    }

    @Override
    public boolean containsIn(Set<Number> keys) {
        return keys.stream().anyMatch(child::containsKey);
    }

    @Override
    public boolean containsNotIn(Set<Number> keys) {
        for (Number number : child.keySet()) {
            if (!keys.contains(number)) {
                return true;
            }
        }
        return false;
    }

    public static class EmptyNodeHolder {
        public static final HashMapNode EMPTY_NODE = new HashMapNode();
    }
}