package top.chitucao.summerframework.trie.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 结果数据构建
 * @param <E> 结果数据类型
 *
 * @author chitucao
 */
public class ResultBuilder<E> {

    /** 具体字段的setter方法 */
    @SuppressWarnings("rawtypes")
    private final Map<String, BiConsumer> setterMap;
    /** 构造方法 */
    private final Supplier<E>             supplier;

    public ResultBuilder(Supplier<E> supplier) {
        setterMap = new HashMap<>();
        this.supplier = supplier;
    }

    @SuppressWarnings("UnusedReturnValue")
    public <R> ResultBuilder<E> addSetter(String property, BiConsumer<E, R> setter) {
        setterMap.put(property, setter);
        return this;
    }

    @SuppressWarnings("rawtypes")
    public Map<String, BiConsumer> getSetterMap() {
        return setterMap;
    }

    public Supplier<E> getSupplier() {
        return supplier;
    }
}