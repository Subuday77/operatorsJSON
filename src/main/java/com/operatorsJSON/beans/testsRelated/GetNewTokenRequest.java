package com.operatorsJSON.beans.testsRelated;

import com.operatorsJSON.SetValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetNewTokenRequest implements SetValues {

    @Autowired
    RequestCommon requestCommon;

    private long operatorId;
    private String uid;
    private int gameId;
    private String currentToken;
    private long tableId;
    private long timestamp;


    @Override
    public void setValues() {
        setOperatorId(requestCommon.getOperatorId());
        setUid(requestCommon.getUid());
        setCurrentToken(requestCommon.getToken());
        setGameId(requestCommon.getGameId());
        setTableId(requestCommon.getTableId());
    }


    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(String currentToken) {
        this.currentToken = currentToken;
    }

    public long getTableId() {
        return tableId;
    }

    public void setTableId(long tableId) {
        this.tableId = tableId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
