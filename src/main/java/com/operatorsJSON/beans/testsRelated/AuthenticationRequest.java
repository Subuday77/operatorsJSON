package com.operatorsJSON.beans.testsRelated;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationRequest {
    private long operatorId;
    private String token;
    private int platformId = 0;
    private long timestamp;

    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getPlatformId() {
        return platformId;
    }

    public void setPlatformId(int platformId) {
        this.platformId = platformId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AuthenticationRequest [operatorId=" + operatorId + ", token=" + token + ", platformId=" + platformId
                + ", timestamp=" + timestamp + "]";
    }

}
