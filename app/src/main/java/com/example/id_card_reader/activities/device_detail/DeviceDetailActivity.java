package com.example.id_card_reader.activities.device_detail;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.id_card_reader.services.device.DeviceIdFactory;
import com.example.id_card_reader.R;
import com.example.id_card_reader.services.api.RetrofitApiClient;
import com.example.id_card_reader.models.DeviceInfo;
import com.example.id_card_reader.services.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_DETAIL = "device_info_data"; // Key for the Intent extra
    private TextView deviceIdTextView, roomIdTextView, stateTextView;
    private ImageView btnRefresh;

    private DeviceInfo deviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        deviceIdTextView = findViewById(R.id.deviceIdValue);
        roomIdTextView = findViewById(R.id.deviceRoomIdValue);
        stateTextView = findViewById(R.id.deviceStateValue);
        btnRefresh = findViewById(R.id.refreshButton);

        btnRefresh.setOnClickListener(v -> getDeviceInfo());
        Bundle extras = getIntent().getExtras();

        if(extras == null) {
            getDeviceInfo();
        }
        else {
            deviceInfo = extras.getParcelable(EXTRA_DEVICE_DETAIL);
            displayDeviceInfo();
        }
    }

    private void getDeviceInfo() {
        ApiService apiService = RetrofitApiClient.getApiService();
        Call<DeviceInfo> call = apiService.getDeviceInfo(new DeviceIdFactory(this).getDeviceId());
        call.enqueue(new Callback<DeviceInfo>() {
            @Override
            public void onResponse(@NonNull Call<DeviceInfo> call, @NonNull Response<DeviceInfo> response) {
                if(response.isSuccessful()) {
                    deviceInfo = response.body();
                    if(deviceInfo == null) {
                        Toast.makeText(DeviceDetailActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        displayDeviceInfo();
                        return;
                    }

                    if(deviceInfo.getRoomId() == null) {
                        deviceInfo.setState(DeviceInfo.STATE_REGISTERED_WITHOUT_ROOM);
                    }
                    else deviceInfo.setState(DeviceInfo.STATE_REGISTERED_WITH_ROOM);
                    displayDeviceInfo();
                }
                else if(response.code() == 404) {
                    deviceInfo = new DeviceInfo(
                            new DeviceIdFactory(DeviceDetailActivity.this).getDeviceId(),
                            null,
                            DeviceInfo.STATE_NOT_REGISTERED
                    );
                    displayDeviceInfo();
                }
                else {
                    deviceInfo = null;
                    displayDeviceInfo();
                    Toast.makeText(DeviceDetailActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DeviceInfo> call, @NonNull Throwable t) {
                deviceInfo = null;
                displayDeviceInfo();
                Toast.makeText(DeviceDetailActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDeviceInfo() {
        if(deviceInfo == null) {
            deviceIdTextView.setText("N/A");
            roomIdTextView.setText("N/A");
            stateTextView.setText("N/A");
        }
        else {
            if(deviceInfo.getId() != null) deviceIdTextView.setText(deviceInfo.getId().toString());
            else deviceIdTextView.setText("N/A");

            if(deviceInfo.getRoomId() != null) roomIdTextView.setText(deviceInfo.getRoomId().toString());
            else roomIdTextView.setText("N/A");

            if (deviceInfo.getState() == DeviceInfo.STATE_NOT_REGISTERED) {
                stateTextView.setText("Not Registered");
            } else {
                stateTextView.setText("Registered");
            }
        }
    }
}
