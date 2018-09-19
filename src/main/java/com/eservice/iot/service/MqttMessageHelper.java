//package com.eservice.iot.service;
//
//import com.alibaba.fastjson.JSONObject;
//import com.eservice.iot.model.IoTDataModel;
//import org.influxdb.InfluxDB;
//import org.influxdb.dto.BatchPoints;
//import org.influxdb.dto.Point;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.integration.mqtt.support.MqttHeaders;
//import org.springframework.messaging.Message;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//
//import java.util.concurrent.TimeUnit;
//
//import static com.sun.org.apache.xalan.internal.lib.ExsltStrings.split;
//
//
///**
// * @author Wilson Hu  2017-12-01
// */
//@Component
//@Service
//public class MqttMessageHelper {
//
//    @Autowired
//    MqttService.MyGateway myGateway;
//
//    private final static Logger logger = LoggerFactory.getLogger(MqttMessageHelper.class);
//    /**
//     * 只接受“iot”开头的MQTT消息
//     */
//    private final static String IOT_TOPIC = "iot";
//    /**
//     * IOT的topic长度限制为2
//     */
//    private final static int IOT_TOPIC_LENGTH = 2;
//
//    @Resource
//    private InfluxDbService influxDbService;
//
//    /**
//     * 通知client更新员工列表
//     */
//    public static final String UPDATE_STAFF_LIST = "/update/staff_list";
//
//    /**
//     * 通知client有考勤签到更新
//     */
//    public static final String UPDATE_ATTENDANCE = "/update/attendance";
//
//    /**
//     * 通知client更新访客列表
//     */
//    public static final String UPDATE_VISITOR_LIST = "/update/visitor_list";
//
//    /**
//     * 通知client有访客签到更新
//     */
//    public static final String UPDATE_VISITOR = "/update/visitor";
//
//
//    /**
//     * 向MQTT发送数据
//     *
//     * @param topic
//     * @param msg
//     */
//    public void sendToClient(String topic, String msg) {
//        try {
//            myGateway.sendToMqtt(topic, msg);
//        } catch (Exception e) {
//            Logger logger = LoggerFactory.getLogger(MqttMessageHelper.class);
//            logger.error("MQTT消息发送异常", e);
//        }
//    }
//
//    /**
//     * 用于接收MQTT数据，具体业务需要解析message对象后完成
//     *
//     * @param message
//     * @throws Exception
//     */
//    public void handleMessage(Message<?> message) throws Exception {
//        String topic = message.getHeaders().get(MqttHeaders.TOPIC).toString();
//        if (topic != null) {
//            //格式：iot/device
//            String[] strs = topic.split("/");
//            if (strs.length == IOT_TOPIC_LENGTH && IOT_TOPIC.equals(strs[0]) && !"".equals(strs[1])) {
//                String deviceName = strs[1];
//                String payload = message.getPayload().toString();
//                if (payload != null) {
//                    IoTDataModel dataModel = JSONObject.parseObject(payload, IoTDataModel.class);
//                    boolean valid = dataModel != null
//                                        && dataModel.getName() != null && !"".equals(dataModel.getName())
//                                        && dataModel.getValue() != null && !"".equals(dataModel.getValue());
//                    if (valid) {
//                        if(!influxDbService.getInfluxDB().databaseExists(deviceName)) {
//                            //If device's db does not exist, create it and keep data for
//                            influxDbService.createDB(deviceName);
//                            influxDbService.setRetentionPolicy(deviceName, InfluxDbService.MAX_KEEP_DAYS);
//                        }
//                        try {
//                            Double value = Double.valueOf(dataModel.getValue());
//                            BatchPoints batchPoints = BatchPoints
//                                    .database(deviceName)
//                                    //.tag("async", "true")
//                                    //.retentionPolicy(rpName)
//                                    .consistency(InfluxDB.ConsistencyLevel.ALL)
//                                    .build();
//                            Point point1 = Point.measurement(deviceName)
//                                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
//                                    //.addField("name", dataModel.getName())
//                                    //.addField("name", dataModel.getName())
//                                    .addField(dataModel.getName(), value)
//                                    .build();
//                            batchPoints.point(point1);
//                            influxDbService.getInfluxDB().write(batchPoints);
//                        } catch (NumberFormatException e) {
//                            logger.warn("Value format is NOT supported!");
//                        }
//                    }
//                } else {
//                    logger.warn("Data is invalid!");
//                }
//                logger.info("Topic:" + topic + " || Payload:" + payload);
//            } else {
//                logger.warn("Not IoT or invalid message received, topic: " + topic + " payload : " + message.getPayload().toString());
//            }
//        }
//    }
//}
