package top.chitucao.summerframework.trie.operation;

import java.util.Map;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;

/**
 * 自定义函数
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Func.java, v 0.1 2025-08-15 13:51 chitucao Exp $$
 */
public interface Func {

    /**
     * 查询符合条件的子节点
     *
     * @param  childMap         子节点
     * @param  property         属性
     * @return                  符合条件的子节点
     */
     Map<?, Node<?>> apply(Map<?, Node<?>> childMap, Property<?, ?, ?> property);

}