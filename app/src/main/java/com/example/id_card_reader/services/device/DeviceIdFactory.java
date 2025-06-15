package com.example.id_card_reader.services.device;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class DeviceIdFactory {
    private static final String PREF_DEVICE_ID = "pref_device_id";
    private static UUID deviceId;

    public DeviceIdFactory(Context context) {
        synchronized (DeviceIdFactory.class) {
            if (deviceId == null) {
                final SharedPreferences sharedPrefs = context.getSharedPreferences(
                        "device_uuid", Context.MODE_PRIVATE);
                final String id = sharedPrefs.getString(PREF_DEVICE_ID, null);

                if (id != null) {
                    deviceId = UUID.fromString(id);
                } else {
                    deviceId = UUID.randomUUID();
                    sharedPrefs.edit().putString(PREF_DEVICE_ID, deviceId.toString()).apply();
                }
            }
        }
    }
    public UUID getDeviceId() {
        return deviceId;
    }
}
