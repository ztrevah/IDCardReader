package com.example.id_card_reader;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static final String API_BASE_URL = "https://aaf6-2a09-bac5-d45a-263c-00-3cf-2a.ngrok-free.app/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Log request and response bodies

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout
                    .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create()) // If your backend returns JSON responses
                    .build();
        }
        return retrofit;
    }
}
