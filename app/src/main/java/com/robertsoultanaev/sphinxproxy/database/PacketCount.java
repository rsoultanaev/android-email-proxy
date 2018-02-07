package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class PacketCount {
    @PrimaryKey
    @NonNull
    public String uuid;

    public int packetsInMessage;
    public int packetsReceived;

    public PacketCount(String uuid, int packetsInMessage, int packetsReceived) {
        this.uuid = uuid;
        this.packetsInMessage = packetsInMessage;
        this.packetsReceived = packetsReceived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PacketCount that = (PacketCount) o;

        if (packetsInMessage != that.packetsInMessage) return false;
        if (packetsReceived != that.packetsReceived) return false;
        return uuid.equals(that.uuid);
    }
}