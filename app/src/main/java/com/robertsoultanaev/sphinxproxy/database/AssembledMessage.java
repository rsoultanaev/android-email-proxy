package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssembledMessage that = (AssembledMessage) o;

        if (!uuid.equals(that.uuid)) return false;
        return Arrays.equals(messageBody, that.messageBody);
    }
}