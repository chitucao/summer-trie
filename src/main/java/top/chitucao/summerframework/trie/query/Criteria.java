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

    /**
     * 未设置 limit 的占位值（表示不限制结果数量）
     */
    public static final int                     NO_LIMIT  = -1;

    private String                              key;
    private final LinkedHashMap<String, Object> criterion = new LinkedHashMap<>();
    private final List<Criteria>                criteriaChain;

    /**
     * 结果数量上限（limit 下推）
     * -1.NO_LIMIT 表示不限制；
     * -2.是整条查询级别的属性，不绑定具体字段；查询会在采集到 limit 条结果后提前终止遍历，避免全量物化；
     */
    private int                                 limit     = NO_LIMIT;

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
     * 设置结果数量上限（limit 下推）
     * -1.作用于整条查询，查询采集到 limit 条结果后即停止遍历，避免全量物化；
     * -2.链式调用即可，例如 {@code Criteria.where("city").eq("BJ").limit(10)}；
     * -3.对 dataSearch / propertySearch / listSearch / treeSearch / queryAsTreeNode 生效；
     *
     * @param limit 结果上限，必须为正整数
     * @return      条件本身（链式）
     */
    public Criteria limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, but was " + limit);
        }
        this.limit = limit;
        return this;
    }

    /**
     * 获取结果数量上限
     * -1.先看当前条件节点，再扫描整条 criteriaChain，使 limit 在链式任意节点设置都能被读到；
     *
     * @return 结果上限，未设置时返回 {@link #NO_LIMIT}
     */
    public int getLimit() {
        if (this.limit > 0) {
            return this.limit;
        }
        for (Criteria criteria : criteriaChain) {
            if (criteria.limit > 0) {
                return criteria.limit;
            }
        }
        return NO_LIMIT;
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