package top.chitucao.summerframework.trie.nodemanager;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
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
public interface NodeManager<T, R> {

    /**
     * 前一个节点管理器
     * 
     * @return  前一个节点管理器
     */
    NodeManager<T, R> prev();

    /**
     * 后一个节点管理器
     * 
     * @return  后一个节点管理器
     */
    NodeManager<T, R> next();

    /**
     * 节点属性
     * 
     * @return  节点属性
     */
    Property<T, R> property();

    /**
     * 节点值转换成字段值
     * 
     * @param dictKeys  多个字典key
     * @return          字段值
     */
    Stream<R> mappingDictValues(Set<Number> dictKeys);

    /**
     * 数据转换成字典值，再转换成字典key
     * 
     * @param t 数据
     * @return  字典key
     */
    Number mappingDictKey(T t);

    /**
     * 创建新节点
     * 
     * @return  新节点
     */
    Node createNewNode();

    /**
     * 创建新节点，根据提供的子节点映射初始化
     * 
     * @param childMap  子节点映射
     * @return          新节点
     */
    Node createNewNode(Map<Number, Node> childMap);

    /**
    * 创建空值节点
    * 一般用于最后一层
    * 
    * @param keys      多个字典key
    * @return          空值节点
    */
    Node createEmptyValueNode(Stream<Number> keys);

    /**
     * 添加子节点
     * 
     * @param parent    父节点
     * @param t         实体
     * @return          子节点
     */
    Node addChildNode(Node parent, T t);

    /**
     * 添加子节点
     * 有的情况下，并不是按照顺序创建下一层节点，需要手动指定下一层节点的创建方式
     *
     * @param parent                父节点
     * @param val                   节点值
     * @param childNodeSupplier     指定子节点的创建方式
     * @return                      子节点
     */
    Node addChildNode(Node parent, Object val, Supplier<Node> childNodeSupplier);

    /**
     * 删除子节点
     * 
     * @param parent    父节点
     * @param t         实体
     */
    void removeChildNode(Node parent, T t);

    /**
     * 查询子节点
     * 
     * @param parent    父节点
     * @param t         实体
     * @return          子节点
     */
    Node findChildNode(Node parent, T t);

    /**
    * 查询并聚合
    *
    * @param cur           当前节点
    * @param criterion     查询条件
    * @param aggregation   聚合条件
    * @return              结果
    */
    Map<Number, Node> searchAndAgg(Node cur, Criterion criterion, Aggregation aggregation);

    /**
     * 根据条件查询
     *
     * @param cur           当前节点
     * @param criterion     查询条件
     * @return              满足条件的元素
     */
    Map<Number, Node> search(Node cur, Criterion criterion);

    /**
     * 是否满足条件
     * 
     * @param cur           当前节点
     * @param criterion     条件
     * @return              是否满足
     */
    boolean contains(Node cur, Criterion criterion);

    /**
     * 根据条件过滤节点
     * 
     * @param cur           当前节点
     * @param criterion     条件
     */
    void slice(Node cur, Criterion criterion);

    /**
    * 根据条件删除节点
    * 
    * @param cur           当前节点
    * @param criterion     条件
    */
    void remove(Node cur, Criterion criterion);

}