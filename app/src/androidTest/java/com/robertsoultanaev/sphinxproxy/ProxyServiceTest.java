package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.javasphinx.SphinxClient;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.javasphinx.Util;
import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.subethamail.smtp.client.SmartClient;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ProxyServiceTest {

    @Test
    public void testSMPTProxy() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        SphinxParams params = new SphinxParams();
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

        DB db = DB.getAppDatabase(context);
        DBQuery dbQuery = db.getDao();
        dbQuery.deleteEverything();

        String senderAddress = "john@example.com";
        String recipientAddress = "mort@rsoultanaev.com";
        String messageStartToken = "jf764594bnf83";
        String messageBodyStr = messageStartToken + "Test message";
        byte[] messageBody = messageBodyStr.getBytes();
        String mixHostname = "localhost";
        int mixPort = 30000;
        String encodedRecipientPrivateKey = "MIGBAgEAMBAGByqGSM49AgEGBSuBBAAhBGowaAIBAQQcLbF1DZ8Tz9Yttnovor3I7FHhdNI/hnDfLEUiqaAHBgUrgQQAIaE8AzoABCNvOS14yIEldac3N0kxLbLEl6N4ckASZB0JfDu0wr3yH8pBFCmU9u3V5IYtFgB1PU/4ai+JMc5D";
        String encodedRecipientPublicKey = "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEI285LXjIgSV1pzc3STEtssSXo3hyQBJkHQl8O7TCvfIfykEUKZT27dXkhi0WAHU9T/hqL4kxzkM=";
        PrivateKey recipientPrivateKey = endToEndCrypto.decodePrivateKey(encodedRecipientPrivateKey);

        Config.setIntValue(R.string.key_proxy_pop3_port, 27000, context);
        Config.setIntValue(R.string.key_proxy_smtp_port, 28000, context);
        Config.setStringValue(R.string.key_proxy_username, "proxyuser", context);
        Config.setStringValue(R.string.key_proxy_password, "12345", context);
        Config.setStringValue(R.string.key_mailbox_hostname, "ec2-35-178-56-77.eu-west-2.compute.amazonaws.com", context);
        Config.setIntValue(R.string.key_mailbox_port, 995, context);
        Config.setStringValue(R.string.key_mailbox_username, "mort", context);
        Config.setStringValue(R.string.key_mailbox_password, "1234", context);
        Config.setIntValue(R.string.key_num_use_mixes, 3, context);
        Config.setKeyPair(encodedRecipientPrivateKey, encodedRecipientPublicKey, context);

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

        db.close();

        Intent proxyIntent = new Intent(context, ProxyService.class);
        context.startService(proxyIntent);

        MockMixnetwork mockMixnetwork = new MockMixnetwork(pki, mixPort);

        Thread.sleep(2000);

        SmartClient client = new SmartClient("localhost", 28000,"localhost");
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
}
