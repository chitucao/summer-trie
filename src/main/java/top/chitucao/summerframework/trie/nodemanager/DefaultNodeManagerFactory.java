package top.chitucao.summerframework.trie.nodemanager;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.Property;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;

/**
 * 默认节点管理器工厂实现
 *
 * @author chitucao
 */
public class DefaultNodeManagerFactory<T> implements NodeManagerFactory<T> {

    private final Configuration configuration;

    public DefaultNodeManagerFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public LinkedList<NodeManager<T, ?>> createNodeManagers() {
        Collection<Property> properties = configuration.getProperties();
        LinkedList<NodeManager<T, ?>> nodeManagers = new LinkedList<>();
        for (Property<T, ?> property : properties) {
            nodeManagers.add(new DefaultNodeManager<>(property));
        }
        nodeManagers.sort(Comparator.comparing(m -> m.property().level()));
        NodeManager<T, ?> tail = nodeManagers.get(0);
        for (int i = 1; i < nodeManagers.size(); i++) {
            DefaultNodeManager tail1 = (DefaultNodeManager) tail;
            DefaultNodeManager cur1 = (DefaultNodeManager) nodeManagers.get(i);
            tail1.setNext(cur1);
            cur1.setPrev(tail1);
            tail = cur1;
        }
        return nodeManagers;
    }

}