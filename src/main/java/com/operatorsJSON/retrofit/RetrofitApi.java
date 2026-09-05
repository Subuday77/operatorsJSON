package com.operatorsJSON.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.operatorsJSON.Logging;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class RetrofitApi {

    @Autowired
    Logging logging;

    private static RetrofitApi instance;
    public static String baseUrl = "http://localhost/";

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss,SSS");

    HttpLoggingInterceptor.Logger fileLogger = new HttpLoggingInterceptor.Logger() {
        @Override
        public void log(String s) {
            LocalDateTime dateTime = LocalDateTime.now();
            logging.logParser(dateTime.format(formatter) + " " + s);
        }
    };

    Interceptor fileLoggerInterceptor = new HttpLoggingInterceptor(fileLogger).setLevel(HttpLoggingInterceptor.Level.BODY);
    Gson gson = new GsonBuilder().setLenient().create();

    private ResponseService responseService;

    public static RetrofitApi getInstance() {
        if (instance == null) {
            instance = new RetrofitApi();
        }
        return instance;
    }

    private RetrofitApi() {
        buildRetrofit(baseUrl);
    }

    private void buildRetrofit(String url) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .addInterceptor(fileLoggerInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);

        configureOptionalProxy(httpClient);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

        responseService = retrofit.create(ResponseService.class);
    }

    private void configureOptionalProxy(OkHttpClient.Builder httpClient) {
        String proxyUrl = System.getenv("OUTBOUND_PROXY_URL");
        if (proxyUrl == null || proxyUrl.isBlank()) {
            return;
        }

        URI proxyUri = URI.create(proxyUrl);
        String host = proxyUri.getHost();
        int port = proxyUri.getPort();
        if (host == null || port < 0) {
            throw new IllegalArgumentException("OUTBOUND_PROXY_URL must include host and port");
        }

        httpClient.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));

        String userInfo = proxyUri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] credentials = userInfo.split(":", 2);
            String proxyUser = credentials[0];
            String proxyPassword = credentials.length > 1 ? credentials[1] : "";

            Authenticator proxyAuthenticator = new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                    String credential = Credentials.basic(proxyUser, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                }
            };
            httpClient.proxyAuthenticator(proxyAuthenticator);
        }
    }

    public ResponseService getResponseService() {
        return responseService;
    }
}
