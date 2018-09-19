package com.eservice.iot.web;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.response.OapiUserListResponse;
import com.eservice.iot.core.Result;
import com.eservice.iot.core.ResultGenerator;
import com.eservice.iot.model.ResponseModel;
import com.eservice.iot.model.SendVisitorData;
import com.eservice.iot.model.Staff;
import com.eservice.iot.model.Visitor;
import com.eservice.iot.service.DingDingService;
import com.eservice.iot.service.ResponseCode;
import com.eservice.iot.service.StaffService;
import com.eservice.iot.service.TokenService;
import com.taobao.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Class Description: DingDing Controller
 *
 * @author Wilson Hu
 * @date 2018/08/21.
 */
@RestController
@RequestMapping("/ding")
public class DingController {

    private final static Logger logger = LoggerFactory.getLogger(DingController.class);
    @Value("${park_base_url}")
    private String PARK_BASE_URL;

    @Resource
    private DingDingService dingDingService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StaffService staffService;

    @PostMapping("/visit")
    public Result visit(@RequestBody String jsonData) {
        SendVisitorData data = JSONObject.parseObject(jsonData, SendVisitorData.class);
        String staffId = data.getStaffId();
        Staff staff = null;
        for (int i = 0; i < staffService.getStaffList().size() && (staff == null); i++) {
            if (staffService.getStaffList().get(i).getStaffId().equals(staffId)) {
                staff = staffService.getStaffList().get(i);
            }
        }
        if (staff != null) {
            String visitorId = data.getVisitorId();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT, "application/json");
            headers.add("Authorization", tokenService.getToken());
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(PARK_BASE_URL + "/visitors/?visitor_id_list=" + visitorId, HttpMethod.GET, entity, String.class);
            if (responseEntity.getStatusCodeValue() == ResponseCode.OK) {
                String body = responseEntity.getBody();
                if (body != null) {
                    ResponseModel responseModel = JSONObject.parseObject(body, ResponseModel.class);
                    if (responseModel != null && responseModel.getResult() != null) {
                        ArrayList<Visitor> tmpList = (ArrayList<Visitor>) JSONArray.parseArray(responseModel.getResult(), Visitor.class);
                        if (tmpList != null && tmpList.size() == 1) {
                            if (dingDingService != null) {
                                boolean success = false;
                                try {
                                    ///发送钉钉
                                    if (dingDingService.sendVisitorMessage(staff.getPersonInformation().getCard_no(), tmpList.get(0)) != null) {
                                        success = true;
                                    }
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                }
                                if (success) {
                                    return ResultGenerator.genSuccessResult();
                                } else {
                                    return ResultGenerator.genFailResult("发送失败！");
                                }
                            } else {
                                return ResultGenerator.genFailResult("钉钉服务未开启！");
                            }
                        }
                    }
                } else {
                    logger.error("获取访客信息失败！");
                }
            }
        }

        return ResultGenerator.genFailResult("发送失败！");
    }

    @PostMapping("/list")
    public Result list(@RequestParam(defaultValue = "") String name) {
        List<OapiUserListResponse.Userlist> resultList = new ArrayList<>();
        if (name != null || name.equals("")) {
            for (OapiUserListResponse.Userlist item : dingDingService.getDingDingUserList()) {
                if(item.getName().contains(name)) {
                    resultList.add(item);
                }
            }
        } else {
            resultList =  dingDingService.getDingDingUserList();
        }
        return ResultGenerator.genSuccessResult(resultList);
    }
}
