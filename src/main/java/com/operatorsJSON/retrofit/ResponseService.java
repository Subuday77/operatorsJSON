package com.operatorsJSON.retrofit;

import com.operatorsJSON.beans.testsRelated.IPcheckerResponse;
import com.operatorsJSON.beans.testsRelated.OperatorResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface ResponseService {
    @Headers("Content-Type: application/json")
    @POST()
    public Call<String> postRequestAsUrl(@Url String requestUrl, @Body String request,
                                                   @Header(value = "hash") String hash);

    @POST()
    public Call<IPcheckerResponse> postRequestAsUrl(@Url String requestUrl);
}
