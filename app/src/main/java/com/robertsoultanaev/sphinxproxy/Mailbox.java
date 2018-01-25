package com.robertsoultanaev.sphinxproxy;

import android.content.Context;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.bouncycastle.util.encoders.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Mailbox {
    private String mailServer;
    private int port;
    private String username;
    private String password;
    private POP3Client pop3Client;
    private Context context;


    public Mailbox(String mailServer, int port, String username, String password, Context context) {
        this.mailServer = mailServer;
        this.port = port;
        this.username = username;
        this.password = password;
        this.context = context;

        pop3Client = new POP3Client();
        pop3Client.setDefaultTimeout(60000);
    }

    public void updateMailbox() {
        DB db = DB.getAppDatabase(context);
        DBQuery dao = db.getDao();

        Packet[] newMessages = pullMessages(true);

        if (newMessages == null) {
            System.out.println("Message pull failed");
        } else {
            System.out.println("Messages for " + username + ": " + newMessages.length);

            for (Packet packet : newMessages) {
                dao.addPacket(packet);
            }
        }

        List<String> readyPacketIds = dao.getReadyPacketIds();

        for (String uuid : readyPacketIds) {
            List<Packet> orderedPackets = dao.getPackets(uuid);
            AssembledMessage msg = SphinxUtil.assemblePackets(orderedPackets);
            dao.addAssembledMessage(msg);
        }
    }

    private Packet[] pullMessages(boolean deleteAfterFetching) {
        try
        {
            pop3Client.connect(mailServer, port);
        }
        catch (IOException e)
        {
            System.err.println("Could not connect to mailServer.");
            e.printStackTrace();
            return null;
        }

        try
        {
            if (!pop3Client.login(username, password)) {
                System.err.println("Could not login to mailServer.");
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
        byte[] encodedMessage = getMessageBody(reader);
        byte[] message = Base64.decode(encodedMessage);

        byte[] headerBytes = Arrays.copyOfRange(message, 0, SphinxUtil.PACKET_HEADER_SIZE);
        ByteBuffer byteBuffer = ByteBuffer.wrap(headerBytes);
        long uuidHigh = byteBuffer.getLong();
        long uuidLow = byteBuffer.getLong();

        int packetsInMessage = byteBuffer.getInt();
        int sequenceNumber = byteBuffer.getInt();
        String uuid = new UUID(uuidHigh, uuidLow).toString();
        byte[] payload = Arrays.copyOfRange(message, 24, message.length);

        return new Packet(uuid, packetsInMessage, sequenceNumber, payload);
    }
}
