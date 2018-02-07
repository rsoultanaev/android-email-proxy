package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

import java.util.Arrays;


@Entity(primaryKeys = {"uuid", "sequenceNumber", "packetsInMessage"})
public class Packet {
    @NonNull
    public String uuid;
    @NonNull
    public int sequenceNumber;
    @NonNull
    public int packetsInMessage;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] payload;

    public Packet(String uuid, int sequenceNumber, int packetsInMessage, byte[] payload) {
        this.uuid = uuid;
        this.sequenceNumber = sequenceNumber;
        this.packetsInMessage = packetsInMessage;
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Packet packet = (Packet) o;

        if (sequenceNumber != packet.sequenceNumber) return false;
        if (packetsInMessage != packet.packetsInMessage) return false;
        if (!uuid.equals(packet.uuid)) return false;
        return Arrays.equals(payload, packet.payload);
    }
}