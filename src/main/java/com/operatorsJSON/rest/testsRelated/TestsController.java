package com.operatorsJSON.rest.testsRelated;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import com.operatorsJSON.PrepareResult;
import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/testscontroller")
public class TestsController {
    private static final int RECOVERY_ACCESS_LEVEL = 3;

    @Autowired
    OperatorDAO operatorDAO;
    @Autowired
    LoginDAO loginDAO;
    @Autowired
    OperatorsDynamicConfigDAO dynamicConfigDAO;
    @Autowired
    HttpServletRequest servletRequest;
    @Autowired
    PrepareResult prepareResult;

    @GetMapping("/initialauth")
    public ResponseEntity<?> initialAuthentication() throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        String initialToken = servletRequest.getHeader("initialToken");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        if (dynamicConfig.isEmpty()) {
            return new ResponseEntity<>("Operator dynamic configuration not found.", HttpStatus.NOT_FOUND);
        }
        dynamicConfig.get().setInitialToken(initialToken);
        dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        return prepareResult.authAttempt(operatorId);
    }

    @GetMapping("/getlastlogrecord")
    public ResponseEntity<?> getLastLogRecord() throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        String caseName = servletRequest.getHeader("caseName");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        if (operatorDAO.findOperatorByOperatorId(operatorId).isEmpty()) {
            return new ResponseEntity<>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(PrepareResult.getLogRecord(caseName, operatorId), HttpStatus.OK);
    }

    @PostMapping("/starttest")
    public ResponseEntity<?> startTestFlow(@RequestBody ArrayList<String> casesList) throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        if (operatorDAO.findOperatorByOperatorId(operatorId).isEmpty()) {
            return new ResponseEntity<>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        return prepareResult.testFlow(operatorId, casesList);
    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        return loginToCheck
                .filter(Login::isActive)
                .filter(login -> login.getAccessLevel() != RECOVERY_ACCESS_LEVEL)
                .map(login -> login.getPassword().equals(password))
                .orElse(false);
    }
}
