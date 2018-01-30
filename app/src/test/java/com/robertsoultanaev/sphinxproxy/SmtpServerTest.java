package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.subethamail.smtp.client.SmartClient;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class SmtpServerTest {

    @Mock
    private SmtpMessageHandler smtpMessageHandler;

    @Mock
    private DBQuery dbQuery;

    @Test
    public void relayTest() throws Exception {
        int smtpPort = 28000;
        String emailStr = "Subject: Test subject\r\n"
                        + "Test message body\r\n"
                        + "\r\n"
                        + "Robert\r\n";

        byte[] email = emailStr.getBytes();

        String recipient = "mort@rsoultanaev.com";
        String from = "robert@sphinx.com";

        MixNode node1 = new MixNode(8000, "0221ad76d2fd0ec503ff72f55e5afb93605f8133c947b800bb2d386e5c");
        MixNode node2 = new MixNode(8001, "037d9959a0ce756953cfad237efa3531a23f297e2b66901862deefdd58");
        MixNode node3 = new MixNode(8002, "033e8682257991788cb911f54661f4f71ba769ae460f66a3c8f515447d");
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();
        nodeList.add(node1);
        nodeList.add(node2);
        nodeList.add(node3);

        when(dbQuery.getMixNodes()).thenReturn(nodeList);

        SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);
        AsyncTcpClient asyncTcpClient = new AsyncTcpClient("localhost", 10000);
        SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient);

        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));
        smtpServer.setPort(smtpPort);
        smtpServer.start();

        SmartClient client = new SmartClient("localhost", smtpPort,"localhost");
        client.from(from);
        client.to(recipient);
        client.dataStart();
        client.dataWrite(email, email.length);
        client.dataEnd();
        client.quit();

        smtpServer.stop();
    }
}
