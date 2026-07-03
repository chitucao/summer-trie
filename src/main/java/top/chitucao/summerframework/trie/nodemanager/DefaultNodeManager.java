package top.chitucao.summerframework.trie.nodemanager;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.LeafNode;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.NodeFactory;
import top.chitucao.summerframework.trie.node.NodeFactoryRegistry;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.node.extra.AbstractNamedMapNode;
import top.chitucao.summerframework.trie.operation.Func;
import top.chitucao.summerframework.trie.operation.Operation;
import top.chitucao.summerframework.trie.operation.OperationRegistry;
import top.chitucao.summerframework.trie.query.Aggregation;
import top.chitucao.summerframework.trie.query.Criterion;

/**
 * 默认节点管理器
 *
 * @author chitucao
 */
public class DefaultNodeManager implements NodeManager {

    protected NodeManager       prev, next;

    protected Property<?, ?, ?> property;

    /** 紧凑只读模式：哈希层节点改用 ADAPTIVE_HASH 紧凑节点 */
    private final boolean       compactReadOnly;

    public DefaultNodeManager(Property<?, ?, ?> property) {
        this(property, false);
    }

    public DefaultNodeManager(Property<?, ?, ?> property, boolean compactReadOnly) {
        this.property = property;
        this.compactReadOnly = compactReadOnly;
    }

    @Override
    public NodeManager prev() {
        return this.prev;
    }

    @Override
    public NodeManager next() {
        return this.next;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Property<?, ?, ?> property() {
        return this.property;
    }

    @Override
    public <K> Node<K> createNewNode() {
        String nodeType = property.nodeType();
        if (compactReadOnly && (NodeType.HASH_MAP.name().equals(nodeType) || NodeType.CONCURRENT_HASH_MAP.name().equals(nodeType))) {
            // 紧凑只读模式：哈希层改用 ADAPTIVE_HASH(紧凑数组)；算子仍按原 nodeType 走，AdaptiveNode.children() 是 Map 视图，零改
            nodeType = NodeType.ADAPTIVE_HASH.name();
        }
        return NodeFactoryRegistry.getInstance().getNodeFactory(nodeType).newNode();
    }

    @Override
    public <R, K> Stream<R> mappingDictValues(Set<K> dictKeys) {
        //noinspection unchecked
        return dictKeys.stream().map(dictKey -> ((Property<?, R, K>) property).nodeKey2FieldValue(dictKey));
    }

    @Override
    public <T, R, K> K mappingDictKey(T t) {
        //noinspection unchecked
        Property<T, R, K> property1 = (Property<T, R, K>) property;
        return property1.mappingNodeKey(property1.mappingFieldValue(t));
    }

    @Override
    public <T, R> R fieldValue(T t) {
        //noinspection unchecked
        Property<T, R, ?> property1 = (Property<T, R, ?>) property;
        return property1.mappingFieldValue(t);
    }

    @Override
    public <T, R, K> Node<K> addChildNode(Node<K> parent, T t) {
        @SuppressWarnings("unchecked")
        Property<T, R, K> property1 = (Property<T, R, K>) property;
        R val = property1.mappingFieldValue(t);
        if (val == null) {
            throw new IllegalArgumentException("Cannot add child node with null value. Property: " + property1.name() + "Data: " + t.toString());
        }
        Supplier<Node<K>> childSupplier;
        if (next() != null) {
            childSupplier = () -> next.createNewNode();
        } else if (parent instanceof AbstractNamedMapNode) {
            // 父为命名节点时，其 putChild 会把子节点强转为命名节点回写名字，故叶子仍用常规节点避免类型不兼容
            childSupplier = this::createNewNode;
        } else {
            // 叶子层：用无 children 容器的轻量叶子节点，省去每个叶子一个空 map 的内存开销
            childSupplier = () -> new LeafNode<K>();
        }
        K dictKey = property1.mappingOrCreateNodeKey(val);
        // 字典属性节点，需要加入树节点和字段值的映射关系
        if (property1.isDictProperty()) {
            property1.getDict().put(dictKey, val);
        }
        return parent.putChild(dictKey, childSupplier);
    }

    @Override
    public <T, R, K> void removeChildNode(Node<K> parent, T t) {
        @SuppressWarnings("unchecked")
        Property<T, R, K> property1 = (Property<T, R, K>) property;
        R val = property1.mappingFieldValue(t);
        if (Objects.isNull(val)) {
            return;
        }
        parent.removeChild(property1.mappingNodeKey(val));
    }

    /**
     * 删除子节点
     *
     * @param parent  父节点
     * @param dictKey 字典key
     */
    @Override
    public <K> void removeChild(Node<K> parent, K dictKey) {
        parent.removeChild(dictKey);
        if (property.isDictProperty()) {
            //noinspection unchecked
            ((Property<?, ?, K>) property).getDict().removeNodeKey(dictKey);
        }
    }

    @Override
    public <T, R, K> Node<K> findChildNode(Node<K> parent, T t) {
        @SuppressWarnings("unchecked")
        Property<T, R, K> property1 = (Property<T, R, K>) property;
        R val = property1.mappingFieldValue(t);
        if (Objects.isNull(val)) {
            return null;
        }
        return parent.child(property1.mappingNodeKey(val));
    }

    @Override
    public <K> Map<K, Node<K>> searchAndAgg(Node<K> cur, Criterion criterion, Aggregation aggregation) {
        if (Objects.isNull(criterion) && Objects.isNull(aggregation)) {
            return cur.children();
        }
        if (Objects.isNull(aggregation)) {
            return search(cur, criterion);
        }
        Map<K, Node<K>> childMap = search(cur, criterion);
        if (childMap.isEmpty() || childMap.size() == 1) {
            return childMap;
        }
        NodeFactory nodeFactory = NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType());

        // 创建返回值
        Map<K, Node<K>> newChildren = nodeFactory.newChildren();
        if (newChildren instanceof NavigableMap) {
            // 适用于TreeMap和SkipListMap，O(logn)
            switch (aggregation) {
                case MIN:
                    Map.Entry<K, Node<K>> minEntry = ((NavigableMap<K, Node<K>>) childMap).firstEntry();
                    newChildren.put(minEntry.getKey(), minEntry.getValue());
                    return newChildren;
                case MAX:
                    Map.Entry<K, Node<K>> maxEntry = ((NavigableMap<K, Node<K>>) childMap).lastEntry();
                    newChildren.put(maxEntry.getKey(), maxEntry.getValue());
                    return newChildren;
                default:
                    return childMap;
            }
        } else {
            // 兜底使用遍历的方式，例如HashMap
            switch (aggregation) {
                case MIN:
                    Map.Entry<K, Node<K>> minEntry = null;
                    for (Map.Entry<K, Node<K>> entry : childMap.entrySet()) {
                        //noinspection unchecked,rawtypes
                        if (minEntry == null || ((Comparable) entry.getKey()).compareTo(minEntry.getKey()) < 0) {
                            minEntry = entry;
                        }
                    }
                    if (Objects.nonNull(minEntry)) {
                        newChildren.put(minEntry.getKey(), minEntry.getValue());
                    }
                    return newChildren;
                case MAX:
                    Map.Entry<K, Node<K>> maxEntry = null;
                    for (Map.Entry<K, Node<K>> entry : childMap.entrySet()) {
                        //noinspection unchecked,rawtypes
                        if (maxEntry == null || ((Comparable) entry.getKey()).compareTo(maxEntry.getKey()) > 0) {
                            maxEntry = entry;
                        }
                    }
                    if (Objects.nonNull(maxEntry)) {
                        newChildren.put(maxEntry.getKey(), maxEntry.getValue());
                    }
                    return newChildren;
                default:
                    return childMap;
            }
        }
    }

    @Override
    public <K> Map<K, Node<K>> search(Node<K> cur, Criterion criterion) {
        Map<K, Node<K>> result = cur.children();
        if (Objects.isNull(criterion)) {
            return result;
        }
        for (Map.Entry<String, Object> entry : criterion.getCriterion().entrySet()) {
            result = operate(result, entry);
        }
        return result;
    }

    @Override
    public <K> boolean contains(Node<K> cur, Criterion criterion) {
        Map<K, Node<K>> childMap = cur.children();
        if (Objects.isNull(criterion)) {
            return true;
        }
        for (Map.Entry<String, Object> entry : criterion.getCriterion().entrySet()) {
            childMap = operate(childMap, entry);
            if (childMap.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <K> void slice(Node<K> cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return;
        }
        // P1-1: search(...) 可能返回 NavigableMap 子视图，setChildren 前必须物化成独立 Map，
        // 否则 view 与原 children 共享底层存储，后续写操作会相互影响。
        Map<K, Node<K>> result = search(cur, criterion);
        @SuppressWarnings("unchecked")
        Map<K, Node<K>> snapshot = NodeFactoryRegistry.getInstance().getNodeFactory(property.nodeType()).newChildren(result);
        cur.setChildren(snapshot);
    }

    /**
     * 根据条件删除节点
     *
     * @param cur       当前节点
     * @param criterion 条件
     */
    @Override
    public <K> void remove(Node<K> cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return;
        }
        // P1-1: search(...) 可能返回 NavigableMap 子视图（与底层共享存储），迭代同时修改底层会 CME；
        // 先把命中的 key 快照到 ArrayList 再逐个删除。
        java.util.List<K> keys = new java.util.ArrayList<>(search(cur, criterion).keySet());
        for (K k : keys) {
            cur.children().remove(k);
        }
    }

    public void setPrev(NodeManager prev) {
        this.prev = prev;
    }

    public void setNext(NodeManager next) {
        this.next = next;
    }

    public Property<?, ?, ?> getProperty() {
        return property;
    }

    public void setProperty(Property<?, ?, ?> property) {
        this.property = property;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <K> Map<K, Node<K>> operate(Map childMap, Map.Entry<String, Object> operationEntry) {
        String operationName = operationEntry.getKey();
        if (Objects.equals(operationName, Operation.FUNC.getValue())) {
            // 执行自定义函数
            return ((Func) operationEntry.getValue()).apply(childMap, property);
        }
        return OperationRegistry.getInstance().getOperate(property.nodeType(), operationEntry.getKey()).query(childMap, property, operationEntry.getValue());
    }

}
