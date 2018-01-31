package com.robertsoultanaev.sphinxproxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
public class SmtpMessageHandlerTest {

    @Mock
    private SphinxUtil sphinxUtil;

    @Mock
    private AsyncTcpClient asyncTcpClient;

    @Test
    public void deliverTest() throws Exception {
        byte[] packet1 = {(byte) 1, (byte) 2};
        byte[] packet2 = {(byte) 3, (byte) 4};
        byte[][] sphinxPackets = {packet1, packet2};

        String emailStr = "Subject: Test subject\r\n"
                        + "Test message body\r\n"
                        + "\r\n"
                        + "Robert\r\n";

        byte[] email = emailStr.getBytes();

        InputStream emailStream = new ByteArrayInputStream(email);
        String recipient = "mort@rsoultanaev.com";
        String from = "robert@sphinx.com";

        when(sphinxUtil.splitIntoSphinxPackets(email, recipient)).thenReturn(sphinxPackets);
        doNothing().when(asyncTcpClient).sendMessage(packet1);
        doNothing().when(asyncTcpClient).sendMessage(packet2);

        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient);
        smtpMessageHandler.deliver(from, recipient, emailStream);

        verify(sphinxUtil, times(1)).splitIntoSphinxPackets(email, recipient);
        verify(asyncTcpClient, times(1)).sendMessage(packet1);
        verify(asyncTcpClient, times(1)).sendMessage(packet2);
        verifyNoMoreInteractions(ignoreStubs(sphinxUtil));
        verifyNoMoreInteractions(ignoreStubs(asyncTcpClient));
    }

    @Test
    public void acceptTest() throws Exception {
        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient);
        String recipient = "mort@rsoultanaev.com";
        String from = "robert@sphinx.com";
        boolean result = smtpMessageHandler.accept(from, recipient);

        assertThat(result, is(true));
        verifyNoMoreInteractions(ignoreStubs(sphinxUtil));
        verifyNoMoreInteractions(ignoreStubs(asyncTcpClient));
    }
}
