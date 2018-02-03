package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

public class SmtpMessageHandler implements SimpleMessageListener {
    private DBQuery dbQuery;
    private SphinxUtil sphinxUtil;
    private AsyncTcpClient asyncTcpClient;

    public SmtpMessageHandler(SphinxUtil sphinxUtil, AsyncTcpClient asyncTcpClient, DBQuery dbQuery) {
        this.sphinxUtil = sphinxUtil;
        this.asyncTcpClient = asyncTcpClient;
        this.dbQuery = dbQuery;
    }

    public boolean accept(String from, String recipient) {
        return (dbQuery.getRecipient(recipient) != null);
    }

    public void deliver(String from, String recipient, InputStream data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        data = new BufferedInputStream(data);

        int current;
        try {
            while ((current = data.read()) >= 0) {
                out.write(current);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read the email", ex);
        }

        byte[] email = out.toByteArray();

        System.out.println("[SMTP] email body start");
        System.out.println(new String(email));
        System.out.println("[SMTP] email body end");
        System.out.println("[SMTP] email length: " + email.length);
        System.out.println("-------------------------");

        String encodedRecipientPublicKey = dbQuery.getRecipient(recipient).encodedPublicKey;
        PublicKey recipientPublicKey = EndToEndCrypto.decodePublicKey(encodedRecipientPublicKey);
        byte[] endToEndEncryptedEmail = EndToEndCrypto.endToEndEncrypt(recipientPublicKey, email);

        byte[][] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(endToEndEncryptedEmail, recipient);

        for (byte[] binMessage : sphinxPackets) {
            asyncTcpClient.sendMessage(binMessage);
        }
    }
}
