package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EndToEndCrypto {
    private final static Provider BC_PROVIDER = new BouncyCastleProvider();

    private final static int SYMMETRIC_KEY_LENGTH = 128;
    private static String SYMMETRIC_ALGORITHM_NAME = "AES";
    private final static String SYMMETRIC_TRANSFORMATION = "AES/GCM/NoPadding";
    private final static int AES_GCM_TAG_LENGTH = 128;
    private final static int AES_GCM_IV_LENGTH = 96;

    private static int RSA_KEY_LENGTH = 4096;
    private static String ASYMMETRIC_ALGORITHM_NAME = "RSA";
    private final static String ASYMMETRIC_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING";

    public static HybridEncryptionResult hybridEncrypt(PublicKey recipientPublicKey, byte[] plaintext) {
        SecretKey symmetricKey = generateSymmetricKey();
        byte[] encodedSymmetricKey = symmetricKey.getEncoded();

        GcmEncryptionResult encryptedPlainTextWithIv = symmetricEncrypt(symmetricKey, plaintext);
        byte[] encryptedSymmetricKey = asymmetricEncrypt(recipientPublicKey, encodedSymmetricKey);

        return new HybridEncryptionResult(encryptedPlainTextWithIv, encryptedSymmetricKey);
    }

    public static byte[] hybridDecrypt(PrivateKey privateKey, HybridEncryptionResult hybridEncryptionResult) {
        byte[] encodedSymmetricKey = asymmetricDecrypt(privateKey, hybridEncryptionResult.encryptedSymmetricKey);
        SecretKey symmetricKey = new SecretKeySpec(encodedSymmetricKey, 0, encodedSymmetricKey.length, SYMMETRIC_ALGORITHM_NAME);
        byte[] plainText = symmetricDecrypt(symmetricKey, hybridEncryptionResult.gcmEncryptionResult);

        return plainText;
    }

    public static KeyPair generateAsymmetricKey() {
        KeyPairGenerator rsaKeyGen;
        KeyPair keypair;

        try {
            rsaKeyGen = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM_NAME, BC_PROVIDER);
            rsaKeyGen.initialize(RSA_KEY_LENGTH);
            keypair = rsaKeyGen.generateKeyPair();
        } catch (Exception ex) {
            throw new RuntimeException("Key gen failed", ex);
        }

        return keypair;
    }

    public static byte[] asymmetricEncrypt(PublicKey publicKey, byte[] plainText) {
        Cipher rsa;
        byte[] cipherText;

        try {
            rsa = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION, BC_PROVIDER);
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherText = rsa.doFinal(plainText);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed", ex);
        }

        return cipherText;
    }

    public static byte[] asymmetricDecrypt(PrivateKey privateKey, byte[] cipherText) {
        Cipher rsa;
        byte[] plainText;

        try {
            rsa = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION, BC_PROVIDER);
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            plainText = rsa.doFinal(cipherText);
        } catch (Exception ex) {
            throw new RuntimeException("Decryption failed", ex);
        }

        return plainText;
    }

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
