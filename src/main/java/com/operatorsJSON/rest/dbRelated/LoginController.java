package com.operatorsJSON.rest.dbRelated;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.retrofit.ResponseServiceClient;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/logincontroller")
public class LoginController {
    private static final int RECOVERY_ACCESS_LEVEL = 3;
    private static final int ADMIN_ACCESS_LEVEL = 2;

    @Autowired
    LoginDAO loginDAO;
    @Autowired
    OperatorDAO operatorDAO;
    @Autowired
    HttpServletRequest servletRequest;
    @Autowired
    ResponseServiceClient responseServiceClient;

    @PostConstruct
    public void createDefaultLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        String bootstrapPassword = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }

        String bootstrapUser = Optional.ofNullable(System.getenv("BOOTSTRAP_ADMIN_USERNAME"))
                .filter(value -> !value.isBlank())
                .orElse("SuperAdmin");

        Optional<Login> defaultLogin = loginDAO.findLoginByUserName(bootstrapUser);
        if (defaultLogin.isEmpty()) {
            Login defaultLoginToCreate = new Login();
            defaultLoginToCreate.setUserName(bootstrapUser);
            defaultLoginToCreate.setPassword(encode(bootstrapPassword));
            defaultLoginToCreate.setAccessLevel(RECOVERY_ACCESS_LEVEL);
            loginDAO.addLogin(defaultLoginToCreate);
        }
    }

    @GetMapping("/login")
    public ResponseEntity<?> login() throws NoSuchAlgorithmException, InvalidKeyException {
        String userNameToCheck = servletRequest.getHeader("userName");
        String passwordToCheck = servletRequest.getHeader("password");
        Optional<Login> login = loginDAO.findLoginByUserName(userNameToCheck);
        if (login.isPresent() && login.get().isActive() && login.get().getPassword().equals(encode(passwordToCheck))) {
            return new ResponseEntity<>(login, HttpStatus.OK);
        }
        return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLogin(@RequestBody Login login) throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        Optional<Login> requester = authenticatedLogin(userName, password);
        if (requester.isEmpty()) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }

        if (requester.get().getAccessLevel() == RECOVERY_ACCESS_LEVEL && login.getAccessLevel() != ADMIN_ACCESS_LEVEL) {
            return new ResponseEntity<>("Recovery account may only create administrator accounts", HttpStatus.FORBIDDEN);
        }

        if (loginDAO.findLoginByUserName(login.getUserName()).isPresent()) {
            return new ResponseEntity<>("User " + login.getUserName() + " already exists", HttpStatus.IM_USED);
        }

        login.setPassword(encode(login.getPassword()));
        loginDAO.addLogin(login);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/createaskam")
    public ResponseEntity<?> createLoginAsKAM(@RequestBody Operator operator) throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        JSONObject loginData = new JSONObject(servletRequest.getHeader("login"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }

        String newUserName = String.valueOf(loginData.get("userName"));
        if (loginDAO.findLoginByUserName(newUserName).isPresent() || operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).isPresent()) {
            return new ResponseEntity<>("Already exists", HttpStatus.IM_USED);
        }

        Login loginToCreate = new Login();
        loginToCreate.setUserName(newUserName);
        loginToCreate.setPassword(encode(String.valueOf(loginData.get("password"))));
        loginDAO.addLogin(loginToCreate);

        Login newLogin = loginDAO.findLoginByUserName(newUserName).orElseThrow();
        operator.setAddedTo(newLogin.getId());
        operatorDAO.addOperator(operator);
        newLogin.getOperators().add(operator);
        loginDAO.addLogin(newLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/renew")
    public ResponseEntity<?> updateTimestamp(@RequestBody Login login) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(login.getUserName());
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
        loginToCheck.get().setTimestamp(System.currentTimeMillis());
        loginDAO.addLogin(loginToCheck.get());
        return new ResponseEntity<>("Timestamp updated", HttpStatus.OK);
    }

    @PostMapping("/addremoveoperators")
    public ResponseEntity<?> addRemoveOperators(@RequestBody ArrayList<Operator> operators) {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Login not found.", HttpStatus.NOT_FOUND);
        }

        Login targetLogin = loginToCheck.get();
        for (Operator operator : operators) {
            if (operator.getAddedTo() == loginId) {
                operator.setAddedTo(loginId);
            } else if (operator.getAddedTo() == targetLogin.getId()) {
                operator.setAddedTo(-1L);
            }
            operatorDAO.addOperator(operator);
        }

        targetLogin.getOperators().clear();
        for (Operator operator : operatorDAO.getAllOperators()) {
            if (operator.getAddedTo() == loginId) {
                targetLogin.getOperators().add(operator);
            }
        }
        loginDAO.addLogin(targetLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/update")
    public ResponseEntity<?> updateLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String oldPassword = servletRequest.getHeader("oldPassword");
        String newPassword = servletRequest.getHeader("newPassword");
        if (!actionAllowed(userName, encode(oldPassword))) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Login " + userName + " not found.", HttpStatus.NOT_FOUND);
        }

        loginToCheck.get().setPassword(encode(newPassword));
        loginDAO.addLogin(loginToCheck.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getallogins")
    public ResponseEntity<?> getAllLogins() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        int accessLevel = Integer.parseInt(servletRequest.getHeader("accessLevel"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }

        List<Login> loginsToSend = new ArrayList<>();
        for (Login login : loginDAO.getAllLogins()) {
            if (accessLevel == ADMIN_ACCESS_LEVEL && login.getAccessLevel() < ADMIN_ACCESS_LEVEL) {
                loginsToSend.add(login);
            } else if (accessLevel == 1 && login.getAccessLevel() == 0) {
                loginsToSend.add(login);
            }
        }
        loginsToSend.sort(Comparator.comparing(Login::getUserName));
        return new ResponseEntity<>(loginsToSend, HttpStatus.OK);
    }

    @GetMapping("/changestate")
    public ResponseEntity<?> changeLoginState() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        String state = servletRequest.getHeader("state");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        Login login = loginToCheck.get();
        boolean active = "Active".equals(state);
        login.setActive(active);
        login.setTimestamp(active ? System.currentTimeMillis() + 2629800000L : -1L);
        if (!active && login.getAccessLevel() == 0) {
            clearTokenHistory(login);
        }
        loginDAO.addLogin(login);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/renamelogin")
    public ResponseEntity<?> renameLogin() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        String newName = servletRequest.getHeader("newName");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        if (loginDAO.findLoginByUserName(newName).isPresent()) {
            return new ResponseEntity<>("Name already exists", HttpStatus.IM_USED);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
        loginToCheck.get().setUserName(newName);
        loginDAO.addLogin(loginToCheck.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/checkal")
    public ResponseEntity<?> getAccessLevel() {
        String userName = servletRequest.getHeader("userName");
        return loginDAO.findLoginByUserName(userName)
                .<ResponseEntity<?>>map(login -> new ResponseEntity<>(login.getAccessLevel(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND));
    }

    @GetMapping("/changettl")
    public ResponseEntity<?> changeTTL() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        long newValue = Long.parseLong(servletRequest.getHeader("newValue"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Login not found.", HttpStatus.NOT_FOUND);
        }

        Login login = loginToCheck.get();
        login.setTimestamp(System.currentTimeMillis() + (newValue * 86400000L));
        if (newValue <= 0) {
            login.setTimestamp(-1L);
            login.setActive(false);
        }
        loginDAO.addLogin(login);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getaddedoperators")
    public ResponseEntity<?> getAddedOperators() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        if (loginDAO.findLoginById(loginId).isEmpty()) {
            return new ResponseEntity<>("Login not found.", HttpStatus.NOT_FOUND);
        }

        List<Operator> relevantOperators = new ArrayList<>();
        for (Operator operator : operatorDAO.getAllOperators()) {
            if (operator.getAddedTo() == -1 || operator.getAddedTo() == loginId) {
                relevantOperators.add(operator);
            }
        }
        return new ResponseEntity<>(relevantOperators, HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteLogin() {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }

        Optional<Login> loginToCheck = loginDAO.findLoginById(loginId);
        if (loginToCheck.isEmpty()) {
            return new ResponseEntity<>("Login not found.", HttpStatus.NOT_FOUND);
        }

        for (Operator operator : operatorDAO.getAllOperators()) {
            if (operator.getAddedTo() == loginId) {
                operator.setAddedTo(-1L);
                operatorDAO.addOperator(operator);
            }
        }
        loginDAO.deleteLogin(loginToCheck.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/ipcheck")
    public ResponseEntity<?> ipCheck() {
        String checkerUrl = System.getenv("IP_CHECK_URL");
        if (checkerUrl == null || checkerUrl.isBlank()) {
            return new ResponseEntity<>("IP_CHECK_URL is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Object response = responseServiceClient.getResponse(checkerUrl);
        if (response == null) {
            return new ResponseEntity<>("IP checker is unavailable", HttpStatus.BAD_GATEWAY);
        }
        JSONObject res = new JSONObject(response);
        return new ResponseEntity<>(String.valueOf(res.get("remoteAddress")), HttpStatus.OK);
    }

    private static String encode(String password) throws NoSuchAlgorithmException, InvalidKeyException {
        String hmacKey = System.getenv("PASSWORD_HMAC_KEY");
        if (hmacKey == null || hmacKey.isBlank()) {
            throw new IllegalStateException("PASSWORD_HMAC_KEY must be configured");
        }
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        return Base64.encodeBase64String(sha256Hmac.doFinal(password.getBytes(StandardCharsets.UTF_8)));
    }

    private Optional<Login> authenticatedLogin(String userName, String password) {
        return loginDAO.findLoginByUserName(userName)
                .filter(Login::isActive)
                .filter(login -> login.getPassword().equals(password));
    }

    private boolean actionAllowed(String userName, String password) {
        return authenticatedLogin(userName, password)
                .filter(login -> login.getAccessLevel() != RECOVERY_ACCESS_LEVEL)
                .isPresent();
    }

    private void clearTokenHistory(Login login) {
        for (Operator operator : login.getOperators()) {
            operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).ifPresent(operatorToCheck -> {
                operatorToCheck.getUsedTokens().clear();
                operatorDAO.addOperator(operatorToCheck);
            });
        }
    }
}
