package com.robertsoultanaev.sphinxproxy;

public class GcmEncryptionResult {
    public final byte[] iv;
    public final byte[] cipherText;

    public GcmEncryptionResult(byte[] iv, byte[] cipherText) {
        this.iv = iv;
        this.cipherText = cipherText;
    }
}
