package com.robertsoultanaev.sphinxproxy;

import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SmtpMessageHandler implements SimpleMessageListener {
    private SphinxUtil sphinxUtil;
    private AsyncTcpClient asyncTcpClient;

    public SmtpMessageHandler(SphinxUtil sphinxUtil, AsyncTcpClient asyncTcpClient) {
        this.sphinxUtil = sphinxUtil;
        this.asyncTcpClient = asyncTcpClient;
    }

    public boolean accept(String from, String recipient) {
        // TODO: Lookup if recipient is in the list of people for whom we have keys
        return true;
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

        // TODO: Look up public key of recipient and do endToEndEncrypt on the email before sphinxing it
        byte[][] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(email, recipient);

        for (byte[] binMessage : sphinxPackets) {
            asyncTcpClient.sendMessage(binMessage);
        }
    }
}
