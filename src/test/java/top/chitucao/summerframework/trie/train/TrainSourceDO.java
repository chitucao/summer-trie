package top.chitucao.summerframework.trie.train;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * BlindboxTrainSourceDO
 *
 * @author chitucao
 */
public class TrainSourceDO implements Serializable {
    /**
     * 主键 		
     */
    protected long id;
    /**
     * 出发城市id 		
     */
    private int    departureCityId;
    /**
     * 出发城市level 		
     */
    private int    departureCityLevel;
    /**
     * 出发区县id 		
     */
    private int    departureDistrictId;
    /**
     * 抵达城市id 		
     */
    private int    arrivalCityId;
    /**
     * 抵达城市level 		
     */
    private int    arrivalCityLevel;
    /**
     * 抵达区县id 		
     */
    private int    arrivalDistrictId;
    /**
     * 出发站点code 		
     */
    private String departureStationCode;
    /**
     * 抵达站点code 		
     */
    private String arrivalStationCode;
    /**
     * 车次类型 		
     */
    private String trainType;
    /**
     * 坐席类型 		
     */
    private String seatClass;
    /**
     * 最低票价 		
     */
    private double minRealPrice;
    /**
     * 最高票价
     */
    private double maxRealPrice;
    /**
     * 最低运行时长 		
     */
    private int    minRunTime;
    /**
     * 最高运行时长 		
     */
    private int    maxRunTime;
    /**
     * 数据生成日期 		
     */
    private Date   createDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getDepartureCityId() {
        return departureCityId;
    }

    public void setDepartureCityId(int departureCityId) {
        this.departureCityId = departureCityId;
    }

    public int getDepartureCityLevel() {
        return departureCityLevel;
    }

    public void setDepartureCityLevel(int departureCityLevel) {
        this.departureCityLevel = departureCityLevel;
    }

    public int getDepartureDistrictId() {
        return departureDistrictId;
    }

    public void setDepartureDistrictId(int departureDistrictId) {
        this.departureDistrictId = departureDistrictId;
    }

    public int getArrivalCityId() {
        return arrivalCityId;
    }

    public void setArrivalCityId(int arrivalCityId) {
        this.arrivalCityId = arrivalCityId;
    }

    public int getArrivalCityLevel() {
        return arrivalCityLevel;
    }

    public void setArrivalCityLevel(int arrivalCityLevel) {
        this.arrivalCityLevel = arrivalCityLevel;
    }

    public int getArrivalDistrictId() {
        return arrivalDistrictId;
    }

    public void setArrivalDistrictId(int arrivalDistrictId) {
        this.arrivalDistrictId = arrivalDistrictId;
    }

    public String getDepartureStationCode() {
        return departureStationCode;
    }

    public void setDepartureStationCode(String departureStationCode) {
        this.departureStationCode = departureStationCode;
    }

    public String getArrivalStationCode() {
        return arrivalStationCode;
    }

    public void setArrivalStationCode(String arrivalStationCode) {
        this.arrivalStationCode = arrivalStationCode;
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

    public double getMinRealPrice() {
        return minRealPrice;
    }

    public void setMinRealPrice(double minRealPrice) {
        this.minRealPrice = minRealPrice;
    }

    public double getMaxRealPrice() {
        return maxRealPrice;
    }

    public void setMaxRealPrice(double maxRealPrice) {
        this.maxRealPrice = maxRealPrice;
    }

    public int getMinRunTime() {
        return minRunTime;
    }

    public void setMinRunTime(int minRunTime) {
        this.minRunTime = minRunTime;
    }

    public int getMaxRunTime() {
        return maxRunTime;
    }

    public void setMaxRunTime(int maxRunTime) {
        this.maxRunTime = maxRunTime;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        TrainSourceDO that = (TrainSourceDO) o;
        return id == that.id && departureCityId == that.departureCityId && departureCityLevel == that.departureCityLevel && departureDistrictId == that.departureDistrictId
               && arrivalCityId == that.arrivalCityId && arrivalCityLevel == that.arrivalCityLevel && arrivalDistrictId == that.arrivalDistrictId
               && Double.compare(minRealPrice, that.minRealPrice) == 0 && Double.compare(maxRealPrice, that.maxRealPrice) == 0 && minRunTime == that.minRunTime
               && maxRunTime == that.maxRunTime && Objects.equals(departureStationCode, that.departureStationCode) && Objects.equals(arrivalStationCode, that.arrivalStationCode)
               && Objects.equals(trainType, that.trainType) && Objects.equals(seatClass, that.seatClass) && Objects.equals(createDate, that.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, departureCityId, departureCityLevel, departureDistrictId, arrivalCityId, arrivalCityLevel, arrivalDistrictId, departureStationCode,
            arrivalStationCode, trainType, seatClass, minRealPrice, maxRealPrice, minRunTime, maxRunTime, createDate);
    }

    @Override
    public String toString() {
        return "TrainSourceDO{" + "id=" + id + ", departureCityId=" + departureCityId + ", departureCityLevel=" + departureCityLevel + ", departureDistrictId="
               + departureDistrictId + ", arrivalCityId=" + arrivalCityId + ", arrivalCityLevel=" + arrivalCityLevel + ", arrivalDistrictId=" + arrivalDistrictId
               + ", departureStationCode='" + departureStationCode + '\'' + ", arrivalStationCode='" + arrivalStationCode + '\'' + ", trainType='" + trainType + '\''
               + ", seatClass='" + seatClass + '\'' + ", minRealPrice=" + minRealPrice + ", maxRealPrice=" + maxRealPrice + ", minRunTime=" + minRunTime + ", maxRunTime="
               + maxRunTime + ", createDate=" + createDate + '}';
    }
}