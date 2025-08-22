package top.chitucao.summerframework.trie.operation;

import java.util.HashMap;
import java.util.Map;

import top.chitucao.summerframework.trie.node.NodeType;

/**
 * 操作实现注册表
 * -1.提供了一些基本的操作方法 {@link OperationRegistry#registerBaseOperations()}
 * -2.也可注册自定义的操作方法 {@link OperationRegistry#registerOperation(String, String, Operate)}
 * 
 * @author chitucao(zhonggang.zhu)
 * @version Id: OperationRegistry.java, v 0.1 2025-08-11 14:29 chitucao Exp $$
 */
public class OperationRegistry {

    public static final OperationRegistry           INSTANCE              = new OperationRegistry();

    /**
     * 节点操作实现注册表
     */
    private final Map<String, Map<String, Operate>> nodeOperationRegistry = new HashMap<>();

    public OperationRegistry() {
        this.registerBaseOperations();
    }

    public static OperationRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一些基本的操作实现
     */
    public void registerBaseOperations() {
        // HashMap
        registerBaseHashMapOperations(NodeType.HASH_MAP.name());
        registerBaseHashMapOperations(NodeType.NAMED_HASH_MAP.name());

        // TreeMap
        registerBaseTreeMapOperations(NodeType.TREE_MAP.name());
        registerBaseTreeMapOperations(NodeType.NAMED_TREE_MAP.name());
    }

    /**
     * 注册节点为hashMap相关的操作
     *
     * @param nodeType          节点类型
     */
    public void registerBaseHashMapOperations(String nodeType) {
        registerOperation(nodeType, Operation.EQ.getValue(), BasicOperates.HASH_MAP_EQ_OP);
        registerOperation(nodeType, Operation.NE.getValue(), BasicOperates.HASH_MAP_NE_OP);
        registerOperation(nodeType, Operation.GT.getValue(), BasicOperates.HASH_MAP_GT_OP);
        registerOperation(nodeType, Operation.GTE.getValue(), BasicOperates.HASH_MAP_GTE_OP);
        registerOperation(nodeType, Operation.LT.getValue(), BasicOperates.HASH_MAP_LT_OP);
        registerOperation(nodeType, Operation.LTE.getValue(), BasicOperates.HASH_MAP_LTE_OP);
        registerOperation(nodeType, Operation.BETWEEN.getValue(), BasicOperates.HASH_MAP_BETWEEN_OP);
        registerOperation(nodeType, Operation.IN.getValue(), BasicOperates.HASH_MAP_IN_OP);
        registerOperation(nodeType, Operation.NIN.getValue(), BasicOperates.HASH_MAP_NIN_OP);
    }

    /**
     * 注册节点为treeMap相关的操作
     *
     * @param nodeType          节点类型
     */
    public void registerBaseTreeMapOperations(String nodeType) {
        registerOperation(nodeType, Operation.EQ.getValue(), BasicOperates.TREE_MAP_EQ_OP);
        registerOperation(nodeType, Operation.NE.getValue(), BasicOperates.TREE_MAP_NE_OP);
        registerOperation(nodeType, Operation.GT.getValue(), BasicOperates.TREE_MAP_GT_OP);
        registerOperation(nodeType, Operation.GTE.getValue(), BasicOperates.TREE_MAP_GTE_OP);
        registerOperation(nodeType, Operation.LT.getValue(), BasicOperates.TREE_MAP_LT_OP);
        registerOperation(nodeType, Operation.LTE.getValue(), BasicOperates.TREE_MAP_LTE_OP);
        registerOperation(nodeType, Operation.BETWEEN.getValue(), BasicOperates.TREE_MAP_BETWEEN_OP);
        registerOperation(nodeType, Operation.IN.getValue(), BasicOperates.TREE_MAP_IN_OP);
        registerOperation(nodeType, Operation.NIN.getValue(), BasicOperates.TREE_MAP_NIN_OP);
    }

    /**
     * 注册一个操作实现
     *
     * @param nodeType          节点类型
     * @param operationName     操作名称
     * @param operation         操作实现
     */
    public void registerOperation(NodeType nodeType, String operationName, Operate operation) {
        registerOperation(nodeType.name(), operationName, operation);
    }

    /**
     * 注册一个操作实现
     * 
     * @param nodeType          节点类型
     * @param operationName     操作名称
     * @param operation         操作实现
     */
    public void registerOperation(String nodeType, String operationName, Operate operation) {
        Map<String, Operate> nodeTypeOperationMap = nodeOperationRegistry.getOrDefault(nodeType, new HashMap<>());
        if (nodeTypeOperationMap.containsKey(operationName)) {
            throw new IllegalArgumentException("Node type " + nodeType + " already has an operation named " + operationName);
        }
        nodeTypeOperationMap.put(operationName, operation);
        nodeOperationRegistry.put(nodeType, nodeTypeOperationMap);
    }

    /**
     * 获取一个操作实现
     * 
     * @param nodeType          节点类型
     * @param operationName     操作名称
     * @return                  操作实现
     */
    public Operate getOperate(String nodeType, String operationName) {
        return nodeOperationRegistry.get(nodeType).get(operationName);
    }

}