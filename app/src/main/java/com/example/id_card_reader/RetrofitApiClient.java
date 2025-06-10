package com.example.id_card_reader;

import com.example.id_card_reader.services.ApiService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitApiClient {
    private static final String API_BASE_URL = "https://c11d-2a09-bac5-d45c-e6-00-17-145.ngrok-free.app/";
    private ApiService apiService;
    private static RetrofitApiClient instance;

    private RetrofitApiClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Log request and response bodies

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create()) // If your backend returns JSON responses
                .build();

        apiService = retrofit.create(ApiService.class);
    }
    public static synchronized RetrofitApiClient getInstance() {
        if (instance == null) {
            instance = new RetrofitApiClient();
        }
        return instance;
    }

    public static ApiService getApiService() {
        return getInstance().apiService;
    }
}
