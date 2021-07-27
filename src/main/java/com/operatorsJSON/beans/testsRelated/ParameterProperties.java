package com.operatorsJSON.beans.testsRelated;

import com.operatorsJSON.beans.Constants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ParameterProperties {
    private boolean mandatory;
    private boolean exists;
    private Constants.ERRORSTATE errorState;
    private String dataFormat;
    private ArrayList<String> foundErrors;

    public ParameterProperties() {
    }

    public ParameterProperties(boolean mandatory, boolean exists, Constants.ERRORSTATE errorState, String dataFormat, ArrayList<String> foundErrors) {
        this.mandatory = mandatory;
        this.exists = exists;
        this.errorState = errorState;
        this.dataFormat = dataFormat;
        this.foundErrors = foundErrors;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public ArrayList<String> getFoundErrors() {
        return foundErrors;
    }

    public void setFoundErrors(ArrayList<String> foundErrors) {
        this.foundErrors = foundErrors;
    }

    public Constants.ERRORSTATE getErrorState() {
        return errorState;
    }

    public void setErrorState(Constants.ERRORSTATE errorState) {
        this.errorState = errorState;
    }

    @Override
    public String toString() {
        return "ParameterProperties{" +
                "mandatory=" + mandatory +
                ", exists=" + exists +
                ", errorState='" + errorState + '\'' +
                ", dataFormat='" + dataFormat + '\'' +
                ", foundErrors=" + foundErrors +
                '}';
    }
}
