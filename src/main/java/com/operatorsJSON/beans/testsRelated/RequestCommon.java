package com.operatorsJSON.beans.testsRelated;

import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestCommon {
    private long operatorId;
    private String uid;
    private String transactionId;
    private int gameId = 1;
    private String token;
    private int betTypeID = 1;
    private int serverId = 102;
    private long roundId;
    private String currency;
    private String seatId = "s" + ((int) (Math.random() * 7) + 1);
    private int platformId = 0;
    private long tableId = 1;
    private long timestamp;

    @Autowired
    OperatorsDynamicConfigDAO dynamicConfigDAO;

    public RequestCommon() {

    }

    public RequestCommon(long operatorId, String uid, String transactionId, int gameId, String token, int betTypeID,
                         int serverId, long roundId, String currency, String seatId, int platformId, long tableId, long timestamp) {
        this.operatorId = operatorId;
        this.uid = uid;
        this.transactionId = transactionId;
        this.gameId = gameId;
        this.token = token;
        this.betTypeID = betTypeID;
        this.serverId = serverId;
        this.roundId = roundId;
        this.currency = currency;
        this.seatId = seatId;
        this.platformId = platformId;
        this.tableId = tableId;
        this.timestamp = timestamp;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getBetTypeID() {
        return betTypeID;
    }

    public void setBetTypeID(int betTypeID) {
        this.betTypeID = betTypeID;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public long getRoundId() {
        return roundId;
    }

    public void setRoundId(long roundId) {
        this.roundId = roundId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public int getPlatformId() {
        return platformId;
    }

    public void setPlatformId(int platformId) {
        this.platformId = platformId;
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



    @Override
    public String toString() {
        return "RequestCommon [operatorId=" + operatorId + ", uid=" + uid + ", transactionId=" + transactionId
                + ", gameId=" + gameId + ", token=" + token + ", betTypeID=" + betTypeID + ", serverId=" + serverId
                + ", roundId=" + roundId + ", currency=" + currency + ", seatId=" + seatId + ", platformId="
                + platformId + ", tableId=" + tableId + ", timestamp=" + timestamp + "]";
    }


    public void setValues(long operatorId) {
        setOperatorId(operatorId);
        setToken(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getSessionToken());
        setUid(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getUid());
        setRoundId(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getStartingRound());
        setCurrency(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getCurrency());
    }
}
