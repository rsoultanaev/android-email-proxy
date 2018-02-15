package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class MixNode {
    @NonNull @PrimaryKey
    public int id;
    @NonNull
    public String host;
    @NonNull
    public int port;
    @NonNull
    public String encodedPublicKey;

    public MixNode(@NonNull int id, @NonNull String host, @NonNull int port, @NonNull String encodedPublicKey) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.encodedPublicKey = encodedPublicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MixNode mixNode = (MixNode) o;

        if (id != mixNode.id) return false;
        if (port != mixNode.port) return false;
        if (!host.equals(mixNode.host)) return false;
        return encodedPublicKey.equals(mixNode.encodedPublicKey);
    }
}
