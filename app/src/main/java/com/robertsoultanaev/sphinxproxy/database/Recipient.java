package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class Recipient {
    @NonNull @PrimaryKey
    public String address;
    @NonNull
    public String encodedPublicKey;

    public Recipient(@NonNull String address, @NonNull String encodedPublicKey) {
        this.address = address;
        this.encodedPublicKey = encodedPublicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recipient recipient = (Recipient) o;

        if (!address.equals(recipient.address)) return false;
        return encodedPublicKey.equals(recipient.encodedPublicKey);
    }
}
