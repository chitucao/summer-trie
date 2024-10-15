package top.chitucao.summerframework.trie.nodemanager;

import java.util.LinkedList;

/**
 * 节点管理器
 *
 * @author chitucao
 */
public interface NodeManagerFactory<T> {

    /**
     * 创建所有节点管理器
     * 
     * @return  所有节点管理器
     */
    LinkedList<NodeManager<T, ?>> createNodeManagers();

}