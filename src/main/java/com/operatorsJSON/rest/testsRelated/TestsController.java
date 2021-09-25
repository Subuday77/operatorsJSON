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
import java.util.*;

@RestController
@CrossOrigin(origins = "*")

@RequestMapping("/testscontroller")
public class TestsController {
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
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        dynamicConfig.get().setInitialToken(initialToken);
        dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        return prepareResult.authAttempt(operatorId);
    }

    @PostMapping("/starttest")
//    public ResponseEntity<?> startTestFlow(@RequestBody ArrayList<String> casesList) throws JsonProcessingException {
    public ResponseEntity<?> startTestFlow() throws IOException {
        ArrayList<String> casesList = new ArrayList<>();
        casesList.add("Case_1");
        casesList.add("Case_2");
        casesList.add("Case_3");
        casesList.add("Case_4");
        casesList.add("Case_5");
        casesList.add("Case_6");
        casesList.add("Case_7");
        casesList.add("Case_8.1");
        casesList.add("Case_8.2");
        casesList.add("Case_9");
        casesList.add("Case_10.1");
        casesList.add("Case_10.2");
        casesList.add("Case_11");
        casesList.add("Case_12");
        casesList.add("Case_13");
        casesList.add("Case_14");
        casesList.add("Case_15");
        casesList.add("Case_16");
        casesList.add("Case_17.1");
        casesList.add("Case_17.2");
        casesList.add("Case_18.1");
        casesList.add("Case_18.2");
        casesList.add("Case_19.1");
        casesList.add("Case_19.2");
        casesList.add("Case_20.1");
        casesList.add("Case_20.2");
        casesList.add("Case_21.1");
        casesList.add("Case_21.2");
        casesList.add("Case_22.1");
        casesList.add("Case_22.2");
        casesList.add("Case_22.3");
        casesList.add("Case_23");
        casesList.add("Case_24");
        casesList.add("Case_25");
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        return prepareResult.testFlow(operatorId, casesList);
    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        return loginToCheck.map(login -> login.getPassword().equals(password)).orElse(false);
    }
}
