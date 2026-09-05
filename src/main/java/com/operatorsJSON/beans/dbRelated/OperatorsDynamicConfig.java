package com.operatorsJSON.beans.dbRelated;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "OperatorsDynamicData")
@Component
@Scope(value = "prototype")
public class OperatorsDynamicConfig {

    private long belongsToOperator;
    private long startingRound;
    private int delay = 0;
    private String initialToken;
    private String sessionToken;
    private String uid;
    private String currency;
    private double initialBalance;
    private double basicBetAmount;
    private boolean tokenUsed;
    private boolean onlyWholeNumbers;

    public OperatorsDynamicConfig() {}

    public OperatorsDynamicConfig(long belongsToOperator, long startingRound, int delay, String initialToken, String sessionToken, String uid, String currency, double initialBalance, double basicBetAmount, boolean tokenUsed, boolean onlyWholeNumbers) {
        this.belongsToOperator = belongsToOperator;
        this.startingRound = startingRound;
        this.delay = delay;
        this.initialToken = initialToken;
        this.sessionToken = sessionToken;
        this.uid = uid;
        this.currency = currency;
        this.initialBalance = initialBalance;
        this.basicBetAmount = basicBetAmount;
        this.tokenUsed = tokenUsed;
        this.onlyWholeNumbers = onlyWholeNumbers;
    }

    @Id @Column public long getBelongsToOperator() { return belongsToOperator; }
    public void setBelongsToOperator(long belongsToOperator) { this.belongsToOperator = belongsToOperator; }
    @Column public long getStartingRound() { return startingRound; }
    public void setStartingRound(long startingRound) { this.startingRound = startingRound; }
    @Column public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
    @Column public String getInitialToken() { return initialToken; }
    public void setInitialToken(String initialToken) { this.initialToken = initialToken; }
    @Column public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    @Column public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    @Column public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    @Column public double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }
    @Column public double getBasicBetAmount() { return basicBetAmount; }
    public void setBasicBetAmount(double basicBetAmount) { this.basicBetAmount = basicBetAmount; }
    @Column public boolean isTokenUsed() { return tokenUsed; }
    public void setTokenUsed(boolean tokenUsed) { this.tokenUsed = tokenUsed; }
    @Column public boolean isOnlyWholeNumbers() { return onlyWholeNumbers; }
    public void setOnlyWholeNumbers(boolean onlyWholeNumbers) { this.onlyWholeNumbers = onlyWholeNumbers; }

    @Override public String toString() {
        return "OperatorsDynamicConfig{" +
                "belongsToOperator=" + belongsToOperator +
                ", startingRound=" + startingRound +
                ", delay=" + delay +
                ", initialToken='[REDACTED]'" +
                ", sessionToken='[REDACTED]'" +
                ", uid='" + uid + '\'' +
                ", currency='" + currency + '\'' +
                ", initialBalance=" + initialBalance +
                ", basicBetAmount=" + basicBetAmount +
                ", tokenUsed=" + tokenUsed +
                ", onlyWholeNumbers=" + onlyWholeNumbers +
                '}';
    }
}
