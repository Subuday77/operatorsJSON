package com.operatorsJSON.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.operatorsJSON.Logging;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
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

    Interceptor fileLoggerInterceptor = new HttpLoggingInterceptor(fileLogger).setLevel(Level.BODY);
    Gson gson = new GsonBuilder().setLenient().create();

    private ResponseService responseService;
    //	 String proxyUrl = System.getenv("IPBURGER_BLUE_HTTP");
    String proxyUrl = "https://CeraKh:Aw10l3S8@66-63-167-138.ip.heroku.ipb.cloud:9080";

    String[] proxyValues = proxyUrl.split("[/(:\\/@)/]+");
    String proxyUser = proxyValues[1];
    String proxyPassword = proxyValues[2];
    String proxyHost = proxyValues[3];
    int proxyPort = Integer.parseInt(proxyValues[4]);
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

       OkHttpClient.Builder httpClient = new OkHttpClient.Builder().addInterceptor(fileLoggerInterceptor).connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
//        OkHttpClient.Builder httpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
        Authenticator proxyAuthenticator = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(proxyUser, proxyPassword);
                return response.request().newBuilder().header("Proxy-Authorization", credential).build();
            }
        };
        httpClient.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
                .proxyAuthenticator(proxyAuthenticator);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson)).client(httpClient.build()).build();

        responseService = retrofit.create(ResponseService.class);
    }

    public ResponseService getResponseService() {
        return responseService;
    }
}
