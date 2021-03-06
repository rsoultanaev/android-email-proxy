package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.javasphinx.Util;
import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.subethamail.smtp.client.SmartClient;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ProxyServiceTest {

    private Context context;
    private DBQuery dbQuery;

    private int pop3ServerPort = 27000;
    private int smtpProxyPort = 28000;

    @Before
    public void setup() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        DB db = DB.getAppDatabase(context);
        dbQuery = db.getDao();
        dbQuery.deleteEverything();

        Config.setIntValue(R.string.key_proxy_pop3_port, pop3ServerPort, context);
        Config.setIntValue(R.string.key_proxy_smtp_port, smtpProxyPort, context);
        Config.setStringValue(R.string.key_proxy_username, "proxyuser", context);
        Config.setStringValue(R.string.key_proxy_password, "12345", context);
        Config.setStringValue(R.string.key_mailbox_hostname, "ec2-35-178-56-77.eu-west-2.compute.amazonaws.com", context);
        Config.setIntValue(R.string.key_mailbox_port, 995, context);
        Config.setStringValue(R.string.key_mailbox_username, "mort", context);
        Config.setStringValue(R.string.key_mailbox_password, "1234", context);
        Config.setIntValue(R.string.key_num_use_mixes, 3, context);
    }

    @After
    public void cleanup() throws Exception {
        Intent proxyIntent = new Intent(context, ProxyService.class);
        context.stopService(proxyIntent);

        Thread.sleep(1000);
    }

    @Test
    public void testSMTPProxy() throws Exception {
        String senderAddress = "john@example.com";
        String recipientAddress = "mort@rsoultanaev.com";
        String mixHostname = "localhost";
        int mixPort = 30000;
        String messageStartToken = "jf764594bnf83";
        String messageBodyStr = messageStartToken + "Test message";
        byte[] messageBody = messageBodyStr.getBytes();
        String encodedRecipientPrivateKey = "MIGBAgEAMBAGByqGSM49AgEGBSuBBAAhBGowaAIBAQQcLbF1DZ8Tz9Yttnovor3I7FHhdNI/hnDfLEUiqaAHBgUrgQQAIaE8AzoABCNvOS14yIEldac3N0kxLbLEl6N4ckASZB0JfDu0wr3yH8pBFCmU9u3V5IYtFgB1PU/4ai+JMc5D";
        String encodedRecipientPublicKey = "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEI285LXjIgSV1pzc3STEtssSXo3hyQBJkHQl8O7TCvfIfykEUKZT27dXkhi0WAHU9T/hqL4kxzkM=";

        SphinxParams params = new SphinxParams();
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

        Config.setKeyPair(encodedRecipientPrivateKey, encodedRecipientPublicKey, context);

        PrivateKey recipientPrivateKey = endToEndCrypto.decodePrivateKey(encodedRecipientPrivateKey);

        HashMap<Integer, MockMixPki> pki = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            BigInteger priv = params.getGroup().genSecret();
            ECPoint pub = params.getGroup().expon(params.getGroup().getGenerator(), priv);
            String encodedPub = Hex.toHexString(pub.getEncoded(true));

            pki.put(i,  new MockMixPki(priv, pub));
            dbQuery.insertMixNode(new MixNode(i, mixHostname, mixPort, encodedPub));
        }

        Recipient recipient = new Recipient(recipientAddress, encodedRecipientPublicKey);
        dbQuery.insertRecipient(recipient);

        Intent proxyIntent = new Intent(context, ProxyService.class);
        context.startService(proxyIntent);

        MockMixnetwork mockMixnetwork = new MockMixnetwork(pki, mixPort);

        Thread.sleep(2000);

        SmartClient client = new SmartClient("localhost", smtpProxyPort,"localhost");
        client.from(senderAddress);
        client.to(recipientAddress);
        client.dataStart();
        client.dataWrite(messageBody, messageBody.length);
        client.dataEnd();
        client.quit();

        Thread.sleep(2000);

        byte[] desphinxedMessage = mockMixnetwork.getDesphinxedMessage();

        // Strip away the header used for reassembly
        byte[] decodedMessage = Util.slice(desphinxedMessage, 24, desphinxedMessage.length);

        byte[] decryptedMessage = endToEndCrypto.endToEndDecrypt(recipientPrivateKey, decodedMessage);
        String decryptedMessageStr = new String(decryptedMessage);

        // Strip away the stuff added to the message by the SMTP server
        int startOfResultingMessage = decryptedMessageStr.indexOf(messageStartToken);
        String finalResultingMessage = decryptedMessageStr.substring(startOfResultingMessage);
        finalResultingMessage = finalResultingMessage.substring(0, finalResultingMessage.length() - 2);

        assertEquals(messageBodyStr, finalResultingMessage);
    }

    @Test
    public void testPOP3Server() throws Exception {
        String msg1id = UUID.randomUUID().toString();
        String msg2id = UUID.randomUUID().toString();
        String msg1Body = "hello" + Pop3Server.CRLF;
        String msg2Body = "bye" + Pop3Server.CRLF;
        AssembledMessage msg1 = new AssembledMessage(msg1id, msg1Body.getBytes());
        AssembledMessage msg2 = new AssembledMessage(msg2id, msg2Body.getBytes());

        dbQuery.addAssembledMessage(msg1);
        dbQuery.addAssembledMessage(msg2);

        Intent proxyIntent = new Intent(context, ProxyService.class);
        context.startService(proxyIntent);

        Thread.sleep(3000);

        POP3Client pop3Client = new POP3Client();

        // Connect
        pop3Client.connect("localhost", pop3ServerPort);

        // USER & PASS
        boolean loginSuccessful = pop3Client.login("proxyuser", "12345");
        assertTrue(loginSuccessful);

        // STAT
        POP3MessageInfo status = pop3Client.status();
        int expectedNumber = 2;
        int expectedSize = msg1.messageBody.length + msg2.messageBody.length;
        assertEquals(expectedNumber, status.number);
        assertEquals(expectedSize, status.size);

        // LIST
        POP3MessageInfo[] msgInfo = pop3Client.listMessages();
        assertEquals(expectedNumber, msgInfo.length);
        assertEquals(1, msgInfo[0].number);
        assertEquals(2, msgInfo[1].number);
        assertEquals(msg1.messageBody.length, msgInfo[0].size);
        assertEquals(msg2.messageBody.length, msgInfo[1].size);

        // UIDL
        msgInfo = pop3Client.listUniqueIdentifiers();
        assertEquals(expectedNumber, msgInfo.length);
        assertEquals(1, msgInfo[0].number);
        assertEquals(2, msgInfo[1].number);
        assertEquals(msg1.uuid, msgInfo[0].identifier);
        assertEquals(msg2.uuid, msgInfo[1].identifier);

        // RETR
        BufferedReader reader;
        reader = (BufferedReader) pop3Client.retrieveMessage(1);
        String retrievedMsg1 = IOUtils.toString(reader);
        reader = (BufferedReader) pop3Client.retrieveMessage(2);
        String retrievedMsg2 = IOUtils.toString(reader);
        assertEquals(new String(msg1.messageBody), retrievedMsg1);
        assertEquals(new String(msg2.messageBody), retrievedMsg2);

        // Disconnect
        pop3Client.logout();
    }
}
