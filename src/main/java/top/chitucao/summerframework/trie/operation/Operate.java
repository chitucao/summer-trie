package top.chitucao.summerframework.trie.operation;

import java.util.Map;

import top.chitucao.summerframework.trie.node.Node;

/**
 * 操作实现
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Operate.java, v 0.1 2025-08-11 14:21 chitucao Exp $$
 */
public interface Operate {

    /**
     * 查询符合条件的子节点
     * 
     * @param  childMap         子节点
     * @param  key              匹配的子节点键
     * @return                  符合条件的子节点
     */
    Map<Number, Node> query(Map<Number, Node> childMap, Object key);

}