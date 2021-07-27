package com.operatorsJSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.operatorsJSON.DAO.dbRelated.OperatorDAO;
import com.operatorsJSON.DAO.dbRelated.OperatorsDynamicConfigDAO;
import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import com.operatorsJSON.beans.testsRelated.AuthenticationRequest;
import com.operatorsJSON.beans.testsRelated.OperatorResponse;
import com.operatorsJSON.beans.testsRelated.ParameterProperties;
import com.operatorsJSON.beans.testsRelated.ResultToSend;
import com.operatorsJSON.rest.testsRelated.TestsController;
import com.operatorsJSON.retrofit.ResponseServiceClient;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    ResultToSend resultToSend;
    @Autowired
    ResponseServiceClient serviceClient;

    ObjectWriter ow = new ObjectMapper().writer();
    ObjectWriter owPretty = new ObjectMapper().writer().withDefaultPrettyPrinter();

    private static HashMap<Long, LinkedHashMap<String, double[]>> cache;

//    public static HashMap<Long, LinkedHashMap<String, double[]>> getCache() {
//        return cache;
//    }
//
//    public static void setCache(HashMap<Long, LinkedHashMap<String, double[]>> cache) {
//        PrepareResult.cache = cache;
//    }

    // Returned balance
    // Expected returned balance
    // Expected calculated balance
    // Calculated balance

    public ResponseEntity<?> authAttempt(long operatorId) throws JsonProcessingException {
        Optional<Operator> operatorToTest = operatorDAO.findOperatorByOperatorId(operatorId);
        String baseUrl = operatorToTest.get().getOperatorUrl() + operatorToTest.get().getContextRootName();
        authenticationRequest.setOperatorId(operatorId);
        authenticationRequest.setToken(dynamicConfigDAO.findDynamicCondigById(operatorId).get().getInitialToken());
        authenticationRequest.setTimestamp(System.currentTimeMillis());
        resultToSend.setRequest(owPretty.writeValueAsString(authenticationRequest));
        resultToSend.setResponse(owPretty.writeValueAsString(serviceClient.getResponse(baseUrl, operatorToTest.get().getAuthMethodName(), ow.writeValueAsString(authenticationRequest),
                generateHash(ow.writeValueAsString(authenticationRequest), operatorToTest.get().getHashKey()))));
        int errorCode = setupDynamicConfig(resultToSend.getResponse(), operatorId);
        switch (errorCode) {
            case -1:
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            case 6:
                return new ResponseEntity<>(HttpStatus.LOCKED);
            case 0:
                double[] balances = new double[4];
                for (double balance : balances) {
                    balance = getBalance(resultToSend.getResponse());
                }
                LinkedHashMap<String, double[]> caseBalances = new LinkedHashMap<>();
                caseBalances.put("Case_0", balances);
                cache.put(operatorId, caseBalances);
                resultToSend.setExpectedResponse(String.valueOf(prepareExpectedResponse("Case_0", resultToSend.getRequest(), resultToSend.getResponse())));
                resultToSend.setCheckResults(checkResults("Case_0", resultToSend.getRequest(), resultToSend.getResponse()));
                return new ResponseEntity<ResultToSend>(resultToSend, HttpStatus.OK);
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    private int setupDynamicConfig(String response, long id) {
        JSONObject responseJSON = new JSONObject(response);
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicCondigById(id);
        int errorCode = responseJSON.optInt("errorCode", -1);
        if (errorCode == 0) {
            dynamicConfig.get().setSessionToken(responseJSON.optString("token"));
            dynamicConfig.get().setUid(responseJSON.optString("uid"));
            dynamicConfig.get().setCurrency(responseJSON.optString("currency"));
            dynamicConfig.get().setInitialBalance(responseJSON.optDouble("balance", 0));
            dynamicConfig.get().setBasicBetAmount(1.01);
            dynamicConfigDAO.addDynamicConfig(dynamicConfig.get());
        }
        return errorCode;
    }

    private HashMap<String, ParameterProperties> checkResults(String caseNumber, String request, String response) {
        JSONObject responseJSON = new JSONObject(response);
        JSONObject requestJSON = new JSONObject(request);
        ArrayList<String> responseKeys = (ArrayList<String>) responseJSON.keySet();
        LinkedHashMap<String, Object> responseMapWithLowerCaseKeys = new LinkedHashMap<>();
        for (String responseKey : responseJSON.keySet()) {
            responseMapWithLowerCaseKeys.put(responseKey.toLowerCase(), responseJSON.get(responseKey));
        }
        Map<String, ParameterProperties> results = new HashMap<>();
        switch (caseNumber) {
            case "Case_0":
                for (String key : successfulAuthMandatoryKeys) {
                    ArrayList<Integer> errorCodes = new ArrayList<>();
                    if (compareKeysIgnoringCase(key, responseKeys)) {
                        if (!responseKeys.contains(key)) {
                            errorCodes.add(102); // Key exists but with capitalisation errors
                        }
                        switch (key.toLowerCase()) {
                            case "operatorid":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key)).equals("Long")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (!responseMapWithLowerCaseKeys.get(key).equals(requestJSON.getLong(key))) {
                                    errorCodes.add(104); // Wrong value
                                }
                                break;
                            case "uid":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key)).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
//                                if(!responseMapWithLowerCaseKeys.get(key).equals
//                                        (dynamicConfigDAO.findDynamicCondigById(requestJSON.getLong("operatorId")).get().getUid())) {
//                                    errorCodes.add(104); // Wrong value
//                                }
                                break;
                            case "token":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key)).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                if (tokenUsed(requestJSON, String.valueOf(responseMapWithLowerCaseKeys.get(key)))) {
                                    errorCodes.add(105); // Token was already used once
                                }
                                break;
                            case "balance":
                                String className = defineObjectType(responseMapWithLowerCaseKeys.get(key));
                                switch (className) {
                                    case "Integer":
                                    case "Long":
                                        errorCodes.add(1031); // Invalid data format not critical
                                        break;
                                    case "String":
                                        try {
                                            Double.parseDouble(String.valueOf(responseMapWithLowerCaseKeys.get(key)));
                                            if (countDecimals(responseMapWithLowerCaseKeys.get(key)) <= 1) {
                                                errorCodes.add(1031); // Invalid data format not critical
                                            } else if (countDecimals(responseMapWithLowerCaseKeys.get(key)) > 2) {
                                                errorCodes.add(1032); // Invalid data format too much decimals
                                            }
                                        } catch (Exception e) {
                                            errorCodes.add(1030); // Invalid data format
                                        }
                                        break;
                                    case "BigDecimal":
                                        if (countDecimals(responseMapWithLowerCaseKeys.get(key)) <= 1) {
                                            errorCodes.add(1031); // Invalid data format not critical
                                        } else if (countDecimals(responseMapWithLowerCaseKeys.get(key)) > 2) {
                                            errorCodes.add(1032); // Invalid data format too much decimals
                                        }
                                        break;
                                    default:
                                        errorCodes.add(1030); // Invalid data format
                                        break;
                                }
                                break;
                            case "currency":
                                if (!defineObjectType(responseMapWithLowerCaseKeys.get(key)).equals("String")) {
                                    errorCodes.add(1030); // Invalid data format
                                } else if (!correctCurrencyFormat(String.valueOf(responseMapWithLowerCaseKeys.get(key)))) {
                                    errorCodes.add(1030); // Invalid data format
                                }
                                break;
                            case "errorCode":

                        }
                    } else {
                        errorCodes.add(101); //Key not exists
                    }
                    if (errorCodes.size() == 0) {
                        errorCodes.add(0);
                    }
                    results.put(key, recordParameterProperties(key, errorCodes));
                }
        }

        return null;
    }

    private ParameterProperties recordParameterProperties(String key, ArrayList<Integer> errorCodes) {
        ParameterProperties parameterProperties = new ParameterProperties();
        ArrayList<String> foundErrors = new ArrayList<>();
        for (int errorCode : errorCodes) {
            switch (errorCode) {

                case 101:
                    parameterProperties.setMandatory(true);
                    parameterProperties.setExists(false);
                    parameterProperties.setErrorState(ERRORSTATE.E);
                    parameterProperties.setDataFormat("N/A");
                    foundErrors.add("Key " + key + " not exists.");
                    parameterProperties.setFoundErrors(foundErrors);
                    break;
            }
        }
        return parameterProperties;

    }

    private OperatorResponse prepareExpectedResponse(String caseNumber, String request, String response) {
        JSONObject responseJSON = new JSONObject(response);
        JSONObject requestJSON = new JSONObject(request);
        ArrayList<String> responseKeys = (ArrayList<String>) responseJSON.keySet();
        OperatorResponse expectedResponse = new OperatorResponse();
        Optional<OperatorsDynamicConfig> dynamicConfig = dynamicConfigDAO.findDynamicCondigById(requestJSON.optLong("operatorId"));
        switch (caseNumber) {
            case "Case_0":
                expectedResponse.setOperatorId(requestJSON.optLong("operatorId"));
                expectedResponse.setUid(dynamicConfig.get().getUid());
                expectedResponse.setToken(dynamicConfig.get().getSessionToken());
                expectedResponse.setBalance(BigDecimal.valueOf(cache.get(requestJSON.optLong("operatorId")).get(caseNumber)[1]).setScale(2, RoundingMode.HALF_DOWN));
                expectedResponse.setCurrency(defineExpectedCurrency(dynamicConfig));
                expectedResponse.setErrorCode(0);
                expectedResponse.setErrorDescription("OK");
                expectedResponse.setTimestamp(System.currentTimeMillis());
                if (compareKeysIgnoringCase(optionalKeys[0], responseKeys)) {
                    expectedResponse.setNickName(responseJSON.optString(optionalKeys[0]));
                }
                if (compareKeysIgnoringCase(optionalKeys[1], responseKeys)) {
                    expectedResponse.setPlayerTokenAtLaunch(requestJSON.optString("token"));
                }
                if (compareKeysIgnoringCase(optionalKeys[2], responseKeys)) {
                    if (checkValidIp(responseJSON.optString(optionalKeys[2]))) {
                        expectedResponse.setClientIP(responseJSON.optString(optionalKeys[2]));
                    } else {
                        expectedResponse.setClientIP("127.0.0.1");
                    }
                }
                if (compareKeysIgnoringCase(optionalKeys[3], responseKeys)) {
                    expectedResponse.setVIP("0");
                }
                if (compareKeysIgnoringCase(optionalKeys[4], responseKeys)) {
                    expectedResponse.setBonusAmount(BigDecimal.valueOf(responseJSON.optDouble("bonusAmount")).setScale(2, RoundingMode.HALF_DOWN));
                }
                return expectedResponse;

            case "Case_1":

        }

        return null;
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
}
