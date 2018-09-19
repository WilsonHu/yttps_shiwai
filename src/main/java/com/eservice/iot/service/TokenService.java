package com.eservice.iot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.util.Util;
import com.taobao.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * @author HT
 */
@Service
public class TokenService {

    @Value("${park_base_url}")
    private String PARK_BASE_URL;
    @Value("${park_username}")
    private String PARK_USERNAME;
    @Value("${park_password}")
    private String PARK_PASSWORD;

    @Value("${dd_corp_id}")
    private String CORP_ID;
    @Value("${dd_corp_secret}")
    private String CORP_SECRET;

    @Value("${dd_corp_id_2}")
    private String CORP_ID_2;
    @Value("${dd_corp_secret_2}")
    private String CORP_SECRET_2;

    @Value("${dingding_url}")
    private String dingdingUrl;

    @Autowired
    private RestTemplate restTemplate;

    private OapiGettokenResponse oapiGettokenResponse;

    private OapiGettokenResponse oapiGettokenResponse2;

    /**
     * 为了防止本机服务器时间和钉钉返回token的过期时间不match，
     * 设置一个十分钟的buffer
     */
    public static final Long EXPIRE_BUFFER = 10 * 60 * 1000L;

    private final static Logger logger = LoggerFactory.getLogger(TokenService.class);

    /**
     * 园区登录，成功则返回token，失败返回null
     */
    public String getToken() {
        String token = null;
        HashMap<String, String> postParameters = new HashMap<>();
        postParameters.put("username", PARK_USERNAME);
        postParameters.put("password", Util.getMD5String(PARK_PASSWORD));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity r = new HttpEntity<>(JSON.toJSONString(postParameters), headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/user/login", r, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                String body = responseEntity.getBody();
                if (body != null) {
                    ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                    if (responseModel != null && responseModel.getResult() != null) {
                        token = responseModel.getResult();
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("Token update error ==> " + exception.getMessage());
        }

        return token;
    }

    /**
     * 钉钉访问Token
     */
    public String getDDToken() {
        String token = null;
        //TODO:If the PC's time does not match the network time, code in below will have a problem.
        if (oapiGettokenResponse != null && (System.currentTimeMillis() < oapiGettokenResponse.getExpiresIn() - EXPIRE_BUFFER)) {
            token = oapiGettokenResponse.getAccessToken();
        } else {
            DingTalkClient client = new DefaultDingTalkClient(dingdingUrl + "/gettoken");
            OapiGettokenRequest request = new OapiGettokenRequest();
            request.setCorpid(CORP_ID);
            request.setCorpsecret(CORP_SECRET);
            request.setHttpMethod("GET");
            try {
                oapiGettokenResponse = client.execute(request);
                if (oapiGettokenResponse != null && oapiGettokenResponse.getErrcode().equals(0L)) {
                    token = oapiGettokenResponse.getAccessToken();
                } else {
                    oapiGettokenResponse = null;
                }
            } catch (ApiException e) {
                e.printStackTrace();
                oapiGettokenResponse = null;
            }
        }
        return token;
    }

    /**
     * 钉钉访问Token
     */
    public String getDDToken2() {
        String token = null;
        DingTalkClient client = new DefaultDingTalkClient(dingdingUrl + "/gettoken");
        OapiGettokenRequest request = new OapiGettokenRequest();
        request.setCorpid(CORP_ID_2);
        request.setCorpsecret(CORP_SECRET_2);
        request.setHttpMethod("GET");
        try {
            oapiGettokenResponse2 = client.execute(request);
            if (oapiGettokenResponse2 != null && oapiGettokenResponse2.getErrcode().equals(0L)) {
                token = oapiGettokenResponse2.getAccessToken();
            } else {
                oapiGettokenResponse2 = null;
            }
        } catch (ApiException e) {
            e.printStackTrace();
            oapiGettokenResponse2 = null;
        }
        return token;
    }

}
