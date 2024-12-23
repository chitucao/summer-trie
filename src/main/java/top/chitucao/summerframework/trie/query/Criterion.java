package top.chitucao.summerframework.trie.query;

/**
 * Criterion
 *
 * @author chitucao
 */
public class Criterion {

    private final String    property;

    private final Condition condition;

    private final Object    value;

    private final Object    secondValue;

    protected Criterion(Condition condition, Object value, String property) {
        this(condition, value, null, property);
    }

    protected Criterion(Condition condition, Object value, Object secondValue, String property) {
        this.property = property;
        this.condition = condition;
        this.value = value;
        this.secondValue = secondValue;
    }

    public String getProperty() {
        return property;
    }

    public Condition getCondition() {
        return condition;
    }

    public Object getValue() {
        return value;
    }

    public Object getSecondValue() {
        return secondValue;
    }

    @Override
    public String toString() {
        return "Criterion{" + "property='" + property + '\'' + ", condition=" + condition + ", value=" + value + ", secondValue=" + secondValue + '}';
    }
}