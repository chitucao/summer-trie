package top.chitucao.summerframework.trie.query;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggrega
 *
 * @author chitucao
 */
public class Aggregations {

    protected Map<String, Aggregation> aggregationMap;

    public Aggregations() {
        this.aggregationMap = new HashMap<>();
    }

    public Aggregations addAggregation(Aggregation aggregation, String property) {
        aggregationMap.put(property, aggregation);
        return this;
    }

    public Map<String, Aggregation> getAggregationMap() {
        return aggregationMap;
    }
}