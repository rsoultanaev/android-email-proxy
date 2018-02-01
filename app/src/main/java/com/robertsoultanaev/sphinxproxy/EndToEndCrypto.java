package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EndToEndCrypto {
    private final static Provider BC_PROVIDER = new BouncyCastleProvider();

    private final static int SYMMETRIC_KEY_LENGTH = 128;
    private static String SYMMETRIC_ALGORITHM_NAME = "AES";
    private final static String SYMMETRIC_TRANSFORMATION = "AES/GCM/NoPadding";
    private final static int AES_GCM_TAG_LENGTH = 128;
    private final static int AES_GCM_IV_LENGTH = 96;

    public static SecretKey generateSymmetricKey() {
        KeyGenerator keyGenerator;
        SecretKey secretKey;

        try {
            keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM_NAME, BC_PROVIDER);
            keyGenerator.init(SYMMETRIC_KEY_LENGTH);
            secretKey = keyGenerator.generateKey();
        } catch (Exception ex) {
            throw new RuntimeException("Key gen failed", ex);
        }

        return secretKey;
    }

    public static GcmEncryptionResult symmetricEncrypt(SecretKey key, byte[] plainText) {
        int ivByteLength = AES_GCM_IV_LENGTH / 8;
        byte[] iv = new byte[ivByteLength];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_LENGTH, iv);

        Cipher aesGcm;
        byte[] cipherText;
        try {
            aesGcm = Cipher.getInstance(SYMMETRIC_TRANSFORMATION, BC_PROVIDER);
            aesGcm.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            cipherText = new byte[aesGcm.getOutputSize(plainText.length)];
            aesGcm.doFinal(plainText, 0, plainText.length, cipherText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return new GcmEncryptionResult(iv, cipherText);
    }

    public static byte[] symmetricDecrypt(SecretKey key, GcmEncryptionResult gcmEncryptionResult) {
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_LENGTH, gcmEncryptionResult.iv);

        Cipher aesGcm;
        byte[] plainText;
        try {
            aesGcm = Cipher.getInstance(SYMMETRIC_TRANSFORMATION, BC_PROVIDER);
            aesGcm.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            plainText = aesGcm.doFinal(gcmEncryptionResult.cipherText, 0, gcmEncryptionResult.cipherText.length);
        } catch (Exception ex) {
            throw new RuntimeException("Decryption failed", ex);
        }

        return plainText;
    }
}