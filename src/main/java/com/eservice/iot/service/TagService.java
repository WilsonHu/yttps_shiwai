package com.eservice.iot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.model.Tag;
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


/**
 * @author HT
 */
@Component
public class TagService {

    private final static Logger logger = LoggerFactory.getLogger(TagService.class);

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
     * 全部tag列表
     */
    private ArrayList<Tag> mAllTagList = new ArrayList<>();

    /**
     * 需要考勤标签名称列表
     */
    private ArrayList<String> SIGNIN_TAG_NAME_LIST;

    /**
     * 需要考勤标签ID列表
     */
    private ArrayList<String> mSignInTagIdList = new ArrayList<>();

    /**
     * VIP标签名称列表，包含了员工和访客
     */
    private ArrayList<String> VIP_TAG_NAME_LIST;

    /**
     * VIP标签ID列表,包含了员工和访客
     */
    private ArrayList<String> mVIPTagIdList = new ArrayList<>();


    public TagService() {
        /**
         * 考勤
         */
        SIGNIN_TAG_NAME_LIST = new ArrayList<>();
        SIGNIN_TAG_NAME_LIST.add("员工");
        /**
         * VIP
         */
        VIP_TAG_NAME_LIST = new ArrayList<>();
        VIP_TAG_NAME_LIST.add("VIP");
        fetchTags();
    }

    /**
     * 一分钟更新一次TAG
     */
    @Scheduled(fixedRate = 1000*60)
    public void fetchTags() {
        if (tokenService != null) {
            if(token == null) {
                token = tokenService.getToken();
            }
            if(token != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.ACCEPT, "application/json");
                headers.add("Authorization", token);
                HttpEntity entity = new HttpEntity(headers);
                try {
                    ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/tags?size=0", HttpMethod.GET, entity, String.class);
                    if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                        String body = responseEntity.getBody();
                        if (body != null) {
                            processTagResponse(body);
                        } else {
                            fetchTags();
                        }
                    }
                } catch (HttpClientErrorException errorException) {
                    if(errorException.getStatusCode().value() == ResponseCode.TOKEN_INVALID) {
                        //token失效,重新获取token后再进行数据请求
                        token = tokenService.getToken();
                        if (token != null) {
                            fetchTags();
                        }
                    }
                }
            }

            if (TAG_INITIAL_FINISHED && mExecutor != null) {
                mExecutor.shutdown();
                mExecutor = null;
            }
        } else {

            ///等待tokenService初始化完成，TAG标签被其他很多service依赖，所以需要其先初始化完毕后
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
                        fetchTags();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void processTagResponse(String body) {
        ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
        if (responseModel != null && responseModel.getResult() != null) {
            ArrayList<Tag> tmpList = (ArrayList<Tag>) JSONArray.parseArray(responseModel.getResult(), Tag.class);
            if (tmpList != null && tmpList.size() > 0) {
                mAllTagList = tmpList;
                ///清除之前需求考勤的tag列表
                mSignInTagIdList.clear();
                ///清除之前VIP的tag列表
                mVIPTagIdList.clear();
                for (Tag tag : tmpList) {
                    for (int i = 0; i < VIP_TAG_NAME_LIST.size(); i++) {
                        if (tag.getTag_name().equals(VIP_TAG_NAME_LIST.get(i))) {
                            mVIPTagIdList.add(tag.getTag_id());
                        }
                    }

                    for (int i = 0; i < SIGNIN_TAG_NAME_LIST.size(); i++) {
                        if (tag.getTag_name().equals(SIGNIN_TAG_NAME_LIST.get(i))) {
                            //由于在访客和考勤中都可以设置相同标签，考勤原则上只针对员工
                            if (tag.getVisible_identity().contains(Constant.STAFF)) {
                                mSignInTagIdList.add(tag.getTag_id());
                            }
                        }
                    }
                }
                TAG_INITIAL_FINISHED = true;
            }
        }
    }

    public boolean createTag(String name, String identity) {
        HashMap<String, Object> postParameters = new HashMap<>();
        ArrayList<Tag> tagList = new ArrayList<>();
        Tag tag = new Tag();
        tag.setTag_name(name);
        ArrayList<String> identityList = new ArrayList<>();
        identityList.add(identity);
        tag.setVisible_identity(identityList);
        postParameters.put("tag_list", tagList);
        tagList.add(tag);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.AUTHORIZATION, tokenService.getToken());
        HttpEntity httpEntity = new HttpEntity<>(JSON.toJSONString(postParameters), headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/tags", httpEntity, String.class);
        if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getSignInTagIdList() {
        return mSignInTagIdList;
    }

    public ArrayList<String> getVIPTagIdList() {
        return mVIPTagIdList;
    }

    public ArrayList<Tag> getmAllTagList() {
        return mAllTagList;
    }

    public boolean isTagInitialFinished() {
        return TAG_INITIAL_FINISHED;
    }
}
