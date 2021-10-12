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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static com.operatorsJSON.beans.Constants.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/operatorcontroller")

public class OperatorController {
    @Autowired
    Operator operator;
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
        if (actionAllowed(userName, password)) {
            Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operator.getOperatorId());
            if (operatorToCheck.isPresent()) {
                return new ResponseEntity<String>("Operator ID " + operator.getOperatorId() + " already exists.", HttpStatus.IM_USED);
            } else {
                OperatorsDynamicConfig dynamicConfig = new OperatorsDynamicConfig();
                dynamicConfig.setBelongsToOperator(operator.getOperatorId());
                dynamicConfigDAO.addDynamicConfig(dynamicConfig);
                operator.setRelatedConfig(dynamicConfig);
                operatorDAO.addOperator(operator);
                return new ResponseEntity<>(HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateOperator(@RequestBody Operator operator) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (actionAllowed(userName, password)) {
            Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operator.getOperatorId());
            if (operatorToCheck.isPresent()) {
                operatorDAO.addOperator(operator);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Operator ID " + operator.getOperatorId() + " not found.", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/getalloperators")
    public ResponseEntity<?> getAllOperators() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
            if (!loginToCheck.isPresent()) {
                return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
            } else {
                if (loginToCheck.get().getAccessLevel() > 0) {
                    return new ResponseEntity<ArrayList<Operator>>((ArrayList<Operator>) operatorDAO.getAllOperators(), HttpStatus.OK);
                } else {
                    ArrayList<Operator> operatorsToSend = new ArrayList<>();
                    for (Operator operator : loginToCheck.get().getOperators()) {
                        operatorsToSend.add(operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).get());
                    }
                    return new ResponseEntity<ArrayList<Operator>>(operatorsToSend, HttpStatus.OK);
                }
            }
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/cleartokenhistory")
    public ResponseEntity<?> clearTokenHistory() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (actionAllowed(userName, password)) {
            Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operatorId);
            if (operatorToCheck.isPresent()) {
                operatorToCheck.get().getUsedTokens().clear();
                operatorDAO.addOperator(operatorToCheck.get());
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Operator ID " + operatorId + " not found", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteOperator() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (actionAllowed(userName, password)) {
            Optional<Operator> operatorToCheck = operatorDAO.findOperatorByOperatorId(operatorId);
            if (operatorToCheck.isPresent()) {
                if (operatorToCheck.get().getAddedTo() < 0) {
                    operatorDAO.deleteOperator(operatorToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    Optional<Login> loginToEdit = loginDAO.findLoginById(operatorToCheck.get().getAddedTo());
                    List<Operator> tmp = loginToEdit.get().getOperators();
                    loginToEdit.get().getOperators().clear();
                    operatorToCheck.get().setAddedTo(-1);
                    operatorDAO.addOperator(operatorToCheck.get());
                    for (Operator oper : operatorDAO.getAllOperators()) {
                        if (oper.getAddedTo() == loginToEdit.get().getId()) {
                            tmp.add(oper);
                        }
                    }
                    loginDAO.addLogin(loginToEdit.get());
                    operatorDAO.deleteOperator(operatorToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                }
            } else {
                return new ResponseEntity<String>("Operator ID " + operatorId + " not found", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping ("/checkcache")
    public ResponseEntity<?> checkIfCacheExists() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        try {
            if (!CACHE.get(operatorId).isEmpty()) {
                return new ResponseEntity<Boolean>(true,HttpStatus.OK);
            }
        } catch (NullPointerException e) {
            return new ResponseEntity<Boolean>(false, HttpStatus.OK);
        }
        return new ResponseEntity<Boolean>(false, HttpStatus.OK);
    }

//    @DeleteMapping("/deleteall")
//    public ResponseEntity deleteSeveralOperators(@RequestBody ArrayList<Long> operators) {
//        try {
//            for (long operatorId : operators) {
//                operatorDAO.deleteOperator(operatorDAO.findOperatorByOperatorId(operatorId).get());
//            }
//            return new ResponseEntity<String>("Done", HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<String>("Something went wrong", HttpStatus.BAD_REQUEST);
//        }
//    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        return loginToCheck.map(login -> login.getPassword().equals(password)).orElse(false);
    }
}
