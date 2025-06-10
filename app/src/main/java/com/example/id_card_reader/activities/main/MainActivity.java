package com.example.id_card_reader.activities.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.id_card_reader.DeviceIdFactory;
import com.example.id_card_reader.R;
import com.example.id_card_reader.RetrofitApiClient;
import com.example.id_card_reader.activities.device_detail.DeviceDetailActivity;
import com.example.id_card_reader.activities.scanning.ScanningActivity;
import com.example.id_card_reader.models.DeviceInfo;
import com.example.id_card_reader.services.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private CardView cardDeviceInfo, cardScanning, cardSettings, cardExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardDeviceInfo = findViewById(R.id.card_device_info);
        cardScanning = findViewById(R.id.card_scanning);
        cardSettings = findViewById(R.id.card_settings);
        cardExit = findViewById(R.id.card_exit);

        cardDeviceInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDeviceDetail();
            }
        });

        cardScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkDeviceScannable();
            }
        });

        cardSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        cardExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the current activity
            }
        });
    }

    private void openDeviceDetail() {
        ApiService apiService = RetrofitApiClient.getApiService();
        Call<DeviceInfo> call = apiService.getDeviceInfo(new DeviceIdFactory(this).getDeviceId());
        call.enqueue(new Callback<DeviceInfo>() {
            @Override
            public void onResponse(@NonNull Call<DeviceInfo> call, @NonNull Response<DeviceInfo> response) {
                DeviceInfo deviceInfo = null;
                if(response.isSuccessful()) {
                    deviceInfo = response.body();
                    if(deviceInfo == null) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(deviceInfo.getRoomId() == null) {
                        deviceInfo.setState(DeviceInfo.STATE_REGISTERED_WITHOUT_ROOM);
                    }
                    else deviceInfo.setState(DeviceInfo.STATE_REGISTERED_WITH_ROOM);

                    Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                    intent.putExtra(DeviceDetailActivity.EXTRA_DEVICE_DETAIL, deviceInfo);
                    startActivity(intent);
                }
                else if(response.code() == 404) {
                    deviceInfo = new DeviceInfo(
                            new DeviceIdFactory(MainActivity.this).getDeviceId(),
                            null,
                            DeviceInfo.STATE_NOT_REGISTERED
                    );

                    Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                    intent.putExtra(DeviceDetailActivity.EXTRA_DEVICE_DETAIL, deviceInfo);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DeviceInfo> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void checkDeviceScannable() {
        ApiService apiService = RetrofitApiClient.getApiService();
        Call<DeviceInfo> call = apiService.getDeviceInfo(new DeviceIdFactory(this).getDeviceId());
        call.enqueue(new Callback<DeviceInfo>() {
            @Override
            public void onResponse(@NonNull Call<DeviceInfo> call, @NonNull Response<DeviceInfo> response) {
                if(response.isSuccessful()) {
                    DeviceInfo deviceInfo = response.body();
                    if(deviceInfo == null) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(deviceInfo.getRoomId() != null) {
                        Intent intent = new Intent(MainActivity.this, ScanningActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Device has not been registered to any room.", Toast.LENGTH_SHORT).show();
                    }
                } else if(response.code() == 404) {
                    Toast.makeText(MainActivity.this, "Device has not been registered.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DeviceInfo> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
