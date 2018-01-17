package com.rsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;

@Entity(primaryKeys = {"uuid", "sequenceNumber", "packetsInMessage"})
public class Packet {
    public String uuid;
    public int sequenceNumber;
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