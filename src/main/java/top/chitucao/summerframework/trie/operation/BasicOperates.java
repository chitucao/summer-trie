package top.chitucao.summerframework.trie.operation;

import java.util.*;
import java.util.stream.Collectors;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.NodeFactoryRegistry;

/**
 * 基本操作实现
 *
 * @author chitucao
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BasicOperates {
    //---------------------------------------- HASH_MAP操作 ----------------------------------------
    // 等于
    // hashMap O(1)
    public static final Operate HASH_MAP_EQ_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        Node<?> childNode = childMap.get(key);
        if (Objects.nonNull(childNode)) {
            result.put(key, childNode);
        }
        return result;
    };

    // 不等于
    // hashMap O(n)
    public static final Operate HASH_MAP_NE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (Objects.equals(entry.getKey(), key)) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    };

    // 大于
    // hashMap O(n)
    public static final Operate HASH_MAP_GT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (((Comparable) entry.getKey()).compareTo(key) > 0) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 大于等于
    // hashMap O(n)
    public static final Operate HASH_MAP_GTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (((Comparable) entry.getKey()).compareTo(key) >= 0) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 小于
    // hashMap O(n)
    public static final Operate HASH_MAP_LT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (((Comparable) entry.getKey()).compareTo(key) < 0) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 小于等于
    // hashMap O(n)
    public static final Operate HASH_MAP_LTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (((Comparable) entry.getKey()).compareTo(key) <= 0) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 区间
    // hashMap O(n)
    public static final Operate HASH_MAP_BETWEEN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        List<Comparable> range = (List<Comparable>) key;
        Comparable left = range.get(0);
        Comparable right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return childMap;
        }

        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.compareTo(right) > 0) {
                return childMap;
            }
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                Comparable entryKey = (Comparable) entry.getKey();
                if (entryKey.compareTo(left) >= 0 && entryKey.compareTo(right) <= 0) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else if (Objects.nonNull(left)) {
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                if (((Comparable) entry.getKey()).compareTo(left) >= 0) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                if (((Comparable) entry.getKey()).compareTo(right) <= 0) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    };

    // 包含
    // hashMap O(n)
    public static final Operate HASH_MAP_IN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());

        // 优化单条数据的in操作
        if (keys.size() == 1) {
            Object key1 = keys.iterator().next();
            Node<?> childNode = childMap.get(key1);
            if (Objects.nonNull(childNode)) {
                result.put(key1, childNode);
            }
            return result;
        }

        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 不包含
    // hashMap O(n)
    public static final Operate HASH_MAP_NIN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    //---------------------------------------- TREE_MAP操作 ----------------------------------------
    // 等于
    // treeMap O(logn)
    public static final Operate NAVIGABLE_MAP_EQ_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        Node<?> childNode = childMap.get(key);
        if (Objects.nonNull(childNode)) {
            result.put(key, childNode);
        }
        return result;
    };

    // 不等于
    // treeMap O(n)
    public static final Operate NAVIGABLE_MAP_NE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (Objects.equals(entry.getKey(), key)) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    };

    // 大于
    // treeMap O(logn + m)
    public static final Operate NAVIGABLE_MAP_GT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).tailMap(key, false);
        return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
    };

    // 大于等于
    // treeMap O(logn + m)
    public static final Operate NAVIGABLE_MAP_GTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).tailMap(key, true);
        return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
    };

    // 小于
    // treeMap O(logn + m)
    public static final Operate NAVIGABLE_MAP_LT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).headMap(key, false);
        return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
    };

    // 小于等于
    // treeMap O(logn + m)
    public static final Operate NAVIGABLE_MAP_LTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).headMap(key, true);
        return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
    };

    // 区间
    // treeMap O(logn + m)
    public static final Operate NAVIGABLE_MAP_BETWEEN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        List<Comparable> range = (List<Comparable>) key;
        Comparable left = range.get(0);
        Comparable right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren((Map) childMap));
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.compareTo(right) > 0) {
                return (Map) (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
            }
            NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).subMap(left, true, right, true);
            return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
        } else if (Objects.nonNull(left)) {
            NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).tailMap(left, true);
            return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
        } else {
            NavigableMap result = ((NavigableMap<Object, Node<?>>) childMap).headMap(right, true);
            return (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result));
        }
    };

    // 包含
    // treeMap O(n)
    public static final Operate NAVIGABLE_MAP_IN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Object, Node<?>> result = new TreeMap<>();

        // 优化单条数据的in操作
        if (keys.size() == 1) {
            Object key1 = keys.iterator().next();
            Node<?> childNode = childMap.get(key1);
            if (Objects.nonNull(childNode)) {
                result.put(key1, childNode);
            }
            return result;
        }

        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 不包含
    // treeMap O(n)
    public static final Operate NAVIGABLE_MAP_NIN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map result = (NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren());
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 将查询参数转换为字典键
    public static <T, R, K> Object mapperDictKey(Property<T, R, K> property, Object value) {
        if (Objects.isNull(value)) {
            return value;
        }
        if (value instanceof Collection) {
            Collection<R> values = (Collection<R>) value;
            if (values.isEmpty()) {
                return Collections.emptyList();
            }
            return values.stream().map(property::mappingNodeKey).collect(Collectors.toList());
        }
        return property.mappingNodeKey((R) value);
    }

    // 获取字典键对应的字典值
    public static <T, R, K> R mapperDictValue(Property<?, ?, ?> property, K key) {
        return ((Property<T, R, K>) property).nodeKey2FieldValue(key);
    }
}