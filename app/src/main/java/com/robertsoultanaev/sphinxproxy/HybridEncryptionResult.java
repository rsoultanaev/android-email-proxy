package com.robertsoultanaev.sphinxproxy;

/**
 * Created by rsoultanaev on 01/02/18.
 */

public class HybridEncryptionResult {
    GcmEncryptionResult gcmEncryptionResult;
    byte[] encryptedSymmetricKey;

    public HybridEncryptionResult(GcmEncryptionResult gcmEncryptionResult, byte[] encryptedSymmetricKey) {
        this.gcmEncryptionResult = gcmEncryptionResult;
        this.encryptedSymmetricKey = encryptedSymmetricKey;
    }
}
