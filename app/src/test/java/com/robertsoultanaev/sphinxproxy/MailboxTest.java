package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.robertsoultanaev.javasphinx.Util.concatByteArrays;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailboxTest {

    @Mock
    private DBQuery dbQuery;

    @Mock
    private POP3Client pop3Client;

    @Test
    public void updateTest() throws Exception {
        String pop3Server = "pop3.anonymousmail.com";
        int port = 110;
        String username = "fred";
        String password = "1234";

        String packet1Str = "header1\r\n" + "header2\r\n" + "\r\n" + "body1\r\n" + "body1\r\n";
        String packet2Str = "body3\r\n" + "body4\r\n";

        UUID msgId = UUID.randomUUID();

        ByteBuffer packet1HeaderBf = ByteBuffer.allocate(SphinxUtil.PACKET_HEADER_SIZE);
        packet1HeaderBf.putLong(msgId.getMostSignificantBits());
        packet1HeaderBf.putLong(msgId.getLeastSignificantBits());
        packet1HeaderBf.putInt(2);
        packet1HeaderBf.putInt(0);
        byte[] packet1Header = packet1HeaderBf.array();
        ByteBuffer packet2HeaderBf = ByteBuffer.allocate(SphinxUtil.PACKET_HEADER_SIZE);
        packet2HeaderBf.putLong(msgId.getMostSignificantBits());
        packet2HeaderBf.putLong(msgId.getLeastSignificantBits());
        packet2HeaderBf.putInt(2);
        packet2HeaderBf.putInt(1);
        byte[] packet2Header = packet2HeaderBf.array();

        String encodedPayload1 = new String(Base64.encode(concatByteArrays(packet1Header, packet1Str.getBytes())));
        String encodedPayload2 = new String(Base64.encode(concatByteArrays(packet2Header, packet2Str.getBytes())));

        String fromMailbox1 = "mailboxHeader1\r\n" + "mailboxHeader1\r\n" + "\r\n" + encodedPayload1 + "\r\n";
        String fromMailbox2 = "mailboxHeader2\r\n" + "mailboxHeader2\r\n" + "\r\n" + encodedPayload2 + "\r\n";

        BufferedReader bfReader1 = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fromMailbox1.getBytes())));
        BufferedReader bfReader2 = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fromMailbox2.getBytes())));

        POP3MessageInfo status = new POP3MessageInfo();
        status.number = 2;

        POP3MessageInfo[] msgInfos = new POP3MessageInfo[2];
        POP3MessageInfo msg1Info = new POP3MessageInfo();
        POP3MessageInfo msg2Info = new POP3MessageInfo();
        msg1Info.number = 1;
        msg1Info.size = packet1Str.getBytes().length;
        msg2Info.number = 2;
        msg2Info.size = packet2Str.getBytes().length;
        msgInfos[0] = msg1Info;
        msgInfos[1] = msg2Info;

        List<String> readyPacketIds = new ArrayList<String>();
        readyPacketIds.add(msgId.toString());

        List<Packet> orderedPackets = new ArrayList<Packet>();
        Packet p1 = new Packet(msgId.toString(), 0, 2, packet1Str.getBytes());
        Packet p2 = new Packet(msgId.toString(), 1, 2, packet2Str.getBytes());
        orderedPackets.add(p1);
        orderedPackets.add(p2);

        doNothing().when(pop3Client).connect(pop3Server, port);
        when(pop3Client.login(username, password)).thenReturn(true);
        when(pop3Client.status()).thenReturn(status);
        when(pop3Client.listMessages()).thenReturn(msgInfos);
        when(pop3Client.retrieveMessage(1)).thenReturn(bfReader1);
        when(pop3Client.retrieveMessage(2)).thenReturn(bfReader2);
        when(pop3Client.deleteMessage(1)).thenReturn(true);
        when(pop3Client.deleteMessage(2)).thenReturn(true);
        when(pop3Client.logout()).thenReturn(true);
        doNothing().when(pop3Client).disconnect();

        doNothing().when(dbQuery).addPacket(any(Packet.class));
        when(dbQuery.getReadyPacketIds()).thenReturn(readyPacketIds);
        when(dbQuery.getPackets(msgId.toString())).thenReturn(orderedPackets);
        doNothing().when(dbQuery).addAssembledMessage(any(AssembledMessage.class));

        Mailbox mailbox = new Mailbox(pop3Server, port, username, password, dbQuery, pop3Client);
        mailbox.updateMailbox();

        verifyNoMoreInteractions(ignoreStubs(pop3Client));
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }
}

