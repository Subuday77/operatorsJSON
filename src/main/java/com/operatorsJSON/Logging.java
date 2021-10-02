package com.operatorsJSON;

import com.operatorsJSON.beans.testsRelated.ResultToSend;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class Logging {
    @Autowired
    ResultToSend resultToSend;
    @Autowired
    PrepareResult prepareResult;

    public String logParser(String log) {
        if ((log.contains("Case_"))
                || (log.contains("-->") || log.contains("<--") || log.contains("hash:"))
                && (!log.contains("END") && !log.contains("https://ip-checker2000.herokuapp.com/request/send/"))) {
//            resultToSend.setLog(resultToSend.getLog() + log + "\n");
            logRecord(log, "");
            return log;
        } else if (log.contains("{")) {
//            resultToSend.setLog(resultToSend.getLog() + log + "\n");
            logRecord(log, "");
            return log;
        }
        return "";
    }

    public String logParser(String log, String operator) {
//        if (resultToSend.getRequest() != null) {
//            JSONObject requestJson = new JSONObject(resultToSend.getRequest());
//            operatorId = String.valueOf(requestJson.optLong("operatorId"));
//        }
        if (log.contains("Case_")) {
//            resultToSend.setLog(resultToSend.getLog() + log + "\n");
            logRecord(log, operator);
            return log;
        }
        return "";
    }

//    public String logParser(String log, String operatorId, ResultToSend resultToSend) {
////        if (resultToSend.getRequest() != null) {
////            JSONObject requestJson = new JSONObject(resultToSend.getRequest());
////            operatorId = String.valueOf(requestJson.optLong("operatorId"));
////        }
//        if ((log.contains("Case "))
//                || (log.contains("-->") || log.contains("<--") || log.contains("hash:")) && (!log.contains("END"))) {
//            resultToSend.setLog(resultToSend.getLog() + log + "\n");
//            logRecord(log, operatorId);
//            return log;
//        } else if (log.contains("{")) {
//            resultToSend.setLog(resultToSend.getLog() + log + "\n");
//            logRecord(log, operatorId);
//            return log;
//        }
//        return "";
//    }

    public void logRecord(String log, String operator) {
        String operatorId = operator;
        if (operator.equals("")) {
//            JSONObject requestJson = new JSONObject(resultToSend.getRequest());
            operatorId = prepareResult.getOperatorInProcess();
        }
        if (operatorId != null && !log.contains("145.239.222.15")) {
            String path = "file\\" + operatorId + "_Test_Log.log";
            try (FileWriter fw = new FileWriter(path, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(log);

            } catch (IOException e) {
                System.out.println("Can't create log");
            }
        }
    }
}
