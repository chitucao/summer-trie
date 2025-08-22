package top.chitucao.summerframework.trie.configuration.property;

import java.util.function.Function;

/**
 * 抽象属性
 *
 * @author chitucao
 */
public abstract class AbstractProperty<T, R, K> implements Property<T, R, K> {

    /** 属性唯一名称 */
    protected final String name;

    /** 层级 */
    protected int          level;

    /** 节点类型 */
    protected final String nodeType;

    /** 指定实体和字段值的映射关系 */
    private Function<T, R> object2FieldMapper;

    /** 是否是用到字典的节点属性 */
    private final boolean  isDictProperty;

    public AbstractProperty(String name, String nodeType, boolean isDictProperty) {
        this.name = name;
        this.nodeType = nodeType;
        this.isDictProperty = isDictProperty;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int level() {
        return this.level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String nodeType() {
        return nodeType;
    }

    @Override
    public R mappingFieldValue(T t) {
        return object2FieldMapper.apply(t);
    }

    @Override
    public boolean isDictProperty() {
        return isDictProperty;
    }

    public void setObject2FieldMapper(Function<T, R> object2FieldMapper) {
        this.object2FieldMapper = object2FieldMapper;
    }
}