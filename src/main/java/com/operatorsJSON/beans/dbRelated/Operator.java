package com.operatorsJSON.beans.dbRelated;

import org.springframework.context.annotation.Scope;

import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "OperatorsPermanentData")
@Component
@Scope(value = "prototype")
public class Operator {
    private long operatorId;
    private String operatorName;
    private String operatorUrl;
    private String contextRootName;
    private String authMethodName;
    private String creditMethodName;
    private String debitMethodName;
    private String rollbackMethodName;
    private String hashKey;
    private long addedTo = -1;

    public Operator() {
    }

    public Operator(long operatorId, String operatorName, String operatorUrl, String contextRootName, String authMethodName, String creditMethodName, String debitMethodName, String rollbackMethodName, String hashKey, long addedTo) {
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.operatorUrl = operatorUrl;
        this.contextRootName = contextRootName;
        this.authMethodName = authMethodName;
        this.creditMethodName = creditMethodName;
        this.debitMethodName = debitMethodName;
        this.rollbackMethodName = rollbackMethodName;
        this.hashKey = hashKey;
        this.addedTo = addedTo;
    }

    @Id
    @Column(nullable = false, updatable = false)
    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }

    @Column
    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    @Column
    public String getOperatorUrl() {
        return operatorUrl;
    }

    public void setOperatorUrl(String operatorUrl) {
        this.operatorUrl = operatorUrl;
    }

    @Column
    public String getContextRootName() {
        return contextRootName;
    }

    public void setContextRootName(String contextRootName) {
        this.contextRootName = contextRootName;
    }

    @Column
    public String getAuthMethodName() {
        return authMethodName;
    }

    public void setAuthMethodName(String authMethodName) {
        this.authMethodName = authMethodName;
    }

    @Column
    public String getCreditMethodName() {
        return creditMethodName;
    }

    public void setCreditMethodName(String creditMethodName) {
        this.creditMethodName = creditMethodName;
    }

    @Column
    public String getDebitMethodName() {
        return debitMethodName;
    }

    public void setDebitMethodName(String debitMethodName) {
        this.debitMethodName = debitMethodName;
    }

    @Column
    public String getRollbackMethodName() {
        return rollbackMethodName;
    }

    public void setRollbackMethodName(String rollbackMethodName) {
        this.rollbackMethodName = rollbackMethodName;
    }

    @Column
    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    @Column
    public long getAddedTo() {
        return addedTo;
    }

    public void setAddedTo(long addedTo) {
        this.addedTo = addedTo;
    }

    @Override
    public String toString() {
        return "Operator{" +
                "operatorId=" + operatorId +
                ", operatorName='" + operatorName + '\'' +
                ", operatorUrl='" + operatorUrl + '\'' +
                ", contextRootName='" + contextRootName + '\'' +
                ", authMethodName='" + authMethodName + '\'' +
                ", creditMethodName='" + creditMethodName + '\'' +
                ", debitMethodName='" + debitMethodName + '\'' +
                ", rollbackMethodName='" + rollbackMethodName + '\'' +
                ", hashKey='" + hashKey + '\'' +
                ", addedTo=" + addedTo +
                '}';
    }
}
