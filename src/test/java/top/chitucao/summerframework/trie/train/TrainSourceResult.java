package top.chitucao.summerframework.trie.train;

import java.io.Serializable;
import java.util.Objects;

/**
 * TrainSourceResult
 *
 * @author chitucao
 */
public class TrainSourceResult implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private String  trainType;

    private String  seatClass;

    private Long    id;

    public Integer getDepCityId() {
        return depCityId;
    }

    public void setDepCityId(Integer depCityId) {
        this.depCityId = depCityId;
    }

    public Integer getArrCityId() {
        return arrCityId;
    }

    public void setArrCityId(Integer arrCityId) {
        this.arrCityId = arrCityId;
    }

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        TrainSourceResult that = (TrainSourceResult) o;
        return Objects.equals(depCityId, that.depCityId) && Objects.equals(arrCityId, that.arrCityId) && Objects.equals(trainType, that.trainType)
               && Objects.equals(seatClass, that.seatClass) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depCityId, arrCityId, trainType, seatClass, id);
    }

    @Override
    public String toString() {
        return "TrainSourceResult{" + "depCityId=" + depCityId + ", arrCityId=" + arrCityId + ", trainType='" + trainType + '\'' + ", seatClass='" + seatClass + '\'' + ", id=" + id
               + '}';
    }
}