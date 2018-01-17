package com.rsoultanaev.sphinxproxy;

import com.rsoultanaev.sphinxproxy.database.Packet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

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

    public Packet[] pullMessages(boolean deleteAfterFetching) {
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
                return new Packet[0];
            }

            POP3MessageInfo[] messages = pop3Client.listMessages();

            if (messages == null)
            {
                System.err.println("Could not retrieve message list.");
                pop3Client.logout();
                pop3Client.disconnect();
                return null;
            }

            Packet[] pulledMessages = new Packet[messages.length];

            for (int i = 0; i < messages.length; i++) {
                BufferedReader reader = (BufferedReader) pop3Client.retrieveMessage(messages[i].number);

                try {
                    pulledMessages[i] = parseMessageToPacket(reader);
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

    private byte[] getMessageBody(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while(line != null) {
            if (line.isEmpty()) {
                break;
            }
            line = reader.readLine();
        }

        return IOUtils.toString(reader).getBytes();
    }

    private Packet parseMessageToPacket(BufferedReader reader) throws IOException {
        byte[] message = getMessageBody(reader);

        System.out.println(message.length);

        ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 16));
        long uuidHigh = byteBuffer.getLong();
        long uuidLow = byteBuffer.getLong();
        String uuid = new UUID(uuidHigh, uuidLow).toString();

        byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(message, 16, 24));
        int packetsInMessage = byteBuffer.getInt();
        int sequenceNumber = byteBuffer.getInt();

        byte[] payload = Arrays.copyOfRange(message, 24, message.length);

        return new Packet(uuid, packetsInMessage, sequenceNumber, payload);
    }
}
