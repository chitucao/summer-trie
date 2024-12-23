package top.chitucao.summerframework.trie.configuration;

import java.util.*;

import top.chitucao.summerframework.trie.configuration.property.Property;

/**
 * 配置
 *
 * @author chitucao
 */
public class Configuration {

    /** 节点属性列表 */
    @SuppressWarnings("rawtypes")
    private final Map<String, Property> properties = new LinkedHashMap<>();

    /** 临时变量，记录下一个层级 */
    private int                         level;

    /** 最后一个节点属性 */
    @SuppressWarnings("rawtypes")
    private Property                    lastProperty;

    /** 快速删除效率比较高，但是不会维护字典的counter，不支持删除字典数据，并且getSize()方法复杂度高点,适用于字典树每次都是重建的场景 */
    private boolean                     useFastErase;

    /** 叶子节点是否存储数据 */
    private boolean                     leafNodeAsDataNode;

    public Configuration() {
        this.level = 0;
        this.useFastErase = false;
    }

    @SuppressWarnings("rawtypes")
    public Collection<Property> getProperties() {
        return properties.values();
    }

    @SuppressWarnings("rawtypes")
    public Property getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @SuppressWarnings("rawtypes")
    public Property getLastProperty() {
        return lastProperty;
    }

    @SuppressWarnings("rawtypes")
    public void addProperty(Property property) {
        property.setLevel(level++);
        lastProperty = property;
        properties.put(property.name(), property);
    }

    public void sortProperties(Comparator<String> comparator) {
        //noinspection rawtypes,unchecked
        List<Map.Entry<String, Property>> entryList = new ArrayList(properties.entrySet());
        entryList.sort((o1, o2) -> comparator.compare(o1.getKey(), o2.getKey()));
        properties.clear();
        this.level = 0;
        //noinspection rawtypes
        for (Map.Entry<String, Property> entry : entryList) {
            entry.getValue().setLevel(level++);
            properties.put(entry.getKey(), entry.getValue());
        }
    }

    public boolean isUseFastErase() {
        return useFastErase;
    }

    public void setUseFastErase(boolean useFastErase) {
        this.useFastErase = useFastErase;
    }

    public boolean isLeafNodeAsDataNode() {
        return leafNodeAsDataNode;
    }

    public void setLeafNodeAsDataNode(boolean leafNodeAsDataNode) {
        this.leafNodeAsDataNode = leafNodeAsDataNode;
    }
}