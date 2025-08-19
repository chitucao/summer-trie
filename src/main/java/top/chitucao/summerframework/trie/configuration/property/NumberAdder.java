package top.chitucao.summerframework.trie.configuration.property;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 数字自增管理
 *
 * @author chitucao
 */
public class NumberAdder {
    private final AtomicLong                                      id = new AtomicLong();

    private final Function<Long, Number>                          KEY_MAPPER;

    private static final Map<DictKeyType, Function<Long, Number>> KEY_MAPPER_MAP;

    static {
        KEY_MAPPER_MAP = new HashMap<>(DictKeyType.values().length);
        KEY_MAPPER_MAP.put(DictKeyType.BYTE, Number::byteValue);
        KEY_MAPPER_MAP.put(DictKeyType.SHORT, Number::shortValue);
        KEY_MAPPER_MAP.put(DictKeyType.INT, Number::intValue);
        KEY_MAPPER_MAP.put(DictKeyType.LONG, e -> e);
    }

    public NumberAdder(DictKeyType keyType) {
        this.KEY_MAPPER = KEY_MAPPER_MAP.get(keyType);
    }

    public long getId() {
        return id.get();
    }

    public void setId(long id) {
        this.id.set(id);
    }

    public Number nextKey() {
        return KEY_MAPPER.apply(id.incrementAndGet());
    }
}