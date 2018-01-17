package com.rsoultanaev.sphinxproxy.database;

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
}