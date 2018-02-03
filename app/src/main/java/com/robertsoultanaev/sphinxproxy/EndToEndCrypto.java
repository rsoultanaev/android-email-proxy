package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class EndToEndCrypto {
    private final static Provider BC_PROVIDER = new BouncyCastleProvider();

    private final static String KEY_GEN_ALGORITHM = "EC";
    private final static String TRANSFORMATION = "ECIES";
    private final static String EC_CURVE_NAME = "secp224r1";

    public static KeyPair generateKeyPair() {
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(EC_CURVE_NAME);
        KeyPairGenerator kpg;
        KeyPair keyPair;

        try {
            kpg = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM, BC_PROVIDER);
            kpg.initialize(ecGenParameterSpec);
            keyPair = kpg.generateKeyPair();
        } catch (Exception ex) {
            throw new RuntimeException("Key gen failed", ex);
        }

        return keyPair;
    }

    public static byte[] endToEndEncrypt(PublicKey recipientPublicKey, byte[] plainText) {
        Cipher ecies;
        byte[] cipherText;

        try {
            ecies = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
            ecies.init(Cipher.ENCRYPT_MODE, recipientPublicKey);
            cipherText = ecies.doFinal(plainText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return cipherText;
    }

    public static byte[] endToEndDecrypt(PrivateKey privateKey, byte[] cipherText) {
        Cipher ecies;
        byte[] plainText;

        try {
            ecies = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
            ecies.init(Cipher.DECRYPT_MODE, privateKey);
            plainText = ecies.doFinal(cipherText);
        } catch (Exception ex) {
            throw new RuntimeException("Decryption failed", ex);
        }

        return plainText;
    }

    public static String encodeKey(Key key) {
        return new String(Base64.encode(key.getEncoded()));
    }

    public static PrivateKey decodePrivateKey(String base64EncodedPrivateKey) {
        byte[] encodedPrivateKey = Base64.decode(base64EncodedPrivateKey.getBytes());

        PrivateKey privateKey;
        try {
            KeyFactory kf = KeyFactory.getInstance(KEY_GEN_ALGORITHM, BC_PROVIDER);
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
        } catch (Exception ex) {
            throw new RuntimeException("Decoding failed", ex);
        }

        return privateKey;
    }

    public static PublicKey decodePublicKey(String base64EncodedPublicKey) {
        byte[] encodedPublicKey = Base64.decode(base64EncodedPublicKey.getBytes());

        PublicKey publicKey;
        try {
            KeyFactory kf = KeyFactory.getInstance(KEY_GEN_ALGORITHM, BC_PROVIDER);
            publicKey = kf.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
        } catch (Exception ex) {
            throw new RuntimeException("Decoding failed", ex);
        }

        return publicKey;
    }
}
