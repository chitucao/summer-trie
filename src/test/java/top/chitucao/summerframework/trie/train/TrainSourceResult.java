package top.chitucao.summerframework.trie.train;

import lombok.Data;

import java.io.Serializable;

/**
 * TrainSourceResult
 *
 * @author chitucao
 */
@Data
public class TrainSourceResult implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private String  trainType;

    private String  seatClass;

    private Long    id;
}