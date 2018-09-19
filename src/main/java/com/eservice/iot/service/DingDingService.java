package com.eservice.iot.service;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiDepartmentListRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiUserListRequest;
import com.dingtalk.api.response.OapiDepartmentListResponse;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.dingtalk.api.response.OapiUserListResponse;
import com.eservice.iot.model.Visitor;
import com.taobao.api.ApiException;
import org.apache.poi.hssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author HT
 */
@Component
public class DingDingService {

    private final static Logger logger = LoggerFactory.getLogger(TagService.class);

    @Autowired
    private TokenService tokenService;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");

    private ThreadPoolTaskExecutor mExecutor;

    private List<OapiUserListResponse.Userlist> dingdingUserList = new ArrayList<>();

    private List<OapiUserListResponse.Userlist> dingdingUserList2 = new ArrayList<>();

    @Value("${dd_department_id}")
    private String DEPARTMENT_ID;

    @Value("${dd_message_agent_id}")
    private String AGENT_ID;

    @Value("${dd_department_id_2}")
    private String DEPARTMENT_ID_2;

    @Value("${dd_message_agent_id_2}")
    private String AGENT_ID_2;

    @Value("${visitor_confirm_url}")
    private String visitorConfirmUrl;

    private String mAgentID = "";

    /**
     * 接口调用有次数限制，每30分钟更新钉钉用户列表
     */
    @Scheduled(fixedRate = 1000 * 60 * 30)
    private void fetchDingDingUserList() {

        boolean updateSuccess = false;
        DingTalkClient clientDep = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
        OapiDepartmentListRequest requestDep = new OapiDepartmentListRequest();
        requestDep.setHttpMethod("GET");
        try {
            OapiDepartmentListResponse responseDep = clientDep.execute(requestDep, tokenService.getDDToken());
            List<OapiDepartmentListResponse.Department> departmentList = responseDep.getDepartment();
            List<OapiUserListResponse.Userlist> allUserList = new ArrayList<>();
            for (int i = 0; i < departmentList.size(); i++) {
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/list");
                OapiUserListRequest request = new OapiUserListRequest();
                request.setDepartmentId(departmentList.get(i).getId());
                request.setHttpMethod("GET");
                try {
                    OapiUserListResponse response = client.execute(request, tokenService.getDDToken());
                    if (response != null && response.isSuccess() && response.getErrcode().equals(0L)) {
                        List<OapiUserListResponse.Userlist> tmpList = response.getUserlist();
                        for (OapiUserListResponse.Userlist item : tmpList) {
                            boolean exist = false;
                            for (int j = 0; j < allUserList.size() && !exist; j++) {
                                if (allUserList.get(j).getUserid().equals(item.getUserid())) {
                                    exist = true;
                                }
                            }
                            if (!exist) {
                                allUserList.add(item);
                            }
                        }
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
            updateSuccess = true;
            dingdingUserList = allUserList;
            createDingDingUserExcelFile("./");
            ///在执行成功后停止线程池
            if (mExecutor != null) {
                mExecutor.shutdown();
                mExecutor = null;
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        if (!updateSuccess) {
            //如果获取用户失败，启动线程
            if (mExecutor == null) {
                mExecutor = new ThreadPoolTaskExecutor();
                mExecutor.setCorePoolSize(1);
                mExecutor.setMaxPoolSize(2);
                mExecutor.setThreadNamePrefix("YTTPS-DingDing-");
                mExecutor.initialize();
            }
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        fetchDingDingUserList();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /**
     * 接口调用有次数限制，每30分钟更新钉钉用户列表
     */
    @Scheduled(fixedRate = 1000 * 60 * 30)
    private void fetchDingDingUserList2() {

        boolean updateSuccess = false;
        DingTalkClient clientDep = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
        OapiDepartmentListRequest requestDep = new OapiDepartmentListRequest();
        requestDep.setHttpMethod("GET");
        try {
            OapiDepartmentListResponse responseDep = clientDep.execute(requestDep, tokenService.getDDToken2());
            List<OapiDepartmentListResponse.Department> departmentList = responseDep.getDepartment();
            List<OapiUserListResponse.Userlist> allUserList = new ArrayList<>();
            for (int i = 0; i < departmentList.size(); i++) {
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/list");
                OapiUserListRequest request = new OapiUserListRequest();
                request.setDepartmentId(departmentList.get(i).getId());
                request.setHttpMethod("GET");
                try {
                    OapiUserListResponse response = client.execute(request, tokenService.getDDToken2());
                    if (response != null && response.isSuccess() && response.getErrcode().equals(0L)) {
                        List<OapiUserListResponse.Userlist> tmpList = response.getUserlist();
                        for (OapiUserListResponse.Userlist item : tmpList) {
                            boolean exist = false;
                            for (int j = 0; j < allUserList.size() && !exist; j++) {
                                if (allUserList.get(j).getUserid().equals(item.getUserid())) {
                                    exist = true;
                                }
                            }
                            if (!exist) {
                                allUserList.add(item);
                            }
                        }
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
            updateSuccess = true;
            dingdingUserList2 = allUserList;
            createDingDingUserExcelFile("./2_");
            ///在执行成功后停止线程池
            if (mExecutor != null) {
                mExecutor.shutdown();
                mExecutor = null;
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        if (!updateSuccess) {
            //如果获取用户失败，启动线程
            if (mExecutor == null) {
                mExecutor = new ThreadPoolTaskExecutor();
                mExecutor.setCorePoolSize(1);
                mExecutor.setMaxPoolSize(2);
                mExecutor.setThreadNamePrefix("YTTPS-DingDing-");
                mExecutor.initialize();
            }
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        fetchDingDingUserList2();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public List<OapiUserListResponse.Userlist> getDingDingUserList() {
        List<OapiUserListResponse.Userlist> list = new ArrayList<>();
        list.addAll(dingdingUserList);
        list.addAll(dingdingUserList2);
        return list;
    }

    /**
     * @param userList, 每个user之间用逗号分开
     * @return
     */
    public OapiMessageCorpconversationAsyncsendV2Response sendTextMessage(String userList, String message) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");

        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setUseridList(userList);
        request.setAgentId(Long.valueOf(AGENT_ID));
        request.setToAllUser(false);

        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype("text");
        msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
        msg.getText().setContent(message);

        request.setMsg(msg);
        OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request,tokenService.getDDToken());
        if(response.isSuccess() && response.getErrcode().equals(0L)) {
            logger.info("Send dingding message success!");
        } else {
            logger.error("Send dingding message failed：{}", response.getErrmsg());
        }
        return response;
    }

    /**
     * @param
     * @return
     */
    public OapiMessageCorpconversationAsyncsendV2Response sendVisitorMessage(String userID, Visitor visitor) throws ApiException {
        OapiUserListResponse.Userlist user = null;
        for (int i = 0; i < dingdingUserList.size(); i++) {
            if (dingdingUserList.get(i).getUserid().equals(userID)) {
                user = dingdingUserList.get(i);
                mAgentID = AGENT_ID;
            }
        }
        if (user == null) {
            for (int i = 0; i < dingdingUserList2.size(); i++) {
                if (dingdingUserList2.get(i).getUserid().equals(userID)) {
                    user = dingdingUserList2.get(i);
                    mAgentID = AGENT_ID_2;
                }
            }
        }

        if (user != null) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");
            OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
            request.setUseridList(userID);
            request.setAgentId(Long.valueOf(mAgentID));
            request.setToAllUser(false);

            OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();

            String title = user.getName() + "，您好！访客【" + visitor.getPerson_information().getName() + "】已于" + formatter.format(new Date()) + "到达！";
            msg.setMsgtype("action_card");
            msg.setActionCard(new OapiMessageCorpconversationAsyncsendV2Request.ActionCard());
            msg.getActionCard().setTitle(title);
            msg.getActionCard().setMarkdown("### " + visitor.getPerson_information().getName() + " " + visitor.getPerson_information().getPhone());
            msg.getActionCard().setSingleTitle("时间：" + formatter.format(new Date()));
            String url = visitorConfirmUrl + "/#/visitor?visitor_id=" + visitor.getVisitor_id();
            logger.warn("visitor url => " + url);
            msg.getActionCard().setSingleUrl(url);

            request.setMsg(msg);
            OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request, mAgentID.equals(AGENT_ID) ? tokenService.getDDToken() : tokenService.getDDToken2());
	        if(response.isSuccess() && response.getErrcode().equals(0L)) {
	            logger.info("Send dingding message success!");
	        } else {
	            logger.error("Send dingding message failed：{}", response.getErrmsg());
	        }
	        return response;
        } else {
            return null;
        }
    }


    public void createDingDingUserExcelFile(String path) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("姓名+UserID");

        ///设置要导出的文件的名字
        String fileName = "钉钉用户表" + ".xls";
        //新增数据行，并且设置单元格数据

        int rowNum = 1;

        String[] headers = {"姓名", "钉钉用户ID"};
        //headers表示excel表中第一行的表头

        HSSFRow row = sheet.createRow(0);
        //在excel表中添加表头

        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell(i);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }

        //在表中存放查询到的数据放入对应的列
        for (OapiUserListResponse.Userlist teacher : path.contains("2") ? dingdingUserList2 : dingdingUserList) {
            HSSFRow row1 = sheet.createRow(rowNum);
            row1.createCell(0).setCellValue(teacher.getName());
            row1.createCell(1).setCellValue(teacher.getUserid());
            rowNum++;
        }
        try {
            FileOutputStream out = new FileOutputStream(path + fileName);
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
