package com.example.id_card_reader.models.dto;

import android.graphics.Bitmap;

import java.util.Date;
import java.util.UUID;

public class AddLogDto {
    private UUID deviceId;
    private String civilianId;
    private Bitmap imageBitmap;

    private Date createdAt = new Date();

    public AddLogDto(UUID deviceId, String civilianId, Bitmap imageBitmap) {
        this.deviceId = deviceId;
        this.civilianId = civilianId;
        this.imageBitmap = imageBitmap;
    }

    public AddLogDto(UUID deviceId, String civilianId, Bitmap imageBitmap, Date createdAt) {
        this.deviceId = deviceId;
        this.civilianId = civilianId;
        this.imageBitmap = imageBitmap;
        this.createdAt = createdAt;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
