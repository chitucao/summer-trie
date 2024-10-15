package top.chitucao.summerframework.trie.train;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * BlindboxTrainSourceDO
 *
 * @author chitucao
 */
@Data
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

}