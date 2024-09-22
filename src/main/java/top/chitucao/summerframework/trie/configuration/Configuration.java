package top.chitucao.summerframework.trie.configuration;

import com.google.common.collect.Lists;
import top.chitucao.summerframework.trie.configuration.property.Property;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 配置
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Configuration.java, v 0.1 2024-09-11 16:12 chitucao Exp $$
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

    /** 是否使用快速删除 如果删除操作不频繁，可以关掉，这样getSize()方法会快点 */
    @Getter
    @Setter
    private boolean                     useFastErase;

    /** 叶子节点是否存储数据 */
    @Getter
    @Setter
    private boolean                     leafNodeAsDataNode;

    public Configuration() {
        this.level = 0;
        this.useFastErase = true;
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
        //noinspection rawtypes
        List<Map.Entry<String, Property>> entryList = Lists.newArrayList(properties.entrySet());
        entryList.sort((o1, o2) -> comparator.compare(o1.getKey(), o2.getKey()));
        properties.clear();
        this.level = 0;
        for (Map.Entry<String, Property> entry : entryList) {
            entry.getValue().setLevel(level++);
            properties.put(entry.getKey(), entry.getValue());
        }
    }

}