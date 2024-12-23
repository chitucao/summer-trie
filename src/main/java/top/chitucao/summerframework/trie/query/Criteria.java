package top.chitucao.summerframework.trie.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 查询条件
 *
 * @author chitucao
 */
public class Criteria {

    protected List<Criterion> criterionList;

    public Criteria() {
        this.criterionList = new ArrayList<>();
    }

    public Criteria addCriterion(Condition condition, Object value1, Object value2, String property) {
        criterionList.add(new Criterion(condition, value1, value2, property));
        return this;
    }

    public Criteria addCriterion(Condition condition, Object value, String property) {
        criterionList.add(new Criterion(condition, value, property));
        return this;
    }

    public Map<String, Criterion> getCriterionMap() {
        return criterionList.stream().collect(Collectors.toMap(Criterion::getProperty, Function.identity()));
    }

    public List<Criterion> getCriterionList() {
        return criterionList;
    }
}