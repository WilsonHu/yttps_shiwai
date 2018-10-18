package com.eservice.iot.service;

/**
 * 访客信息被访问确认或者拒绝后，发送该结果至Pad端
 */
public class VisitorPassResult {
    private int result;
    private String name;
    private String msg;
    /**
     * "1" ==> Chinese; "0" ==> English
     */
    private int isChinese = 1;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIsChinese() {
        return isChinese;
    }

    public void setIsChinese(int isChinese) {
        this.isChinese = isChinese;
    }
}
