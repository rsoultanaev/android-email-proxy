package com.rsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class AssembledMessage {
    @PrimaryKey
    @NonNull
    public String uuid;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] message;

    public AssembledMessage(String uuid, byte[] message) {
        this.uuid = uuid;
        this.message = message;
    }
}