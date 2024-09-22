package top.chitucao.summerframework.trie.nodemanager;

import java.util.LinkedList;

/**
 * 节点管理器
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: NodeManagerFactory.java, v 0.1 2024-08-08 上午10:50 chitucao Exp $$
 */
public interface NodeManagerFactory<T> {

    /**
     * 创建所有节点管理器
     * 
     * @return  所有节点管理器
     */
    LinkedList<NodeManager<T, ?>> createNodeManagers();

}