package com.robertsoultanaev.sphinxproxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/*
        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));

        smtpServer.setPort(smtpPort);
        smtpServer.start();

        SmartClient client = new SmartClient("localhost", smtpPort,"localhost");
        client.from("robert@sphinx.com");
        client.to(dest);
        client.dataStart();
        client.dataWrite(email, email.length);
        client.dataEnd();
        client.quit();

        smtpServer.stop();
*/

@RunWith(MockitoJUnitRunner.class)
public class SmtpMessageHandlerTest {

    @Mock
    private DBQuery dbQuery;

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
    }

    @Test
    public void acceptTest() throws Exception {
        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient);
        String recipient = "mort@rsoultanaev.com";
        String from = "robert@sphinx.com";
        boolean result = smtpMessageHandler.accept(from, recipient);

        assertThat(result, is(true));
    }

    @Test
    public void testTest() {
        MixNode node1 = new MixNode(1, "123");
        MixNode node2 = new MixNode(2, "456");

        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();
        nodeList.add(node1);
        nodeList.add(node2);

        String s = "1 123\n2 456\n";

        when(dbQuery.getMixNodes()).thenReturn(nodeList);
        TestObject t = new TestObject(dbQuery);
        String res = t.doThing();

        assertThat(res, is(s));
    }
}
