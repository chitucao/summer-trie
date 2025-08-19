package top.chitucao.summerframework.trie.nodemanager;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.HashMapNode;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.TreeMapNode;
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

    public DefaultNodeManager(Property<?, ?, ?> property) {
        this.property = property;
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
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                return new TreeMapNode<>();
            default:
                return new HashMapNode<>();
        }
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
    public <T, R, K> Node<K> addChildNode(Node<K> parent, T t) {
        @SuppressWarnings("unchecked")
        Property<T, R, K> property1 = (Property<T, R, K>) property;
        R val = property1.mappingFieldValue(t);
        if (val == null) {
            throw new IllegalArgumentException("Cannot add child node with null value. Property: " + property1.name() + "Data: " + t.toString());
        }
        Supplier<Node<K>> childSupplier = next() == null ? this::createNewNode : () -> next.createNewNode();
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
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                //noinspection SortedCollectionWithNonComparableKeys
                TreeMap<K, Node<K>> treeMapResult = new TreeMap<>();
                switch (aggregation) {
                    case MIN:
                        Map.Entry<K, Node<K>> minEntry = ((TreeMap<K, Node<K>>) childMap).firstEntry();
                        treeMapResult.put(minEntry.getKey(), minEntry.getValue());
                        return treeMapResult;
                    case MAX:
                        Map.Entry<K, Node<K>> maxEntry = ((TreeMap<K, Node<K>>) childMap).lastEntry();
                        treeMapResult.put(maxEntry.getKey(), maxEntry.getValue());
                        return treeMapResult;
                    default:
                        return childMap;
                }
            default:
                HashMap<K, Node<K>> hashMapResult = new HashMap<>();
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
                            hashMapResult.put(minEntry.getKey(), minEntry.getValue());
                        }
                        return hashMapResult;
                    case MAX:
                        Map.Entry<K, Node<K>> maxEntry = null;
                        for (Map.Entry<K, Node<K>> entry : childMap.entrySet()) {
                            //noinspection unchecked,rawtypes
                            if (maxEntry == null || ((Comparable) entry.getKey()).compareTo(maxEntry.getKey()) > 0) {
                                maxEntry = entry;
                            }
                        }
                        if (Objects.nonNull(maxEntry)) {
                            hashMapResult.put(maxEntry.getKey(), maxEntry.getValue());
                        }
                        return hashMapResult;
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
        cur.setChildren(search(cur, criterion));
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
        for (K k : search(cur, criterion).keySet()) {
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
        try {
            return OperationRegistry.getInstance().getOperate(property.nodeType().name(), operationEntry.getKey()).query(childMap, property, operationEntry.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
