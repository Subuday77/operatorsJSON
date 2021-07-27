package com.operatorsJSON;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class Logging {

    public String logParser(String log) {
        if ((log.contains("Case "))
                || (log.contains("-->") || log.contains("<--") || log.contains("hash:")) && (!log.contains("END"))) {
            logRecord(log);
            return log;
        } else if (log.contains("{")) {
            logRecord(log);
            return log;
        }
        return "";
    }

    public void logRecord(String log) {
        String path = "file\\_Test_Log.log";
        try (FileWriter fw = new FileWriter(path, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(log);

        } catch (IOException e) {
            System.out.println("Can't create log");
        }
    }
}
