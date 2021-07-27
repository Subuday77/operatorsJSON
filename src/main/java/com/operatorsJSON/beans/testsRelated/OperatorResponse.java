package com.operatorsJSON.beans.testsRelated;


import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.stereotype.Component;

@Component
@JsonInclude(JsonInclude.Include.NON_DEFAULT)

public class OperatorResponse<T> {
    private long operatorId;
    private long roundId;
    private String uid;
    private String nickName;
    private String playerTokenAtLaunch;
    private String token;
    private T balance;
    private String transactionId;
    private String currency;
    private String language;
    private String date;
    private String clientIP;
    private T VIP;
    private T bonusAmount;
    private int errorCode=-123;
    private String errorDescription;
    private long timestamp;

    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(T operatorId) {
        this.operatorId = (long) operatorId;
    }

    public long getRoundId() {
        return roundId;
    }

    public void setRoundId(T roundId) {
        this.roundId = (long) roundId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public T getBalance() {
        return balance;
    }

    public void setBalance(T balance) {
        this.balance = balance;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public T getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(T bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPlayerTokenAtLaunch() {
        return playerTokenAtLaunch;
    }

    public void setPlayerTokenAtLaunch(String playerTokenAtLaunch) {
        this.playerTokenAtLaunch = playerTokenAtLaunch;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public T getVIP() {
        return VIP;
    }

    public void setVIP(T vIP) {
        VIP = vIP;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OperatorResponse [operatorId=" + operatorId + ", roundId=" + roundId + ", uid=" + uid + ", nickName="
                + nickName + ", playerTokenAtLaunch=" + playerTokenAtLaunch + ", token=" + token + ", balance="
                + balance + ", transactionId=" + transactionId + ", currency=" + currency + ", language=" + language
                + ", date=" + date + ", clientIP=" + clientIP + ", VIP=" + VIP + ", bonusAmount=" + bonusAmount
                + ", errorCode=" + errorCode + ", errorDescription=" + errorDescription + ", timestamp=" + timestamp
                + "]";
    }
}
