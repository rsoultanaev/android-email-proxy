package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.HeaderAndDelta;
import com.robertsoultanaev.javasphinx.ProcessedPacket;
import com.robertsoultanaev.javasphinx.SphinxClient;
import com.robertsoultanaev.javasphinx.SphinxNode;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SphinxUtilTest {

    class PkiEntry {
        BigInteger priv;
        ECPoint pub;

        public PkiEntry(BigInteger priv, ECPoint pub) {
            this.priv = priv;
            this.pub = pub;
        }
    }

    @Test
    public void splitProcessParseAndReassembleTest() throws Exception {
        SphinxParams params = new SphinxParams();

        int basePort = 8000;
        HashMap<Integer, PkiEntry> pki = new HashMap<Integer, PkiEntry>();
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();

        for (int i = 0; i < 3; i++) {
            BigInteger priv = params.getGroup().genSecret();
            ECPoint pub = params.getGroup().expon(params.getGroup().getGenerator(), priv);

            int port = basePort + i;
            String host = "host" + Integer.toString(i);
            pki.put(i,  new PkiEntry(priv, pub));
            nodeList.add(new MixNode(i, host, port, Hex.toHexString(pub.getEncoded(true))));
        }

        int numUseMixes = 3;
        SphinxUtil sphinxUtil = new SphinxUtil(nodeList, numUseMixes);

        StringBuilder sb = new StringBuilder();
        while (sb.length() < 1500) {
            sb.append("0123456789");
        }
        String emailStr = sb.toString();
        byte[] email = emailStr.getBytes();
        String recipient = "mort@rsoultanaev.com";

        SphinxPacketWithRouting[] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(email, recipient);

        byte[][] processedMessages = new byte[sphinxPackets.length][];

        // Process each sphinx packet by a sequence of mixes
        for (int i = 0; i < sphinxPackets.length; i++) {
            byte[] binMessage = sphinxPackets[i].binMessage;
            HeaderAndDelta headerAndDelta = SphinxClient.unpackMessage(binMessage).headerAndDelta;

            BigInteger currentPrivKey = pki.get(sphinxPackets[i].firstNodeId).priv;

            MessageUnpacker unpacker;

            while (true) {
                ProcessedPacket ret = SphinxNode.sphinxProcess(params, currentPrivKey, headerAndDelta);
                headerAndDelta = ret.headerAndDelta;

                byte[] encodedRouting = ret.routing;

                unpacker = MessagePack.newDefaultUnpacker(encodedRouting);
                unpacker.unpackArrayHeader();
                String flag = unpacker.unpackString();

                if (flag.equals(SphinxClient.RELAY_FLAG)) {
                    int nextNodeId = unpacker.unpackInt();
                    currentPrivKey = pki.get(nextNodeId).priv;

                    unpacker.close();
                } else if (flag.equals(SphinxClient.DEST_FLAG)) {
                    unpacker.close();

                    byte[] zeroes = new byte[params.getKeyLength()];
                    java.util.Arrays.fill(zeroes, (byte) 0x00);

                    byte[] processedMessage = SphinxClient.receiveForward(params, ret.macKey, ret.headerAndDelta.delta).message;
                    processedMessages[i] = Base64.encode(processedMessage);

                    break;
                }
            }
        }

        List<Packet> packetList = new ArrayList<Packet>();
        for (byte[] processedMessage : processedMessages) {
            packetList.add(sphinxUtil.parseMessageToPacket(processedMessage));
        }

        AssembledMessage assembledMessage = sphinxUtil.assemblePackets(packetList);
        String assembledMessageStr = new String(assembledMessage.messageBody);

        assertThat(emailStr, is(equalTo(assembledMessageStr)));
    }
}
