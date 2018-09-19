package com.eservice.iot.model;

public class ResponseModel {
    private String result;

    private int rtn;

    private String message;

    private int total;

    public String getResult() {
        return result;
    }

    public int getRtn() {
        return rtn;
    }

    public String getMessage() {
        return message;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setRtn(int rtn) {
        this.rtn = rtn;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
