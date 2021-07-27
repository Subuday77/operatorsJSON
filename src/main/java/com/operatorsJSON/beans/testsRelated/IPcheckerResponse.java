package com.operatorsJSON.beans.testsRelated;

import org.springframework.stereotype.Component;

@Component
public class IPcheckerResponse {
    private String requestType;
    private String remoteAddress;

    public IPcheckerResponse() {
        // TODO Auto-generated constructor stub
    }

    public IPcheckerResponse(String requestType, String remoteAddress) {
        this.requestType = requestType;
        this.remoteAddress = remoteAddress;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

}
