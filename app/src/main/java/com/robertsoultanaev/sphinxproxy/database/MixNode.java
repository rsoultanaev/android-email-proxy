package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

@Entity(primaryKeys = {"port", "encodedPublicKey"})
public class MixNode {
    @NonNull
    public int port;
    @NonNull
    public String encodedPublicKey;

    public MixNode(@NonNull int port, @NonNull String encodedPublicKey) {
        this.port = port;
        this.encodedPublicKey = encodedPublicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MixNode mixNode = (MixNode) o;

        if (port != mixNode.port) return false;
        return encodedPublicKey.equals(mixNode.encodedPublicKey);
    }
}
