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
    private static final int OPERATOR_ACCESS_LEVEL = 0;
    private static final int KAM_ACCESS_LEVEL = 1;
    private static final int ADMIN_ACCESS_LEVEL = 2;
    private static final int RECOVERY_ACCESS_LEVEL = 3;

    @Autowired LoginDAO loginDAO;
    @Autowired OperatorDAO operatorDAO;
    @Autowired HttpServletRequest servletRequest;
    @Autowired ResponseServiceClient responseServiceClient;

    @PostConstruct
    public void createDefaultLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        String bootstrapPassword = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) return;
        String bootstrapUser = Optional.ofNullable(System.getenv("BOOTSTRAP_ADMIN_USERNAME"))
                .filter(value -> !value.isBlank()).orElse("SuperAdmin");
        if (loginDAO.findLoginByUserName(bootstrapUser).isEmpty()) {
            Login recovery = new Login();
            recovery.setUserName(bootstrapUser);
            recovery.setPassword(encode(bootstrapPassword));
            recovery.setAccessLevel(RECOVERY_ACCESS_LEVEL);
            loginDAO.addLogin(recovery);
        }
    }

    @GetMapping("/login")
    public ResponseEntity<?> login() throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String rawPassword = servletRequest.getHeader("password");
        Optional<Login> login = loginDAO.findLoginByUserName(userName);
        if (login.isPresent() && login.get().isActive() && login.get().getPassword().equals(encode(rawPassword))) {
            return new ResponseEntity<>(login.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLogin(@RequestBody Login login) throws NoSuchAlgorithmException, InvalidKeyException {
        Optional<Login> requester = currentLogin();
        if (requester.isEmpty() || !canCreateLevel(requester.get(), login.getAccessLevel())) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
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
        Optional<Login> requester = currentLogin();
        if (requester.isEmpty() || requester.get().getAccessLevel() != KAM_ACCESS_LEVEL) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        JSONObject loginData = new JSONObject(servletRequest.getHeader("login"));
        String newUserName = String.valueOf(loginData.get("userName"));
        if (loginDAO.findLoginByUserName(newUserName).isPresent() || operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).isPresent()) {
            return new ResponseEntity<>("Already exists", HttpStatus.IM_USED);
        }
        Login loginToCreate = new Login();
        loginToCreate.setUserName(newUserName);
        loginToCreate.setPassword(encode(String.valueOf(loginData.get("password"))));
        loginToCreate.setAccessLevel(OPERATOR_ACCESS_LEVEL);
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
        Optional<Login> requester = currentLogin();
        Optional<Login> target = loginDAO.findLoginByUserName(login.getUserName());
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        target.get().setTimestamp(System.currentTimeMillis());
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>("Timestamp updated", HttpStatus.OK);
    }

    @PostMapping("/addremoveoperators")
    public ResponseEntity<?> addRemoveOperators(@RequestBody ArrayList<Operator> operators) {
        Optional<Login> requester = currentLogin();
        long loginId = Long.parseLong(servletRequest.getHeader("id"));
        Optional<Login> target = loginDAO.findLoginById(loginId);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get()) || target.get().getAccessLevel() != OPERATOR_ACCESS_LEVEL) {
            return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        }
        target.get().getOperators().clear();
        for (Operator operator : operators) {
            operator.setAddedTo(operator.getAddedTo() == loginId ? loginId : -1L);
            operatorDAO.addOperator(operator);
        }
        for (Operator operator : operatorDAO.getAllOperators()) {
            if (operator.getAddedTo() == loginId) target.get().getOperators().add(operator);
        }
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/update")
    public ResponseEntity<?> updateLogin() throws NoSuchAlgorithmException, InvalidKeyException {
        String userName = servletRequest.getHeader("userName");
        String oldPassword = servletRequest.getHeader("oldPassword");
        String newPassword = servletRequest.getHeader("newPassword");
        Optional<Login> login = loginDAO.findLoginByUserName(userName);
        if (login.isEmpty() || !login.get().isActive() || !login.get().getPassword().equals(encode(oldPassword))) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        String newCredential = encode(newPassword);
        login.get().setPassword(newCredential);
        loginDAO.addLogin(login.get());
        return new ResponseEntity<>(newCredential, HttpStatus.OK);
    }

    @GetMapping("/setdefpass")
    public ResponseEntity<?> setDefaultPassword() throws NoSuchAlgorithmException, InvalidKeyException {
        Optional<Login> requester = currentLogin();
        String targetUserName = servletRequest.getHeader("targetUserName");
        Optional<Login> target = loginDAO.findLoginByUserName(targetUserName);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        String defaultPassword = System.getenv("RESET_PASSWORD_LEVEL_" + target.get().getAccessLevel());
        if (defaultPassword == null || defaultPassword.isBlank()) {
            return new ResponseEntity<>("Reset password is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        target.get().setPassword(encode(defaultPassword));
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getallogins")
    public ResponseEntity<?> getAllLogins() {
        Optional<Login> requester = currentLogin();
        if (requester.isEmpty() || requester.get().getAccessLevel() < KAM_ACCESS_LEVEL) {
            return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        }
        List<Login> result = new ArrayList<>();
        for (Login login : loginDAO.getAllLogins()) {
            if (canManage(requester.get(), login)) result.add(login);
        }
        result.sort(Comparator.comparing(Login::getUserName));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/changestate")
    public ResponseEntity<?> changeLoginState() {
        Optional<Login> requester = currentLogin();
        long id = Long.parseLong(servletRequest.getHeader("id"));
        Optional<Login> target = loginDAO.findLoginById(id);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        boolean active = "Active".equals(servletRequest.getHeader("state"));
        target.get().setActive(active);
        target.get().setTimestamp(active ? System.currentTimeMillis() + 2629800000L : -1L);
        if (!active && target.get().getAccessLevel() == OPERATOR_ACCESS_LEVEL) clearTokenHistory(target.get());
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/renamelogin")
    public ResponseEntity<?> renameLogin() {
        Optional<Login> requester = currentLogin();
        long id = Long.parseLong(servletRequest.getHeader("id"));
        String newName = servletRequest.getHeader("newName");
        Optional<Login> target = loginDAO.findLoginById(id);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        if (loginDAO.findLoginByUserName(newName).isPresent()) return new ResponseEntity<>("Name already exists", HttpStatus.IM_USED);
        target.get().setUserName(newName);
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/checkal")
    public ResponseEntity<?> getAccessLevel() {
        Optional<Login> requester = currentLogin();
        Optional<Login> target = loginDAO.findLoginByUserName(servletRequest.getHeader("targetUserName"));
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) return new ResponseEntity<>("Forbidden!", HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(target.get().getAccessLevel(), HttpStatus.OK);
    }

    @GetMapping("/changettl")
    public ResponseEntity<?> changeTTL() {
        Optional<Login> requester = currentLogin();
        long id = Long.parseLong(servletRequest.getHeader("id"));
        long days = Long.parseLong(servletRequest.getHeader("newValue"));
        Optional<Login> target = loginDAO.findLoginById(id);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get()) || target.get().getAccessLevel() != OPERATOR_ACCESS_LEVEL) return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        target.get().setTimestamp(days <= 0 ? -1L : System.currentTimeMillis() + days * 86400000L);
        if (days <= 0) target.get().setActive(false);
        loginDAO.addLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/getaddedoperators")
    public ResponseEntity<?> getAddedOperators() {
        Optional<Login> requester = currentLogin();
        long id = Long.parseLong(servletRequest.getHeader("id"));
        Optional<Login> target = loginDAO.findLoginById(id);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get()) || target.get().getAccessLevel() != OPERATOR_ACCESS_LEVEL) return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        List<Operator> relevant = new ArrayList<>();
        for (Operator operator : operatorDAO.getAllOperators()) if (operator.getAddedTo() == -1 || operator.getAddedTo() == id) relevant.add(operator);
        return new ResponseEntity<>(relevant, HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteLogin() {
        Optional<Login> requester = currentLogin();
        long id = Long.parseLong(servletRequest.getHeader("id"));
        Optional<Login> target = loginDAO.findLoginById(id);
        if (requester.isEmpty() || target.isEmpty() || !canManage(requester.get(), target.get())) return new ResponseEntity<>("Access deny", HttpStatus.FORBIDDEN);
        for (Operator operator : operatorDAO.getAllOperators()) {
            if (operator.getAddedTo() == id) { operator.setAddedTo(-1L); operatorDAO.addOperator(operator); }
        }
        loginDAO.deleteLogin(target.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/ipcheck")
    public ResponseEntity<?> ipCheck() {
        String checkerUrl = System.getenv("IP_CHECK_URL");
        if (checkerUrl == null || checkerUrl.isBlank()) return new ResponseEntity<>("IP_CHECK_URL is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        Object response = responseServiceClient.getResponse(checkerUrl);
        if (response == null) return new ResponseEntity<>("IP checker is unavailable", HttpStatus.BAD_GATEWAY);
        return new ResponseEntity<>(String.valueOf(new JSONObject(response).get("remoteAddress")), HttpStatus.OK);
    }

    private Optional<Login> currentLogin() {
        String userName = servletRequest.getHeader("userName");
        String credential = servletRequest.getHeader("password");
        if (userName == null || credential == null) return Optional.empty();
        return loginDAO.findLoginByUserName(userName).filter(Login::isActive).filter(login -> login.getPassword().equals(credential));
    }

    private boolean canCreateLevel(Login requester, int targetLevel) {
        if (requester.getAccessLevel() == RECOVERY_ACCESS_LEVEL) return targetLevel == ADMIN_ACCESS_LEVEL;
        if (requester.getAccessLevel() == ADMIN_ACCESS_LEVEL) return targetLevel >= OPERATOR_ACCESS_LEVEL && targetLevel < ADMIN_ACCESS_LEVEL;
        return false;
    }

    private boolean canManage(Login requester, Login target) {
        if (requester.getId() == target.getId()) return false;
        if (requester.getAccessLevel() == RECOVERY_ACCESS_LEVEL) return target.getAccessLevel() == ADMIN_ACCESS_LEVEL;
        if (requester.getAccessLevel() == ADMIN_ACCESS_LEVEL) return target.getAccessLevel() < ADMIN_ACCESS_LEVEL;
        if (requester.getAccessLevel() == KAM_ACCESS_LEVEL) return target.getAccessLevel() == OPERATOR_ACCESS_LEVEL;
        return false;
    }

    private void clearTokenHistory(Login login) {
        for (Operator operator : login.getOperators()) operatorDAO.findOperatorByOperatorId(operator.getOperatorId()).ifPresent(found -> {
            found.getUsedTokens().clear();
            operatorDAO.addOperator(found);
        });
    }

    private static String encode(String password) throws NoSuchAlgorithmException, InvalidKeyException {
        String key = System.getenv("PASSWORD_HMAC_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("PASSWORD_HMAC_KEY must be configured");
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.encodeBase64String(hmac.doFinal(password.getBytes(StandardCharsets.UTF_8)));
    }
}
