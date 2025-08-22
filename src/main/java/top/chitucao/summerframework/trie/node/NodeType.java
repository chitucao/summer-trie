package top.chitucao.summerframework.trie.node;

/**
 * 节点类型
 * -1.不同的节点类型在增删改查方面有所区别，比如TreeMap在范围、比较查询时对比HashMap性能要高，适用于金额、日期等的索引；
 * -2.目前用了标准库中的两种类型，也可以扩展一下；
 *
 * @author chitucao
 */
public enum NodeType {

                      /** 树映射 */
                      TREE_MAP,

                      /** 哈希映射 */
                      HASH_MAP,

                      /** 树映射（每个节点有一个名称属性） */
                      NAMED_TREE_MAP,

                      /** 哈希映射（每个节点有一个名称属性） */
                      NAMED_HASH_MAP;

}