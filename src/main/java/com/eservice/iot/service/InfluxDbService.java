//package com.eservice.iot.service;
//
//import com.eservice.iot.core.ServiceException;
//import org.influxdb.BatchOptions;
//import org.influxdb.InfluxDB;
//import org.influxdb.InfluxDBFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Service;
//
//
//@Service
//public class InfluxDbService implements CommandLineRunner {
//
//    @Value("${influx.url}")
//    private String influxDBUrl;
//
//    @Value("${influx.port}")
//    private String influxDBPort;
//
//
//    @Value("${influx.user}")
//    private String influxDBUser;
//
//    @Value("${influx.password}")
//    private String influxDBPassword;
//
//    private InfluxDB influxDB;
//
//    private final static Logger logger = LoggerFactory.getLogger(InfluxDbService.class);
//
//    @Value("${influx.data_keep_days}")
//    public static Integer MAX_KEEP_DAYS;
//
//    @Override
//    public void run(String... args) throws Exception {
//        logger.info("InfluxDB Initialization begin!");
//        getInfluxDB();
//        logger.info("InfluxDB Initialization success!");
//    }
//
//    public InfluxDB getInfluxDB() throws ServiceException {
//        if(influxDB == null) {
//            if(influxDBUrl == null || influxDBUrl == "") {
//                throw new ServiceException("InfluxDB URL is NULL or invalid!");
//            } else if(influxDBPort == null || influxDBPort == "") {
//                throw new ServiceException("InfluxDB Port is NULL or invalid!");
//            } else if(influxDBUser == null || influxDBUser == "") {
//                throw new ServiceException("InfluxDB user name is NULL or invalid!");
//            } else if(influxDBPassword == null || influxDBPassword == "") {
//                throw new ServiceException("InfluxDB password is NULL or invalid!");
//            }else {
//                try {
//                    influxDB = InfluxDBFactory.connect("http://" + influxDBUrl + ":" + influxDBPort, influxDBUser, influxDBPassword);
//                    // Flush every 2000 Points, at least every 100ms
//                    influxDB.enableBatch(BatchOptions.DEFAULTS.actions(2000).flushDuration(100));
//                } catch (Exception e) {
//                    logger.error("InfluxDB connecting error!" );
//                    logger.error("InfluxDB URL : "  + influxDBUrl);
//                    logger.error("InfluxDB Port : " + influxDBPort);
//                    logger.error("InfluxDB User Name : " + influxDBUser);
//                    logger.error("InfluxDB Password : " + influxDBPassword);
//                    throw new ServiceException("InfluxDB Connecting error!");
//                }
//            }
//        }
//        return influxDB;
//    }
//
//    public void createDB(String dbName) {
//        InfluxDB influxDB = getInfluxDB();
//        if(!influxDB.databaseExists(dbName)) {
//            influxDB.createDatabase(dbName);
//        }
//    }
//
//    public void deleteDB(String dbName) {
//        InfluxDB influxDB = getInfluxDB();
//        if(influxDB.databaseExists(dbName)) {
//            influxDB.deleteDatabase(dbName);
//        }
//    }
//
//    public void setRetentionPolicy(String dbName, Integer keepDays) {
//        InfluxDB influxDB = getInfluxDB();
//        influxDB.createRetentionPolicy("aRetentionPolicy", dbName, keepDays + "d", "60m", 2, true);
//    }
//
//    public void closeDB() {
//        if(influxDB != null) {
//            influxDB.close();
//        }
//    }
//}
