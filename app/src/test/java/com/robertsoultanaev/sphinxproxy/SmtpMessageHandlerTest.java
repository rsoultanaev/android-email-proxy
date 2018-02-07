package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;

@RunWith(MockitoJUnitRunner.class)
public class SmtpMessageHandlerTest {

    @Mock
    private DBQuery dbQuery;

    @Mock
    private EndToEndCrypto endToEndCrypto;

    @Mock
    private SphinxUtil sphinxUtil;

    @Mock
    private AsyncTcpClient asyncTcpClient;

    @Test
    public void deliverTest() throws Exception {
        int packet1FirstNodeId = 8000;
        int packet2FirstNodeId = 8001;
        byte[] packet1BinMessage = {(byte) 1, (byte) 2};
        byte[] packet2BinMessage = {(byte) 3, (byte) 4};
        SphinxPacketWithRouting packet1 = new SphinxPacketWithRouting(packet1FirstNodeId, packet1BinMessage);
        SphinxPacketWithRouting packet2 = new SphinxPacketWithRouting(packet2FirstNodeId, packet2BinMessage);

        SphinxPacketWithRouting[] sphinxPackets = {packet1, packet2};

        String emailStr = "Subject: Test subject\r\n"
                        + "Test message body\r\n"
                        + "\r\n"
                        + "Robert\r\n";

        byte[] email = emailStr.getBytes();
        byte[] encryptedEmail = new byte[100];

        InputStream emailStream = new ByteArrayInputStream(email);
        String recipientAddress = "mort@rsoultanaev.com";
        PublicKey recipientPublicKey = (new EndToEndCrypto()).generateKeyPair().getPublic();
        String encodedRecipientPublicKey = "encodedPublicKey";
        Recipient recipient = new Recipient(recipientAddress, encodedRecipientPublicKey);

        String from = "robert@sphinx.com";

        when(endToEndCrypto.decodePublicKey(encodedRecipientPublicKey)).thenReturn(recipientPublicKey);
        when(endToEndCrypto.endToEndEncrypt(recipientPublicKey, email)).thenReturn(encryptedEmail);
        when(dbQuery.getRecipient(recipientAddress)).thenReturn(recipient);
        when(sphinxUtil.splitIntoSphinxPackets(encryptedEmail, recipientAddress)).thenReturn(sphinxPackets);
        doNothing().when(asyncTcpClient).sendMessage(packet1);
        doNothing().when(asyncTcpClient).sendMessage(packet2);

        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient, dbQuery, endToEndCrypto);
        smtpMessageHandler.deliver(from, recipientAddress, emailStream);

        verify(sphinxUtil, times(1)).splitIntoSphinxPackets(encryptedEmail, recipientAddress);
        verify(asyncTcpClient, times(1)).sendMessage(packet1);
        verify(asyncTcpClient, times(1)).sendMessage(packet2);
        verifyNoMoreInteractions(ignoreStubs(sphinxUtil));
        verifyNoMoreInteractions(ignoreStubs(asyncTcpClient));
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
        verifyNoMoreInteractions(ignoreStubs(endToEndCrypto));
    }

    @Test
    public void acceptTest() throws Exception {
        String recipientAddress = "mort@rsoultanaev.com";
        String encodedRecipientPublicKey = "encodedPublicKey";
        Recipient recipient = new Recipient(recipientAddress, encodedRecipientPublicKey);

        String from = "robert@sphinx.com";

        when(dbQuery.getRecipient(recipientAddress)).thenReturn(recipient);

        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient, dbQuery, endToEndCrypto);
        boolean result = smtpMessageHandler.accept(from, recipientAddress);

        assertThat(result, is(true));
        verifyNoMoreInteractions(ignoreStubs(sphinxUtil));
        verifyNoMoreInteractions(ignoreStubs(asyncTcpClient));
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
        verifyNoMoreInteractions(ignoreStubs(endToEndCrypto));
    }
}
