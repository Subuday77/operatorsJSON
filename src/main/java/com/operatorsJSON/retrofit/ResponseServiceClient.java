package com.operatorsJSON.retrofit;

import com.operatorsJSON.beans.testsRelated.IPcheckerResponse;
import com.operatorsJSON.beans.testsRelated.OperatorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

@Component
public class ResponseServiceClient {
//    @Autowired
//    Starter starter;

    @Autowired
    RetrofitApi retrofitApi;


    public String getResponse(String baseUrl, String method, String request, String hash) {
//        String baseUrl = starter.getOperatorUrl() + starter.getContextRootName();
//        String baseUrl = "http://127.0.0.1";
        RetrofitApi.baseUrl = baseUrl;
        RetrofitApi.getInstance();
        ResponseService service = retrofitApi.getResponseService();
        String requestUrl = baseUrl + method;
        Call<String> callSync = service.postRequestAsUrl(requestUrl, request, hash);

        try {

            Response<String> response = callSync.execute();

            return String.valueOf(response.body());

        } catch (Exception ex) {

            System.out.println(ex.getMessage());
        }

        return null;
    }

    public IPcheckerResponse getResponse(String requestUrl) {
        RetrofitApi.baseUrl = requestUrl;
        RetrofitApi.getInstance();
        ResponseService service = retrofitApi.getResponseService();
        Call<IPcheckerResponse> callSync = service.postRequestAsUrl(requestUrl);

        try {

            Response<IPcheckerResponse> response = callSync.execute();
            IPcheckerResponse apiResponse = response.body();

            return apiResponse;

        } catch (Exception ex) {

            System.out.println(ex.getMessage());
        }

        return null;
    }
}
