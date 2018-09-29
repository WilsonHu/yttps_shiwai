package com.eservice.iot.service;

/**
 * 访客信息被访问确认或者拒绝后，发送该结果至Pad端
 */
public class VisitorPassResult {
    private int result;
    private String msg;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
