//package com.eservice.iot.web;
//
//import com.eservice.iot.core.Result;
//import com.eservice.iot.core.ResultGenerator;
//import com.eservice.iot.service.InfluxDbService;
//import org.influxdb.InfluxDB;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//
//@RestController
//@RequestMapping("/db")
//public class InfluxDbMannagerController {
//
//    @Resource
//    InfluxDbService influxDbService;
//
//    private final static Logger logger = LoggerFactory.getLogger(InfluxDbMannagerController.class);
//
//    @PostMapping("/create")
//    public Result create(@RequestParam String dbName) {
//        if(dbName == null || dbName == "") {
//            return ResultGenerator.genFailResult("DB name should NOT be NULL!");
//        }
//        try {
//            influxDbService.createDB(dbName);
//        } catch (Exception e) {
//            return ResultGenerator.genFailResult("Create DB error！ Error message :" + e.getMessage());
//        }
//        return ResultGenerator.genSuccessResult();
//    }
//
//    @PostMapping("/delete")
//    public Result delete(@RequestParam String dbName) {
//        if(dbName == null || dbName == "") {
//            return ResultGenerator.genFailResult("DB name should NOT be NULL!");
//        }
//        try {
//            influxDbService.deleteDB(dbName);
//        } catch (Exception e) {
//            return ResultGenerator.genFailResult("Delete DB error！ Error message :" + e.getMessage());
//        }
//        return ResultGenerator.genSuccessResult();
//    }
//
//    @PostMapping("/retention/policy")
//    public Result retentionPolicy(@RequestParam String dbName, @RequestParam ( defaultValue = "30")  Integer keepDays) {
//        if(dbName == null || dbName == "") {
//            return ResultGenerator.genFailResult("DB name should NOT be NULL!");
//        }
//        if(keepDays < 1) {
//            keepDays = 1;
//        } else if(keepDays > InfluxDbService.MAX_KEEP_DAYS) {
//            keepDays = InfluxDbService.MAX_KEEP_DAYS;
//        }
//        try {
//            if(influxDbService.getInfluxDB().databaseExists(dbName)) {
//                influxDbService.setRetentionPolicy(dbName, keepDays);
//            } else {
//                return ResultGenerator.genFailResult("DB does NOT exist!");
//            }
//        } catch (Exception e) {
//            return ResultGenerator.genFailResult("Retention Policy error！ Error message :" + e.getMessage());
//        }
//        //Return the real keep days
//        return ResultGenerator.genSuccessResult(keepDays);
//    }
//}
