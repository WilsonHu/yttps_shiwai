package com.eservice.iot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eservice.iot.model.Person;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.model.VisitRecord;
import com.eservice.iot.model.Visitor;
import com.eservice.iot.util.Util;
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
import java.util.concurrent.Executor;

/**
 * @author HT
 */
@Component
public class AccessService {

    private final static Logger logger = LoggerFactory.getLogger(AccessService.class);

    @Value("${park_base_url}")
    private String PARK_BASE_URL;


    @Autowired
    private RestTemplate restTemplate;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Token
     */
    private String token;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private Executor myExecutePool;

    /**
     * 查询开始时间,单位为秒
     */
    private Long queryStartTime = 0L;

    public AccessService() {
        //准备初始数据，此时获取到访客列表后不去通知，初始化开始查询时间
        queryStartTime = Util.getDateStartTime().getTime() / 1000;
    }

    /**
     * 每分钟查询一次访客记录信息，并remove访客拥有的对应tag标签
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void fetchVisitorRecordScheduled() {
        boolean skip = policyService != null && policyService.getmCustomerInDeviceIdList().size() <= 0
                && policyService.getmCustomerInTagIdList().size() <= 0
                && policyService.getmCustomerOutDeviceIdList().size() <= 0
                && policyService.getmCustomerOutTagIdList().size() <= 0;
        if (skip) {
            return;
        }

        if (token == null && tokenService != null) {
            token = tokenService.getToken();
        }
        if(token != null) {
            querySignInVisitor(queryStartTime);
        } else {
            logger.error("Token is null, fetch visitor record error!");
        }
    }

    /**
     * 晚上21点清除daily标签或者标签不存在的访客,保证其可以再次从pad登记
     */
    @Scheduled(cron = "0 0 21 * * ?")
    public void removeVisitorWithDailyTag() {
        token = tokenService.getToken();
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
                        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                        if (responseModel != null && responseModel.getResult() != null) {
                            List<Visitor> tmpList = JSONArray.parseArray(responseModel.getResult(), Visitor.class);
                            if (tmpList != null && tmpList.size() > 0) {
                                String dailyTagID = null;
                                if (tagService != null) {
                                    for (int i = 0; i < tagService.getmAllTagList().size(); i++) {
                                        if (tagService.getmAllTagList().get(i).getTag_name().equals("daily")) {
                                            dailyTagID = tagService.getmAllTagList().get(i).getTag_id();
                                            break;
                                        }
                                    }
                                }
                                for (int i = 0; i < tmpList.size(); i++) {
                                    if ((dailyTagID != null && tmpList.get(i).getTag_id_list().contains(dailyTagID))
                                            || tmpList.get(i).getTag_id_list().size() == 0) {
                                        Visitor visitor = tmpList.get(i);
                                        myExecutePool.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                ResponseEntity<String> removeVisitor = restTemplate.exchange(PARK_BASE_URL + "/visitors/" + visitor.getVisitor_id(), HttpMethod.DELETE, entity, String.class);
                                                if (removeVisitor.getStatusCodeValue() == ResponseCode.OK) {
                                                    logger.warn("Clear daily visitor：{}, {}", visitor.getPerson_information().getName(), formatter.format(new Date()));
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } else {
                        removeVisitorWithDailyTag();
                    }
                }
            } catch (HttpClientErrorException errorException) {
                if(errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                    logger.warn(errorException.getMessage());
                    token = tokenService.getToken();
                    removeVisitorWithDailyTag();
                }
            }
        } else {
            logger.error("Token is null, remove daily tag error.");
        }
    }

    private void processAccessRecordResponse(String body, boolean initial) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<VisitRecord> tempList = (ArrayList<VisitRecord>) JSONArray.parseArray(responseModel.getResult(), VisitRecord.class);
            if (tempList != null && tempList.size() > 0) {
                ArrayList<VisitRecord> filterList = new ArrayList<>();
                for (VisitRecord visitRecord : tempList) {
                    boolean isSame = false;
                    for (int i = 0; i < filterList.size() && !isSame; i++) {
                        if (filterList.get(i).getPerson().getPerson_id().equals(visitRecord.getPerson().getPerson_id())
                                && filterList.get(i).getDevice_id().equals(visitRecord.getDevice_id())) {
                            isSame = true;
                        }
                    }
                    if (!isSame) {
                        filterList.add(visitRecord);
                    }
                }
                for (VisitRecord item : filterList) {
                    if (policyService.getmCustomerInDeviceIdList().contains(item.getDevice_id())) {
                        myExecutePool.execute(new Runnable() {
                            @Override
                            public void run() {
                                logger.warn("清除{}的customer_in标签, {}", item.getPerson().getPerson_information().getName(), formatter.format(new Date()));
                                removeTags(item, policyService.getmCustomerInTagIdList());
                            }
                        });
                    }
                    if (policyService.getmCustomerOutDeviceIdList().contains(item.getDevice_id())) {
                        myExecutePool.execute(new Runnable() {
                            @Override
                            public void run() {
                                logger.warn("清除{}的customer_out标签, {}", item.getPerson().getPerson_information().getName(), formatter.format(new Date()));
                                removeTags(item, policyService.getmCustomerOutTagIdList());
                            }
                        });
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
        postParameters.put("identity_list", identity);
        //tag id
        ArrayList<String> tmpTagList = (ArrayList<String>) policyService.getmCustomerInTagIdList().clone();
        tmpTagList.addAll((ArrayList<String>) policyService.getmCustomerOutTagIdList().clone());
        postParameters.put("tag_id_list", tmpTagList);
        //Device ID
        ArrayList<String> tmpDeviceList = (ArrayList<String>) policyService.getmCustomerInDeviceIdList().clone();
        tmpDeviceList.addAll((ArrayList<String>) policyService.getmCustomerOutDeviceIdList().clone());
        postParameters.put("device_id_list", tmpDeviceList);

        HttpEntity httpEntity = new HttpEntity<>(JSON.toJSONString(postParameters), headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/access/record", httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                String body = responseEntity.getBody();
                if (body != null) {
                    processAccessRecordResponse(body, startTime.equals(Util.getDateStartTime().getTime() / 1000));
                    queryStartTime = queryEndTime - 1;
                }
            }
        } catch (HttpClientErrorException errorException) {
            if(errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                //token失效,重新获取token后再进行数据请求
                token = tokenService.getToken();
                if (token != null) {
                    querySignInVisitor(startTime);
                }
            }
        }
    }

    private void removeTags(VisitRecord item, ArrayList<String> tagList) {
        if (token == null) {
            token = tokenService.getToken();
        }
        if(token != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.add("Authorization", token);
            HttpEntity entity = new HttpEntity(headers);
            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors/" + item.getPerson().getPerson_id(), HttpMethod.GET, entity, String.class);
                if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                    String body = responseEntity.getBody();
                    if (body != null) {
                        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                        if (responseModel != null && responseModel.getResult() != null) {
                            Person person = JSON.parseObject(responseModel.getResult(), Person.class);
                            boolean needUpdate = false;
                            if (person != null) {
                                for (int i = 0; i < tagList.size(); i++) {
                                    if (person.getTag_id_list().contains(tagList.get(i))) {
                                        person.getTag_id_list().remove(tagList.get(i));
                                        needUpdate = true;
                                    }
                                }
                            }

                            if (needUpdate) {
                                HttpHeaders headers2 = new HttpHeaders();
                                headers2.add(HttpHeaders.CONTENT_TYPE, "application/json");
                                headers2.add(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
                                headers2.add("Authorization", token);
                                HttpEntity entity2 = new HttpEntity<>(JSON.toJSONString(person), headers2);
                                ResponseEntity<String> responseEntity2 = restTemplate.exchange(PARK_BASE_URL + "/visitors/" + item.getPerson().getPerson_id(), HttpMethod.PUT, entity2, String.class);
                                if (responseEntity2.getStatusCodeValue() == ResponseCode.OK) {

                                }
                            }
                        }
                    }
                }
            } catch (HttpClientErrorException errorException) {
                if(errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                    //token失效,重新获取token后再进行数据请求
                    token = tokenService.getToken();
                    removeTags(item, tagList);
                }
            }
        } else {
            logger.error("Token is null, remove tag error!");
        }
    }
}
