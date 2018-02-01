package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EndToEndCrypto {
    public final static int TAG_BIT_LENGTH = 128;
    public final static int IV_BYTE_LENGTH = 12;
    public final static int KEY_BYTE_LENGTH = 16;
    public final static String TRANSFORMATION = "AES/GCM/NoPadding";
    public final static Provider BC_PROVIDER = new BouncyCastleProvider();

    public static SecretKey generateSymmetricKey() {
        byte[] key = new byte[KEY_BYTE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        return new SecretKeySpec(key, EndToEndCrypto.TRANSFORMATION);
    }

    public static GcmEncryptionResult symmetricEncrypt(SecretKey key, byte[] plainText) {
        byte[] iv = new byte[IV_BYTE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);

        Cipher aesGcm;
        byte[] cipherText;
        try {
            aesGcm = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
            aesGcm.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            cipherText = new byte[aesGcm.getOutputSize(plainText.length)];
            aesGcm.doFinal(plainText, 0, plainText.length, cipherText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return new GcmEncryptionResult(iv, cipherText);
    }

    public static byte[] symmetricDecrypt(SecretKey key, GcmEncryptionResult gcmEncryptionResult) {
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, gcmEncryptionResult.iv);

        Cipher aesGcm;
        byte[] plainText;
        try {
            aesGcm = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
            aesGcm.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            plainText = aesGcm.doFinal(gcmEncryptionResult.cipherText, 0, gcmEncryptionResult.cipherText.length);
        } catch (Exception ex) {
            throw new RuntimeException("Decryption failed", ex);
        }

        return plainText;
    }
}
