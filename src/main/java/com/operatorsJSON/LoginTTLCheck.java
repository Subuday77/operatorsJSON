package com.operatorsJSON;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.beans.dbRelated.Login;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@EnableAsync
public class LoginTTLCheck {
    @Autowired
    LoginDAO loginDAO;

    @Async
    @Scheduled (cron = "@midnight")//(cron = "0 0 0 * * *")(fixedRate = 1000 * 60 * 60 * 12)
    public void cleanUp() throws InterruptedException {
        Date date = new Date();
        System.out.println("Cleanup run at " + date);
        for (Login login : loginDAO.getAllLogins()) {
            if (login.getTimestamp() < System.currentTimeMillis() && login.getAccessLevel() == 0 && login.getTimestamp() >= 0) {
                login.setActive(false);
                login.setTimestamp(-1);
                loginDAO.addLogin(login);
            }
        }
    }
}
