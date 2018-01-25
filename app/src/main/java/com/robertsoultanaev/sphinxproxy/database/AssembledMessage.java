package com.robertsoultanaev.sphinxproxy.database;

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
    public byte[] messageBody;

    public AssembledMessage(String uuid, byte[] messageBody) {
        this.uuid = uuid;
        this.messageBody = messageBody;
    }
}