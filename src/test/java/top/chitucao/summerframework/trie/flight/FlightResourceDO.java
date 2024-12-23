package top.chitucao.summerframework.trie.flight;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * FlightVO
 *
 * @author chitucao
 */
public class FlightResourceDO implements Serializable {
    /** 航班号*/
    private String flightNo;

    /** 航司名称*/
    private String airCompanyName;

    /** 航司二字码*/
    private String airCompanyCode;

    /** 出发城市名称*/
    private String departureCityName;

    /** 出发城市三字码*/
    private String departureCity;

    /** 出发机场三字码*/
    private String departureAirport;

    /** 出发机场名称*/
    private String departureAirportName;

    /** 航班出发时间*/
    private Date   departureTime;

    /** 航班出发时间 yyyy-MM-dd*/
    private String departureDate;

    /** 目的地城市名称*/
    private String arrivalCityName;

    /** 目的地城市三字码*/
    private String arrivalCity;

    /** 目的地机场三字码*/
    private String arrivalAirport;

    /** 目的地机场名称*/
    private String arrivalAirportName;

    /** 航班到达时间*/
    private Date   arrivalTime;

    /** 航班到达时间 yyyy-MM-dd */
    private String arrivalDate;

    /** 出发机场航站楼*/
    private String departurePoint;

    /** 目的地机场航站楼*/
    private String arrivalPoint;

    /** 飞行时间分钟 */
    private Long   flyTime;

    /** 最低价舱位价格*/
    private int    lcp;

    /** 最低价舱位余票量*/
    private String lcn;

    /** 舱等类型：1经济舱，2公务舱，3头等舱，4超级经济舱，5超值公务舱，6超值头等舱*/
    private int    cabinType;

    /** 舱等类型：Y C F S*/
    private String cabinClass;

    /** 舱位数量 */
    private int    cabinNum;

    /** 是否云上公交 1:是,0否 */
    private int    g5Flag;

    /** 更新时间*/
    private String createTime;

    public String getFlightNo() {
        return flightNo;
    }

    public void setFlightNo(String flightNo) {
        this.flightNo = flightNo;
    }

    public String getAirCompanyName() {
        return airCompanyName;
    }

    public void setAirCompanyName(String airCompanyName) {
        this.airCompanyName = airCompanyName;
    }

    public String getAirCompanyCode() {
        return airCompanyCode;
    }

    public void setAirCompanyCode(String airCompanyCode) {
        this.airCompanyCode = airCompanyCode;
    }

    public String getDepartureCityName() {
        return departureCityName;
    }

    public void setDepartureCityName(String departureCityName) {
        this.departureCityName = departureCityName;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getDepartureAirportName() {
        return departureAirportName;
    }

    public void setDepartureAirportName(String departureAirportName) {
        this.departureAirportName = departureAirportName;
    }

    public Date getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Date departureTime) {
        this.departureTime = departureTime;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getArrivalCityName() {
        return arrivalCityName;
    }

    public void setArrivalCityName(String arrivalCityName) {
        this.arrivalCityName = arrivalCityName;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public void setArrivalCity(String arrivalCity) {
        this.arrivalCity = arrivalCity;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public String getArrivalAirportName() {
        return arrivalAirportName;
    }

    public void setArrivalAirportName(String arrivalAirportName) {
        this.arrivalAirportName = arrivalAirportName;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Date arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public String getDeparturePoint() {
        return departurePoint;
    }

    public void setDeparturePoint(String departurePoint) {
        this.departurePoint = departurePoint;
    }

    public String getArrivalPoint() {
        return arrivalPoint;
    }

    public void setArrivalPoint(String arrivalPoint) {
        this.arrivalPoint = arrivalPoint;
    }

    public Long getFlyTime() {
        return flyTime;
    }

    public void setFlyTime(Long flyTime) {
        this.flyTime = flyTime;
    }

    public int getLcp() {
        return lcp;
    }

    public void setLcp(int lcp) {
        this.lcp = lcp;
    }

    public String getLcn() {
        return lcn;
    }

    public void setLcn(String lcn) {
        this.lcn = lcn;
    }

    public int getCabinType() {
        return cabinType;
    }

    public void setCabinType(int cabinType) {
        this.cabinType = cabinType;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }

    public int getCabinNum() {
        return cabinNum;
    }

    public void setCabinNum(int cabinNum) {
        this.cabinNum = cabinNum;
    }

    public int getG5Flag() {
        return g5Flag;
    }

    public void setG5Flag(int g5Flag) {
        this.g5Flag = g5Flag;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        FlightResourceDO that = (FlightResourceDO) o;
        return lcp == that.lcp && cabinType == that.cabinType && cabinNum == that.cabinNum && g5Flag == that.g5Flag && Objects.equals(flightNo, that.flightNo)
               && Objects.equals(airCompanyName, that.airCompanyName) && Objects.equals(airCompanyCode, that.airCompanyCode)
               && Objects.equals(departureCityName, that.departureCityName) && Objects.equals(departureCity, that.departureCity)
               && Objects.equals(departureAirport, that.departureAirport) && Objects.equals(departureAirportName, that.departureAirportName)
               && Objects.equals(departureTime, that.departureTime) && Objects.equals(departureDate, that.departureDate) && Objects.equals(arrivalCityName, that.arrivalCityName)
               && Objects.equals(arrivalCity, that.arrivalCity) && Objects.equals(arrivalAirport, that.arrivalAirport)
               && Objects.equals(arrivalAirportName, that.arrivalAirportName) && Objects.equals(arrivalTime, that.arrivalTime) && Objects.equals(arrivalDate, that.arrivalDate)
               && Objects.equals(departurePoint, that.departurePoint) && Objects.equals(arrivalPoint, that.arrivalPoint) && Objects.equals(flyTime, that.flyTime)
               && Objects.equals(lcn, that.lcn) && Objects.equals(cabinClass, that.cabinClass) && Objects.equals(createTime, that.createTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightNo, airCompanyName, airCompanyCode, departureCityName, departureCity, departureAirport, departureAirportName, departureTime, departureDate,
            arrivalCityName, arrivalCity, arrivalAirport, arrivalAirportName, arrivalTime, arrivalDate, departurePoint, arrivalPoint, flyTime, lcp, lcn, cabinType, cabinClass,
            cabinNum, g5Flag, createTime);
    }

    @Override
    public String toString() {
        return "FlightResourceDO{" + "flightNo='" + flightNo + '\'' + ", airCompanyName='" + airCompanyName + '\'' + ", airCompanyCode='" + airCompanyCode + '\''
               + ", departureCityName='" + departureCityName + '\'' + ", departureCity='" + departureCity + '\'' + ", departureAirport='" + departureAirport + '\''
               + ", departureAirportName='" + departureAirportName + '\'' + ", departureTime=" + departureTime + ", departureDate='" + departureDate + '\'' + ", arrivalCityName='"
               + arrivalCityName + '\'' + ", arrivalCity='" + arrivalCity + '\'' + ", arrivalAirport='" + arrivalAirport + '\'' + ", arrivalAirportName='" + arrivalAirportName
               + '\'' + ", arrivalTime=" + arrivalTime + ", arrivalDate='" + arrivalDate + '\'' + ", departurePoint='" + departurePoint + '\'' + ", arrivalPoint='" + arrivalPoint
               + '\'' + ", flyTime=" + flyTime + ", lcp=" + lcp + ", lcn='" + lcn + '\'' + ", cabinType=" + cabinType + ", cabinClass='" + cabinClass + '\'' + ", cabinNum="
               + cabinNum + ", g5Flag=" + g5Flag + ", createTime='" + createTime + '\'' + '}';
    }
}
