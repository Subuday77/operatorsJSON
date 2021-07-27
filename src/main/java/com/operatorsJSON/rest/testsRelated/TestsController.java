package com.operatorsJSON.rest.testsRelated;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    public ResponseEntity<?> initialAuthentication() throws JsonProcessingException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        String initialToken = servletRequest.getHeader("initialToken");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (!operatorToTest.isPresent()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicCondigById(operatorId);
        dynamicConfig.get().setInitialToken(initialToken);
        dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        return prepareResult.authAttempt(operatorId);
    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (loginToCheck.isPresent()) {
            return loginToCheck.get().getPassword().equals(password);
        }
        return false;
    }
}
