package top.chitucao.summerframework.trie.node;

import java.util.HashMap;
import java.util.Map;

import top.chitucao.summerframework.trie.node.extra.NamedHashMapNode;
import top.chitucao.summerframework.trie.node.extra.NamedTreeMapNode;

/**
 * 节点工厂实现注册表
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: NodeFactoryRegistry.java, v 0.1 2025-08-20 16:32 chitucao Exp $$
 */
public class NodeFactoryRegistry {

    public static final NodeFactoryRegistry INSTANCE = new NodeFactoryRegistry();

    /**
     * 节点工厂实现注册表
     */
    private final Map<String, NodeFactory>  registry = new HashMap<>();

    public NodeFactoryRegistry() {
        this.registerBaseNodeFactories();
    }

    public static NodeFactoryRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一些基本的节点工厂
     */
    private void registerBaseNodeFactories() {
        registerNodeFactory(NodeType.HASH_MAP, new NodeFactory() {
            @Override
            public boolean isFromTreeMap() {
                return false;
            }

            @Override
            public <K> Node<K> newNode() {
                return new HashMapNode<>();
            }
        });

        registerNodeFactory(NodeType.TREE_MAP, new NodeFactory() {
            @Override
            public boolean isFromTreeMap() {
                return true;
            }

            @Override
            public <K> Node<K> newNode() {
                return new TreeMapNode<>();
            }
        });

        registerNodeFactory(NodeType.NAMED_HASH_MAP, new NodeFactory() {
            @Override
            public boolean isFromTreeMap() {
                return false;
            }

            @Override
            public <K> Node<K> newNode() {
                return new NamedHashMapNode<>();
            }
        });

        registerNodeFactory(NodeType.NAMED_TREE_MAP, new NodeFactory() {
            @Override
            public boolean isFromTreeMap() {
                return true;
            }

            @Override
            public <K> Node<K> newNode() {
                return new NamedTreeMapNode<>();
            }
        });
    }

    /**
     * 注册节点工厂
     *
     * @param nodeType          节点类型
     * @param nodeFactory       节点工厂
     */
    public void registerNodeFactory(NodeType nodeType, NodeFactory nodeFactory) {
        registry.put(nodeType.name(), nodeFactory);
    }

    /**
     * 注册节点工厂
     * 提供给用户扩展
     *
     * @param nodeType          节点类型
     * @param nodeFactory       节点工厂
     */
    public void registerNodeFactory(String nodeType, NodeFactory nodeFactory) {
        registry.put(nodeType, nodeFactory);
    }

    /**
     * 获取节点工厂
     *
     * @param nodeType          节点类型
     * @return                  节点工厂
     */
    public NodeFactory getNodeFactory(String nodeType) {
        return registry.get(nodeType);
    }

}