package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.DestinationAndMessage;
import com.robertsoultanaev.javasphinx.HeaderAndDelta;
import com.robertsoultanaev.javasphinx.ParamLengths;
import static com.robertsoultanaev.javasphinx.SphinxClient.createForwardMessage;
import static com.robertsoultanaev.javasphinx.SphinxClient.packMessage;
import static com.robertsoultanaev.javasphinx.SphinxClient.getMaxPayloadSize;
import static com.robertsoultanaev.javasphinx.Util.concatByteArrays;
import static com.robertsoultanaev.javasphinx.Util.decodeECPoint;

import com.robertsoultanaev.javasphinx.SphinxClient;
import com.robertsoultanaev.javasphinx.SphinxPacket;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Packet;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SphinxUtil {

    public static int PACKET_HEADER_SIZE = 24;

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

    public SphinxUtil(DBQuery dbQuery) {
        params = new SphinxParams();
        publicKeys = new HashMap<Integer, ECPoint>();

        for (MixNode mixNode : dbQuery.getMixNodes()) {
            ECPoint publicKey = decodeECPoint(Hex.decode(mixNode.encodedPublicKey));
            publicKeys.put(mixNode.port, publicKey);
        }
    }

    public byte[][] splitIntoSphinxPackets(byte[] email, String recipient) {
        UUID messageId = UUID.randomUUID();
        byte[] dest = recipient.getBytes();

        // Compute the size of the packet payload such that if we append
        // the header to it and then base64 encode it, we arrive close to
        // but not above the sphinx max payload limit
        int targetEncodedSize = getMaxPayloadSize(params) - dest.length;
        int packetPayloadSize = (int) (((double) (3 * targetEncodedSize) / 4) - PACKET_HEADER_SIZE - 3);
        int packetsInMessage = (int) Math.ceil((double) email.length / packetPayloadSize);
        byte[][] sphinxPackets = new byte[packetsInMessage][];

        for (int i = 0; i < packetsInMessage; i++) {

            ByteBuffer packetHeader = ByteBuffer.allocate(SphinxUtil.PACKET_HEADER_SIZE);
            packetHeader.putLong(messageId.getMostSignificantBits());
            packetHeader.putLong(messageId.getLeastSignificantBits());
            packetHeader.putInt(packetsInMessage);
            packetHeader.putInt(i);

            byte[] packetPayload = copyUpToNum(email, packetPayloadSize * i, packetPayloadSize);
            byte[] encodedSphinxPayload = Base64.encode(concatByteArrays(packetHeader.array(), packetPayload));

            RoutingInformation routingInformation = generateRoutingInformation();

            sphinxPackets[i] = createBinSphinxPacket(dest, encodedSphinxPayload, routingInformation);
        }

        return sphinxPackets;
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

    // TODO: endToEndDecrypt the final message using our private key
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

    public static Packet parseMessageToPacket(byte[] encodedMessage) throws IOException {
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
