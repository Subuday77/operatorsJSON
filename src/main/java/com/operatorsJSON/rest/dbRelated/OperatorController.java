package com.operatorsJSON.rest.dbRelated;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/operatorcontroller")
public class OperatorController {
    private static final int RECOVERY_ACCESS_LEVEL = 3;

    @Autowired
    OperatorDAO operatorDAO;
    @Autowired
    LoginDAO loginDAO;
    @Autowired
    OperatorsDynamicConfigDAO dynamicConfigDAO;
    @Autowired
    HttpServletRequest servletRequest;

    @PostMapping("/create")
    public ResponseEntity<?> createOperator(@RequestBody Operator operator) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        if (operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).isPresent()) {
            return new ResponseEntity<>("Operator ID " + operator.getOperatorId() + " already exists.", HttpStatus.IM_USED);
        }

        OperatorsDynamicConfig dynamicConfig = new OperatorsDynamicConfig();
        dynamicConfig.setBelongsToOperator(operator.getOperatorId());
        dynamicConfigDAO.addDynamicConfig(dynamicConfig);
        operator.setRelatedConfig(dynamicConfig);
        operatorDAO.addOperator(operator);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateOperator(@RequestBody Operator operator) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        if (operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).isEmpty()) {
            return new ResponseEntity<>("Operator ID " + operator.getOperatorId() + " not found.", HttpStatus.NOT_FOUND);
        }
        operatorDAO.addOperator(operator);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getalloperators")
    public ResponseEntity<?> getAllOperators() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
        if (loginToCheck.get().getAccessLevel() > 0) {
            return new ResponseEntity<>(new ArrayList<>(operatorDAO.getAllOperators()), HttpStatus.OK);
        }

        ArrayList<Operator> operatorsToSend = new ArrayList<>();
        for (Operator operator : loginToCheck.get().getOperators()) {
            operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).ifPresent(operatorsToSend::add);
        }
        return new ResponseEntity<>(operatorsToSend, HttpStatus.OK);
    }

    @GetMapping("/cleartokenhistory")
    public ResponseEntity<?> clearTokenHistory() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToCheck.isEmpty()) {
            return new ResponseEntity<>("Operator ID " + operatorId + " not found", HttpStatus.NOT_FOUND);
        }
        operatorToCheck.get().getUsedTokens().clear();
        operatorDAO.addOperator(operatorToCheck.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteOperator() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToCheck.isEmpty()) {
            return new ResponseEntity<>("Operator ID " + operatorId + " not found", HttpStatus.NOT_FOUND);
        }

        if (operatorToCheck.get().getAddedTo() >= 0) {
            loginDAO.findLoginById(operatorToCheck.get().getAddedTo()).ifPresent(loginToEdit -> {
                loginToEdit.getOperators().removeIf(operator -> operator.getOperatorId() == operatorId);
                loginDAO.addLogin(loginToEdit);
            });
        }
        operatorDAO.deleteOperator(operatorToCheck.get());
        return new ResponseEntity<>(HttpStatus.OK);
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
