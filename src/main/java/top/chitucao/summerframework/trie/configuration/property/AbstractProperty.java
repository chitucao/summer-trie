package top.chitucao.summerframework.trie.configuration.property;

import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.dict.HashMapDict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 抽象属性
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: AbstractProperty.java, v 0.1 2024-08-09 下午4:06 chitucao Exp $$
 */
public abstract class AbstractProperty<T, R> implements Property<T, R> {

    /** 属性唯一名称 */
    protected final String   name;

    /** 层级 */
    protected int            level;

    /** 是否是叶子节点 */
    protected boolean        isLeaf;

    /** 节点类型 */
    @Getter
    protected final NodeType nodeType;

    /** 字段字典 */
    protected final Dict<R>  dict;

    /** 指定实体和字段值的映射关系 */
    @Setter
    private Function<T, R>   propertyMapper;

    public AbstractProperty(String name) {
        this.name = name;
        this.nodeType = NodeType.HASH_MAP;
        this.dict = new HashMapDict<>();
    }

    public AbstractProperty(String name, NodeType nodeType) {
        this.name = name;
        this.nodeType = nodeType;
        this.dict = new HashMapDict<>();
    }

    public AbstractProperty(String name, Dict<R> dict) {
        this.name = name;
        this.nodeType = NodeType.HASH_MAP;
        this.dict = dict;
    }

    public AbstractProperty(String name, NodeType nodeType, Dict<R> dict) {
        this.name = name;
        this.nodeType = nodeType;
        this.dict = dict;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int level() {
        return this.level;
    }

    // fixme
    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public NodeType nodeType() {
        return nodeType;
    }

    @Override
    public R mappingValue(T t) {
        return propertyMapper.apply(t);
    }

    @Override
    public Dict<R> dict() {
        return dict;
    }
}