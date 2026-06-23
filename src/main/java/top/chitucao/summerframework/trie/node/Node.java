package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * trie节点
 * @param <K>   节点值类型
 *
 * @author chitucao
 */
public interface Node<K> {

    /**
     * 子节点映射表
     *
     * @return  子节点映射表
     */
    Map<K, Node<K>> children();

    /**
     * 设置子节点映射表
     *
     * @param children  子节点映射表
     */
    void setChildren(Map<K, Node<K>> children);

    /**
     * 子节点数量
     * 
     * @return  子节点数量
     */
    int childSize();

    /**
    * 子节点key集合
    *
    * @return  子节点key集合
    */
    Set<K> childKeySet();

    /**
     * 子节点
     *
     * @return  子节点key
     */
    Node<K> child(K key);

    /**
    * 添加子节点
     *
    * @param key        子节点key
    * @param child      子节点
    */
    Node<K> putChild(K key, Node<K> child);

    /**
    * 添加子节点
     *
    * @param key                   子节点key
    * @param childSupplier         子节点提供者
    * @return  添加子节点
    */
    Node<K> putChild(K key, Supplier<Node<K>> childSupplier);

    /**
     * 删除子节点
     *
     * @param key   字典key
     */
    void removeChild(K key);

    /**
     * 子树数据(记录)总数
     * -1.叶子(数据)节点为1，内部节点为其子树下所有数据条数之和；
     * -2.由 insert 与精确删除(normErase/erase(T))沿路径维护，用于 O(1) 的 getSize 与 count；
     *
     * @return 子树数据总数
     */
    int subtreeSize();

    /**
     * 原子地增减子树数据总数（并发插入/删除下安全，避免读改写丢更新）
     *
     * @param delta 增量(可为负)
     */
    void addSubtreeSize(int delta);
}