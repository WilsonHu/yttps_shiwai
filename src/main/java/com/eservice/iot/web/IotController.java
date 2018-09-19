//package com.eservice.iot.web;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.eservice.iot.core.Result;
//import com.eservice.iot.core.ResultGenerator;
//import com.eservice.iot.core.ServiceException;
//import com.eservice.iot.service.InfluxDbService;
//import io.netty.util.internal.StringUtil;
//import org.influxdb.InfluxDB;
//import org.influxdb.dto.Query;
//import org.influxdb.dto.QueryResult;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//import javax.validation.constraints.NotNull;
//
///**
// * @author Wilson Hu
// * To get device data
// */
//@RestController
//@RequestMapping("/iot/")
//public class IotController {
//
//    @Resource
//    InfluxDbService influxDbService;
//
//    private final static Logger logger = LoggerFactory.getLogger(IotController.class);
//    @PostMapping("/getDeviceData")
//    public Result getDeviceData(@RequestParam @NotNull String deviceName, @RequestParam @NotNull String tag, @RequestParam @NotNull String interval) {
//        if(deviceName.equals("")) {
//            return ResultGenerator.genFailResult("Device name should NOT be empty!");
//        }
//        if(tag.equals("")) {
//            return ResultGenerator.genFailResult("Tag should NOT be empty!");
//        }
//        //query interval should be end with "m" , "h" or "d"
//        boolean intervalInvalid = interval.equals("") || (!interval.endsWith("d") && !interval.endsWith("h") && !interval.endsWith("m"));
//        if(intervalInvalid) {
//            return ResultGenerator.genFailResult("Query interval should NOT be empty or with error format!");
//        }
//        try {
//            InfluxDB influxDB = influxDbService.getInfluxDB();
//            if(influxDB.databaseExists(deviceName)) {
//                QueryResult queryResult = influxDB.query(new Query("SELECT " + tag + " FROM " + deviceName + " WHERE time >= now() - " + interval, deviceName));
//                return ResultGenerator.genSuccessResult(queryResult.getResults());
//            } else {
//                logger.error("Device data does NOT exist!");
//                return ResultGenerator.genFailResult("Device data does NOT exist!");
//            }
//        } catch (Exception e) {
//            logger.error("InfluxDB query error!");
//            return ResultGenerator.genFailResult("InfluxDB query error!");
//        }
//    }
//
//    @PostMapping("/getRTDeviceData")
//    public Result getRTDeviceData(@RequestParam @NotNull String deviceName, @RequestParam @NotNull String tag, @RequestParam @NotNull String interval) {
//        if(deviceName.equals("")) {
//            return ResultGenerator.genFailResult("Device name should NOT be empty!");
//        }
//        if(tag.equals("")) {
//            return ResultGenerator.genFailResult("Tag should NOT be empty!");
//        }
//        //query interval should be end with "m" or "d"
//        if(interval.equals("") || (!interval.endsWith("d") && !interval.endsWith("m"))) {
//            return ResultGenerator.genFailResult("Query interval should NOT be empty or with error format!");
//        }
//        try {
//            InfluxDB influxDB = influxDbService.getInfluxDB();
//            if(influxDB.databaseExists(deviceName)) {
//                QueryResult queryResult = influxDB.query(new Query("SELECT " + tag + " FROM " + deviceName + " WHERE time >= now() - " + interval + " ORDER BY time DESC LIMIT 1", deviceName));
//                return ResultGenerator.genSuccessResult(queryResult.getResults());
//            } else {
//                logger.error("Device data does NOT exist!");
//                return ResultGenerator.genFailResult("Device data does NOT exist!");
//            }
//        } catch (Exception e) {
//            logger.error("InfluxDB query error!");
//            return ResultGenerator.genFailResult("InfluxDB query error!");
//        }
//    }
//
//}
