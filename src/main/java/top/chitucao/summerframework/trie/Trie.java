package top.chitucao.summerframework.trie;

import java.util.List;
import java.util.Set;

import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.query.Aggregations;
import top.chitucao.summerframework.trie.query.Criteria;
import top.chitucao.summerframework.trie.query.ResultBuilder;
import top.chitucao.summerframework.trie.query.TreeNode;

/**
 * 字典树
 *
 * @author chitucao
 */
public interface Trie<T> {

    /**
     * 根节点
     * @return  根节点
     */
    Node<?> getRoot();

    /**
     * 深度
     *
     * @return 深度
     */
    int getDepth();

    /**
     * 数据总量
     * 返回的是最后一层的数据总量
     *
     * @return 数据总量
     */
    int getSize();

    /**
     * 插入数据
     *
     * @param t 数据
     */
    void insert(T t);

    /**
     * 删除数据
     *
     * @param criteria 删除条件
     * @return 删除的数据条数（快速删除模式下返回-1）
     */
    int erase(Criteria criteria);

    /**
     * 删除数据
     *
     * @param t 数据
     */
    void erase(T t);

    /**
     * 是否包含
     *
     * @param criteria 查询条件
     */
    boolean contains(Criteria criteria);

    /**
     * 是否包含某个数据
     *
     * @param t 数据
     * @return 是否包含
     */
    boolean contains(T t);

    /**
     * 原始数据查询
     * -1.返回的是叶子节点的数据，所以要求叶子节点必须存储数据，属于propertySearch查询最后一层时的特殊情况；
     *
     * @param criteria 查询条件
     * @return 数据列表
     */
    List<T> dataSearch(Criteria criteria);

    /**
     * 按层查询
     * -1.指定需要层级对应的字段，返回该层的所有数据，结果会做去重处理；
     *
     * @param criteria 查询条件
     * @param property 该层对应的字段
     * @param <R>      字段数据类型
     * @return 该层级字段列表
     */
    <R> List<R> propertySearch(Criteria criteria, String property);

    /**
     * 列表结构查询
     * -1.可以指定多个层级的字段，并将查询结果树平铺成一个列表后返回；
     * -2.支持对字段进行聚合；
     *
     * @param criteria      查询条件
     * @param aggregations  聚合条件
     * @param resultBuilder 结果构建器
     * @return 数据列表
     */
    <E> List<E> listSearch(Criteria criteria, Aggregations aggregations, ResultBuilder<E> resultBuilder);

    /**
     * 树结构查询
     * -1.指定查询条件和需要展示的字段，返回前缀树的子树视图
     * -2.只查询一个字段返回去重后list，多个字段返回hashmap，hashmap，是一个树结构；
     *
     * @param criteria   查询条件
     * @param properties 展示字段
     * @return 基于查询条件和展示字段构建的子树
     */
    Object treeSearch(Criteria criteria, Aggregations aggregations, String... properties);

    /**
     * 字典值查询
     * -1.返回某个字段的所有字典值，如果不指定dictKeys，则返回所有字典值
     * -2.最后一层放数据的时候，dictKey一般指定为数据id，这个方法很适合根据id拿到数据
     *
     * @param property  查询字段
     * @param dictKeys  字典key列表
     * @param <R>       字段数据类型
     * @return          该字段所有字典值
     */
    <R> Set<R> dictValues(String property, Object... dictKeys);

    /**
     * 查询为树结构
     * -1.返回查询结果为树结构
     *
     * @param criteria   查询条件
     * @param aggregations 聚合条件
     * @param properties 展示字段
     * @return 树结构
     */
    TreeNode queryAsTreeNode(Criteria criteria, Aggregations aggregations, String... properties);

    //    /**
    //     * 所有字段的字典大小
    //     * 可以在配合压缩数据的时候使用，一般是字典值较小的字段放在前面压缩效率更高，整体可以认为是一个梯形，下底是固定长度，所以上底较小面积最小
    //     *
    //     * @return  每个字段的字典大小  
    //     */
    //    Map<String, Integer> dictSizes();

    //    /**
    //     * 序列化
    //     *
    //     * @return 字节数组
    //     */
    //    byte[] serialize();
    //
    //    /**
    //     * 反序列化
    //     *
    //     * @param bytes 字节数组
    //     */
    //    void deserialize(byte[] bytes);
}