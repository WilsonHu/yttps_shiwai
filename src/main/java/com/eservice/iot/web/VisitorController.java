package com.eservice.iot.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eservice.iot.core.Result;
import com.eservice.iot.core.ResultGenerator;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.model.Tag;
import com.eservice.iot.model.Visitor;
import com.eservice.iot.service.*;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Class Description: xxx
 *
 * @author Wilson Hu
 * @date 2018/08/21.
 */
@RestController
@RequestMapping("/visitors")
public class VisitorController {

    private final static Logger logger = LoggerFactory.getLogger(VisitorController.class);

    @Resource
    private TokenService tokenService;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VisitorService visitorService;
    @Resource
    private TagService tagService;
    @Resource
    private MqttMessageHelper mqttMessageHelper;

    @Value("${park_base_url}")
    private String PARK_BASE_URL;

    @PostMapping("/add")
    public Result add(@RequestParam String jsonData) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add("Authorization", tokenService.getToken());
        HttpEntity httpEntity = new HttpEntity<>(jsonData, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(PARK_BASE_URL + "/visitors", httpEntity, String.class);
        if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
            String body = responseEntity.getBody();
            if (body != null) {
                ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                if (responseModel != null && responseModel.getResult() != null) {
                    List<Visitor> tmpList = JSONArray.parseArray(responseModel.getResult(), Visitor.class);
                    if (tmpList != null && tmpList.size() > 0) {
                        //TODO:发送钉钉

                    }
                }
            }
        }
        return ResultGenerator.genSuccessResult();
    }

    @PostMapping("/getVisitor")
    public Result getVisitor(@RequestParam String visitorId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add("Authorization", tokenService.getToken());
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors?visitor_id_list=" + visitorId, HttpMethod.GET, entity, String.class);
        if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
            String body = responseEntity.getBody();
            if (body != null) {
                ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                if (responseModel != null && responseModel.getResult() != null) {
                    List<Visitor> tmpList = JSONArray.parseArray(responseModel.getResult(), Visitor.class);
                    if (tmpList != null && tmpList.size() > 0) {
                        for (Visitor visitor : tmpList) {
                            String url = PARK_BASE_URL + "/image/" + visitor.getFace_list().get(0).getFace_image_id();
                            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
                            visitor.getFace_list().get(0).setFace_image_id("data:image/jpeg;base64," + Base64Utils.encodeToString(imageBytes));
                        }
                        PageInfo pageInfo = new PageInfo(tmpList);
                        return ResultGenerator.genSuccessResult(pageInfo);
                    }
                }
            }
        }
        return ResultGenerator.genFailResult("获取访客信息失败！");
    }

    @PostMapping("/deleteVisitor")
    public Result deleteVisitor(@RequestParam String visitorId) {
        List<Visitor> visitorList = visitorService.getVisitorList();
        Visitor visitor = null;
        for (int i = 0; i < visitorList.size() && visitor == null; i++) {
            if (visitorList.get(i).getVisitor_id().equals(visitorId)) {
                visitor = visitorList.get(i);
            }
        }
        if(visitor != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.add("Authorization", tokenService.getToken());
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors/" + visitorId, HttpMethod.DELETE, entity, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                VisitorPassResult result = new VisitorPassResult();
                result.setResult(0);
                result.setName(visitor.getPerson_information().getName());
                ///去掉空格后去匹配英文字符，“0” ==> English, "1" ==> Chinese
                boolean isEnglish = visitor.getPerson_information().getName().replaceAll(" ", "").matches("^[a-zA-Z]*");
                if(isEnglish) {
                    result.setIsChinese(0);
                    result.setMsg("Sorry, " + visitor.getPerson_information().getName() + " is not allowed to visit");
                } else {
                    result.setIsChinese(1);
                    result.setMsg("访客 " + visitor.getPerson_information().getName() + " 拒绝来访");
                }
                mqttMessageHelper.sendToClient("visitor/response/" + visitorId, JSON.toJSONString(result));
                logger.warn("Visitor is rejected! Visitor ID : ==>{}", visitorId);
                return ResultGenerator.genSuccessResult();
            }
        }
        return ResultGenerator.genFailResult("拒绝访客失败！");
    }

    @PostMapping("/acceptVisitor")
    public Result acceptVisitor(@RequestParam String visitorId) {
        List<Visitor> visitorList = visitorService.getVisitorList();
        Visitor visitor = null;
        for (int i = 0; i < visitorList.size() && visitor == null; i++) {
            if (visitorList.get(i).getVisitor_id().equals(visitorId)) {
                visitor = visitorList.get(i);
            }
        }

        if (visitor != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.add("Authorization", tokenService.getToken());
            headers.add(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
            ArrayList<Tag> mTagList = tagService.getmAllTagList();
            boolean needUpdate = false;
            for (Tag tag : mTagList) {
                if (tag.getTag_name().equals("daily") || tag.getTag_name().equals("customer_in") || tag.getTag_name().equals("customer_out")) {
                    if (!visitor.getTag_id_list().contains(tag.getTag_id())) {
                        visitor.getTag_id_list().add(tag.getTag_id());
                        needUpdate = true;
                    }
                }
            }
            if (needUpdate) {
                HttpEntity entity = new HttpEntity(JSON.toJSONString(visitor), headers);
                ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors/" + visitorId, HttpMethod.PUT, entity, String.class);
                if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                    VisitorPassResult result = new VisitorPassResult();
                    result.setResult(1);
                    result.setName(visitor.getPerson_information().getName());
                    ///去掉空格后去匹配英文字符，“0” ==> English, "1" ==> Chinese
                    boolean isEnglish = visitor.getPerson_information().getName().replaceAll(" ", "").matches("^[a-zA-Z]*");
                    if(isEnglish) {
                        result.setIsChinese(0);
                        result.setMsg( "Allow " + visitor.getPerson_information().getName() + " to visit.");
                    } else {
                        result.setIsChinese(1);
                        result.setMsg("访客" + visitor.getPerson_information().getName() + " 同意来访");
                    }
                    mqttMessageHelper.sendToClient("visitor/response/" + visitorId, JSON.toJSONString(result));
                    logger.warn("Visitor is accept! Visitor ID : ==>{}", visitorId);
                    return ResultGenerator.genSuccessResult();
                }
            }
        }
        return ResultGenerator.genFailResult("同意访客失败！");
    }
}
