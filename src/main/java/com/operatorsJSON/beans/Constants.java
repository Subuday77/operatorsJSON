package com.operatorsJSON.beans;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Constants {
    public static Map<Long, ArrayList<double[]>> CACHE = new LinkedHashMap<>();

    public static final String[] successfulAuthMandatoryKeys = {"operatorId", "uid", "token", "balance", "currency", "errorCode", "errorDescription", "timestamp"};
    public static final String[] unsuccessfulAuthMandatoryKeys = {"operatorId", "errorCode", "errorDescription", "timestamp"};
    public static final String[] mandatoryKeys = {"operatorId", "roundId", "uid", "token", "balance", "transactionId", "currency", "errorCode", "errorDescription", "timestamp"};
    public static final String[] optionalKeys = {"nickName", "playerTokenAtLaunch", "clientIP", "VIP", "bonusAmount"};
    public static final String[] allKeys = {"operatorId", "uid", "token", "balance", "currency", "errorCode", "errorDescription",
            "timestamp", "roundId", "transactionId", "nickName", "playerTokenAtLaunch", "clientIP", "VIP", "bonusAmount"};

    public static String formatMyDouble(double num) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        return decimalFormat.format(num);
    }

    public enum ERRORSTATE {OK, W, E}
}
