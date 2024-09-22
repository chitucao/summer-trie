package top.chitucao.summerframework.trie.query;

import lombok.Data;

/**
 * Criterion
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Criterion.java, v 0.1 2024-08-08 下午3:33 chitucao Exp $$
 */
@Data
public class Criterion {

    private String property;

    private Condition condition;

    private Object value;

    private Object secondValue;

    protected Criterion(Condition condition, Object value, String property) {
        this(condition, value, null, property);
    }

    protected Criterion(Condition condition, Object value, Object secondValue, String property) {
        this.property = property;
        this.condition = condition;
        this.value = value;
        this.secondValue = secondValue;
    }

}