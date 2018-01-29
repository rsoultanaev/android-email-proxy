package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SmtpMessageHandler implements SimpleMessageListener {
    private DBQuery dbQuery;

    public SmtpMessageHandler(DBQuery dbQuery) {
        this.dbQuery = dbQuery;
    }

    public boolean accept(String from, String recipient) {
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

        SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);
        byte[][] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(email, recipient);

        AsyncTcpClient asyncTcpClient = new AsyncTcpClient("localhost", 10000);

        for (byte[] binMessage : sphinxPackets) {
            asyncTcpClient.sendMessage(binMessage);
        }
    }
}
