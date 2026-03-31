package com.example.myapplication.http.model;

import okhttp3.Headers;

/**
 * ================================================
 * 作    者：ZJF-summoner
 * 版    本：1.0
 * 创建日期：2022/7/7 9:13
 * 描    述：统一接口数据结构
 * 修订历史：
 * ================================================
 */
public class HttpData<T> {

    //初始结构 内有data content 对象
    /**
     * 请求头
     */
    private Headers headers;

    /**
     * 返回码
     */
    private String code;
    /**
     * 提示语
     */
    private String message;
    /**
     * 状态
     */
    private String messageType;

    public String getResultDesc() {
        return message;
    }

    public String getMessageType() {
        return messageType;
    }

    /**
     * 数据
     */
    private T data;
    /**
     * 数据
     */
    private T content;

    public T getContent() {
        return content;
    }

    public HttpData<T> setContent(T content) {
        this.content = content;
        return this;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getResultCode() {
        return code;
    }


    public T getData() {
        return data;
    }

    /**
     * 是否请求成功
     */
    public boolean isRequestSuccess() {
        return "0".equals(code);
    }
    /**
     * 是否请求成功
     */
    public boolean isRequestSuccessError() {
        return !"0".equals(code);
    }

    /**
     * 是否 Token 失效
     */
    public boolean isTokenFailure() {
        return "40100".equals(code);
    }
}