package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Config {
    public static boolean setupDone(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String setupDoneKey = context.getString(R.string.key_setup_done);
        return sharedPreferences.getBoolean(setupDoneKey, false);
    }

    public static void setSetupDone(Context context, boolean setupDone) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(R.string.key_setup_done);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, setupDone);
        editor.apply();
    }

    public static void setIntValue(int keyId, int value, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(keyId);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int getIntValue(int keyId, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(keyId);

        return sharedPreferences.getInt(key, -1);
    }

    public static void setStringValue(int keyId, String value, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(keyId);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getStringValue(int keyId, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(keyId);

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
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(R.string.key_private_key);

        String base64EncodedPrivateKey = sharedPreferences.getString(key, null);

        if (base64EncodedPrivateKey == null) {
            throw new RuntimeException("Private key not set");
        }

        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

        return endToEndCrypto.decodePrivateKey(base64EncodedPrivateKey);
    }

    public static PublicKey getPublicKey(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String key = context.getString(R.string.key_public_key);

        String base64EncodedPublicKey = sharedPreferences.getString(key, null);

        if (base64EncodedPublicKey == null) {
            throw new RuntimeException("Public key not set");
        }

        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

        return endToEndCrypto.decodePublicKey(base64EncodedPublicKey);
    }

    public static TrustManager getTrustManager(Context context) {
        String certString = getStringValue(R.string.key_mailbox_cert, context);
        TrustManager trustManager;

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream certInput = new ByteArrayInputStream(certString.getBytes());

            Certificate ca = certFactory.generateCertificate(certInput);
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

    private static SharedPreferences getSharedPreferences(Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        return context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);
    }
}
