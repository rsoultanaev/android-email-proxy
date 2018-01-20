package com.rsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.DestinationAndMessage;
import com.robertsoultanaev.javasphinx.HeaderAndDelta;
import com.robertsoultanaev.javasphinx.ParamLengths;
import static com.robertsoultanaev.javasphinx.SphinxClient.createForwardMessage;
import static com.robertsoultanaev.javasphinx.SphinxClient.packMessage;
import static com.robertsoultanaev.javasphinx.Util.concatByteArrays;
import static com.robertsoultanaev.javasphinx.Util.decodeECPoint;

import com.robertsoultanaev.javasphinx.SphinxClient;
import com.robertsoultanaev.javasphinx.SphinxPacket;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.rsoultanaev.sphinxproxy.database.AssembledMessage;
import com.rsoultanaev.sphinxproxy.database.Packet;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SphinxUtil {
    public static void sendMailWithSphinx(byte[] email) throws IOException {
        class PkiEntry {
            BigInteger x;
            ECPoint y;

            public PkiEntry(BigInteger x, ECPoint y) {
                this.x = x;
                this.y = y;
            }
        }

        SphinxParams params;
        HashMap<Integer, PkiEntry> pkiPriv;
        byte[][] nodesRouting;
        ECPoint[] nodeKeys;
        int[] useNodes;

        params = new SphinxParams();

        int r = 5;

        pkiPriv = new HashMap<Integer, PkiEntry>();
        HashMap<Integer, PkiEntry> pkiPub = new HashMap<Integer, PkiEntry>();

        for (int i = 0; i < 10; i++) {
            BigInteger x = params.getGroup().genSecret();
            ECPoint y = params.getGroup().expon(params.getGroup().getGenerator(), x);

            PkiEntry privEntry = new PkiEntry(x, y);
            PkiEntry pubEntry = new PkiEntry(null, y);

            int port = 8000 + i;

            pkiPriv.put(port, privEntry);
            pkiPub.put(port, pubEntry);
        }

        Object[] pubKeys = pkiPub.keySet().toArray();
        int[] nodePool = new int[pubKeys.length];
        for (int i = 0; i < nodePool.length; i++) {
            nodePool[i] = (Integer) pubKeys[i];
        }
        useNodes = SphinxClient.randSubset(nodePool, 3);
        int[] mynodes = {8000, 8001, 8002};
        useNodes = mynodes;

        nodesRouting = new byte[useNodes.length][];
        for (int i = 0; i < useNodes.length; i++) {
            nodesRouting[i] = SphinxClient.encodeNode(useNodes[i]);
        }

        nodeKeys = new ECPoint[useNodes.length];
        nodeKeys[0] = decodeECPoint(Hex.decode("036457e713498b559afe446158aaa08613530022b25e418c59b8b2a624"));
        nodeKeys[1] = decodeECPoint(Hex.decode("039d95b858383fdeee0d493a1675d513c29671de322c367d23a08cd5bf"));
        nodeKeys[2] = decodeECPoint(Hex.decode("02739a6205b940db5dd4c62c17fe568dc1b061a150322df9a45543898f"));

        byte[] dest = "mort@rsoultanaev.com".getBytes();
        byte[][] splitMessage = splitIntoSphinxPackets(dest, email, params, nodesRouting, nodeKeys);

        AsyncTcpClient asyncTcpClient = new AsyncTcpClient("localhost", 10000);

        for (byte[] binMessage : splitMessage) {
            asyncTcpClient.sendMessage(binMessage);
        }
    }

    public static byte[] createBinSphinxPacket(byte[] dest, byte[] message, SphinxParams params, byte[][] nodesRouting, ECPoint[] nodeKeys) throws IOException {
        DestinationAndMessage destinationAndMessage = new DestinationAndMessage(dest, message);
        HeaderAndDelta headerAndDelta = createForwardMessage(params, nodesRouting, nodeKeys, destinationAndMessage);
        ParamLengths paramLengths = new ParamLengths(params.getHeaderLength(), params.getBodyLength());
        SphinxPacket sphinxPacket = new SphinxPacket(paramLengths, headerAndDelta);
        return packMessage(sphinxPacket);
    }

    public static byte[][] splitIntoSphinxPackets(byte[] dest, byte[] message, SphinxParams params, byte[][] nodesRouting, ECPoint[] nodeKeys) throws IOException {
        UUID uuid = UUID.randomUUID();
        int total = (int) Math.ceil((double) message.length / Constants.PACKET_PAYLOAD_SIZE);

        byte[][] sphinxPackets = new byte[total][];
        for (int i = 0; i < total; i++) {

            ByteBuffer packetHeader = ByteBuffer.allocate(Constants.PACKET_HEADER_SIZE);
            packetHeader.putLong(uuid.getMostSignificantBits());
            packetHeader.putLong(uuid.getLeastSignificantBits());
            packetHeader.putInt(total);
            packetHeader.putInt(i);

            byte[] packetPayload = copyUpToNum(message, Constants.PACKET_PAYLOAD_SIZE * i, Constants.PACKET_PAYLOAD_SIZE);
            byte[] encodedSphinxPayload = Base64.encode(concatByteArrays(packetHeader.array(), packetPayload));

            sphinxPackets[i] = createBinSphinxPacket(dest, encodedSphinxPayload, params, nodesRouting, nodeKeys);
        }

        return sphinxPackets;
    }

    // Assume all packets present and in sorted order
    public static AssembledMessage assemblePackets(List<Packet> packets) {
        String uuid = packets.get(0).uuid;
        byte[][] payloads = new byte[packets.size()][];
        for (int i = 0; i < packets.size(); i++) {
            payloads[i] = packets.get(i).payload;
        }
        byte[] message = concatByteArrays(payloads);

        return new AssembledMessage(uuid, message);
    }

    // Copies numBytes from offset if possible, otherwise copies from offset to the end of source array
    private static byte[] copyUpToNum(byte[] source, int offset, int numBytes) {
        if (offset + numBytes > source.length) {
            numBytes = source.length - offset;
        }

        byte[] result = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            result[i] = source[offset + i];
        }

        return result;
    }
}
