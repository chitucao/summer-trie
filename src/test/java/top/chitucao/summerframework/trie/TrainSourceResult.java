package top.chitucao.summerframework.trie;

import lombok.Data;

import java.io.Serializable;

/**
 * TrainSourceResult
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: TrainSourceResult.java, v 0.1 2024-09-19 11:10 chitucao Exp $$
 */
@Data
public class TrainSourceResult implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private String  trainType;

    private String  seatClass;

    private Long    id;
}