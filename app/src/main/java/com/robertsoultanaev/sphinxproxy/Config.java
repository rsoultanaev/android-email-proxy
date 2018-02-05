package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
//        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
//        String encodedPrivateKey = endToEndCrypto.encodeKey(keyPair.getPrivate());
//        String encodedPublicKey = endToEndCrypto.encodeKey(keyPair.getPublic());

        // Fix keys for now to do testing
        String encodedPrivateKey = "MIGBAgEAMBAGByqGSM49AgEGBSuBBAAhBGowaAIBAQQcLbF1DZ8Tz9Yttnovor3I7FHhdNI/hnDfLEUiqaAHBgUrgQQAIaE8AzoABCNvOS14yIEldac3N0kxLbLEl6N4ckASZB0JfDu0wr3yH8pBFCmU9u3V5IYtFgB1PU/4ai+JMc5D";
        String encodedPublicKey = "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEI285LXjIgSV1pzc3STEtssSXo3hyQBJkHQl8O7TCvfIfykEUKZT27dXkhi0WAHU9T/hqL4kxzkM=";

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

    public static TrustManager getTrustManager(Context context) {
        String certString = getKey(R.string.key_mailbox_cert, context);
        TrustManager trustManager;

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream certInput = new ByteArrayInputStream(certString.getBytes());

            Certificate ca = certFactory.generateCertificate(certInput);;
            certInput.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            trustManager = tmf.getTrustManagers()[0];
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load the trust manager", ex);
        }

        return trustManager;
    }
}
