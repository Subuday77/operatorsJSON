package com.operatorsJSON.beans.testsRelated;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class ResultToSend {
    private String request;
    private String response;
    private HashMap<String, ParameterProperties> checkResults;
    private String expectedResponse;
    private String log;

    public ResultToSend() {
    }

    public ResultToSend(String request, String response, HashMap<String, ParameterProperties> checkResults, String expectedResponse, String log) {
        this.request = request;
        this.response = response;
        this.checkResults = checkResults;
        this.expectedResponse = expectedResponse;
        this.log = log;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public HashMap<String, ParameterProperties> getCheckResults() {
        return checkResults;
    }

    public void setCheckResults(HashMap<String, ParameterProperties> checkResults) {
        this.checkResults = checkResults;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    @Override
    public String toString() {
        return "ResultToSend{" +
                "request=" + request +
                ", response=" + response +
                ", checkResults=" + checkResults +
                ", expectedResponse=" + expectedResponse +
                ", log='" + log + '\'' +
                '}';
    }
}
