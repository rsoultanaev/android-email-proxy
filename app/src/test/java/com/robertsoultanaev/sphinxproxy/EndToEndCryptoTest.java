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
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        KeyPair keyPair = endToEndCrypto.generateKeyPair();

        byte[] encryptionResult = endToEndCrypto.endToEndEncrypt(keyPair.getPublic(), plaintext.getBytes());
        String decryptionResult = new String(endToEndCrypto.endToEndDecrypt(keyPair.getPrivate(), encryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }

    @Test
    public void decodePrivateKeyTest() throws Exception {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        PrivateKey privateKey = endToEndCrypto.generateKeyPair().getPrivate();

        String encodedPrivateKey = endToEndCrypto.encodeKey(privateKey);
        PrivateKey decodedPrivateKey = endToEndCrypto.decodePrivateKey(encodedPrivateKey);

        assertThat(privateKey, is(equalTo(decodedPrivateKey)));
    }

    @Test
    public void decodePublicKeyTest() throws Exception {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        PublicKey publicKey = endToEndCrypto.generateKeyPair().getPublic();

        String encodedPublicKey = endToEndCrypto.encodeKey(publicKey);
        PublicKey decodedPublicKey = endToEndCrypto.decodePublicKey(encodedPublicKey);

        assertThat(publicKey, is(equalTo(decodedPublicKey)));
    }
}
