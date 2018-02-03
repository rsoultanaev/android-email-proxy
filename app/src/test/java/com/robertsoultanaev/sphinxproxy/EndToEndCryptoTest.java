package com.robertsoultanaev.sphinxproxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class EndToEndCryptoTest {

    @Test
    public void endToEndEncryptDecryptTest() throws Exception {
        String plaintext = "hello";
        KeyPair keyPair = EndToEndCrypto.generateKeyPair();

        byte[] encryptionResult = EndToEndCrypto.endToEndEncrypt(keyPair.getPublic(), plaintext.getBytes());
        String decryptionResult = new String(EndToEndCrypto.endToEndDecrypt(keyPair.getPrivate(), encryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }

    @Test
    public void decodePrivateKeyTest() throws Exception {
        PrivateKey privateKey = EndToEndCrypto.generateKeyPair().getPrivate();

        String encodedPrivateKey = EndToEndCrypto.encodeKey(privateKey);
        PrivateKey decodedPrivateKey = EndToEndCrypto.decodePrivateKey(encodedPrivateKey);

        assertThat(privateKey, is(equalTo(decodedPrivateKey)));
    }

    @Test
    public void decodePublicKeyTest() throws Exception {
        PublicKey publicKey = EndToEndCrypto.generateKeyPair().getPublic();

        String encodedPublicKey = EndToEndCrypto.encodeKey(publicKey);
        PublicKey decodedPublicKey = EndToEndCrypto.decodePublicKey(encodedPublicKey);

        assertThat(publicKey, is(equalTo(decodedPublicKey)));
    }
}
