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
    private EndToEndCrypto endToEndCrypto;

    public SmtpMessageHandler(SphinxUtil sphinxUtil, AsyncTcpClient asyncTcpClient, DBQuery dbQuery, EndToEndCrypto endToEndCrypto) {
        this.sphinxUtil = sphinxUtil;
        this.asyncTcpClient = asyncTcpClient;
        this.dbQuery = dbQuery;
        this.endToEndCrypto = endToEndCrypto;
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

        String encodedRecipientPublicKey = dbQuery.getRecipient(recipient).encodedPublicKey;
        PublicKey recipientPublicKey = endToEndCrypto.decodePublicKey(encodedRecipientPublicKey);
        byte[] endToEndEncryptedEmail = endToEndCrypto.endToEndEncrypt(recipientPublicKey, email);

        SphinxPacketWithRouting[] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(endToEndEncryptedEmail, recipient);

        for (SphinxPacketWithRouting sphinxPacketWithRouting : sphinxPackets) {
            asyncTcpClient.sendMessage(sphinxPacketWithRouting);
        }

        System.out.println("[SMTP] sent sphinx message to: " + recipient);
    }
}
