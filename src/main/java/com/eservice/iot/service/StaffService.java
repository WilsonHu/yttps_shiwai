package com.eservice.iot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.response.OapiUserListResponse;
import com.eservice.iot.model.Person;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.model.Staff;
import com.eservice.iot.model.VisitRecord;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
public class StaffService {

    private final static Logger logger = LoggerFactory.getLogger(StaffService.class);

    @Value("${park_base_url}")
    private String PARK_BASE_URL;

    @Autowired
    private RestTemplate restTemplate;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Token
     */
    private String token;
    /**
     * 员工列表
     */
    private ArrayList<Staff> staffList = new ArrayList<>();

    /**
     * 当天已签到员工列表
     */
    private ArrayList<Person> staffSignInList = new ArrayList<>();

    /**
     * 当天已签到VIP员工列表
     */
    private ArrayList<Person> vipSignInList = new ArrayList<>();

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TagService tagService;


    @Autowired
    private DingDingService dingDingService;

    private ThreadPoolTaskExecutor mExecutor;

    /**
     * 查询开始时间,单位为秒
     */
    private Long queryStartTime = 0L;


    public StaffService() {
        //准备初始数据，此时获取到考勤列表后不去通知钉钉，初始化开始查询时间
        queryStartTime = Util.getDateStartTime().getTime() / 1000;
    }

    /**
     * 每秒查询一次考勤信息
     */
    @Scheduled(fixedRate = 1000)
    public void fetchSignInScheduled() {
        ///当员工列表数为0，或者已全部签核完成,以及当前处于程序初始化状态情况下，可以跳过不再去获取考勤数据
        boolean skip = staffList.size() <= 0 || staffList.size() == staffSignInList.size()
                || tagService == null || !tagService.isTagInitialFinished();
        if (skip) {
            return;
        }
        if (token == null && tokenService != null) {
            token = tokenService.getToken();
        }
        if (token != null) {
            querySignInStaff(queryStartTime);
        }
    }

    /**
     * 每分钟获取一次员工信息
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void fetchStaffScheduled() {
        if (token == null && tokenService != null) {
            token = tokenService.getToken();
        }
        if (token != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT, "application/json");
            headers.add("Authorization", token);
            HttpEntity entity = new HttpEntity(headers);
            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/staffs", HttpMethod.GET, entity, String.class);
                if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                    String body = responseEntity.getBody();
                    if (body != null) {
                        processStaffResponse(body);
                    } else {
                        fetchStaffScheduled();
                    }
                }
            } catch (HttpClientErrorException exception) {
                if (exception.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                    token = tokenService.getToken();
                    if (token != null) {
                        fetchStaffScheduled();
                    }
                }
            }
        }
    }

    /**
     * 凌晨1点清除签到记录
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void resetStaffDataScheduled() {
        logger.info("每天凌晨一点清除前一天签到记录：{}", formatter.format(new Date()));
        if (staffSignInList != null & staffSignInList.size() > 0) {
            staffSignInList.clear();
        }
    }

    private void processStaffResponse(String body) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<Staff> tmpList = (ArrayList<Staff>) JSONArray.parseArray(responseModel.getResult(), Staff.class);
            boolean changed = false;
            if (tmpList != null && tmpList.size() != 0) {
                if (tmpList.size() != staffList.size()) {
                    changed = true;
                } else {
                    if (!tmpList.equals(staffList)) {
                        changed = true;
                    }
                }
                if (changed) {
                    logger.info("The number of staff：{} ==> {}", staffList.size(), tmpList.size());
                }
                staffList = tmpList;
            }
        }
    }

    private void processStaffSignInResponse(String body, boolean initial) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<VisitRecord> tempList = (ArrayList<VisitRecord>) JSONArray.parseArray(responseModel.getResult(), VisitRecord.class);
            if (tempList != null && tempList.size() > 0) {
                ArrayList<Person> sendSignInList = new ArrayList<>();
                ArrayList<Person> sendVipList = new ArrayList<>();
                for (VisitRecord visitRecord : tempList) {
                    List<String> tagList = visitRecord.getPerson().getTag_id_list();
                    boolean needSignIn = false;
                    if (tagList != null) {
                        for (int i = 0; i < tagList.size() && !needSignIn; i++) {
                            ///在考勤标签列表中
                            if (tagService.getSignInTagIdList().contains(tagList.get(i))) {
                                needSignIn = true;
                            }
                        }
                    }
                    if (needSignIn) {
                        boolean exit = false;
                        for (int i = 0; i < staffSignInList.size() && !exit; i++) {
                            if (staffSignInList.get(i).getPerson_id().equals(visitRecord.getPerson().getPerson_id())) {
                                exit = true;
                            }
                        }
                        if (!exit) {
                            staffSignInList.add(visitRecord.getPerson());
                            //如果是程序initial，则不推送(保罗client和钉钉等)考勤
                            if (!initial) {
                                sendSignInList.add(visitRecord.getPerson());
                            }
                        }
                    }

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
                        for (int i = 0; i < vipSignInList.size() && !exit; i++) {
                            if (vipSignInList.get(i).getPerson_id().equals(visitRecord.getPerson().getPerson_id())) {
                                exit = true;
                            }
                        }
                        if (!exit) {
                            vipSignInList.add(visitRecord.getPerson());
                            if (!initial) {
                                sendVipList.add(visitRecord.getPerson());
                            }
                        }
                    }
                    //建立线程池发送钉钉
                    if (sendSignInList.size() > 0) {
                        if (mExecutor == null) {
                            initExecutor();
                        }
                        for (Person person : sendSignInList) {
                            String phoneNum = person.getPerson_information().getPhone();
                            boolean found = false;
                            OapiUserListResponse.Userlist dingUser = null;
                            if (phoneNum != null) {
                                for (int i = 0; i < dingDingService.getDingDingUserList().size() && !found; i++) {
                                    String mobile = dingDingService.getDingDingUserList().get(i).getMobile();
                                    if (mobile != null && mobile.equals(phoneNum)) {
                                        found = true;
                                        dingUser = dingDingService.getDingDingUserList().get(i);
                                    }
                                }
                            }
                            if (found & dingUser != null) {
                                OapiUserListResponse.Userlist finalDingUser = dingUser;
                                mExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        //TODO:发送钉钉
//                                        try {
//                                            dingDingService.sendTextMessage(finalDingUser.getUserid(), "上班考勤：" + formatter.format(new Date()));
//                                            logger.warn("{} 发送钉钉！", finalDingUser.getName());
//                                        } catch (ApiException e) {
//                                            e.printStackTrace();
//                                            //TODO:发送考勤数据失败
//                                        }
                                    }
                                });
                            }
                        }
                    }

                    ///由于VIP和考勤信息可能存在重复，所以在把消息推送至client时，两个send数组进行合并
                    for (Person vip : sendVipList) {
                        ///Person的meta字段设置成VIP，则前台显示VIP
                        vip.setMeta(Constant.VIP);
                        ///由于sendVipList和sendSignInList来之同一个数组，所以可以根据person对象进行contain
                        if (!sendSignInList.contains(vip)) {
                            ///VIP员工，不在本次考勤上传数组中，但是需要发送到大屏欢迎
                            sendSignInList.add(vip);
                        }
                    }
                    if (mExecutor == null) {
                        initExecutor();
                    }
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            //TODO:发送Client
                            logger.error("socket发送Client！");
                        }
                    });
                }
            }
        }
    }

    private void querySignInStaff(Long startTime) {
        if (token == null) {
            token = tokenService.getToken();
        }
        HashMap<String, Object> postParameters = new HashMap<>();
//        ///考勤记录查询开始时间
        postParameters.put("start_timestamp", startTime);
//        ///考勤记录查询结束时间
        Long queryEndTime = System.currentTimeMillis() / 1000;
        postParameters.put("end_timestamp", queryEndTime);
        //只获取员工数据
        ArrayList<String> identity = new ArrayList<>();
        identity.add("STAFF");
        postParameters.put("identity_list", identity);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.AUTHORIZATION, token);
        HttpEntity httpEntity = new HttpEntity<>(JSON.toJSONString(postParameters), headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/visit_record/query", httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                String body = responseEntity.getBody();
                if (body != null) {
                    processStaffSignInResponse(body, startTime.equals(Util.getDateStartTime().getTime() / 1000));
                    //query成功后用上一次查询的结束时间作为下一次开始时间，减去1秒形成闭区间
                    queryStartTime = queryEndTime - 1;
                }
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                //token失效,重新获取token后再进行数据请求
                token = tokenService.getToken();
                querySignInStaff(startTime);
            }
        }
    }

    private void initExecutor() {
        mExecutor = new ThreadPoolTaskExecutor();
        mExecutor.setCorePoolSize(2);
        mExecutor.setMaxPoolSize(5);
        mExecutor.setThreadNamePrefix("YTTPS-");
        mExecutor.initialize();
    }

    public ArrayList<Staff> getStaffList() {
        return staffList;
    }
}
