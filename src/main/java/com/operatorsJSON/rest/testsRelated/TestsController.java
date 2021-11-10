package com.operatorsJSON.rest.testsRelated;

import com.operatorsJSON.DAO.dbRelated.LoginDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import com.operatorsJSON.PrepareResult;
import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.operatorsJSON.beans.Constants.CACHE;

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
//        boolean isAdvanced = Boolean.parseBoolean(servletRequest.getHeader("isAdvanced"));
        boolean isGetNewToken = Boolean.parseBoolean(servletRequest.getHeader("isGetNewToken"));
        String getNewTokenMethodName = servletRequest.getHeader("getNewTokenMethodName");
        JSONObject dynamicConfigJSON = new JSONObject(servletRequest.getHeader("dynamicConfig"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        dynamicConfig.get().setInitialToken(initialToken);
        if (dynamicConfigJSON.optString("startingRound").equals("")) {
            dynamicConfigJSON.remove("startingRound");
        }
        long startingRound = Long.parseLong(dynamicConfigJSON.optString("startingRound", String.valueOf((int) (Math.random() * (999999 - 100000 + 1)) + 100000)));
        dynamicConfig.get().setStartingRound(startingRound);
        boolean onlyWholeNumbers = (dynamicConfigJSON.getBoolean("onlyWholeNumbers"));
        dynamicConfig.get().setOnlyWholeNumbers(onlyWholeNumbers);
        if (dynamicConfigJSON.optString("basicBetAmount").equals("")) {
            dynamicConfigJSON.remove("basicBetAmount");
        }
        double basicBetAmount = Double.parseDouble(dynamicConfigJSON.optString("basicBetAmount", String.valueOf(onlyWholeNumbers ? 1 : 1.01)));
        dynamicConfig.get().setBasicBetAmount(basicBetAmount);
        dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        if (isGetNewToken) {
            ResponseEntity <?> responseEntity = prepareResult.authAttempt(operatorId);
            if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                return responseEntity;
            }
            int getNewTokenErrorCode = prepareResult.getNewToken(operatorId, getNewTokenMethodName);
            if (getNewTokenErrorCode == 0) {
                return responseEntity;
            }
            return new ResponseEntity<Integer>(getNewTokenErrorCode, HttpStatus.I_AM_A_TEAPOT); //418
        }
        return prepareResult.authAttempt(operatorId);
    }

    @GetMapping("/getlastlogrecord")
    public ResponseEntity<?> getLastLogRecord() throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        String caseName = servletRequest.getHeader("caseName");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(PrepareResult.getLogRecord(caseName, operatorId), HttpStatus.OK);
    }




    @PostMapping("/starttest")
    public ResponseEntity<?> startTestFlow(@RequestBody ArrayList<String> casesList) throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        long operatorId = Long.parseLong(servletRequest.getHeader("operatorId"));
        boolean isAdvanced = Boolean.parseBoolean(servletRequest.getHeader("isAdvanced"));
        boolean isGetNewToken = Boolean.parseBoolean(servletRequest.getHeader("isGetNewToken"));
        String getNewTokenMethodName = servletRequest.getHeader("getNewTokenMethodName");
        JSONObject dynamicConfigJSON = new JSONObject(servletRequest.getHeader("dynamicConfig"));
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        if (operatorToTest.isEmpty()) {
            return new ResponseEntity<String>("Operator not found.", HttpStatus.NOT_FOUND);
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        if (isAdvanced) {
            if (dynamicConfigJSON.getBoolean("tokenUsed")) {
                String initialToken = servletRequest.getHeader("initialToken");
                dynamicConfig.get().setInitialToken(initialToken);
                dynamicConfig.get().setSessionToken(dynamicConfigJSON.getString("sessionToken"));
                dynamicConfig.get().setUid(dynamicConfigJSON.getString("uid"));
                dynamicConfig.get().setInitialBalance(dynamicConfigJSON.getDouble("initialBalance"));
                dynamicConfig.get().setCurrency(dynamicConfigJSON.getString("currency"));
                CACHE.remove(operatorId);
                double[] balances = new double[4];
                for (int i = 0; i < 4; i++) {
                    balances[i] = dynamicConfigJSON.getDouble("initialBalance");
                }
                ArrayList<double[]> caseBalances = new ArrayList<>();
                caseBalances.add(balances);
                CACHE.put(operatorId, caseBalances);
                if (CACHE.containsKey(operatorId)) {
                    CACHE.replace(operatorId, caseBalances);
                }
            }
        }
        if (dynamicConfigJSON.optString("startingRound").equals("")) {
            dynamicConfigJSON.remove("startingRound");
        }
        long startingRound = Long.parseLong(dynamicConfigJSON.optString("startingRound", String.valueOf((int) (Math.random() * (999999 - 100000 + 1)) + 100000)));
        dynamicConfig.get().setStartingRound(startingRound);
        boolean onlyWholeNumbers = (dynamicConfigJSON.getBoolean("onlyWholeNumbers"));
        dynamicConfig.get().setOnlyWholeNumbers(onlyWholeNumbers);
        if (dynamicConfigJSON.optString("basicBetAmount").equals("")) {
            dynamicConfigJSON.remove("basicBetAmount");
        }
        double basicBetAmount = Double.parseDouble(dynamicConfigJSON.optString("basicBetAmount", String.valueOf(onlyWholeNumbers ? 1 : 1.01)));
        dynamicConfig.get().setBasicBetAmount(basicBetAmount);
        dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        if (isGetNewToken) {
//            ResponseEntity <?> responseEntity = prepareResult.authAttempt(operatorId);
            int getNewTokenErrorCode = prepareResult.getNewToken(operatorId, getNewTokenMethodName);
            if (getNewTokenErrorCode == 0) {
                return prepareResult.testFlow(operatorId, casesList);
            }
            return new ResponseEntity<Integer>(getNewTokenErrorCode, HttpStatus.I_AM_A_TEAPOT); //418
        }
        return prepareResult.testFlow(operatorId, casesList);
    }

    @GetMapping("/testtokenttl")
    public ResponseEntity<?> checkTokenTTL() throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        return prepareResult.checkTokenTTL(Long.parseLong(servletRequest.getHeader("operatorId")));
    }

    @GetMapping("/getfile")
    public ResponseEntity<?> getFile() throws IOException {
        String userName = servletRequest.getHeader("userName");
        String password = servletRequest.getHeader("password");
        String operatorId = servletRequest.getHeader("operatorId");
        if (!actionAllowed(userName, password)) {
            return new ResponseEntity<String>("Access deny", HttpStatus.FORBIDDEN);
        }
        String fileName = operatorId + "_Test_Log.log";
        File file = new File("file\\" + fileName);
        if (file.exists()) {
            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            return ResponseEntity.ok().headers(headers).contentLength(file.length())
                    .contentType(MediaType.TEXT_PLAIN).body(resource);
        }
        return new ResponseEntity<String>("File not found", HttpStatus.NOT_FOUND);
    }

    private boolean actionAllowed(String userName, String password) {
        Optional<Login> loginToCheck = loginDAO.findLoginByUserName(userName);
        return loginToCheck.map(login -> login.getPassword().equals(password)).orElse(false);
    }
}
