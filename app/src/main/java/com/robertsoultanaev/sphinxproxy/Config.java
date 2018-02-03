package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Config {
    public static void setKey(int keyId, String value, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(keyId);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getKey(int keyId, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(keyId);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        return sharedPreferences.getString(key, null);
    }

    public static void setKeyPair(KeyPair keyPair, Context context) {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        String encodedPrivateKey = endToEndCrypto.encodeKey(keyPair.getPrivate());
        String encodedPublicKey = endToEndCrypto.encodeKey(keyPair.getPublic());

        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String keyPrivateKey = context.getString(R.string.key_private_key);
        String keyPublicKey = context.getString(R.string.key_public_key);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(keyPrivateKey, encodedPrivateKey);
        editor.putString(keyPublicKey, encodedPublicKey);
        editor.apply();
    }

    public static PrivateKey getPrivateKey(Context context) {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(R.string.key_private_key);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        String base64EncodedPrivateKey = sharedPreferences.getString(key, null);

        if (base64EncodedPrivateKey == null) {
            throw new RuntimeException("Private key not set");
        }

        return endToEndCrypto.decodePrivateKey(base64EncodedPrivateKey);
    }



    public static PublicKey getPublicKey(Context context) {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(R.string.key_public_key);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        String base64EncodedPublicKey = sharedPreferences.getString(key, null);

        if (base64EncodedPublicKey == null) {
            throw new RuntimeException("Public key not set");
        }

        return endToEndCrypto.decodePublicKey(base64EncodedPublicKey);
    }
}
