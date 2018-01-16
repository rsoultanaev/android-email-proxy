package com.rsoultanaev.sphinxproxy.server;

import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.rsoultanaev.sphinxproxy.SphinxUtil.sendMailWithSphinx;

public class SmtpMessageHandler implements SimpleMessageListener {
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

        byte[] email = out.toByteArray();

        System.out.println("[SMTP] email body start");
        System.out.println(new String(email));
        System.out.println("[SMTP] email body end");
        System.out.println("[SMTP] email length: " + email.length);
        System.out.println("-------------------------");

        sendMailWithSphinx(email);
    }
}
