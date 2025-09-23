package top.chitucao.summerframework.trie.node;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import top.chitucao.summerframework.trie.node.extra.NamedHashMapNode;
import top.chitucao.summerframework.trie.node.extra.NamedTreeMapNode;

/**
 * 节点工厂实现注册表
 *
 * @author chitucao
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
        registerNodeFactory(new NodeFactory() {
            /**
            * 节点类型
            *
            * @return 节点类型
            */
            @Override
            public NodeType nodeType() {
                return NodeType.HASH_MAP;
            }

            /**
            * 新建一个node节点
            * 
            * @return  新建的node节点
            * @param <K>   节点值类型
            */
            @Override
            public <K> Node<K> newNode() {
                return new HashMapNode<>();
            }

            /**
             * 新建一个children节点
             *
             * @return 新建的children节点
             */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                return new HashMap<>();
            }

            /**
             * 新建一个children节点
             *
             * @param childMap 子节点map
             * @return 新建的children节点
             */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new HashMap<>(childMap);
            }
        });

        registerNodeFactory(new NodeFactory() {
            /**
             * 节点类型
             *
             * @return 节点类型
             */
            @Override
            public NodeType nodeType() {
                return NodeType.TREE_MAP;
            }

            /**
            * 新建一个node节点
            * 
            * @return  新建的node节点
            * @param <K>   节点值类型
            */
            @Override
            public <K> Node<K> newNode() {
                return new TreeMapNode<>();
            }

            /**
            * 新建一个children节点
            *
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                //noinspection SortedCollectionWithNonComparableKeys
                return new TreeMap<>();
            }

            /**
            * 新建一个children节点
            *
            * @param childMap 子节点map
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new TreeMap<>(childMap);
            }
        });

        registerNodeFactory(new NodeFactory() {
            /**
             * 节点类型
             *
             * @return 节点类型
             */
            @Override
            public NodeType nodeType() {
                return NodeType.CONCURRENT_HASH_MAP;
            }

            @Override
            public <K> Node<K> newNode() {
                return new ConcurrentHashMapNode<>();
            }

            /**
            * 新建一个children节点
            *
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                return new ConcurrentHashMap<>();
            }

            /**
            * 新建一个children节点
            *
            * @param childMap 子节点map
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new ConcurrentHashMap<>(childMap);
            }
        });

        registerNodeFactory(new NodeFactory() {
            /**
             * 节点类型
             *
             * @return 节点类型
             */
            @Override
            public NodeType nodeType() {
                return NodeType.CONCURRENT_SKIP_LIST_MAP;
            }

            @Override
            public <K> Node<K> newNode() {
                return new ConcurrentSkipListMapNode<>();
            }

            /**
            * 新建一个children节点
            *
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                //noinspection SortedCollectionWithNonComparableKeys
                return new ConcurrentSkipListMap<>();
            }

            /**
            * 新建一个children节点
            *
            * @param childMap 子节点map
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new ConcurrentSkipListMap<>(childMap);
            }
        });

        registerNodeFactory(new NodeFactory() {
            /**
            * 节点类型
            *
            * @return 节点类型
            */
            @Override
            public NodeType nodeType() {
                return NodeType.NAMED_HASH_MAP;
            }

            @Override
            public <K> Node<K> newNode() {
                return new NamedHashMapNode<>();
            }

            /**
            * 新建一个children节点
            *
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                return new HashMap<>();
            }

            /**
            * 新建一个children节点
            *
            * @param childMap 子节点map
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new HashMap<>(childMap);
            }
        });

        registerNodeFactory(new NodeFactory() {
            /**
             * 节点类型
             *
             * @return 节点类型
             */
            @Override
            public NodeType nodeType() {
                return NodeType.NAMED_TREE_MAP;
            }

            @Override
            public <K> Node<K> newNode() {
                return new NamedTreeMapNode<>();
            }

            /**
            * 新建一个children节点
            *
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren() {
                //noinspection SortedCollectionWithNonComparableKeys
                return new TreeMap<>();
            }

            /**
            * 新建一个children节点
            *
            * @param childMap 子节点map
            * @return 新建的children节点
            */
            @Override
            public <K> Map<K, Node<K>> newChildren(Map<K, Node<K>> childMap) {
                if (childMap == null || childMap.isEmpty()) {
                    return newChildren();
                }
                return new TreeMap<>(childMap);
            }
        });
    }

    /**
     * 注册节点工厂
     *
     * @param nodeFactory       节点工厂
     */
    public void registerNodeFactory(NodeFactory nodeFactory) {
        registry.put(nodeFactory.nodeType().name(), nodeFactory);
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