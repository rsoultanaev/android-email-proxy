package com.rsoultanaev.sphinxproxy;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.jsoup.Jsoup;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Pop3Puller {
    private String server;
    private int port;
    private String username;
    private String password;
    private POP3Client pop3Client;


    public Pop3Puller(String server, int port, String username, String password) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;

        pop3Client = new POP3Client();
        pop3Client.setDefaultTimeout(60000);
    }

    public byte[][] pullMessages(boolean deleteAfterFetching) {
        try
        {
            pop3Client.connect(server, port);
        }
        catch (IOException e)
        {
            System.err.println("Could not connect to server.");
            e.printStackTrace();
            return null;
        }

        try
        {
            if (!pop3Client.login(username, password)) {
                System.err.println("Could not login to server.");
                pop3Client.disconnect();
                return null;
            }

            POP3MessageInfo status = pop3Client.status();
            if (status == null) {
                System.err.println("Could not retrieve status.");
                pop3Client.logout();
                pop3Client.disconnect();
                return null;
            }

            // No new messages - return empty array
            if (status.number == 0) {
                pop3Client.logout();
                pop3Client.disconnect();
                return new byte[0][];
            }

            POP3MessageInfo[] messages = pop3Client.listMessages();

            if (messages == null)
            {
                System.err.println("Could not retrieve message list.");
                pop3Client.logout();
                pop3Client.disconnect();
                return null;
            }

            Properties props = System.getProperties();
            Session session = Session.getDefaultInstance(props);
            byte[][] pulledMessages = new byte[messages.length][];

            for (int i = 0; i < messages.length; i++) {
                BufferedReader reader = (BufferedReader) pop3Client.retrieveMessage(messages[i].number);
                InputStream inputStream = IOUtils.toInputStream(IOUtils.toString(reader), Charsets.UTF_8);

                try {
                    MimeMessage message = new MimeMessage(session, inputStream);
                    pulledMessages[i] = getTextFromMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            if (deleteAfterFetching) {
                for (int i = 0; i < messages.length; i++) {
                    pop3Client.deleteMessage(messages[i].number);
                }
            }

            pop3Client.logout();
            pop3Client.disconnect();

            return pulledMessages;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result.getBytes();
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break;
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }
}
