package top.chitucao.summerframework.trie.nodemanager;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.HashMapNode;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.node.TreeMapNode;
import top.chitucao.summerframework.trie.query.Aggregation;
import top.chitucao.summerframework.trie.query.Criterion;

/**
 * 默认节点管理器
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: DefaultNodeManager.java, v 0.1 2024-08-06 下午4:14 chitucao Exp $$
 */
@Getter
@Setter
public class DefaultNodeManager<T, R> implements NodeManager<T, R> {

    @Setter
    protected NodeManager<T, R> prev, next;

    protected Property<T, R>    property;

    public DefaultNodeManager(Property<T, R> property) {
        this.property = property;
    }

    @Override
    public NodeManager<T, R> prev() {
        return this.prev;
    }

    @Override
    public NodeManager<T, R> next() {
        return this.next;
    }

    @Override
    public Property<T, R> property() {
        return this.property;
    }

    @Override
    public Node createNewNode() {
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                return new TreeMapNode();
            default:
                return new HashMapNode();
        }
    }

    @Override
    public Node createNewNode(Map<Number, Node> childMap) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                return new TreeMapNode(childMap);
            default:
                return new HashMapNode(childMap);
        }
    }

    @Override
    public Node createEmptyValueNode(Stream<Number> keys) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                return new TreeMapNode(keys);
            default:
                return new HashMapNode(keys);
        }
    }

    @Override
    public Stream<R> mappingDictValues(Set<Number> dictKeys) {
        return dictKeys.stream().map(dictKey -> property.dict().getDictValue(dictKey));
    }

    @Override
    public Number mappingDictKey(T t) {
       return property.dict().getDictKey(property.mappingValue(t));
    }

    @Override
    public Node addChildNode(Node parent, T t) {
        R val = property.mappingValue(t);
        Supplier<Node> childSupplier = next() == null ? this::createNewNode : () -> next.createNewNode();
        Number dictKey = property.mappingDictKey(val);
        property.dict().putDict(dictKey, val);
        return parent.addChild(dictKey, childSupplier);
    }

    @Override
    public Node addChildNode(Node parent, Object val1, Supplier<Node> childNodeSupplier) {
        @SuppressWarnings("unchecked")
        R val = (R) val1;
        Number dictKey = property.mappingDictKey(val);
        property.dict().putDict(dictKey, val);
        return parent.addChild(dictKey, childNodeSupplier);
    }

    @Override
    public void removeChildNode(Node parent, T t) {
        R val = property.mappingValue(t);
        if (Objects.isNull(val) || !property.dict().containsDictValue(val)) {
            return;
        }
        parent.removeChild(property.getDictKey(val));
    }

    @Override
    public Node findChildNode(Node parent, T t) {
        R val = property.mappingValue(t);
        if (Objects.isNull(val) || !property.dict().containsDictValue(val)) {
            return null;
        }
        return parent.getChild(property.getDictKey(val));
    }

    @Override
    public Map<Number, Node> searchAndAgg(Node cur, Criterion criterion, Aggregation aggregation) {
        if (Objects.isNull(criterion) && Objects.isNull(aggregation)) {
            return cur.childMap();
        }
        if (Objects.isNull(aggregation)) {
            return search(cur, criterion);
        }
        Map<Number, Node> childMap = search(cur, criterion);
        if (childMap.isEmpty()) {
            return childMap;
        }
        //noinspection SwitchStatementWithTooFewBranches
        switch (property.nodeType()) {
            case TREE_MAP:
                //noinspection SortedCollectionWithNonComparableKeys
                TreeMap<Number, Node> treeMapResult = new TreeMap<>();
                switch (aggregation) {
                    case MIN:
                        Map.Entry<Number, Node> minEntry = ((TreeMap<Number, Node>) childMap).firstEntry();
                        treeMapResult.put(minEntry.getKey(), minEntry.getValue());
                        return treeMapResult;
                    case MAX:
                        Map.Entry<Number, Node> maxEntry = ((TreeMap<Number, Node>) childMap).lastEntry();
                        treeMapResult.put(maxEntry.getKey(), maxEntry.getValue());
                        return treeMapResult;
                    default:
                        return childMap;
                }
            default:
                HashMap<Number, Node> hashMapResult = new HashMap<>();
                switch (aggregation) {
                    case MIN:
                        Number minDictKey = Long.MAX_VALUE;
                        for (Number dictKey : childMap.keySet()) {
                            if (dictKey.longValue() < minDictKey.longValue()) {
                                minDictKey = dictKey;
                            }
                        }
                        hashMapResult.put(minDictKey, childMap.get(minDictKey));
                        return hashMapResult;
                    case MAX:
                        Number maxDictKey = Long.MIN_VALUE;
                        for (Number dictKey : childMap.keySet()) {
                            if (dictKey.longValue() > maxDictKey.longValue()) {
                                maxDictKey = dictKey;
                            }
                        }
                        hashMapResult.put(maxDictKey, childMap.get(maxDictKey));
                        return hashMapResult;
                    default:
                        return childMap;
                }
        }
    }

    @Override
    public Map<Number, Node> search(Node cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return cur.childMap();
        }
        switch (criterion.getCondition()) {
            case EQUAL:
                //noinspection unchecked
                return cur.eq(property.getDictKey((R) criterion.getValue()));
            case BETWEEN:
                //noinspection unchecked
                return cur.between(property.getDictKey((R) criterion.getValue()), property.getDictKey((R) criterion.getSecondValue()));
            case GTE:
                //noinspection unchecked
                return cur.between(property.getDictKey((R) criterion.getValue()), null);
            case LTE:
                //noinspection unchecked
                return cur.between(null, property.getDictKey((R) criterion.getValue()));
            case IN:
                //noinspection unchecked
                List<R> inValues = (List<R>) criterion.getValue();
                if (Objects.isNull(inValues) || inValues.isEmpty()) {
                    return cur.childMap();
                }
                return cur.in((inValues.stream().map(property::getDictKey).collect(Collectors.toSet())));
            case NOT_IN:
                //noinspection unchecked
                List<R> notInValues = (List<R>) criterion.getValue();
                if (Objects.isNull(notInValues) || notInValues.isEmpty()) {
                    return cur.childMap();
                }
                return cur.notIn((notInValues.stream().map(property::getDictKey).collect(Collectors.toSet())));
            default:
                return cur.childMap();
        }
    }

    @Override
    public boolean contains(Node cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return true;
        }
        switch (criterion.getCondition()) {
            case EQUAL:
                //noinspection unchecked
                return cur.containsEq(property.getDictKey((R) criterion.getValue()));
            case BETWEEN:
                //noinspection unchecked
                return cur.containsBetween(property.getDictKey((R) criterion.getValue()), property.getDictKey((R) criterion.getSecondValue()));
            case GTE:
                //noinspection unchecked
                return cur.containsBetween(property.getDictKey((R) criterion.getValue()), null);
            case LTE:
                //noinspection unchecked
                return cur.containsBetween(null, property.getDictKey((R) criterion.getValue()));
            case IN:
                //noinspection unchecked
                List<R> inValues = (List<R>) criterion.getValue();
                if (Objects.isNull(inValues) || inValues.isEmpty()) {
                    return cur.getSize() > 0;
                }
                return cur.containsIn((inValues.stream().map(property::getDictKey).collect(Collectors.toSet())));
            case NOT_IN:
                //noinspection unchecked
                List<R> notInValues = (List<R>) criterion.getValue();
                if (Objects.isNull(notInValues) || notInValues.isEmpty()) {
                    return cur.getSize() > 0;
                }
                return cur.containsNotIn(notInValues.stream().map(property::getDictKey).collect(Collectors.toSet()));
            default:
                return true;
        }
    }

    @Override
    public void slice(Node cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return;
        }
        cur.setChild(search(cur, criterion));
    }

    /**
     * 根据条件删除节点
     *
     * @param cur       当前节点
     * @param criterion 条件
     */
    @Override
    public void remove(Node cur, Criterion criterion) {
        if (Objects.isNull(criterion)) {
            return;
        }
        for (Number k : search(cur, criterion).keySet()) {
            cur.childMap().remove(k);
        }
    }
}