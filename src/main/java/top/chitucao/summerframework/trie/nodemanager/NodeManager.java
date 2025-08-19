package top.chitucao.summerframework.trie.nodemanager;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.query.Aggregation;
import top.chitucao.summerframework.trie.query.Criterion;

/**
 * 节点管理器
 * -1.主要是管理节点的创建销毁，节点和字典的映射，以及父子节点之间的关系；
 * -2.是一个链式结构，每个对应一层，管理一个字段；
 *
 * @author chitucao
 */
public interface NodeManager {

    /**
     * 前一个节点管理器
     * 
     * @return  前一个节点管理器
     */
    NodeManager prev();

    /**
     * 后一个节点管理器
     * 
     * @return  后一个节点管理器
     */
    NodeManager next();

    /**
     * 节点属性
     * 
     * @return  节点属性
     */
    <T, R, K> Property<T, R, K> property();

    /**
     * 节点值转换成字段值
     * 
     * @param dictKeys  多个字典key
     * @return          字段值
     */
    <R, K> Stream<R> mappingDictValues(Set<K> dictKeys);

    /**
     * 数据转换成字典值，再转换成字典key
     * 
     * @param t 数据
     * @return  字典key
     */
    <T, R, K> K mappingDictKey(T t);

    /**
     * 创建新节点
     * 
     * @return  新节点
     */
    <K> Node<K> createNewNode();

    /**
     * 添加子节点
     * 
     * @param parent    父节点
     * @param t         实体
     * @return          子节点
     */
    <T, R, K> Node<K> addChildNode(Node<K> parent, T t);

    /**
     * 删除子节点
     * 
     * @param parent    父节点
     * @param t         实体
     */
    <T, R, K> void removeChildNode(Node<K> parent, T t);

    /**
     * 删除子节点
     * 
     * @param parent    父节点
     * @param dictKey   字典key
     */
    <K> void removeChild(Node<K> parent, K dictKey);

    /**
     * 查询子节点
     * 
     * @param parent    父节点
     * @param t         实体
     * @return          子节点
     */
    <T, R, K> Node<K> findChildNode(Node<K> parent, T t);

    /**
    * 查询并聚合
    *
    * @param cur           当前节点
    * @param criterion     查询条件
    * @param aggregation   聚合条件
    * @return              结果
    */
    <K> Map<K, Node<K>> searchAndAgg(Node<K> cur, Criterion criterion, Aggregation aggregation);

    /**
     * 根据条件查询
     *
     * @param cur           当前节点
     * @param criterion     查询条件
     * @return              满足条件的元素
     */
    <K> Map<K, Node<K>> search(Node<K> cur, Criterion criterion);

    /**
     * 是否满足条件
     * 
     * @param cur           当前节点
     * @param criterion     条件
     * @return              是否满足
     */
    <K> boolean contains(Node<K> cur, Criterion criterion);

    /**
     * 根据条件过滤节点
     * 
     * @param cur           当前节点
     * @param criterion     条件
     */
    <K> void slice(Node<K> cur, Criterion criterion);

    /**
    * 根据条件删除节点
    * 
    * @param cur           当前节点
    * @param criterion     条件
    */
    <K> void remove(Node<K> cur, Criterion criterion);

}