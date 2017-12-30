package com.rsoultanaev.sphinxproxy.server;

import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MessageListener implements SimpleMessageListener {
    public boolean accept(String from, String recipient) {
        return true;
    }

    public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        data = new BufferedInputStream(data);

        int current;
        while ((current = data.read()) >= 0)
        {
            out.write(current);
        }

        byte[] bytes = out.toByteArray();
        String emailBody = new String(bytes);

        System.out.println("[MessageListener] Received email\n"
                         + "[MessageListener] from: " + from + "\n"
                         + "[MessageListener] recipient: " + recipient + "\n"
                         + "[MessageListener] email body start ----------------\n"
                         + emailBody
                         + "[MessageListener] email body end ------------------\n");
    }
}
