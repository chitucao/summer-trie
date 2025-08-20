package top.chitucao.summerframework.trie.operation;

import java.util.*;
import java.util.stream.Collectors;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;

/**
 * 基本操作实现
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: BasicOperates.java, v 0.1 2025-08-11 15:11 chitucao Exp $$
 */
public class BasicOperates {
    //---------------------------------------- HASH_MAP操作 ----------------------------------------
    // 等于
    // hashMap O(1)
    public static final Operate HASH_MAP_EQ_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Object, Node<?>> result = new HashMap<>();
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
        Map<Object, Node<?>> result = new HashMap<>();
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
        Map<Object, Node<?>> result = new HashMap<>();
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            //noinspection unchecked,rawtypes
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
        Map<Object, Node<?>> result = new HashMap<>();
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            //noinspection unchecked,rawtypes
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
        Map<Object, Node<?>> result = new HashMap<>();
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            //noinspection unchecked,rawtypes
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
        Map<Object, Node<?>> result = new HashMap<>();
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            //noinspection unchecked,rawtypes
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
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<Comparable> range = (List<Comparable>) key;
        @SuppressWarnings("rawtypes")
        Comparable left = range.get(0);
        @SuppressWarnings("rawtypes")
        Comparable right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return childMap;
        }

        Map<Object, Node<?>> result = new HashMap<>();
        if (Objects.nonNull(left) && Objects.nonNull(right)) {
            //noinspection unchecked
            if (left.compareTo(right) > 0) {
                return childMap;
            }
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                //noinspection rawtypes
                Comparable entryKey = (Comparable) entry.getKey();
                //noinspection unchecked
                if (entryKey.compareTo(left) >= 0 && entryKey.compareTo(right) <= 0) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else if (Objects.nonNull(left)) {
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                //noinspection unchecked,rawtypes
                if (((Comparable) entry.getKey()).compareTo(left) >= 0) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
                //noinspection unchecked,rawtypes
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
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Object, Node<?>> result = new HashMap<>();

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
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Object, Node<?>> result = new HashMap<>();
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
    public static final Operate TREE_MAP_EQ_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Object, Node<?>> result = new TreeMap<>();
        Node<?> childNode = childMap.get(key);
        if (Objects.nonNull(childNode)) {
            result.put(key, childNode);
        }
        return result;
    };

    // 不等于
    // treeMap O(n)
    public static final Operate TREE_MAP_NE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Object, Node<?>> result = new TreeMap<>();
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
    public static final Operate TREE_MAP_GT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        //noinspection unchecked
        return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).tailMap(key, false));
    };

    // 大于等于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_GTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        //noinspection unchecked
        return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).tailMap(key, true));
    };

    // 小于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_LT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        //noinspection unchecked
        return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).headMap(key, false));
    };

    // 小于等于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_LTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        //noinspection unchecked
        return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).headMap(key, true));
    };

    // 区间
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_BETWEEN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<Comparable> range = (List<Comparable>) key;
        @SuppressWarnings("rawtypes")
        Comparable left = range.get(0);
        @SuppressWarnings("rawtypes")
        Comparable right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return new TreeMap<>(childMap);
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            //noinspection unchecked
            if (left.compareTo(right) > 0) {
                return new TreeMap<>();
            }
            //noinspection unchecked
            return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).subMap(left, true, right, true));
        } else if (Objects.nonNull(left)) {
            //noinspection unchecked
            return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).tailMap(left, true));
        } else {
            //noinspection unchecked
            return new TreeMap<>(((TreeMap<Object, Node<?>>) childMap).headMap(right, true));
        }
    };

    // 包含
    // treeMap O(n)
    public static final Operate TREE_MAP_IN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
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
    public static final Operate TREE_MAP_NIN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Object, Node<?>> result = new TreeMap<>();
        for (Map.Entry<?, Node<?>> entry : childMap.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 将查询参数转换为字典键
    @SuppressWarnings("unchecked")
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
        //noinspection unchecked
        return ((Property<T, R, K>) property).nodeKey2FieldValue(key);
    }
}