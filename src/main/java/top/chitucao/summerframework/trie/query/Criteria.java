package top.chitucao.summerframework.trie.query;

import java.util.*;

import top.chitucao.summerframework.trie.operation.Func;
import top.chitucao.summerframework.trie.operation.Operate;
import top.chitucao.summerframework.trie.operation.Operation;
import top.chitucao.summerframework.trie.operation.OperationRegistry;

/**
 * 条件
 *
 * @author chitucao
 */
public class Criteria {

    private String                              key;
    private final LinkedHashMap<String, Object> criterion = new LinkedHashMap<>();
    private final List<Criteria>                criteriaChain;

    public Criteria() {
        this.criteriaChain = new ArrayList<>();
    }

    private Criteria(String key) {
        this.key = key;
        this.criteriaChain = new ArrayList<>();
        this.criteriaChain.add(this);
    }

    private Criteria(List<Criteria> criteriaChain, String key) {
        this.key = key;
        this.criteriaChain = criteriaChain;
        this.criteriaChain.add(this);
    }

    public static Criteria where(String key) {
        return new Criteria(key);
    }

    public Criteria and(String key) {
        return new Criteria(this.criteriaChain, key);
    }

    public Criteria eq(Object value) {
        addCriterion(Operation.EQ.getValue(), value);
        return this;
    }

    public Criteria ne(Object value) {
        addCriterion(Operation.NE.getValue(), value);
        return this;
    }

    public Criteria gt(Object value) {
        addCriterion(Operation.GT.getValue(), value);
        return this;
    }

    public Criteria gte(Object value) {
        addCriterion(Operation.GTE.getValue(), value);
        return this;
    }

    public Criteria lt(Object value) {
        addCriterion(Operation.LT.getValue(), value);
        return this;
    }

    public Criteria lte(Object value) {
        addCriterion(Operation.LTE.getValue(), value);
        return this;
    }

    public Criteria between(Object value, Object secondValue) {
        addCriterion(Operation.BETWEEN.getValue(), Arrays.asList(value, secondValue));
        return this;
    }

    public Criteria in(Object... values) {
        return in(Arrays.asList(values));
    }

    public Criteria in(Collection<?> values) {
        addCriterion(Operation.IN.getValue(), values);
        return this;
    }

    public Criteria nin(Object... values) {
        return nin(Arrays.asList(values));
    }

    public Criteria nin(Collection<?> values) {
        addCriterion(Operation.NIN.getValue(), values);
        return this;
    }

    /**
     * 执行自定义操作
     * -1.这种自定义的操作是全局的，非方法级别的
     * 使用{@link OperationRegistry#registerOperation(String, String, Operate)}注册自定义操作（全局共享的）
     * 
     * @param operation 操作名称
     * @param value     参数
     * @return          条件
     */
    public Criteria match(String operation, Object value) {
        addCriterion(operation, value);
        return this;
    }

    /**
     * 执行自定义函数
     * 
     * @param func      自定义操作
     * @return          条件
     */
    public Criteria func(Func func) {
        addCriterion(Operation.FUNC.getValue(), func);
        return this;
    }

    /**
     * 获取所有条件
     * 
     * @return  所有条件
     */
    public Map<String, Criterion> getAllCriterion() {
        Map<String, Criterion> allCriterionMap = new HashMap<>();
        for (Criteria criteria : criteriaChain) {
            Criterion criterion = allCriterionMap.getOrDefault(criteria.key, new Criterion());
            criterion.getCriterion().putAll(criteria.criterion);
            allCriterionMap.put(criteria.key, criterion);
        }
        return allCriterionMap;
    }

    /**
     * 添加一个条件
     * 
     * @param operation 操作
     * @param value     值
     */
    public void addCriterion(String operation, Object value) {
        this.criterion.put(operation, value);
    }

}