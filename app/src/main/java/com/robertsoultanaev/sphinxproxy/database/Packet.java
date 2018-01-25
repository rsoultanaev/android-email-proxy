package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;


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
}