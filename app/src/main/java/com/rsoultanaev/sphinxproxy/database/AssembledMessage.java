package com.rsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class AssembledMessage {
    @PrimaryKey
    public String uuid;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] message;

    public AssembledMessage(String uuid, byte[] message) {
        this.uuid = uuid;
        this.message = message;
    }
}