package com.example.id_card_reader.services.api;

import com.example.id_card_reader.models.DeviceInfo;

import java.util.UUID;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody; // For the response
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    @Multipart
    @POST("api/logs")
    Call<ResponseBody> addLog(
            @Part MultipartBody.Part image,
            @Part("DeviceId") RequestBody deviceId,
            @Part("CivilianId") RequestBody civilianId,
            @Part("CreatedAt") RequestBody createdAt
    );

    @GET("api/devices/{deviceId}/members/{civilianId}")
    Call<ResponseBody> getMemberInfo(
            @Path("deviceId") UUID deviceId,
            @Path("civilianId") String civilianId
    );

    @GET("api/devices/{deviceId}")
    Call<DeviceInfo> getDeviceInfo(
            @Path("deviceId") UUID deviceId
    );
}
