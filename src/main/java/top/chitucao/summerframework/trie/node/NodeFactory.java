package top.chitucao.summerframework.trie.node;

import java.util.Map;

/**
 * 节点工厂
 *
 * @author chitucao
 */
public interface NodeFactory {

    /**
     * 节点类型
     * 
     * @return  节点类型
     */
    NodeType nodeType();

    /**
     * 新建一个node节点
     * 
     * @return  新建的node节点
     * @param <K>   节点值类型
     */
    <K> Node<K> newNode();

    /**
     * 新建一个children节点
     * 
     * @return  新建的children节点
     * @param <K>   节点值类型
     */
    <K> Map<K, Node<K>> newChildren();

    /**
     * 新建一个children节点
     * 
     * @param childMap  子节点map
     * @return  新建的children节点
     * @param <K>   节点值类型
     */
    <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap);
}