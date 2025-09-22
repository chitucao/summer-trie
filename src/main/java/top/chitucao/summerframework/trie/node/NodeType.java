package top.chitucao.summerframework.trie.node;

/**
 * 节点类型
 * -1.不同的节点类型在增删改查方面有所区别，比如TreeMap在范围、比较查询时对比HashMap性能要高，适用于金额、日期等的索引；
 * -2.目前用了标准库中的两种类型，也可以扩展一下；
 * -3.如果trie有多线程并发使用的场景，建议使用线程安全的映射，如ConcurrentHashMap、ConcurrentSkipListMap；
 *
 * @author chitucao
 */
public enum NodeType {

                      /** 哈希映射 */
                      HASH_MAP,

                      /** 线程安全的哈希映射 */
                      CONCURRENT_HASH_MAP,

                      /** 树映射（支持排序操作） */
                      TREE_MAP,

                      /** 线程安全的调表映射（支持排序操作） */
                      CONCURRENT_SKIP_LIST_MAP,

                      /** 树映射（每个节点有一个名称属性） */
                      NAMED_TREE_MAP,

                      /** 哈希映射（每个节点有一个名称属性） */
                      NAMED_HASH_MAP;

}