package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3SClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.List;

public class Mailbox {
    private String mailServer;
    private int port;
    private String username;
    private String password;
    private POP3SClient pop3Client;
    private DBQuery dbQuery;
    private EndToEndCrypto endToEndCrypto;
    private PrivateKey privateKey;
    private SphinxUtil sphinxUtil;

    public Mailbox(String mailServer, int port, String username, String password, DBQuery dbQuery, POP3SClient pop3Client, EndToEndCrypto endToEndCrypto, PrivateKey privateKey, SphinxUtil sphinxUtil) {
        this.mailServer = mailServer;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dbQuery = dbQuery;
        this.pop3Client = pop3Client;
        this.endToEndCrypto = endToEndCrypto;
        this.privateKey = privateKey;
        this.sphinxUtil = sphinxUtil;
    }

    public void updateMailbox() {
        Packet[] newMessages = pullMessages(true);

        if (newMessages == null) {
            System.out.println("Message pull failed");
        } else {
            System.out.println("Messages for " + username + ": " + newMessages.length);

            for (Packet packet : newMessages) {
                dbQuery.addPacket(packet);
            }
        }

        List<String> readyPacketIds = dbQuery.getReadyPacketIds();

        for (String uuid : readyPacketIds) {
            List<Packet> orderedPackets = dbQuery.getPackets(uuid);
            AssembledMessage msg = sphinxUtil.assemblePackets(orderedPackets);
            msg.messageBody = endToEndCrypto.endToEndDecrypt(privateKey, msg.messageBody);
            dbQuery.addAssembledMessage(msg);
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
                    byte[] encodedMessage = getMessageBody(reader);
                    pulledMessages[i] = sphinxUtil.parseMessageToPacket(encodedMessage);
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
}
