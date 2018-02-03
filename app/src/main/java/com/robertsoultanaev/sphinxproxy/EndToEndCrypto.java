package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;

public class EndToEndCrypto {
    private final static Provider BC_PROVIDER = new BouncyCastleProvider();

    private final static String KEY_GEN_ALGORITHM = "EC";
    private final static String TRANSFORMATION = "ECIES";
    private final static String EC_CURVE_NAME = "secp224r1";

    public static KeyPair generateKeyPair() {
        Security.addProvider(BC_PROVIDER);

        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(EC_CURVE_NAME);
        KeyPairGenerator kpg;
        KeyPair keyPair;

        try {
            kpg = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(ecGenParameterSpec);
            keyPair = kpg.generateKeyPair();
        } catch (Exception ex) {
            throw new RuntimeException("Key gen failed", ex);
        }

        return keyPair;
    }

    public static byte[] endToEndEncrypt(PublicKey recipientPublicKey, byte[] plainText) {
        Security.addProvider(BC_PROVIDER);

        Cipher ecies;
        byte[] cipherText;

        try {
            ecies = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            ecies.init(Cipher.ENCRYPT_MODE, recipientPublicKey);
            cipherText = ecies.doFinal(plainText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return cipherText;
    }

    public static byte[] endToEndDecrypt(PrivateKey privateKey, byte[] cipherText) {
        Security.addProvider(BC_PROVIDER);

        Cipher ecies;
        byte[] plainText;

        try {
            ecies = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            ecies.init(Cipher.DECRYPT_MODE, privateKey);
            plainText = ecies.doFinal(cipherText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return plainText;
    }
}
