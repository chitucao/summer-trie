package top.chitucao.summerframework.trie.train;

import java.io.Serializable;
import java.util.Objects;

/**
 * TrainSourceAgg
 *
 * @author chitucao
 */
public class TrainSourceResultAgg implements Serializable {

    private Integer depCityId;

    private Integer arrCityId;

    private Integer minPrice;

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

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        TrainSourceResultAgg that = (TrainSourceResultAgg) o;
        return Objects.equals(depCityId, that.depCityId) && Objects.equals(arrCityId, that.arrCityId) && Objects.equals(minPrice, that.minPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depCityId, arrCityId, minPrice);
    }

    @Override
    public String toString() {
        return "TrainSourceResultAgg{" + "depCityId=" + depCityId + ", arrCityId=" + arrCityId + ", minPrice=" + minPrice + '}';
    }
}