package com.eservice.iot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eservice.iot.model.Policy;
import com.eservice.iot.model.ResponseModel;
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


/**
 * @author HT
 */
@Component
public class PolicyService {

    private final static Logger logger = LoggerFactory.getLogger(PolicyService.class);

    @Value("${park_base_url}")
    private String PARK_BASE_URL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TokenService tokenService;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ThreadPoolTaskExecutor mExecutor;

    private static boolean TAG_INITIAL_FINISHED = false;

    /**
     * Token
     */
    private String token;

    /**
     * 访客进入策略名称
     */
    private String CUSTOMER_IN_POLICY_NAME = "customer_in";

    private ArrayList<String> mCustomerInTagIdList = new ArrayList<>();

    private ArrayList<String> mCustomerInDeviceIdList = new ArrayList<>();

    /**
     * 访客出去策略名称
     */
    private String CUSTOMER_OUT_POLICY_NAME = "customer_out";

    private ArrayList<String> mCustomerOutTagIdList = new ArrayList<>();

    private ArrayList<String> mCustomerOutDeviceIdList = new ArrayList<>();

    public PolicyService() {

        fetchPolicy();
    }

    /**
     * 每分钟更新通行策略
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void fetchPolicy() {
        if (tokenService != null) {
            token = tokenService.getToken();
            if(token != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.ACCEPT, "application/json");
                headers.add("Authorization", token);
                HttpEntity entity = new HttpEntity(headers);
                try {
                    ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/access/policy", HttpMethod.GET, entity, String.class);
                    if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                        String body = responseEntity.getBody();
                        if (body != null) {
                            processPolicyResponse(body);
                        } else {
                            fetchPolicy();
                        }
                    }
                } catch (HttpClientErrorException errorException) {
                    if (errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                        fetchPolicy();
                    }
                }
            } else {
                logger.error("Token is null, fetch policy error!");
            }
            if (TAG_INITIAL_FINISHED && mExecutor != null) {
                mExecutor.shutdown();
                mExecutor = null;
            }
        } else {

            //等待tokenService初始化完成，TAG标签被其他很多service依赖，所以需要其先初始化完毕后
            if (mExecutor == null) {
                mExecutor = new ThreadPoolTaskExecutor();
                mExecutor.setCorePoolSize(1);
                mExecutor.setMaxPoolSize(2);
                mExecutor.setThreadNamePrefix("YTTPS-");
                mExecutor.initialize();
            }
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        fetchPolicy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void processPolicyResponse(String body) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<Policy> tmpList = (ArrayList<Policy>) JSONArray.parseArray(responseModel.getResult(), Policy.class);
            if (tmpList != null && tmpList.size() > 0) {
                for (Policy policy : tmpList) {
                    if(policy.getName()!= null && policy.getName().equals(CUSTOMER_IN_POLICY_NAME) && policy.isEnabled()) {
                        mCustomerInDeviceIdList.clear();
                        mCustomerInTagIdList.clear();
                        mCustomerInDeviceIdList = policy.getDevice_id_list();
                        mCustomerInTagIdList = policy.getTag_id_list();
                    } else if(policy.getName()!= null && policy.getName().equals(CUSTOMER_OUT_POLICY_NAME)  && policy.isEnabled()) {
                        mCustomerOutDeviceIdList.clear();
                        mCustomerOutTagIdList.clear();
                        mCustomerOutDeviceIdList = policy.getDevice_id_list();
                        mCustomerOutTagIdList = policy.getTag_id_list();
                    }
                }
                TAG_INITIAL_FINISHED = true;
            }
        }
    }

    public ArrayList<String> getmCustomerInTagIdList() {
        return mCustomerInTagIdList;
    }

    public ArrayList<String> getmCustomerInDeviceIdList() {
        return mCustomerInDeviceIdList;
    }

    public ArrayList<String> getmCustomerOutTagIdList() {
        return mCustomerOutTagIdList;
    }

    public ArrayList<String> getmCustomerOutDeviceIdList() {
        return mCustomerOutDeviceIdList;
    }

}
