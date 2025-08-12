package top.chitucao.summerframework.trie.operation;

import java.util.*;

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
    public static final Operate HASH_MAP_EQ_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        @SuppressWarnings("SuspiciousMethodCalls")
        Node childNode = childMap.get(key);
        if (Objects.nonNull(childNode)) {
            result.put((Number) key, childNode);
        }
        return result;
    };

    // 不等于
    // hashMap O(n)
    public static final Operate HASH_MAP_NE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (Objects.equals(entry.getKey(), key)) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    };

    // 大于
    // hashMap O(n)
    public static final Operate HASH_MAP_GT_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (entry.getKey().longValue() > ((Number) key).longValue()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 大于等于
    // hashMap O(n)
    public static final Operate HASH_MAP_GTE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (entry.getKey().longValue() >= ((Number) key).longValue()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 小于
    // hashMap O(n)
    public static final Operate HASH_MAP_LT_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (entry.getKey().longValue() < ((Number) key).longValue()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 小于等于
    // hashMap O(n)
    public static final Operate HASH_MAP_LTE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (entry.getKey().longValue() <= ((Number) key).longValue()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    };

    // 区间
    // hashMap O(n)
    public static final Operate HASH_MAP_BETWEEN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings("unchecked")
        List<Number> range = (List<Number>) key;
        Number left = range.get(0);
        Number right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return childMap;
        }

        Map<Number, Node> result = new HashMap<>();
        if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.longValue() > right.longValue()) {
                return childMap;
            }
            for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
                if (entry.getKey().longValue() >= left.longValue() && entry.getKey().longValue() <= right.longValue()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else if (Objects.nonNull(left)) {
            for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
                if (entry.getKey().longValue() >= left.longValue()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
                if (entry.getKey().longValue() <= right.longValue()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    };

    // 包含
    // hashMap O(n)
    public static final Operate HASH_MAP_IN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<Number> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 不包含
    // hashMap O(n)
    public static final Operate HASH_MAP_NIN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<Number> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        Map<Number, Node> result = new HashMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    //---------------------------------------- TREE_MAP操作 ----------------------------------------
    // 等于
    // treeMap O(logn)
    public static final Operate TREE_MAP_EQ_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings("SortedCollectionWithNonComparableKeys")
        Map<Number, Node> result = new TreeMap<>();
        @SuppressWarnings("SuspiciousMethodCalls")
        Node childNode = childMap.get(key);
        if (Objects.nonNull(childNode)) {
            result.put((Number) key, childNode);
        }
        return result;
    };

    // 不等于
    // treeMap O(n)
    public static final Operate TREE_MAP_NE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings("SortedCollectionWithNonComparableKeys")
        Map<Number, Node> result = new TreeMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (Objects.equals(entry.getKey(), key)) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    };

    // 大于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_GT_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new TreeMap<>(((TreeMap<Number, Node>) childMap).tailMap((Number) key, false));
    };

    // 大于等于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_GTE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new TreeMap<>(((TreeMap<Number, Node>) childMap).tailMap((Number) key, true));
    };

    // 小于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_LT_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new TreeMap<>(((TreeMap<Number, Node>) childMap).headMap((Number) key, false));
    };

    // 小于等于
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_LTE_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        return new TreeMap<>(((TreeMap<Number, Node>) childMap).headMap((Number) key, true));
    };

    // 区间
    // treeMap O(logn + m)
    public static final Operate TREE_MAP_BETWEEN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings("unchecked")
        List<Number> range = (List<Number>) key;
        Number left = range.get(0);
        Number right = range.get(1);

        if (Objects.isNull(left) && Objects.isNull(right)) {
            return new TreeMap<>(childMap);
        } else if (Objects.nonNull(left) && Objects.nonNull(right)) {
            if (left.longValue() > right.longValue()) {
                //noinspection SortedCollectionWithNonComparableKeys
                return new TreeMap<>();
            }
            return new TreeMap<>(((TreeMap<Number, Node>) childMap).subMap(left, true, right, true));
        } else if (Objects.nonNull(left)) {
            return new TreeMap<>(((TreeMap<Number, Node>) childMap).tailMap(left, true));
        } else {
            return new TreeMap<>(((TreeMap<Number, Node>) childMap).headMap(right, true));
        }
    };

    // 包含
    // treeMap O(n)
    public static final Operate TREE_MAP_IN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<Number> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        //noinspection SortedCollectionWithNonComparableKeys
        Map<Number, Node> result = new TreeMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };

    // 不包含
    // treeMap O(n)
    public static final Operate TREE_MAP_NIN_OP = (childMap, key) -> {
        if (childMap.isEmpty() || Objects.isNull(key)) {
            return childMap;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<Number> keys = new HashSet<>((List) key);

        if (keys.isEmpty()) {
            return childMap;
        }

        //noinspection SortedCollectionWithNonComparableKeys
        Map<Number, Node> result = new TreeMap<>();
        for (Map.Entry<Number, Node> entry : childMap.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    };
}