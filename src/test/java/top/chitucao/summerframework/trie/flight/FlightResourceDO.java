package top.chitucao.summerframework.trie.flight;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * FlightVO
 *
 * @author hing(huangxin 1011853)
 * @version Id: FlightVO, v 0.1 2021/8/13 2:20 下午 huangxin Exp $
 */
@Data
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
}
