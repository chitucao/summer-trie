package top.chitucao.summerframework.trie.node;

/**
 * 节点工厂
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: NodeFactory.java, v 0.1 2025-08-20 16:35 chitucao Exp $$
 */
public interface NodeFactory {

    /**
     * 是否使用了TreeMap这个数据结构
     * 
     * @return  是否使用了TreeMap这个数据结构
     */
    boolean isFromTreeMap();

    /**
     * 新建一个node节点
     * 
     * @return  新建的node节点
     * @param <K>   节点值类型
     */
    <K> Node<K> newNode();
}