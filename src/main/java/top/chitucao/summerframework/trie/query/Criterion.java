package top.chitucao.summerframework.trie.query;

import java.util.LinkedHashMap;

/**
 * 条件
 *
 * @author chitucao
 */
public class Criterion {

    // key -> operation
    // value -> match value
    private final LinkedHashMap<String, Object> criterion = new LinkedHashMap<>();

    public LinkedHashMap<String, Object> getCriterion() {
        return criterion;
    }

}