package top.chitucao.summerframework.trie.query;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggrega
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Aggrega.java, v 0.1 2024-09-18 13:51 chitucao Exp $$
 */
public class Aggregations {

    @Getter
    protected Map<String, Aggregation> aggregationMap;

    public Aggregations() {
        this.aggregationMap = new HashMap<>();
    }

    public Aggregations addAggregation(Aggregation aggregation, String property) {
        aggregationMap.put(property, aggregation);
        return this;
    }

}