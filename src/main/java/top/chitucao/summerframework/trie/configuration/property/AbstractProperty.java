package top.chitucao.summerframework.trie.configuration.property;

import java.util.Collections;
import java.util.function.Function;

import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 抽象属性
 *
 * @author chitucao
 */
public abstract class AbstractProperty<T, R, K> implements Property<T, R, K> {

    /** 属性唯一名称 */
    protected final String   name;

    /** 层级 */
    protected int            level;

    /** 节点类型 */
    protected final NodeType nodeType;

    /** 指定实体和字段值的映射关系 */
    private Function<T, R>   propertyMapper;

    /** 是否是用到字典的节点属性 */
    private final boolean    isDictProperty;

    public AbstractProperty(String name, NodeType nodeType, boolean isDictProperty) {
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
    public NodeType nodeType() {
        return nodeType;
    }

    @Override
    public R mappingFieldValue(T t) {
        return propertyMapper.apply(t);
    }

    @Override
    public boolean isDictProperty() {
        return isDictProperty;
    }

    public void setPropertyMapper(Function<T, R> propertyMapper) {
        this.propertyMapper = propertyMapper;
    }
}