package com.operatorsJSON.rest.dbRelated;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.beans.dbRelated.Operator;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.operatorsJSON.beans.Constants.HASHKEY;

@RestController
@CrossOrigin(origins = "*")

@RequestMapping("/logincontroller")
public class LoginController {
    @Autowired
    Login login;
    @Autowired
    LoginDAO loginDAO;
    @Autowired
    OperatorDAO operatorDAO;
    @Autowired
    HttpServletRequest servletRequest;

    @PostConstruct
    public void createDefaultLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        Optional<Login> defaultLogin = loginDAO.findLoginByUserName("SuperAdmin");
        if (!defaultLogin.isPresent()) {
            Login defaultLoginToCreate = new Login();
            defaultLoginToCreate.setUserName("SuperAdmin");
            defaultLoginToCreate.setPassword(encode("P@ssword02091945"));
            defaultLoginToCreate.setAccessLevel(3);
            loginDAO.addLogin(defaultLoginToCreate);
        }
    }

    @GetMapping("/login")
    public ResponseEntity<?> login() throws NoSuchAlgorithmException, InvalidKeyException {
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        String userNameToCheck = servletRequest.getHeader("userName");
        String passwordToCheck = servletRequest.getHeader("password");
        Optional<Login> login = loginDAO.findLoginByUserName(userNameToCheck);
        if (login.isPresent()) {
            if (login.get().getPassword().equals(encode(passwordToCheck))) {

                return new ResponseEntity<Optional<Login>>(login, HttpStatus.OK);
            }
//        } else {
//            return new ResponseEntity<String>("Login " + userNameToCheck + " not found.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLogin(@RequestBody Login login) throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginByUserName(login.getUserName());
            if (!loginToCheck.isPresent()) {
                login.setPassword(encode(login.getPassword()));
                loginDAO.addLogin(login);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("User " + login.getUserName() + " already exists", HttpStatus.IM_USED);
            }
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/renew")
    public ResponseEntity<?> updateTimestamp(@RequestBody Login login) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(login.getUserName());
        if (loginToCheck.isPresent()) {
            loginToCheck.get().setTimestamp(System.currentTimeMillis());
            loginDAO.addLogin(loginToCheck.get());
            return new ResponseEntity<String>("Timestamp updated", HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/addremoveoperators")
    public ResponseEntity<?> addRemoveOperators(@RequestBody ArrayList<Operator> operators) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            List<Operator> tmp = loginToCheck.get().getOperators();
            Map<Long, Long> operatorsOld = new HashMap<>();
            for (Operator operator : operatorDAO.getAllOperators()) {
                operatorsOld.put(operator.getOperatorId(), operator.getAddedTo());
            }
            if (loginToCheck.isPresent()) {
                for (Operator operator : operators) {
                    if (operator.getAddedTo() == loginId) {
                        if (operatorsOld.get(operator.getOperatorId()) != loginId) {
                            operator.setAddedTo(loginToCheck.get().getId());
                            operatorDAO.addOperator(operator);
                            tmp.add(operator);
                            loginDAO.addLogin(loginToCheck.get());
                        }
                    } else {
                        operator.setAddedTo(-1l);
                        operatorDAO.addOperator(operator);
                        loginToCheck.get().getOperators().clear();
                        for (Operator oper : operatorDAO.getAllOperators()) {
                            if (oper.getAddedTo() == loginId) {
                                tmp.add(oper);
                            }
                        }
                        loginDAO.addLogin(loginToCheck.get());
                    }
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Login not found.", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/update")
    public ResponseEntity<?> updateLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String oldPassword = servletRequest.getHeader("oldPassword");
        String newPassword = servletRequest.getHeader("newPassword");
        if (actionAllowed(userName, encode(oldPassword))) {
            Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
            if (!loginToCheck.isPresent()) {
                return new ResponseEntity<String>("Login " + userName + " not found.", HttpStatus.NOT_FOUND);
            } else {
                loginToCheck.get().setPassword(encode(newPassword));
                loginDAO.addLogin(loginToCheck.get());
                return new ResponseEntity<String>(loginToCheck.get().getPassword(), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/setdefpass")
    public ResponseEntity<?> setDefaultPassword() throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        int accessLevel = Integer.parseInt(servletRequest.getHeader("accessLevel"));
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (!loginToCheck.isPresent()) {
            return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
        } else {
            switch (accessLevel) {
                case 0:
                    loginToCheck.get().setPassword(encode("A123123"));
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                case 1:
                    loginToCheck.get().setPassword(encode("A456456"));
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                case 2:
                    loginToCheck.get().setPassword(encode("A12345678"));
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                case 3:
                    loginToCheck.get().setPassword(encode("P@ssword02091945"));
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
            }
//            if (accessLevel == 2) {
//                loginToCheck.get().setPassword(encode("A12345678"));
//                loginDAO.addLogin(loginToCheck.get());
//                return new ResponseEntity<>(HttpStatus.OK);
//            } else if (accessLevel == 3) {
//                loginToCheck.get().setPassword(encode("P@ssword02091945"));
//                loginDAO.addLogin(loginToCheck.get());
//                return new ResponseEntity<>(HttpStatus.OK);
//            }
        }
        return new ResponseEntity<String>("Bad request", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/getallogins")
    public ResponseEntity<?> getAllLogins() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        int accessLevel = Integer.parseInt(servletRequest.getHeader("accessLevel"));
        List<Login> loginsToSend = new ArrayList<>();
        if (actionAllowed(userName, password)) {
            ArrayList<Login> allLogins = (ArrayList<Login>) loginDAO.getAllLogins();
            for (Login login : allLogins) {
                if (accessLevel == 3 && login.getAccessLevel() == 2) {
                    loginsToSend.add(login);
                } else if (accessLevel == 2 && login.getAccessLevel() < 2) {
                    loginsToSend.add(login);
                }
            }
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            loginsToSend.sort(Comparator.comparing(Login::getUserName));
            return new ResponseEntity<ArrayList<Login>>((ArrayList<Login>) loginsToSend, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Forbidden!", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/changestate")
    public ResponseEntity<?> changeLoginState() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        String state = servletRequest.getHeader("state");
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            if (loginToCheck.isPresent()) {
                if (state.equals("Active")) {
                    loginToCheck.get().setActive(true);
                    loginToCheck.get().setTimestamp(System.currentTimeMillis() + 2629800000l);
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    loginToCheck.get().setActive(false);
                    loginToCheck.get().setTimestamp(-1);
                    loginDAO.addLogin(loginToCheck.get());
                    return new ResponseEntity<>(HttpStatus.OK);
                }
            } else {
                return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/renamelogin")
    public ResponseEntity<?> renameLogin() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        String newName = servletRequest.getHeader("newName");
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            if (loginToCheck.isPresent()) {
                loginToCheck.get().setUserName(newName);
                loginDAO.addLogin(loginToCheck.get());
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/checkal")
    public ResponseEntity<?> getAccessLevel() {
        String userName = servletRequest.getHeader("userName");
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (loginToCheck.isPresent()) {
            return new ResponseEntity<Integer>(loginToCheck.get().getAccessLevel(), HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/changettl")
    public ResponseEntity<?> changeTTL() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        long newValue = Long.parseLong(servletRequest.getHeader("newValue"));
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            if (loginToCheck.isPresent()) {
                loginToCheck.get().setTimestamp(System.currentTimeMillis() + (newValue * 86400000));
                if (newValue <= 0) {
                    loginToCheck.get().setTimestamp(-1);
                    loginToCheck.get().setActive(false);
                }
//                System.out.println(System.currentTimeMillis() + (newValue * 86400000));
//                System.out.println(newValue);
//                System.out.println(System.currentTimeMillis());
//                System.out.println(loginToCheck.get().getTimestamp());
                loginDAO.addLogin(loginToCheck.get());

                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Login not found.", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/getaddedoperators")
    public ResponseEntity<?> getAddedOperators() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            if (loginToCheck.isPresent()) {
                List<Operator> allRelevantOperators = new ArrayList<>();
                List<Operator> allOperators = (List<Operator>) operatorDAO.getAllOperators();
                for (Operator operator : allOperators) {
                    if (operator.getAddedTo() == -1 || operator.getAddedTo() == loginId) {
                        allRelevantOperators.add(operator);
                    }
                }
                return new ResponseEntity<List<Operator>>(allRelevantOperators, HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Login not found.", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteLogin() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (actionAllowed(userName, password)) {
            Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
            if (loginToCheck.isPresent()) {
                loginToCheck.get().getOperators().clear();
                for (Operator operator : operatorDAO.getAllOperators()) {
                    if (operator.getAddedTo() == loginToCheck.get().getId()) {
                        operator.setAddedTo(-1l);
                        operatorDAO.addOperator(operator);
                    }
                }
                loginDAO.deleteLogin(loginToCheck.get());
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Login " + loginToCheck.get().getUserName() + " not found.", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
    }

    private static String encode(String password) throws NoSuchAlgorithmException, InvalidKeyException {

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(HASHKEY.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.encodeBase64String(sha256_HMAC.doFinal(password.getBytes()));
    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (loginToCheck.isPresent()) {
            return loginToCheck.get().getPassword().equals(password);
        }
        return false;
    }

    private static long timestampToTTL(long timestamp) {
        return (System.currentTimeMillis() - timestamp) / 1000 / 60 / 60 / 24;
    }
}
