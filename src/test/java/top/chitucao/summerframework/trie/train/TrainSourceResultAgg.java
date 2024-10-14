package top.chitucao.summerframework.trie.train;

import lombok.Data;

import java.io.Serializable;

/**
 * TrainSourceAgg
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: TrainSourceAgg.java, v 0.1 2024-09-18 17:29 chitucao Exp $$
 */
@Data
public class TrainSourceResultAgg implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private Integer minPrice;

}