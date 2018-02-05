package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3SClient;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.robertsoultanaev.javasphinx.Util.concatByteArrays;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailboxTest {

    @Mock
    private SphinxUtil sphinxUtil;

    @Mock
    private EndToEndCrypto endToEndCrypto;

    @Mock
    private DBQuery dbQuery;

    @Mock
    private POP3SClient pop3Client;

    @Test
    public void updateTest() throws Exception {
        String pop3Server = "pop3.anonymousmail.com";
        int port = 110;
        String username = "fred";
        String password = "1234";

        UUID msgId = UUID.randomUUID();
        String packet1Str = "header1\r\n" + "header2\r\n" + "\r\n" + "body1\r\n" + "body1\r\n";
        String packet2Str = "body3\r\n" + "body4\r\n";

        AssembledMessage assembledMessage = new AssembledMessage(msgId.toString(), (packet1Str + packet2Str).getBytes());

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
        orderedPackets.add(new Packet(msgId.toString(), 0, 2, packet1Str.getBytes()));
        orderedPackets.add(new Packet(msgId.toString(), 1, 2, packet2Str.getBytes()));

        PrivateKey privateKey = (new EndToEndCrypto()).generateKeyPair().getPrivate();

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

        when(sphinxUtil.assemblePackets(orderedPackets)).thenReturn(assembledMessage);
        when(sphinxUtil.parseMessageToPacket(encodedPayload1.getBytes())).thenReturn(orderedPackets.get(0));
        when(sphinxUtil.parseMessageToPacket(encodedPayload2.getBytes())).thenReturn(orderedPackets.get(1));

        when(endToEndCrypto.endToEndDecrypt(eq(privateKey), any(byte[].class))).thenReturn(assembledMessage.messageBody);

        Mailbox mailbox = new Mailbox(pop3Server, port, username, password, dbQuery, pop3Client, endToEndCrypto, privateKey, sphinxUtil);
        mailbox.updateMailbox();

        verifyNoMoreInteractions(ignoreStubs(pop3Client));
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }
}

