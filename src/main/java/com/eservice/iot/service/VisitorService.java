package com.eservice.iot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.response.OapiUserListResponse;
import com.eservice.iot.model.*;
import com.eservice.iot.util.Util;
import com.taobao.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * @author HT
 */
@Component
public class VisitorService {

    private final static Logger logger = LoggerFactory.getLogger(VisitorService.class);

    @Value("${park_base_url}")
    private String PARK_BASE_URL;

    /**
     * 长期有效的访客
     */
    public static final String LONG_TIME_VALID = "0";

    /**
     * 临时访客
     */
    public static final String TMP_VALID = "1";

    @Autowired
    private RestTemplate restTemplate;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * Token
     */
    private String token;
    /**
     * 访客列表
     */
    private List<Visitor> visitorList = new ArrayList<>();

    /**
     * 当天访客到访列表
     */
    private ArrayList<VisitRecord> visitorSignInList = new ArrayList<>();

    /**
     * 当天已签到VIP访客列表
     */
    private ArrayList<Person> vipVisitorList = new ArrayList<>();

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TagService tagService;

    @Autowired
    private StaffService signInService;

    @Autowired
    private DingDingService dingDingService;

    /**
     * 查询开始时间,单位为秒
     */
    private Long queryStartTime = 0L;

    private int visitorNum = 0;

    public VisitorService() {
        //准备初始数据，此时获取到访客列表后不去通知，初始化开始查询时间
        queryStartTime = Util.getDateStartTime().getTime() / 1000;
    }

    /**
     * 每秒查询访客记录
     */
    @Scheduled(fixedRate = 1000)
    public void fetchVisitorRecordScheduled() {
        boolean skip = visitorList.size() <= 0 || visitorList.size() == visitorSignInList.size()
                || tagService == null || !tagService.isTagInitialFinished();
        if (skip) {
            return;
        }

        if (token == null && tokenService != null) {
            token = tokenService.getToken();
        }
        if( token != null) {
            querySignInVisitor(queryStartTime);
        } else {
            logger.error("Token is null, fetch visitor record error!");
        }
    }

    /**
     * cron: 秒、分、时、日、月、年
     * 早上六点至傍晚八点，每10秒钟获取一次当天访客信息
     */
    @Scheduled(fixedRate = 1000 * 10)
    public void fetchVisitorListScheduled() {
        if (token == null) {
            token = tokenService.getToken();
        }
        if(token != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT, "application/json");
            headers.add("Authorization", token);
            HttpEntity entity = new HttpEntity(headers);
            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors", HttpMethod.GET, entity, String.class);
                if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                    String body = responseEntity.getBody();
                    if (body != null) {
                        processVisitorResponse(body);
                    } else {
                        fetchVisitorListScheduled();
                    }
                }
            } catch (HttpClientErrorException errorException) {
                if (errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                    token = tokenService.getToken();
                    fetchVisitorListScheduled();
                }
            }
        } else {
            logger.error("Token is null, fetch visitor list error!");
        }
    }

    private void processVisitorResponse(String body) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            List<Visitor> tmpList = JSONArray.parseArray(responseModel.getResult(), Visitor.class);
            if (tmpList != null && tmpList.size() > 0) {
                boolean changed = false;
                if (tmpList.size() != visitorList.size()) {
                    changed = true;
                } else {
                    if (!tmpList.equals(visitorList)) {
                        changed = true;
                    }
                }
                if (changed) {
                    logger.info("The number of visitor：{} ==> {}", visitorList.size(), tmpList.size());
                }
                visitorList = tmpList;
            }
        }
    }

    private void processVisitorRecordResponse(String body, boolean initial) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<VisitRecord> tempList = (ArrayList<VisitRecord>) JSONArray.parseArray(responseModel.getResult(), VisitRecord.class);
            if (tempList != null && tempList.size() > 0) {
                ArrayList<Person> sendVipList = new ArrayList<>();
                for (VisitRecord visitRecord : tempList) {
                    List<String> tagList = visitRecord.getPerson().getTag_id_list();
                    boolean isVIP = false;
                    if (tagList != null) {
                        for (int i = 0; i < tagList.size() && !isVIP; i++) {
                            ///在VIP标签列表中
                            if (tagService.getVIPTagIdList().contains(tagList.get(i))) {
                                isVIP = true;
                            }
                        }
                    }
                    if (isVIP) {
                        boolean exit = false;
                        for (int i = 0; i < vipVisitorList.size() && !exit; i++) {
                            if (vipVisitorList.get(i).getPerson_id().equals(visitRecord.getPerson().getPerson_id())) {
                                exit = true;
                            }
                        }
                        if (!exit) {
                            vipVisitorList.add(visitRecord.getPerson());
                            if (!initial) {
                                sendVipList.add(visitRecord.getPerson());
                            }
                        }
                    }
                    if (sendVipList.size() > 0) {
                        visitorSignInList.add(visitRecord);
                        //如果是程序initial，则不推送钉钉
                        if (!initial) {
                            //todo:推送client
                        }
                    }
                }
            }
        }
    }


    private void querySignInVisitor(Long startTime) {
        HashMap<String, Object> postParameters = new HashMap<>();
        ///考勤记录查询开始时间
        postParameters.put("start_timestamp", startTime);
        ///考勤记录查询结束时间
        Long queryEndTime = System.currentTimeMillis() / 1000;
        postParameters.put("end_timestamp", queryEndTime);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.AUTHORIZATION, token);
        //只获取访客数据
        ArrayList<String> identity = new ArrayList<>();
        identity.add("VISITOR");
        HttpEntity httpEntity = new HttpEntity<>(JSON.toJSONString(postParameters), headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/visitor_visit/record", httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                String body = responseEntity.getBody();
                if (body != null) {
                    processVisitorRecordResponse(body, startTime.equals(Util.getDateStartTime().getTime() / 1000));
                    queryStartTime = queryEndTime - 1;
                }
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                //token失效,重新获取token后再进行数据请求
                token = tokenService.getToken();
                querySignInVisitor(startTime);
            }
        }
    }

    public List<Visitor> getVisitorList() {
        return visitorList;
    }
}
