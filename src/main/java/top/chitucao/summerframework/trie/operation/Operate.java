package top.chitucao.summerframework.trie.operation;

import java.util.Map;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;

/**
 * 操作实现
 *
 * @author chitucao
 */
public interface Operate {

    /**
     * 查询符合条件的子节点
     * 
     * @param  childMap         子节点
     * @param  property         属性
     * @param  value            匹配的值
     * @return                  符合条件的子节点
     */
    Map<?, Node<?>> query(Map<?, Node<?>> childMap, Property<?, ?, ?> property, Object value);

}