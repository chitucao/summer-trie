package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.node.NodeType;
import lombok.Getter;

/**
 * 简单节点属性
 * -1.这里的字典key是原子自增的，每纳入一个新的字段值，字典key会增加一下，不需要指定映射关系；
 * -2.由于字典key没有表达顺序关系，所以仅支持等值匹配，不支持比较、范围查询；
 * -3.根据字段值的范围给字典key选择合适数据类型可以节省一部分字典空间，比如字段所有不同的值不超过128，使用byte类型也就足够了；
 *
 * @author chitucao
 */
@Getter
public class SimpleProperty<T, R> extends AbstractProperty<T, R> {

    protected NumberAdder dictKeyAdder;

    public SimpleProperty(String name) {
        super(name);
        this.dictKeyAdder = new NumberAdder();
    }

    public SimpleProperty(String name, DictKeyType dictKeyType) {
        super(name);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    public SimpleProperty(String name, NodeType nodeType) {
        super(name, nodeType);
        this.dictKeyAdder = new NumberAdder();
    }

    public SimpleProperty(String name, NodeType nodeType, DictKeyType dictKeyType) {
        super(name, nodeType);
        this.dictKeyAdder = new NumberAdder(dictKeyType);
    }

    @Override
    public Number mappingDictKey(R r) {
        if (dict.containsDictValue(r)) {
            return dict.getDictKey(r);
        }
        return dictKeyAdder.nextKey();
    }

    @Override
    public Number getDictKey(R r) {
        return dict.getDictKey(r);
    }
}