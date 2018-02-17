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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SphinxUtil {

    public static int PACKET_HEADER_SIZE = 24;

    private class RoutingInformation {
        byte[][] nodesRouting;
        ECPoint[] nodeKeys;
        int firstNodeId;

        public RoutingInformation(byte[][] nodesRouting, ECPoint[] nodeKeys, int firstNodeId) {
            this.nodesRouting = nodesRouting;
            this.nodeKeys = nodeKeys;
            this.firstNodeId = firstNodeId;
        }
    }

    private final SphinxParams params;
    private final HashMap<Integer, InetSocketAddress> mixNodeAddresses;
    private final HashMap<Integer, ECPoint> mixNodePublicKeys;

    public SphinxUtil(DBQuery dbQuery) {
        params = new SphinxParams();
        mixNodePublicKeys = new HashMap<Integer, ECPoint>();
        mixNodeAddresses = new HashMap<Integer, InetSocketAddress>();

        for (MixNode mixNode : dbQuery.getMixNodes()) {
            ECPoint publicKey = decodeECPoint(Hex.decode(mixNode.encodedPublicKey));
            mixNodePublicKeys.put(mixNode.id, publicKey);
            mixNodeAddresses.put(mixNode.id, new InetSocketAddress(mixNode.host, mixNode.port));
        }
    }

    public SphinxPacketWithRouting[] splitIntoSphinxPackets(byte[] email, String recipient) {
        UUID messageId = UUID.randomUUID();
        byte[] dest = recipient.getBytes();

        int packetPayloadSize = getMaxPayloadSize(params) - dest.length - PACKET_HEADER_SIZE;
        int packetsInMessage = (int) Math.ceil((double) email.length / packetPayloadSize);
        SphinxPacketWithRouting[] sphinxPackets = new SphinxPacketWithRouting[packetsInMessage];

        for (int i = 0; i < packetsInMessage; i++) {

            ByteBuffer packetHeader = ByteBuffer.allocate(SphinxUtil.PACKET_HEADER_SIZE);
            packetHeader.putLong(messageId.getMostSignificantBits());
            packetHeader.putLong(messageId.getLeastSignificantBits());
            packetHeader.putInt(packetsInMessage);
            packetHeader.putInt(i);

            byte[] packetPayload = copyUpToNum(email, packetPayloadSize * i, packetPayloadSize);
            byte[] sphinxPayload = concatByteArrays(packetHeader.array(), packetPayload);

            RoutingInformation routingInformation = generateRoutingInformation(3);

            sphinxPackets[i] = createBinSphinxPacket(dest, sphinxPayload, routingInformation);
        }

        return sphinxPackets;
    }

    private SphinxPacketWithRouting createBinSphinxPacket(byte[] dest, byte[] message, RoutingInformation routingInformation) {
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

        int firstNodeId = routingInformation.firstNodeId;
        InetSocketAddress firstNodeAddress = mixNodeAddresses.get(firstNodeId);

        return new SphinxPacketWithRouting(firstNodeId, firstNodeAddress, binSphinxPacket);
    }

    private RoutingInformation generateRoutingInformation(int numRouteNodes) {
        byte[][] nodesRouting;
        ECPoint[] nodeKeys;

        ArrayList<Integer> orderedNodeIds = new ArrayList<Integer>(mixNodePublicKeys.keySet());
        int[] nodePool = new int[orderedNodeIds.size()];
        for (int i = 0; i < nodePool.length; i++) {
            nodePool[i] = orderedNodeIds.get(i);
        }
        int[] useNodes = SphinxClient.randSubset(nodePool, numRouteNodes);

        nodesRouting = new byte[useNodes.length][];
        for (int i = 0; i < useNodes.length; i++) {
            try {
                nodesRouting[i] = SphinxClient.encodeNode(useNodes[i]);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to encode node", ex);
            }
        }

        nodeKeys = new ECPoint[useNodes.length];
        for (int i = 0; i < useNodes.length; i++) {
            nodeKeys[i] = mixNodePublicKeys.get(useNodes[i]);
        }

        return new RoutingInformation(nodesRouting, nodeKeys, useNodes[0]);
    }

    // Assume all packets present and in sorted order
    public AssembledMessage assemblePackets(List<Packet> packets) {
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

    public Packet parseMessageToPacket(byte[] encodedMessage) throws IOException {
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
