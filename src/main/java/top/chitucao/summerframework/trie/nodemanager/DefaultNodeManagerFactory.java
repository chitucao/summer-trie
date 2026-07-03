package top.chitucao.summerframework.trie.nodemanager;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.Property;

/**
 * 默认节点管理器工厂实现
 *
 * @author chitucao
 */
public class DefaultNodeManagerFactory implements NodeManagerFactory {

    private final Configuration configuration;

    public DefaultNodeManagerFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @SuppressWarnings({ "rawtypes"})
    @Override
    public LinkedList<NodeManager> createNodeManagers() {
        Collection<Property> properties = configuration.getProperties();
        LinkedList<NodeManager> nodeManagers = new LinkedList<>();
        for (Property<?, ?, ?> property : properties) {
            nodeManagers.add(new DefaultNodeManager(property));
        }
        nodeManagers.sort(Comparator.comparing(m -> m.property().level()));
        NodeManager tail = nodeManagers.get(0);
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