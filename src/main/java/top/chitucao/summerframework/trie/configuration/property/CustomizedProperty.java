package top.chitucao.summerframework.trie.configuration.property;

import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.NodeType;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

/**
 * 自定义节点属性
 * -1.这里的自定义指的是可以手动指定字典key和字段值的映射关系，合理的映射关系将支持范围和比较查询；
 * -2.比如金额，数字，日期等可以转换成一个对应的的数字；
 * -3.范围和比较查询时推荐和TreeMapNode结合使用，查询时间复杂度会降低到logn；
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: CustomizedProperty.java, v 0.1 2024-08-09 下午2:27 chitucao Exp $$
 */
@Getter
@Setter
public class CustomizedProperty<T, R> extends AbstractProperty<T, R> {

    /** 指定如何将字段值映射成字典key */
    private Function<R, Number> dictKeyMapper;

    public CustomizedProperty(String name) {
        super(name);
    }

    public CustomizedProperty(String name, NodeType nodeType) {
        super(name, nodeType);
    }

    public CustomizedProperty(String name, Dict<R> dict) {
        super(name, dict);
    }

    public CustomizedProperty(String name, NodeType nodeType, Dict<R> dict) {
        super(name, nodeType, dict);
    }

    @Override
    public Number mappingDictKey(R r) {
        return dictKeyMapper.apply(r);
    }

    @Override
    public Number getDictKey(R r) {
        return dictKeyMapper.apply(r);
    }
}