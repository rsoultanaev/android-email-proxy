package com.robertsoultanaev.sphinxproxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class EndToEndCryptoTest {
    @Test
    public void symmetricEncryptDecryptTest() throws Exception {
        String plaintext = "hello";
        SecretKey key = EndToEndCrypto.generateSymmetricKey();

        GcmEncryptionResult gcmEncryptionResult = EndToEndCrypto.symmetricEncrypt(key, plaintext.getBytes());
        String decryptionResult = new String(EndToEndCrypto.symmetricDecrypt(key, gcmEncryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }

    @Test
    public void asymmetricEncryptDecryptTest() throws Exception {
        String plaintext = "hello";
        KeyPair keyPair = EndToEndCrypto.generateAsymmetricKey();

        byte[] encryptionResult = EndToEndCrypto.asymmetricEncrypt(keyPair.getPublic(), plaintext.getBytes());
        String decryptionResult = new String(EndToEndCrypto.asymmetricDecrypt(keyPair.getPrivate(), encryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }
}
