package com.example.id_card_reader.models;

import android.graphics.Bitmap;

import java.io.File;
import java.util.UUID;

public class AddLog {
    private UUID deviceId;
    private String civilianId;
    private Bitmap imageBitmap;

    public AddLog(UUID deviceId, String civilianId, Bitmap imageBitmap) {
        this.deviceId = deviceId;
        this.civilianId = civilianId;
        this.imageBitmap = imageBitmap;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getCivilianId() {
        return civilianId;
    }

    public void setCivilianId(String civilianId) {
        this.civilianId = civilianId;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }
}
