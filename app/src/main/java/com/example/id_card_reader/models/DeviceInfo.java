package com.example.id_card_reader.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

public class DeviceInfo implements Parcelable {
    private UUID Id;
    private UUID roomId = null;
    public static final int STATE_NOT_REGISTERED = 0;
    public static final int STATE_REGISTERED_WITHOUT_ROOM = 1;
    public static final int STATE_REGISTERED_WITH_ROOM = 2;
    private int state = STATE_NOT_REGISTERED;
    protected DeviceInfo(Parcel in) {
        Id = (UUID) in.readSerializable();
        roomId = (UUID) in.readSerializable();
        state = in.readInt();
    }

    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public DeviceInfo(UUID Id) {
        this.Id = Id;
    }
    public DeviceInfo(UUID Id, UUID roomId, int state) {
        this.Id = Id;
        this.roomId = roomId;
        this.state = state;
    }

    public UUID getId() {
        return Id;
    }

    public void setId(UUID id) {
        this.Id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeSerializable(Id);
        dest.writeSerializable(roomId);
        dest.writeInt(state);
    }
}
