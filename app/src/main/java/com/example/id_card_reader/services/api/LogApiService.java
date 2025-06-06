package com.example.id_card_reader.services.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody; // For the response
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface LogApiService {
    @Multipart
    @POST("api/logs")
    Call<ResponseBody> addLog(
            @Part MultipartBody.Part image,
            @Part("DeviceId") RequestBody deviceId,
            @Part("CivilianId") RequestBody civilianId,
            @Part("CreatedAt") RequestBody createdAt
    );
}
