package com.operatorsJSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import com.operatorsJSON.beans.testsRelated.*;
import com.operatorsJSON.retrofit.ResponseServiceClient;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.operatorsJSON.beans.Constants.*;

@Component
public class PrepareResult {
    @Autowired
    OperatorDAO operatorDAO;
    @Autowired
    OperatorsDynamicConfigDAO dynamicConfigDAO;
    @Autowired
    AuthenticationRequest authenticationRequest;
    @Autowired
    RequestCommon requestCommon;
    @Autowired
    DebitRequest debitRequest;
    @Autowired
    CreditRequest creditRequest;
    @Autowired
    CreditRequestWithoutDebitTransactionId creditRequestWithoutDebitTransactionId;
    @Autowired
    RollbackRequest rollbackRequest;
    @Autowired
    GetNewTokenRequest getNewTokenRequest;
    @Autowired
    ResultToSend resultToSend;
    @Autowired
    ResponseServiceClient serviceClient;
    @Autowired
    Logging logging;

    Gson gson = new Gson();
    ObjectWriter ow = new ObjectMapper().writer();
    ObjectWriter owPretty = new ObjectMapper().writer().withDefaultPrettyPrinter();

    private String operatorInProcess;

    public String getOperatorInProcess() {
        return operatorInProcess;
    }

    public void setOperatorInProcess(String operatorInProcess) {
        this.operatorInProcess = operatorInProcess;
    }

    public synchronized ResponseEntity<?> authAttempt(long operatorId) throws IOException {
        LinkedHashMap<String, ResultToSend> resultsToSend = new LinkedHashMap<>();
        CACHE.remove(operatorId);
        TTLCACHE.remove(operatorId);
        clearLog(operatorId);
        setOperatorInProcess(String.valueOf(operatorId));
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        String baseUrl = operatorToTest.get().getOperatorUrl() + operatorToTest.get().getContextRootName();
        ArrayList<String> cacheKeys = new ArrayList<>();
        authenticationRequest.setOperatorId(operatorId);
        authenticationRequest.setToken(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getInitialToken());
        authenticationRequest.setTimestamp(System.currentTimeMillis());
        logging.logParser("Case_0 Authentication", String.valueOf(operatorId));
        resultToSend.setRequest(owPretty.writeValueAsString(authenticationRequest));
        resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getAuthMethodName(), ow.writeValueAsString(authenticationRequest),
                generateHash(ow.writeValueAsString(authenticationRequest), operatorToTest.get().getHashKey()))));
        resultToSend.setLog(getLogRecord("Case_0", operatorId));
        int errorCode = setupDynamicConfig(resultToSend.getResponse(), operatorId);
        switch (errorCode) {
            case -2:
                return new ResponseEntity<>(HttpStatus.GONE);
            case -1:
                return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
            case 6:
                return new ResponseEntity<>(HttpStatus.LOCKED);
            case 1:
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
            case 0:
                double[] balances = new double[4];
                for (int i = 0; i < 4; i++) {
                    balances[i] = getBalance(resultToSend.getResponse());
                }
                ArrayList<double[]> caseBalances = new ArrayList<>();
                caseBalances.add(balances);
                cacheKeys.add("Case_0");
                CACHE.put(operatorId, caseBalances);
                if (CACHE.containsKey(operatorId)) {
                    CACHE.replace(operatorId, caseBalances);
                }
//              checkBalances("Case_0", operatorId);
                resultToSend.setExpectedResponse(String.valueOf(prepareExpectedResponse("Case_0", resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys)));
                resultToSend.setCheckResults(checkResults("Case_0", resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                resultsToSend.put("Case_0", resultToSend);
                TTLCACHE.put(operatorId, new long[]{System.currentTimeMillis(), System.currentTimeMillis()});
                if (TTLCACHE.containsKey(operatorId)) {
                    TTLCACHE.replace(operatorId, new long[]{System.currentTimeMillis(), System.currentTimeMillis()});
                }
                return new ResponseEntity<LinkedHashMap<String, ResultToSend>>(resultsToSend, HttpStatus.OK);
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    public synchronized ResponseEntity<?> testFlow(long operatorId, ArrayList<String> casesList) throws IOException {
        try {
            if (TTLCACHE.get(operatorId).length == 0) {
                TTLCACHE.put(operatorId, new long[]{System.currentTimeMillis(), System.currentTimeMillis()});
            }
        } catch (NullPointerException e) {
            TTLCACHE.put(operatorId, new long[]{System.currentTimeMillis(), System.currentTimeMillis()});
        }
        setOperatorInProcess(String.valueOf(operatorId));
//        System.out.println(getOperatorInProcess());
        LinkedHashMap<String, ResultToSend> resultsToSend = new LinkedHashMap<>();
        JSONObject responseJSON;
        double returnedBalance;
        ResultToSend resultToSend;
        ArrayList<String> cacheKeys = new ArrayList<>();
        cacheKeys.add("Case_0");
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        String baseUrl = operatorToTest.get().getOperatorUrl() + operatorToTest.get().getContextRootName();
        requestCommon.setValues(operatorId);
        debitRequest.setValues();
        creditRequest.setValues();
        creditRequestWithoutDebitTransactionId.setValues();
        rollbackRequest.setValues();
        debitRequest.setDebitAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
        creditRequest.setCreditAmount(BigDecimal.valueOf(Double.parseDouble(formatMyDouble(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.99)))).setScale(2, RoundingMode.HALF_DOWN));
        creditRequestWithoutDebitTransactionId.setCreditAmount(BigDecimal.valueOf(Double.parseDouble(formatMyDouble(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.99)))).setScale(2, RoundingMode.HALF_DOWN));
        for (String testCase : casesList) {
            switch (testCase) {
                case "Case_1": // Repeated authentication
                    authenticationRequest.setOperatorId(operatorId);
                    authenticationRequest.setToken(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getInitialToken());
                    resultToSend = new ResultToSend();
                    authenticationRequest.setTimestamp(System.currentTimeMillis());
                    logging.logParser(testCase + " Repeated authentication", String.valueOf(operatorId));
                    resultToSend.setRequest(owPretty.writeValueAsString(authenticationRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getAuthMethodName(), ow.writeValueAsString(authenticationRequest),
                            generateHash(ow.writeValueAsString(authenticationRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = responseJSON.optDouble("balance", 0);
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(String.valueOf(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys)));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_2": // Debit
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_3": // Retry for debit
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Retry for debit", String.valueOf(operatorId));
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_4": // Rollback
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Rollback", String.valueOf(operatorId));
                    rollbackRequest.setRollbackAmount(debitRequest.getDebitAmount());
                    rollbackRequest.setTransactionId(debitRequest.getTransactionId());
                    rollbackRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(rollbackRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getRollbackMethodName(), ow.writeValueAsString(rollbackRequest),
                            generateHash(ow.writeValueAsString(rollbackRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_5": // Retry for Rollback
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Retry for Rollback", String.valueOf(operatorId));
                    rollbackRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(rollbackRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getRollbackMethodName(), ow.writeValueAsString(rollbackRequest),
                            generateHash(ow.writeValueAsString(rollbackRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_6": // Rollback before Debit
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Rollback before Debit", String.valueOf(operatorId));
                    rollbackRequest.setRollbackAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    rollbackRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    rollbackRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(rollbackRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getRollbackMethodName(), ow.writeValueAsString(rollbackRequest),
                            generateHash(ow.writeValueAsString(rollbackRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_7": // Debit after Rollback
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit after Rollback", String.valueOf(operatorId));
                    debitRequest.setDebitAmount(rollbackRequest.getRollbackAmount());
                    debitRequest.setTransactionId(rollbackRequest.getTransactionId());
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_8.1": // Credit (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_8.2": // Credit (credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit (credit part)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_9": // Retry for credit
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Retry for credit", String.valueOf(operatorId));
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_10.1": // Credit with amount 0 (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with amount 0 (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_10.2": // Credit with amount 0 (credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with amount 0 (credit part)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setCreditAmount(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_DOWN));
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    creditRequest.setCreditAmount(BigDecimal.valueOf(Double.parseDouble(formatMyDouble(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                            (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.99)))).setScale(2, RoundingMode.HALF_DOWN));
                    break;
                case "Case_11": // Insufficient funds
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Insufficient funds", String.valueOf(operatorId));
                    double balance = 0;
                    for (int i = CACHE.get(operatorId).size() - 1; i >= 0; --i) {
                        if (CACHE.get(operatorId).get(i)[2] > 0) {
                            balance = CACHE.get(operatorId).get(i)[2];
                            break;
                        }
                    }
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    double temp = Double.parseDouble(formatMyDouble(balance + (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.01)));
                    temp = dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? Math.round(temp) : temp;
                    debitRequest.setDebitAmount(BigDecimal.valueOf(temp));
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setDebitAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    break;
                case "Case_12": // Debit with the wrong token
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit with the wrong token", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    debitRequest.setToken(corruptString(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getSessionToken()));
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setToken(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getSessionToken());
                    break;
                case "Case_13": // Debit from unknown user
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit from unknown user", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    debitRequest.setUid(corruptString(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getUid()));
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setUid(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getUid());
                    break;
                case "Case_14": // Debit with negative amount
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit with negative amount", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    debitRequest.setDebitAmount(BigDecimal.valueOf(debitRequest.getDebitAmount().doubleValue() * (-1)).setScale(2, RoundingMode.HALF_DOWN));
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setDebitAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    break;
                case "Case_15": // Debit all player's balance
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit all player's balance", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    debitRequest.setDebitAmount(BigDecimal.valueOf
                            (CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0] > 0 ? CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0] : CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2])
                            .setScale(2, RoundingMode.HALF_DOWN));
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setDebitAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    break;
                case "Case_16": // Return reason 1 (cancel bet)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Return reason 1 (cancel bet)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setCreditAmount(BigDecimal.valueOf
                            (CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 2)[0] > 0 ? CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 2)[0] : CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 2)[2])
                            .setScale(2, RoundingMode.HALF_DOWN));
                    creditRequest.setReturnReason(1);
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    creditRequest.setReturnReason(0);
                    creditRequest.setCreditAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                            (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.99)).setScale(2, RoundingMode.HALF_DOWN));
                    break;
                case "Case_17.1": // Return reason 2 (cancel round, debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Return reason 2 (cancel round, debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_17.2": // Return reason 2 (cancel round, credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Return reason 2 (cancel round, credit part)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setCreditAmount(BigDecimal.valueOf(debitRequest.getDebitAmount().doubleValue()).setScale(2, RoundingMode.HALF_DOWN));
                    creditRequest.setReturnReason(2);
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    creditRequest.setReturnReason(0);
                    creditRequest.setCreditAmount(BigDecimal.valueOf(Double.parseDouble(formatMyDouble(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                            (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.99)))).setScale(2, RoundingMode.HALF_DOWN));
                    break;
                case "Case_18.1": // Rollback with wrong amount (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Rollback with wrong amount (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_18.2": // Rollback with wrong amount (rollback part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Rollback with wrong amount (rollback part)", String.valueOf(operatorId));
                    rollbackRequest.setRollbackAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    rollbackRequest.setTransactionId(debitRequest.getTransactionId());
                    rollbackRequest.setTimestamp(System.currentTimeMillis());
                    rollbackRequest.setRollbackAmount(BigDecimal.valueOf(Double.parseDouble(formatMyDouble(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount() +
                            (dynamicConfigDAO.findDynamicConfigById(operatorId).get().isOnlyWholeNumbers() ? 1 : 0.01)))).setScale(2, RoundingMode.HALF_DOWN));
                    resultToSend.setRequest(owPretty.writeValueAsString(rollbackRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getRollbackMethodName(), ow.writeValueAsString(rollbackRequest),
                            generateHash(ow.writeValueAsString(rollbackRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    rollbackRequest.setRollbackAmount(BigDecimal.valueOf(dynamicConfigDAO.findDynamicConfigById(operatorId).get().getBasicBetAmount()));
                    break;
                case "Case_19.1": // Credit without debitTransactionId key (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit without debitTransactionId key (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_19.2": // Credit without debitTransactionId key (credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit without debitTransactionId key (credit part)", String.valueOf(operatorId));
                    creditRequestWithoutDebitTransactionId.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequestWithoutDebitTransactionId.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequestWithoutDebitTransactionId));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequestWithoutDebitTransactionId),
                            generateHash(ow.writeValueAsString(creditRequestWithoutDebitTransactionId), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_20.1": // Credit without debitTransactionId value (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit without debitTransactionId value (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_20.2": // Credit without debitTransactionId value (credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit without debitTransactionId value (credit part)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setDebitTransactionId("");
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_21.1": // Credit with debitTransactionId which never was processed (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with debitTransactionId which never was processed (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_21.2": // Credit with debitTransactionId which never was processed (credit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with debitTransactionId which never was processed (credit part)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setDebitTransactionId(generateDebitTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_22.1": // Credit with debitTransactionId which already was processed (debit part)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with debitTransactionId which already was processed (debit part)", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_22.2": // Credit with debitTransactionId which already was processed (credit part I)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with debitTransactionId which already was processed (credit part I)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId(debitRequest.getTransactionId()));
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_22.3": // Credit with debitTransactionId which already was processed (credit part II)
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Credit with debitTransactionId which already was processed (credit part II)", String.valueOf(operatorId));
                    creditRequest.setTransactionId(generateCreditTransactionId("-1"));
                    creditRequest.setDebitTransactionId(debitRequest.getTransactionId());
                    creditRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(creditRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getCreditMethodName(), ow.writeValueAsString(creditRequest),
                            generateHash(ow.writeValueAsString(creditRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_23": // Debit with invalid hash
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Debit with invalid hash", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey() + "00"))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    break;
                case "Case_24": // Unknown Game ID
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Unknown Game ID", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setGameId(99);
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setGameId(requestCommon.getGameId());
                    break;
                case "Case_25": // Invalid Bet type ID
                    resultToSend = new ResultToSend();
                    logging.logParser(testCase + " Invalid Bet type", String.valueOf(operatorId));
                    debitRequest.setTransactionId(generateDebitTransactionId());
                    updateRoundId();
                    debitRequest.setBetTypeID(debitRequest.getBetTypeID() + 100);
                    debitRequest.setTimestamp(System.currentTimeMillis());
                    resultToSend.setRequest(owPretty.writeValueAsString(debitRequest));
                    resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                            generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
                    resultToSend.setLog(getLogRecord(testCase, operatorId));
                    responseJSON = new JSONObject(resultToSend.getResponse());
                    returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
                    cacheSetup(returnedBalance, operatorId, testCase, cacheKeys);
                    resultToSend.setExpectedResponse(prepareExpectedResponse(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultToSend.setCheckResults(checkResults(testCase, resultToSend.getRequest(), resultToSend.getResponse(), cacheKeys));
                    resultsToSend.put(testCase, resultToSend);
                    debitRequest.setBetTypeID(requestCommon.getBetTypeID());
                    break;
                default:
                    break;
            }
        }
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        for (int i = 0; i < 4; i++) {
            double balance = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[i];
            if (balance > 0) {
                dynamicConfig.get().setInitialBalance(balance);
                dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
                break;
            }
        }
        TTLCACHE.get(operatorId)[1] = System.currentTimeMillis();
        return new ResponseEntity<LinkedHashMap<String, ResultToSend>>(resultsToSend, HttpStatus.OK);
    }

    private int setupDynamicConfig(String response, long id) {
        int errorCode;
        try {
            JSONObject responseJSON = new JSONObject(response);
            Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(id);
            errorCode = responseJSON.optInt("errorCode", -1);
            if (errorCode == 0) {
                try {
                    dynamicConfig.get().setSessionToken(responseJSON.getString("token"));
                    dynamicConfig.get().setUid(responseJSON.getString("uid"));
                    dynamicConfig.get().setCurrency(responseJSON.getString("currency"));
                    dynamicConfig.get().setInitialBalance(responseJSON.getDouble("balance"));
                } catch (JSONException e) {
                    errorCode = -2;
                }
//            dynamicConfig.get().setBasicBetAmount(1.01);
                dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
            }
        } catch (JSONException e) {
            errorCode = -1;
        }
        return errorCode;
    }

    public synchronized int getNewToken(long operatorId, String getNewTokenMethodName) {
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(operatorId);
        String baseUrl = operatorToTest.get().getOperatorUrl() + operatorToTest.get().getContextRootName();
        requestCommon.setValues(operatorId);
        getNewTokenRequest.setValues();
//        getNewTokenRequest.setOperatorId(operatorId);
//        getNewTokenRequest.setCurrentToken(requestCommon.getToken());
        getNewTokenRequest.setTimestamp(System.currentTimeMillis());
        JSONObject getNewTokenResponseJSON;
        logging.logParser("Case_0.1 Get New Token", String.valueOf(operatorId));
        int errorCode = -1;
        try {
            getNewTokenResponseJSON = new JSONObject(serviceClient.getResponse(baseUrl, getNewTokenMethodName, ow.writeValueAsString(getNewTokenRequest),
                    generateHash(ow.writeValueAsString(getNewTokenRequest), operatorToTest.get().getHashKey())));

        } catch (JSONException | JsonProcessingException e) {
            return -2;
        }
        try {
            errorCode = getNewTokenResponseJSON.getInt("errorCode");
        } catch (JSONException e) {
            return errorCode;
        }
        if (errorCode != 0) {
            return errorCode;
        }
        try {
            double balance = getNewTokenResponseJSON.getDouble("balance");
            String token = getNewTokenResponseJSON.getString("token");
            if (balance != CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0]) {
                return -4;
            }
            dynamicConfig.get().setSessionToken(token);
            dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
            return errorCode;
        } catch (JSONException e) {
            return -3;
        }
    }

    private HashMap<String, ParameterProperties> checkResults(String caseNumber, String request, String response, ArrayList<String> cacheKeys) {
        JSONObject responseJSON = new JSONObject(response);
        JSONObject requestJSON = new JSONObject(request);
        ArrayList<String> responseKeys = new ArrayList<String>(responseJSON.keySet());
        LinkedHashMap<String, Object> responseMapWithLowerCaseKeys = new LinkedHashMap<>();
        for (String responseKey : responseJSON.keySet()) {
            responseMapWithLowerCaseKeys.put(responseKey.toLowerCase(), responseJSON.get(responseKey));
        }
        HashMap<String, ParameterProperties> results = new HashMap<>();
        switch (caseNumber) {
            case "Case_0":
                for (String key : allKeys) {
                    HashSet<Integer> errorCodes = new HashSet<>();
                    if (compareKeysIgnoringCase(key, responseKeys)) {
                        if (!responseKeys.contains(key)) {
                            if (Arrays.stream(successfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                                errorCodes.add(102); // Key exists but with capitalisation errors
                            } else {
                                errorCodes.add(202); // Key exists but with capitalisation errors non mandatory
                            }
                        }
                        switch (key.toLowerCase()) {
                            case "operatorid":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Long")
                                        && !defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != requestJSON.getLong(key)) {
                                    errorCodes.add(1040); // Wrong value
                                }
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                break;
                            case "uid":
                            case "errordescription":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
//                                if(!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals
//                                        (dynamicConfigDAO.findDynamicCondigById(requestJSON.getLong("operatorId")).get().getUid())) {
//                                    errorCodes.add(104); // Wrong value
//                                }
                                break;
                            case "token":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (tokenUsed(requestJSON, String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                                    errorCodes.add(105); // Token was already used once
                                }
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                break;
                            case "balance":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                String BalanceClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                                switch (BalanceClassName) {
                                    case "Integer":
                                    case "Long":
                                        errorCodes.add(1031); // Invalid data format not critical
                                        break;
                                    case "String":
                                        try {
                                            Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                            if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                                errorCodes.add(1031); // Invalid data format not critical
                                            } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                                errorCodes.add(1032); // Invalid data format too many decimals
                                            }
                                        } catch (Exception e) {
                                            errorCodes.add(1030); // Invalid data format
                                        }
                                        break;
                                    case "BigDecimal":
                                        if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                            errorCodes.add(1031); // Invalid data format not critical
                                        } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                            errorCodes.add(1032); // Invalid data format too many decimals
                                        }
                                        break;
                                    default:
                                        errorCodes.add(1030); // Invalid data format
                                        break;
                                }
                                break;
                            case "currency":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                } else if (!correctCurrencyFormat(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                break;
                            case "errorcode":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                                    errorCodes.add(1030); // Invalid data format
                                } else {
                                    if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 0) {
                                        errorCodes.add(106); // Invalid error code
                                    }
                                }
                                break;
                            case "timestamp":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                String TimestampClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                                switch (TimestampClassName) {
                                    case "Integer":
                                        errorCodes.add(1031); // Invalid data format not critical
                                        break;
                                    case "String":
                                        try {
                                            Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                            if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() != 13) {
                                                errorCodes.add(1031); // Invalid data format not critical
                                            }
                                        } catch (Exception e) {
                                            errorCodes.add(1030); // Invalid data format
                                        }
                                        break;
                                    case "BigDecimal":
                                        errorCodes.add(1030); // Invalid data format
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case "nickname":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(207); // Value is missing non mandatory
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                break;
                            case "playertokenatlaunch":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(207); // Value is missing non mandatory
                                }
                                if (!dynamicConfigDAO.findDynamicConfigById(responseJSON.getLong("operatorId")).get()
                                        .getInitialToken().equals(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) {
                                    errorCodes.add(2040); //Wrong value non mandatory
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                if (responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals(responseMapWithLowerCaseKeys.get("token"))) {
                                    errorCodes.add(2051); // Wrong value non mandatory Initial token same as session
                                }
                                if (tokenUsed(requestJSON, String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                                    errorCodes.add(205); // Token was already used once
                                }
                                break;
                            case "clientip":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(207); // Value is missing non mandatory
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                if (!checkValidIp(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                break;
                            case "vip":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(207); // Value is missing non mandatory
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                try {
                                    int temp = Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                    if (temp < 0 || temp > 10) {
                                        errorCodes.add(2040); //Wrong value non mandatory
                                    }
                                } catch (Exception e) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                break;
                            case "bonusamount":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(207); // Value is missing non mandatory
                                }
                                String BonusAmountClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                                switch (BonusAmountClassName) {
                                    case "Integer":
                                    case "Long":
                                        errorCodes.add(2031); // Invalid data format not critical non mandatory
                                        break;
                                    case "String":
                                        try {
                                            Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                            if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                                errorCodes.add(2031); // Invalid data format not critical non mandatory
                                            } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                                errorCodes.add(2032); // Invalid data format too many decimals non mandatory
                                            }
                                        } catch (Exception e) {
                                            errorCodes.add(2030); // Invalid data format non mandatory
                                        }
                                        break;
                                    case "BigDecimal":
                                        if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                            errorCodes.add(2031); // Invalid data format not critical non mandatory
                                        } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                            errorCodes.add(2032); // Invalid data format too many decimals non mandatory
                                        }
                                        break;
                                    default:
                                        errorCodes.add(2030); // Invalid data format non mandatory
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        if (Arrays.stream(successfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                            errorCodes.add(101); //Key doesn't exist
                        }
                    }
                    if (responseKeys.contains(key)) {
                        if (errorCodes.size() == 0) {
                            if (Arrays.stream(successfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                                errorCodes.add(0);
                            } else if (Arrays.stream(optionalKeys).anyMatch(key::equalsIgnoreCase)) {
                                errorCodes.add(2);
                            }
                        }
                    }
                    if (errorCodes.size() > 0) {
                        results.put(key, recordParameterProperties(key, responseJSON, errorCodes));
                    }
                }
                return results;
            case "Case_1":
                for (String key : allKeys) {
                    HashSet<Integer> errorCodes = new HashSet<>();
                    if (compareKeysIgnoringCase(key, responseKeys)) {
                        if (!responseKeys.contains(key)) {
                            if (Arrays.stream(unsuccessfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                                errorCodes.add(102); // Key exists but with capitalisation errors
                            } else {
                                errorCodes.add(3); // No need in this key here
                            }
                        }
                        switch (key.toLowerCase()) {
                            case "operatorid":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Long")
                                        && !defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != requestJSON.getLong(key)) {
                                    errorCodes.add(1040); // Wrong value
                                }
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                break;
                            case "errorcode":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                                    errorCodes.add(1030); // Invalid data format
                                } else {
                                    if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 6) {
                                        errorCodes.add(106); // Invalid error code
                                    }
                                }
                                break;
                            case "timestamp":
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                String TimestampClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                                switch (TimestampClassName) {
                                    case "Integer":
                                        errorCodes.add(1031); // Invalid data format not critical
                                        break;
                                    case "String":
                                        try {
                                            Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                            if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() != 13) {
                                                errorCodes.add(1031); // Invalid data format not critical
                                            }
                                        } catch (Exception e) {
                                            errorCodes.add(1030); // Invalid data format
                                        }
                                        break;
                                    case "BigDecimal":
                                        errorCodes.add(1030); // Invalid data format
                                        break;
                                    default:
                                        break;

                                }
                                break;
                            case "errordescription":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                                    errorCodes.add(107); // Value is missing
                                }
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("not found")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        if (Arrays.stream(unsuccessfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                            errorCodes.add(101); //Key doesn't exist
                        }
                    }
                    if (responseKeys.contains(key)) {
                        if (errorCodes.size() == 0) {
                            if (Arrays.stream(unsuccessfulAuthMandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                                errorCodes.add(0);
                            } else {
                                errorCodes.add(3); // No need in this key here
                            }
                        }
                    }
                    if (errorCodes.size() > 0) {
                        results.put(key, recordParameterProperties(key, responseJSON, errorCodes));
                    }
                }
                return results;
        }
        for (String key : allKeys) {
            HashSet<Integer> errorCodes = new HashSet<>();
            if (compareKeysIgnoringCase(key, responseKeys)) {
                if (!responseKeys.contains(key)) {
                    if (Arrays.stream(mandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                        errorCodes.add(102); // Key exists but with capitalisation errors
                    } else {
                        errorCodes.add(202); // Key exists but with capitalisation errors non mandatory
                    }
                }
                switch (key.toLowerCase()) {
                    case "operatorid":
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Long")
                                && !defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        if (Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != requestJSON.getLong(key)) {
                            errorCodes.add(1040); // Wrong value
                        }
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        break;
                    case "transactionid":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals(requestJSON.get("transactionId"))) {
                            errorCodes.add(1040); // Wrong value
                        }
                        break;
                    case "roundid":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).toString().equals(requestJSON.get("roundId").toString())) {
                            errorCodes.add(1040); // Wrong value
                        }
                        break;
                    case "uid":
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        if (!caseNumber.equals("Case_13")) {
                            if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals
                                    (dynamicConfigDAO.findDynamicConfigById(requestJSON.getLong("operatorId")).get().getUid())) {
                                errorCodes.add(1040); // Wrong value
                            }
                        } else {
                            if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals(requestJSON.get("uid"))) {
                                errorCodes.add(1040); // Wrong value
                            }
                        }
                        break;
                    case "errordescription":
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        switch (caseNumber) {
                            case "Case_3":
                            case "Case_5":
                            case "Case_9":
                            case "Case_22.3":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("already")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_6":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("before") && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("not found")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_7":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("after") && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("already")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_11":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("insufficient")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_12":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("token")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_13":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("user") && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("uid")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_14":
                            case "Case_18.2":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("negative") && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("amount")
                                        && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("invalid")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_19.2":
                            case "Case_20.2":
                            case "Case_21.2":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("not found")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_23":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("hash")
                                        && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("invalid")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            case "Case_24":
                            case "Case_25":
                                if (!String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("unknown")
                                        && !String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).toLowerCase().contains("invalid")) {
                                    errorCodes.add(1041); // Possible wrong value;
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case "token":
                        if (!caseNumber.equals("Case_12")) {
                            if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals
                                    (dynamicConfigDAO.findDynamicConfigById(requestJSON.getLong("operatorId")).get().getSessionToken())) {
                                errorCodes.add(1040); // Wrong value
                            }
                        } else {
                            if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals(requestJSON.get("token"))) {
                                errorCodes.add(1040); // Wrong value
                            }
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(1030); // Invalid data format
                        }
//                        if (tokenUsed(requestJSON, String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
//                            errorCodes.add(105); // Token was already used once
//                        }
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        break;
                    case "balance":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        String BalanceClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                        switch (BalanceClassName) {
                            case "Integer":
                            case "Long":
                                errorCodes.add(1031); // Invalid data format not critical
                                break;
                            case "String":
                                try {
                                    Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                    if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                        errorCodes.add(1031); // Invalid data format not critical
                                    } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                        errorCodes.add(1032); // Invalid data format too many decimals
                                    }
                                } catch (Exception e) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                break;
                            case "BigDecimal":
                                if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                    errorCodes.add(1031); // Invalid data format not critical
                                } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                    errorCodes.add(1032); // Invalid data format too many decimals
                                }
                                break;
                            default:
                                errorCodes.add(1030); // Invalid data format
                                break;
                        }
                        if (caseNumber.equals("Case_14") || caseNumber.equals("Case_23")) {
                            if (Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != CACHE.get(requestJSON.getLong("operatorId")).get(CACHE.get(requestJSON.optLong("operatorId")).size() - 1)[1] &&
                                    Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != CACHE.get(requestJSON.getLong("operatorId")).get(CACHE.get(requestJSON.optLong("operatorId")).size() - 1)[2]) {
                                errorCodes.add(1040); // Wrong value
                            }
                        } else {
                            if (Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != CACHE.get(requestJSON.getLong("operatorId")).get(CACHE.get(requestJSON.optLong("operatorId")).size() - 1)[1]) {
                                errorCodes.add(1040); // Wrong value
                            }
                        }
                        break;
                    case "currency":
                        if (!responseMapWithLowerCaseKeys.get(key.toLowerCase()).equals
                                (dynamicConfigDAO.findDynamicConfigById(requestJSON.getLong("operatorId")).get().getCurrency())) {
                            errorCodes.add(1040); // Wrong value
                        }
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(1030); // Invalid data format
                        } else if (!correctCurrencyFormat(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        break;
                    case "errorcode":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("Integer")) {
                            errorCodes.add(1030); // Invalid data format
                        }
                        switch (caseNumber) {
                            case "Case_2":
                            case "Case_3":
                            case "Case_4":
                            case "Case_5":
                            case "Case_8.1":
                            case "Case_8.2":
                            case "Case_9":
                            case "Case_10.1":
                            case "Case_10.2":
                            case "Case_15":
                            case "Case_16":
                            case "Case_17.1":
                            case "Case_17.2":
                            case "Case_18.1":
                            case "Case_19.1":
                            case "Case_20.1":
                            case "Case_21.1":
                            case "Case_22.1":
                            case "Case_22.2":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 0) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_6":
                            case "Case_19.2":
                            case "Case_20.2":
                            case "Case_21.2":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 9) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_7":
                            case "Case_14":
                            case "Case_18.2":
                            case "Case_22.3":
                            case "Case_23":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 1) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_11":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 3) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_12":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 6) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_13":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 7) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            case "Case_24":
                            case "Case_25":
                                if (Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 1
                                        && Integer.parseInt(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase()))) != 0) {
                                    errorCodes.add(106); // Invalid error code
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case "timestamp":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(107); // Value is missing
                        }
                        String TimestampClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                        switch (TimestampClassName) {
                            case "Integer":
                                errorCodes.add(1031); // Invalid data format not critical
                                break;
                            case "String":
                                try {
                                    Long.parseLong(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                    if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() != 13) {
                                        errorCodes.add(1031); // Invalid data format not critical
                                    }
                                } catch (Exception e) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                break;
                            case "BigDecimal":
                                errorCodes.add(1030); // Invalid data format
                                break;
                            default:
                                break;
                        }
                        break;
                    case "nickname":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(207); // Value is missing non mandatory
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(2030); // Invalid data format non mandatory
                        }
                        break;
                    case "playertokenatlaunch":
                    case "vip":
                        errorCodes.add(3); // No need in this key here
                        break;
                    case "clientip":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(207); // Value is missing non mandatory
                        }
                        if (!defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase())).equals("String")) {
                            errorCodes.add(2030); // Invalid data format non mandatory
                        }
                        if (!checkValidIp(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())))) {
                            errorCodes.add(2030); // Invalid data format non mandatory
                        }
                        break;
                    case "bonusamount":
                        if (String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())).length() == 0) {
                            errorCodes.add(207); // Value is missing non mandatory
                        }
                        String BonusAmountClassName = defineObjectType(responseMapWithLowerCaseKeys.get(key.toLowerCase()));
                        switch (BonusAmountClassName) {
                            case "Integer":
                            case "Long":
                                errorCodes.add(2031); // Invalid data format not critical non mandatory
                                break;
                            case "String":
                                try {
                                    Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key.toLowerCase())));
                                    if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                        errorCodes.add(2031); // Invalid data format not critical non mandatory
                                    } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                        errorCodes.add(2032); // Invalid data format too many decimals non mandatory
                                    }
                                } catch (Exception e) {
                                    errorCodes.add(2030); // Invalid data format non mandatory
                                }
                                break;
                            case "BigDecimal":
                                if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) <= 1) {
                                    errorCodes.add(2031); // Invalid data format not critical non mandatory
                                } else if (countDecimals(responseMapWithLowerCaseKeys.get(key.toLowerCase())) > 2) {
                                    errorCodes.add(2032); // Invalid data format too many decimals non mandatory
                                }
                                break;
                            default:
                                errorCodes.add(2030); // Invalid data format non mandatory
                                break;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                if (Arrays.stream(mandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                    errorCodes.add(101); //Key doesn't exist
                }
            }
            if (responseKeys.contains(key)) {
                if (errorCodes.size() == 0) {
                    if (Arrays.stream(mandatoryKeys).anyMatch(key::equalsIgnoreCase)) {
                        errorCodes.add(0);
                    } else if (Arrays.stream(optionalKeys).anyMatch(key::equalsIgnoreCase)) {
                        errorCodes.add(2);
                    }
                }
            }
            if (errorCodes.size() > 0) {
                results.put(key, recordParameterProperties(key, responseJSON, errorCodes));
            }
        }

        return results;
    }


    private ParameterProperties recordParameterProperties(String key, JSONObject responseJSON, HashSet<Integer> errorCodes) {
        ParameterProperties parameterProperties = new ParameterProperties();
        ArrayList<String> foundErrors = new ArrayList<>();
        for (int errorCode : errorCodes) {
            if (errorCode != 101) {
                try {
                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                } catch (JSONException e) {
                    String temp = String.valueOf(responseJSON).toLowerCase();
                    JSONObject tempObject = new JSONObject(temp);
                    parameterProperties.setDataFormat(defineObjectType(tempObject.get(key.toLowerCase())));
                }
            }
            switch (errorCode) {
                case 0:
                case 2:
                    parameterProperties.setMandatory(errorCode == 0);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.OK);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("No errors found");
                    parameterProperties.setFoundErrors(foundErrors);
                    return parameterProperties;
                case 101:
                    parameterProperties.setMandatory(true);
                    parameterProperties.setExists(false);
                    parameterProperties.setErrorState(ERRORSTATE.E);
                    parameterProperties.setDataFormat("N/A");
                    foundErrors.add("Key <b>" + key + "</b> is missing.");
                    parameterProperties.setFoundErrors(foundErrors);
                    return parameterProperties;
                case 102:
                case 202:
                    parameterProperties.setMandatory(errorCode == 102);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    try {
//                        parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
//                    } catch (JSONException e) {
//                        String temp = String.valueOf(responseJSON).toLowerCase();
//                        JSONObject tempObject = new JSONObject(temp);
//                        parameterProperties.setDataFormat(defineObjectType(tempObject.get(key.toLowerCase())));
//                    }
                    foundErrors.add("Key <b>" + key + "</b> has invalid key format (Case Sensitive).");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 1030:
                case 2030:
                    parameterProperties.setMandatory(errorCode == 1030);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.opt(key)));
                    foundErrors.add("Key <b>" + key + "</b> has invalid data format.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 1031:
                case 2031:
                    parameterProperties.setMandatory(errorCode == 1031);
                    parameterProperties.setExists(true);
//                    if (!parameterProperties.getErrorState().equals(ERRORSTATE.E)) {
//                        parameterProperties.setErrorState(ERRORSTATE.W);
//                    }
                    try {
                        parameterProperties.setErrorState(parameterProperties.getErrorState().equals(ERRORSTATE.E) ? ERRORSTATE.E : ERRORSTATE.W);
                    } catch (NullPointerException e) {
                        parameterProperties.setErrorState(ERRORSTATE.W);
                    }
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> has invalid data format.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 1032:
                case 2032:
                    parameterProperties.setMandatory(errorCode == 1032);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> has invalid data format. Too many decimals.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 1040:
                case 2040:
                    parameterProperties.setMandatory(errorCode == 1040);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> has invalid value.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 1041:
                    parameterProperties.setMandatory(true);
                    parameterProperties.setExists(true);
//                    parameterProperties.setErrorState(ERRORSTATE.W);
                    try {
                        parameterProperties.setErrorState(parameterProperties.getErrorState().equals(ERRORSTATE.E) ? ERRORSTATE.E : ERRORSTATE.W);
                    } catch (NullPointerException e) {
                        parameterProperties.setErrorState(ERRORSTATE.W);
                    }
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> probably has invalid value.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
//                case 1042:
//                    parameterProperties.setMandatory(errorCode == 1042);
//                    parameterProperties.setExists(true);
//                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
//                    foundErrors.add("Key <b>" + key + "</b> has invalid value.");
//                    parameterProperties.setFoundErrors(foundErrors);
//                    break;
                case 2051:
                    parameterProperties.setMandatory(false);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Initial token same as session one.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 105:
                case 205:
                    parameterProperties.setMandatory(errorCode == 105);
                    parameterProperties.setExists(true);
                    try {
                        parameterProperties.setErrorState(parameterProperties.getErrorState().equals(ERRORSTATE.E) ? ERRORSTATE.E : ERRORSTATE.W);
                    } catch (NullPointerException e) {
                        parameterProperties.setErrorState(errorCode == 105 ? ERRORSTATE.E : ERRORSTATE.W);
                    }
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> has value, which already was used once.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 106:
                    parameterProperties.setMandatory(true);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Not expected error code.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 107:
                case 207:
                    parameterProperties.setMandatory(errorCode == 107);
                    parameterProperties.setExists(true);
                    parameterProperties.setErrorState(ERRORSTATE.E);
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("Key <b>" + key + "</b> exists, but value is missing.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                case 3:
                    parameterProperties.setMandatory(false);
                    parameterProperties.setExists(true);
//                    parameterProperties.setErrorState(ERRORSTATE.W);
                    try {
                        parameterProperties.setErrorState(parameterProperties.getErrorState().equals(ERRORSTATE.E) ? ERRORSTATE.E : ERRORSTATE.W);
                    } catch (NullPointerException e) {
                        parameterProperties.setErrorState(ERRORSTATE.W);
                    }
//                    parameterProperties.setDataFormat(defineObjectType(responseJSON.get(key)));
                    foundErrors.add("No need to return key <b>" + key + "</b> for this case.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
                default:
                    break;
            }
        }
        return parameterProperties;

    }

    private String prepareExpectedResponse(String caseNumber, String request, String response, ArrayList<String> cacheKeys) {
        JSONObject responseJSON = new JSONObject(response);
        JSONObject requestJSON = new JSONObject(request);
        HashMap<String, Object> expectedResponseMap = new LinkedHashMap<>();
        String expectedResponseString = "";
        ArrayList<String> responseKeys = new ArrayList<String>(responseJSON.keySet());
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicConfigById(requestJSON.optLong("operatorId"));
        expectedResponseMap.put("operatorId", requestJSON.optLong("operatorId"));
        switch (caseNumber) {
            case "Case_0":
                expectedResponseMap.put("uid", dynamicConfig.get().getUid());
                expectedResponseMap.put("token", dynamicConfig.get().getSessionToken());
                expectedResponseMap.put("balance", BigDecimal.valueOf(CACHE.get(requestJSON.optLong("operatorId"))
                        .get(0)[1]).setScale(2, RoundingMode.HALF_DOWN));
                expectedResponseMap.put("currency", defineExpectedCurrency(dynamicConfig));
                expectedResponseMap.put("errorCode", 0);
                expectedResponseMap.put("errorDescription", "OK");
                expectedResponseMap.put("timestamp", System.currentTimeMillis());
                if (compareKeysIgnoringCase(optionalKeys[0], responseKeys)) {
                    expectedResponseMap.put(optionalKeys[0], responseJSON.optString(optionalKeys[0], "Player's Nickname"));
                }
                if (compareKeysIgnoringCase(optionalKeys[1], responseKeys)) {
                    expectedResponseMap.put(optionalKeys[1], requestJSON.optString("token"));
                }
                if (compareKeysIgnoringCase(optionalKeys[2], responseKeys)) {
                    if (checkValidIp(responseJSON.optString(optionalKeys[2]))) {
                        expectedResponseMap.put(optionalKeys[2], responseJSON.optString(optionalKeys[2]));
                    } else {
                        expectedResponseMap.put(optionalKeys[2], "127.0.0.1");
                    }
                }
                if (compareKeysIgnoringCase(optionalKeys[3], responseKeys)) {
                    expectedResponseMap.put(optionalKeys[3], "0");
                }
                if (compareKeysIgnoringCase(optionalKeys[4], responseKeys)) {
                    expectedResponseMap.put(optionalKeys[4], BigDecimal.valueOf(responseJSON.optDouble("bonusAmount")).setScale(2, RoundingMode.HALF_DOWN));
                }
                expectedResponseString = gson.toJson(expectedResponseMap);

                return beautifyJsonString(expectedResponseString);

            case "Case_1":
                expectedResponseMap.put("errorCode", 6);
                expectedResponseMap.put("errorDescription", "Token not found");
                expectedResponseMap.put("timestamp", System.currentTimeMillis());
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
        }
        expectedResponseMap.put("uid", dynamicConfig.get().getUid());
        expectedResponseMap.put("token", dynamicConfig.get().getSessionToken());
        expectedResponseMap.put("roundId", requestJSON.get("roundId"));
        expectedResponseMap.put("transactionId", requestJSON.get("transactionId"));
        expectedResponseMap.put("balance", BigDecimal.valueOf(CACHE.get(requestJSON.optLong("operatorId"))
                .get(CACHE.get(requestJSON.optLong("operatorId")).size() - 1)[1]).setScale(2, RoundingMode.HALF_DOWN));
        expectedResponseMap.put("currency", defineExpectedCurrency(dynamicConfig));
        expectedResponseMap.put("timestamp", System.currentTimeMillis());
        if (compareKeysIgnoringCase(optionalKeys[0], responseKeys)) {
            expectedResponseMap.put(optionalKeys[0], responseJSON.optString(optionalKeys[0], "Player's Nickname"));
        }
        if (compareKeysIgnoringCase(optionalKeys[2], responseKeys)) {
            if (checkValidIp(responseJSON.optString(optionalKeys[2]))) {
                expectedResponseMap.put(optionalKeys[2], responseJSON.optString(optionalKeys[2]));
            } else {
                expectedResponseMap.put(optionalKeys[2], "127.0.0.1");
            }
        }
        if (compareKeysIgnoringCase(optionalKeys[4], responseKeys)) {
            expectedResponseMap.put(optionalKeys[4], BigDecimal.valueOf(responseJSON.optDouble("bonusAmount")).setScale(2, RoundingMode.HALF_DOWN));
        }
        switch (caseNumber) {
            case "Case_2":
            case "Case_4":
            case "Case_8.1":
            case "Case_8.2":
            case "Case_10.1":
            case "Case_10.2":
            case "Case_15":
            case "Case_16":
            case "Case_17.1":
            case "Case_17.2":
            case "Case_18.1":
            case "Case_19.1":
            case "Case_20.1":
            case "Case_21.1":
            case "Case_22.1":
            case "Case_22.2":
                expectedResponseMap.put("errorCode", 0);
                expectedResponseMap.put("errorDescription", "OK");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_3":
            case "Case_5":
            case "Case_9":
                expectedResponseMap.put("errorCode", 0);
                expectedResponseMap.put("errorDescription", "Transaction already processed");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_6":
                expectedResponseMap.put("errorCode", 9);
                expectedResponseMap.put("errorDescription", "Transaction not found");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_7":
                expectedResponseMap.put("errorCode", 1);
                expectedResponseMap.put("errorDescription", "Debit after rollback");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_11":
                expectedResponseMap.put("errorCode", 3);
                expectedResponseMap.put("errorDescription", "Insufficient funds");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_12":
                expectedResponseMap.put("errorCode", 6);
                expectedResponseMap.put("errorDescription", "Token not found");
                expectedResponseMap.replace("token", requestJSON.get("token"));
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_13":
                expectedResponseMap.put("errorCode", 7);
                expectedResponseMap.put("errorDescription", "User not found");
                expectedResponseMap.replace("uid", requestJSON.get("uid"));
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_14":
                expectedResponseMap.put("errorCode", 1);
                expectedResponseMap.put("errorDescription", "Negative amount");
                if (CACHE.get(requestJSON.optLong("operatorId")).get(cacheKeys.size() - 1)[0] != 0) {
                    expectedResponseMap.remove("balance");
                    expectedResponseMap.put("balance", BigDecimal.valueOf(CACHE.get(requestJSON.optLong("operatorId"))
                            .get(cacheKeys.size() - 1)[2]).setScale(2, RoundingMode.HALF_DOWN));
                }
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_18.2":
                expectedResponseMap.put("errorCode", 1);
                expectedResponseMap.put("errorDescription", "Invalid amount");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_19.2":
            case "Case_20.2":
            case "Case_21.2":
                expectedResponseMap.put("errorCode", 9);
                expectedResponseMap.put("errorDescription", "Debit transaction ID not found");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_22.3":
                expectedResponseMap.put("errorCode", 1);
                expectedResponseMap.put("errorDescription", "Debit transaction already processed");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_23":
                expectedResponseMap.put("errorCode", 1);
                expectedResponseMap.put("errorDescription", "Invalid hash");
                if (CACHE.get(requestJSON.optLong("operatorId")).get(cacheKeys.size() - 1)[0] != 0) {
                    if (CACHE.get(requestJSON.optLong("operatorId")).get(cacheKeys.size() - 1)[0] == CACHE.get(requestJSON.optLong("operatorId")).get(cacheKeys.size() - 1)[2]) {
                        expectedResponseMap.remove("balance");
                        expectedResponseMap.put("balance", BigDecimal.valueOf(CACHE.get(requestJSON.optLong("operatorId"))
                                .get(cacheKeys.size() - 1)[2]).setScale(2, RoundingMode.HALF_DOWN));
                    } else {
                        expectedResponseMap.remove("balance");
                        expectedResponseMap.put("balance", BigDecimal.valueOf(Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", CACHE.get(requestJSON.optLong("operatorId"))
                                .get(cacheKeys.size() - 1)[2]) + dynamicConfig.get().getBasicBetAmount()))).setScale(2, RoundingMode.HALF_DOWN));
                    }
                }
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_24":
                expectedResponseMap.put("errorCode", responseJSON.optInt("errorCode", 1) == 0 ? 0 : 1);
                expectedResponseMap.put("errorDescription", responseJSON.optInt("errorCode", 1) == 0 ? "OK" : "Unknown Game ID");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
            case "Case_25":
                expectedResponseMap.put("errorCode", responseJSON.optInt("errorCode", 1) == 0 ? 0 : 1);
                expectedResponseMap.put("errorDescription", responseJSON.optInt("errorCode", 1) == 0 ? "OK" : "Invalid Bet type");
                expectedResponseString = gson.toJson(expectedResponseMap);
                return beautifyJsonString(expectedResponseString);
        }
        return beautifyJsonString(expectedResponseString);
    }

    private void cacheSetup(double returnedBalance, long operatorId, String testCase, ArrayList<String> cacheKeys) {
        double[] balances = new double[4];
        balances[0] = returnedBalance;
        if (balances[0] > 0) {
            balances[1] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0];
        } else {
            balances[1] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[1];
        }
//        if (testCase.equals("Case_1")) {
//            balances[1] = 0;
//        }
        switch (testCase) {
            case "Case_1":
            case "Case_12":
            case "Case_13":
            case "Case_14":
            case "Case_23":
                balances[1] = 0;
                balances[3] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3];
                break;
            case "Case_2":
            case "Case_8.1":
            case "Case_10.1":
            case "Case_15":
            case "Case_17.1":
            case "Case_18.1":
            case "Case_19.1":
            case "Case_20.1":
            case "Case_21.1":
            case "Case_22.1":
                if (balances[1] > 0) {
                    balances[1] = Double.parseDouble(formatMyDouble(balances[1] - debitRequest.getDebitAmount().doubleValue()));
                } else {
                    balances[1] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2] - debitRequest.getDebitAmount().doubleValue()));
                }
                balances[3] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3] - debitRequest.getDebitAmount().doubleValue()));
                break;
            case "Case_3":
            case "Case_5":
            case "Case_6":
            case "Case_7":
            case "Case_9":
            case "Case_10.2":
            case "Case_11":
            case "Case_18.2":
            case "Case_19.2":
            case "Case_20.2":
            case "Case_21.2":
            case "Case_22.3":
                if (balances[1] == 0) {
                    balances[1] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2];
                }
                balances[3] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3];
                break;
            case "Case_4":
                if (balances[1] > 0) {
                    balances[1] = Double.parseDouble(formatMyDouble(balances[1] + rollbackRequest.getRollbackAmount().doubleValue()));
                } else {
                    balances[1] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2] + rollbackRequest.getRollbackAmount().doubleValue()));
                }
                balances[3] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3] + rollbackRequest.getRollbackAmount().doubleValue()));
                break;
            case "Case_8.2":
            case "Case_16":
            case "Case_17.2":
            case "Case_22.2":
                if (balances[1] > 0) {
                    balances[1] = Double.parseDouble(formatMyDouble(balances[1] + creditRequest.getCreditAmount().doubleValue()));
                } else {
                    balances[1] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2] + creditRequest.getCreditAmount().doubleValue()));
                }
                balances[3] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3] + creditRequest.getCreditAmount().doubleValue()));
                break;
            case "Case_24":
            case "Case_25":
                if (CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0] == balances[0] || CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2] == balances[0]) {
                    if (balances[1] == 0) {
                        balances[1] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2];
                    }
                    balances[3] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3];
                } else {
                    if (balances[1] > 0) {
                        balances[1] = Double.parseDouble(formatMyDouble(balances[1] - debitRequest.getDebitAmount().doubleValue()));
                    } else {
                        balances[1] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2] - debitRequest.getDebitAmount().doubleValue()));
                    }
                    balances[3] = Double.parseDouble(formatMyDouble(CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3] - debitRequest.getDebitAmount().doubleValue()));
                }
                break;
            default:
                break;
        }
        if (balances[1] > 0) {
            balances[2] = balances[1];
        } else {
            balances[2] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2];
            if (testCase.equals("Case_15")) {
                balances[2] = 0;
            }
            if (testCase.equals("Case_23")) {
                if (balances[0] == 0) {
                    balances[2] = CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2];
                } else {
                    balances[2] = dynamicConfigDAO.findDynamicConfigById(operatorId).get().getInitialBalance();
                }
            }
        }
        balances[1] = balances[1] < 0 ? 0 : balances[1];
        balances[2] = balances[2] < 0 ? 0 : balances[2];
        balances[3] = balances[3] < 0 ? 0 : balances[3];


        cacheKeys.add(testCase);
        CACHE.get(operatorId).add(balances);
//       checkBalances(testCase, operatorId);

    }

    public synchronized ResponseEntity<?> checkTokenTTL(long operatorId) throws JsonProcessingException {
        ResultToSend resultToSend = new ResultToSend();
        logging.logParser("Case_X Token TTL Check", String.valueOf(operatorId));
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        String baseUrl = operatorToTest.get().getOperatorUrl() + operatorToTest.get().getContextRootName();
        requestCommon.setValues(operatorId);
        debitRequest.setValues();
        debitRequest.setDebitAmount(BigDecimal.valueOf(1));
        debitRequest.setTransactionId(generateDebitTransactionId());
        debitRequest.setTimestamp(System.currentTimeMillis());
        resultToSend.setResponse(beautifyJsonString(serviceClient.getResponse(baseUrl, operatorToTest.get().getDebitMethodName(), ow.writeValueAsString(debitRequest),
                generateHash(ow.writeValueAsString(debitRequest), operatorToTest.get().getHashKey()))));
        int errorCode = -1;
        try {
            JSONObject responseJSON = new JSONObject(resultToSend.getResponse());
            errorCode = responseJSON.optInt("errorCode", -1);
            if (errorCode != 0) {
                CACHE.remove(operatorId);
                TTLCACHE.remove(operatorId);
                return new ResponseEntity<Integer>(errorCode, HttpStatus.OK);
            }
            double returnedBalance = Double.parseDouble(formatMyDouble(responseJSON.optDouble("balance", 0)));
            double[] balances = new double[4];
            for (int i = 0; i < 4; i++) {
                balances[i] = returnedBalance;
            }
            ArrayList<double[]> caseBalances = new ArrayList<>();
            caseBalances.add(balances);
            CACHE.put(operatorId, caseBalances);
            TTLCACHE.get(operatorId)[1] = System.currentTimeMillis();
            return new ResponseEntity<Integer>(errorCode, HttpStatus.OK);
        } catch (Exception e) {
            CACHE.remove(operatorId);
            TTLCACHE.remove(operatorId);
            return new ResponseEntity<Integer>(errorCode, HttpStatus.OK);
        }
    }

    private static String defineExpectedCurrency(Optional<OperatorsDynamicConfig> dynamicConfig) {
        String returnedCurrency = dynamicConfig.get().getCurrency();
        String currencyToReturn = "USD";
        if (returnedCurrency.length() == 3) {
            currencyToReturn = returnedCurrency.toUpperCase();
        } else if (returnedCurrency.length() == 4) {
            String firstChar = String.valueOf(returnedCurrency.charAt(0)).toLowerCase();
            returnedCurrency = returnedCurrency.substring(1).toUpperCase();
            currencyToReturn = firstChar + returnedCurrency;
        }
        return currencyToReturn;
    }

    private static double getBalance(String response) {
        JSONObject responseJSON = new JSONObject(response);
        return responseJSON.optDouble("balance", -753);
    }

    private boolean tokenUsed(JSONObject requestJSON, String token) {
        int lng = operatorDAO.findOperatorByOperatorId(requestJSON.getLong("operatorId")).get().getUsedTokens().size();
        HashSet<String> temp = operatorDAO.findOperatorByOperatorId(requestJSON.getLong("operatorId")).get().getUsedTokens();
        temp.add(token);
        operatorDAO.addOperator(operatorDAO.findOperatorByOperatorId(requestJSON.getLong("operatorId")).get());
        return lng == temp.size();
    }

    private static String defineObjectType(Object object) {
        String[] temp = String.valueOf(object.getClass()).split("\\.");
        return temp[temp.length - 1];
    }

    private static int countDecimals(Object object) {
        if (String.valueOf(object).contains(".")) {
            String[] temp = String.valueOf(object).split("\\.");
            return temp[temp.length - 1].length();
        }
        return 0;
    }

    private static boolean correctCurrencyFormat(String currencyName) {
        char[] currencyAsCharArray = currencyName.toCharArray();
        if (currencyAsCharArray.length < 3 || currencyAsCharArray.length > 4) {
            return false;
        }
        if (currencyAsCharArray.length == 3) {
            for (char letter : currencyAsCharArray) {
                if (letter < 65 || letter > 90) {
                    return false;
                }
            }
        } else {
            if (currencyAsCharArray[currencyAsCharArray.length - 1] > 47 && currencyAsCharArray[currencyAsCharArray.length - 1] < 58) {
                for (int i = 0; i <= 2; ++i) {
                    if (currencyAsCharArray[i] < 65 || currencyAsCharArray[i] > 90) {
                        return false;
                    }
                }
            } else {
                if (currencyAsCharArray[0] < 97 || currencyAsCharArray[0] > 122) {
                    return false;
                }
                for (int i = 1; i <= 3; ++i) {
                    if (currencyAsCharArray[i] < 65 || currencyAsCharArray[i] > 90) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean compareKeysIgnoringCase(String key, ArrayList<String> keys) {
        return keys.stream().anyMatch(x -> x.equalsIgnoreCase(key));
    }

    private static boolean checkValidIp(String ipAddress) {
        return (InetAddressUtils.isIPv4Address(ipAddress));
    }

    private static String beautifyJsonString(String jsonString) {
        while (jsonString.contains(", ")) {
            jsonString = jsonString.replace(", ", ",");
        }
        return jsonString.replace("\n", "").replace("{", "{\r\n")
                .replace(",\"", ",\r\n\"").replace("}", "\r\n}");
    }

    private void updateRoundId() {
        requestCommon.setRoundId(requestCommon.getRoundId() + 1);
        debitRequest.setRoundId(requestCommon.getRoundId());
        creditRequest.setRoundId(requestCommon.getRoundId());
        creditRequestWithoutDebitTransactionId.setRoundId(requestCommon.getRoundId());
        rollbackRequest.setRoundId(requestCommon.getRoundId());
    }

    private static String generateDebitTransactionId() {
        String transactionId = String.valueOf(UUID.randomUUID());
        transactionId = transactionId.replaceFirst(String.valueOf(transactionId.charAt(0)), "d");
        return transactionId;
    }

    private static String generateCreditTransactionId(String debitTransactionId) {
        String transactionId = "";
        if (debitTransactionId.equals("-1")) {
            transactionId = generateDebitTransactionId();
            transactionId = transactionId.replaceFirst(String.valueOf(transactionId.charAt(0)), "c");
        } else {
            transactionId = debitTransactionId.replaceFirst(String.valueOf(debitTransactionId.charAt(0)), "c");
        }
        return transactionId;
    }

    private static String corruptString(String stringToCorrupt) {
        String result = stringToCorrupt.substring(0, stringToCorrupt.length() / 2) + stringToCorrupt.substring((stringToCorrupt.length() / 2) + 1);
        result = result.equals("") ? String.valueOf((int) (Math.random() * (999999 - 100000 + 1)) + 100000) : result;
        return result;
    }

    public static String getLogRecord(String caseName, long operatorId) throws IOException {
        String path = "file\\" + operatorId + "_Test_Log.log";
        Charset encoding = StandardCharsets.UTF_8;
        String[] logRecords = readFile(path, encoding).split(caseName);
        return logRecords[logRecords.length - 1];
    }

    public static void clearLog(long operatorId) {
        String path = "file\\" + operatorId + "_Test_Log.log";
        try (FileWriter fw = new FileWriter(path, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("");

        } catch (IOException e) {
            System.out.println("Can't clear log");
        }
    }

    private static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private static String generateHash(String request, String secret) {
        try {
            String message = request;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(message.getBytes()));

            return hash;
        } catch (Exception e) {
            System.out.println("Generate Hash Error");
        }

        return null;

    }

    private static void checkBalances(String caseName, long operatorId) {
        // Returned balance
        // Expected returned balance
        // Expected calculated balance
        // Calculated balance
        System.out.println(caseName);
        System.out.println("Returned balance: " + CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[0]);
        System.out.println("Expected returned balance: " + CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[1]);
        System.out.println("Expected calculated balance: " + CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[2]);
        System.out.println("Calculated balance: " + CACHE.get(operatorId).get(CACHE.get(operatorId).size() - 1)[3]);
    }
}
