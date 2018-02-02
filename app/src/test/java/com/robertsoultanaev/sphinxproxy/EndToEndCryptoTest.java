package com.robertsoultanaev.sphinxproxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyPair;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class EndToEndCryptoTest {

    @Test
    public void endToEndEncryptDecryptTest() throws Exception {
        String plaintext = "hello";
        KeyPair keyPair = EndToEndCrypto.generateAsymmetricKey();

        byte[] encryptionResult = EndToEndCrypto.endToEndEncrypt(keyPair.getPublic(), plaintext.getBytes());
        String decryptionResult = new String(EndToEndCrypto.endToEndDecrypt(keyPair.getPrivate(), encryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }
}
