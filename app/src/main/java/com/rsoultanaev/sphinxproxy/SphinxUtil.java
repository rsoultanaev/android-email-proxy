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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SphinxUtil {

    private class RoutingInformation {
        byte[][] nodesRouting;
        ECPoint[] nodeKeys;

        public RoutingInformation(byte[][] nodesRouting, ECPoint[] nodeKeys) {
            this.nodesRouting = nodesRouting;
            this.nodeKeys = nodeKeys;
        }
    }

    private final SphinxParams params;
    private final HashMap<Integer, ECPoint> publicKeys;

    public SphinxUtil() {
        params = new SphinxParams();
        publicKeys = new HashMap<Integer, ECPoint>();

        // TODO: Implement this
        publicKeys.put(8000, decodeECPoint(Hex.decode("036457e713498b559afe446158aaa08613530022b25e418c59b8b2a624")));
        publicKeys.put(8001, decodeECPoint(Hex.decode("039d95b858383fdeee0d493a1675d513c29671de322c367d23a08cd5bf")));
        publicKeys.put(8002, decodeECPoint(Hex.decode("02739a6205b940db5dd4c62c17fe568dc1b061a150322df9a45543898f")));
    }

    public void sendMailWithSphinx(byte[] email) {
        byte[] dest = "mort@rsoultanaev.com".getBytes();
        byte[][] splitMessage = splitIntoSphinxPackets(dest, email);

        AsyncTcpClient asyncTcpClient = new AsyncTcpClient("localhost", 10000);

        for (byte[] binMessage : splitMessage) {
            asyncTcpClient.sendMessage(binMessage);
        }
    }

    private byte[] createBinSphinxPacket(byte[] dest, byte[] message, RoutingInformation routingInformation) {
        DestinationAndMessage destinationAndMessage = new DestinationAndMessage(dest, message);

        HeaderAndDelta headerAndDelta;
        try {
            headerAndDelta = createForwardMessage(params, routingInformation.nodesRouting, routingInformation.nodeKeys, destinationAndMessage);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create forward message", ex);
        }

        ParamLengths paramLengths = new ParamLengths(params.getHeaderLength(), params.getBodyLength());
        SphinxPacket sphinxPacket = new SphinxPacket(paramLengths, headerAndDelta);

        byte[] binSphinxPacket;
        try {
            binSphinxPacket = packMessage(sphinxPacket);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to pack forward message", ex);
        }

        return binSphinxPacket;
    }

    private byte[][] splitIntoSphinxPackets(byte[] dest, byte[] message) {
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

            RoutingInformation routingInformation = generateRoutingInformation();

            sphinxPackets[i] = createBinSphinxPacket(dest, encodedSphinxPayload, routingInformation);
        }

        return sphinxPackets;
    }

    // TODO: Implement this
    private RoutingInformation generateRoutingInformation() {
        byte[][] nodesRouting;
        ECPoint[] nodeKeys;

        int[] useNodes = {8000, 8001, 8002};

        nodesRouting = new byte[useNodes.length][];
        for (int i = 0; i < useNodes.length; i++) {
            try {
                nodesRouting[i] = SphinxClient.encodeNode(useNodes[i]);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to encode node", ex);
            }
        }

        nodeKeys = new ECPoint[useNodes.length];
        nodeKeys[0] = publicKeys.get(useNodes[0]);
        nodeKeys[1] = publicKeys.get(useNodes[1]);
        nodeKeys[2] = publicKeys.get(useNodes[2]);

        return new RoutingInformation(nodesRouting, nodeKeys);
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
    private byte[] copyUpToNum(byte[] source, int offset, int numBytes) {
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
