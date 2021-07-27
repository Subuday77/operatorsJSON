package com.operatorsJSON.beans.testsRelated;

import com.operatorsJSON.SetValues;
import com.operatorsJSON.beans.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RollbackRequest implements SetValues {


    @Autowired
    RequestCommon requestCommon;

    private long operatorId;
    private String uid;
    private String transactionId;
    private int gameId;
    private String token;
    private BigDecimal rollbackAmount; // unique
    private int betTypeID;
    private int serverId;
    private long roundId;
    private String currency;
    private String seatId;
    private int platformId;
    private long tableId;
    private long timestamp;

    @Override
    public void setValues() {
        setOperatorId(requestCommon.getOperatorId());
        setUid(requestCommon.getUid());
        setToken(requestCommon.getToken());
        setBetTypeID(requestCommon.getBetTypeID());
        setRoundId(requestCommon.getRoundId());
        setCurrency(requestCommon.getCurrency());
        setSeatId(requestCommon.getSeatId());
        setPlatformId(requestCommon.getPlatformId());
        setServerId(requestCommon.getServerId());
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

    public BigDecimal getRollbackAmount() {
        return rollbackAmount;
    }

    public void setRollbackAmount(BigDecimal rollbackAmount) {
        this.rollbackAmount = rollbackAmount.setScale(2, RoundingMode.HALF_DOWN);
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
        return "RollbackRequest{" +
                "requestCommon=" + requestCommon +
                ", operatorId=" + operatorId +
                ", uid='" + uid + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", gameId=" + gameId +
                ", token='" + token + '\'' +
                ", rollbackAmount=" + rollbackAmount +
                ", betTypeID=" + betTypeID +
                ", serverId=" + serverId +
                ", roundId=" + roundId +
                ", currency='" + currency + '\'' +
                ", seatId='" + seatId + '\'' +
                ", platformId=" + platformId +
                ", tableId=" + tableId +
                ", timestamp=" + timestamp +
                '}';
    }
}
