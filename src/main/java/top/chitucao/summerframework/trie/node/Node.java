package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 节点
 *
 * @author chitucao
 */
public interface Node {
    /**
     * 子节点数量
     * 
     * @return  子节点数量
     */
    int getSize();

    /**
     * 所有子节点映射
     * 
     * @return  所有子节点映射
     */
    Map<Number, Node> childMap();

    /**
    * 所有字典key
    *
    * @return  所有字典key
    */
    Set<Number> keys();

    /**
    * 添加子节点
    * 
    * @return  添加子节点
    */
    Node addChild(Number key, Node child);

    /**
    * 添加子节点
    * 延迟了子节点的创建，因为有些情况下并不需要创建子节点，没必要提前创建
    * 
    * @return  添加子节点
    */
    Node addChild(Number key, Supplier<Node> childSupplier);

    /**
    * 设置子节点
    * 
    * @param childMap  子节点映射
    */
    void setChild(Map<Number, Node> childMap);

    /**
    * 根据字典key获取
    * 
    * @return  字典key
    */
    Node getChild(Number key);

    /**
     * 根据字典key删除子节点
     * 
     * @param key   字典key
     */
    void removeChild(Number key);

}