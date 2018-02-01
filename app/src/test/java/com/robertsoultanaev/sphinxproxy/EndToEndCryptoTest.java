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
    public void hybridEncryptDecryptTest() throws Exception {
        String plaintext = "hello";
        KeyPair keyPair = EndToEndCrypto.generateAsymmetricKey();

        HybridEncryptionResult hybridEncryptionResult = EndToEndCrypto.hybridEncrypt(keyPair.getPublic(), plaintext.getBytes());
        String decryptionResult = new String(EndToEndCrypto.hybridDecrypt(keyPair.getPrivate(), hybridEncryptionResult));

        assertThat(plaintext, is(equalTo(decryptionResult)));
    }
}
