package top.chitucao.summerframework.trie.operation;

import java.util.*;
import java.util.stream.Collectors;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.NodeFactoryRegistry;
import top.chitucao.summerframework.trie.utils.FilteredChildMap;

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
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图，避免每次新建 HashMap 并物化所有命中节点
    public static final Operate HASH_MAP_NE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new FilteredChildMap(childMap, (Map.Entry entry) -> !Objects.equals(entry.getKey(), key));
    };

    // 大于
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_GT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(key) > 0);
    };

    // 大于等于
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_GTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(key) >= 0);
    };

    // 小于
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_LT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(key) < 0);
    };

    // 小于等于
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_LTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(key) <= 0);
    };

    // 区间
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
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

        if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.compareTo(right) > 0) {
                return childMap;
            }
            return new FilteredChildMap(childMap, (Map.Entry entry) -> {
                Comparable entryKey = (Comparable) entry.getKey();
                return entryKey.compareTo(left) >= 0 && entryKey.compareTo(right) <= 0;
            });
        } else if (Objects.nonNull(left)) {
            return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(left) >= 0);
        } else {
            return new FilteredChildMap(childMap, (Map.Entry entry) -> ((Comparable) entry.getKey()).compareTo(right) <= 0);
        }
    };

    // 包含
    // hashMap 单值 O(1)、多值 O(n)，P1-2b: 多值分支返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_IN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        // 优化单条数据的 in 操作：走 O(1) 的 HashMap.get
        if (keys.size() == 1) {
            Map result = NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren();
            Object key1 = keys.iterator().next();
            Node<?> childNode = childMap.get(key1);
            if (Objects.nonNull(childNode)) {
                result.put(key1, childNode);
            }
            return result;
        }

        return new FilteredChildMap(childMap, (Map.Entry entry) -> keys.contains(entry.getKey()));
    };

    // 不包含
    // hashMap O(n)，P1-2b: 返回 FilteredChildMap 懒视图
    public static final Operate HASH_MAP_NIN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Set<?> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        return new FilteredChildMap(childMap, (Map.Entry entry) -> !keys.contains(entry.getKey()));
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
    // treeMap O(log n + m)，直接返回 NavigableMap 子视图，避免 new TreeMap<>(result) 的 O(m log m) 拷贝；
    // 调用方只做只读遍历，写操作路径（slice/remove/normErase）已改为显式快照。
    public static final Operate NAVIGABLE_MAP_GT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return ((NavigableMap<Object, Node<?>>) childMap).tailMap(key, false);
    };

    // 大于等于
    // treeMap O(log n + m)，返回子视图（见 NAVIGABLE_MAP_GT_OP 注释）
    public static final Operate NAVIGABLE_MAP_GTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return ((NavigableMap<Object, Node<?>>) childMap).tailMap(key, true);
    };

    // 小于
    // treeMap O(log n + m)，返回子视图（见 NAVIGABLE_MAP_GT_OP 注释）
    public static final Operate NAVIGABLE_MAP_LT_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return ((NavigableMap<Object, Node<?>>) childMap).headMap(key, false);
    };

    // 小于等于
    // treeMap O(log n + m)，返回子视图（见 NAVIGABLE_MAP_GT_OP 注释）
    public static final Operate NAVIGABLE_MAP_LTE_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return ((NavigableMap<Object, Node<?>>) childMap).headMap(key, true);
    };

    // 区间
    // treeMap O(log n + m)，返回子视图（见 NAVIGABLE_MAP_GT_OP 注释）
    public static final Operate NAVIGABLE_MAP_BETWEEN_OP = (childMap, property, value) -> {
        Object key = mapperDictKey(property, value);
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        List<Comparable> range = (List<Comparable>) key;
        Comparable left = range.get(0);
        Comparable right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return childMap;
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.compareTo(right) > 0) {
                return (Map) NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren();
            }
            return ((NavigableMap<Object, Node<?>>) childMap).subMap(left, true, right, true);
        } else if (Objects.nonNull(left)) {
            return ((NavigableMap<Object, Node<?>>) childMap).tailMap(left, true);
        } else {
            return ((NavigableMap<Object, Node<?>>) childMap).headMap(right, true);
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
