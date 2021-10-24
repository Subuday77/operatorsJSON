package com.operatorsJSON;

import com.operatorsJSON.beans.testsRelated.ResultToSend;
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
            logRecord(log, "");
            return log;
        } else if (log.contains("{")) {
            logRecord(log, "");
            return log;
        }
        return "";
    }

    public String logParser(String log, String operator) {
        if (log.contains("Case_")) {
            logRecord(log, operator);
            return log;
        }
        return "";
    }


    public void logRecord(String log, String operator) {
        String operatorId = operator;
        if (operator.equals("")) {
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
