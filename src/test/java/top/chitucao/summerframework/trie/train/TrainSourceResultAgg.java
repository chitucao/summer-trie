package top.chitucao.summerframework.trie.train;

import lombok.Data;

import java.io.Serializable;

/**
 * TrainSourceAgg
 *
 * @author chitucao
 */
@Data
public class TrainSourceResultAgg implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private Integer minPrice;

}