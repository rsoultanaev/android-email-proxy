package com.rsoultanaev.sphinxproxy;

import com.rsoultanaev.javasphinx.DestinationAndMessage;
import com.rsoultanaev.javasphinx.HeaderAndDelta;
import com.rsoultanaev.javasphinx.ParamLengths;
import static com.rsoultanaev.javasphinx.SphinxClient.createForwardMessage;
import static com.rsoultanaev.javasphinx.SphinxClient.packMessage;
import static com.rsoultanaev.javasphinx.Util.concatByteArrays;
import com.rsoultanaev.javasphinx.SphinxPacket;
import com.rsoultanaev.javasphinx.SphinxParams;

import org.bouncycastle.math.ec.ECPoint;
import org.subethamail.smtp.TooMuchDataException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class SphinxUtil {
    public static byte[] genTestMessage(String repeatStr, int msgLen) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < msgLen; i += repeatStr.length()) {
            for (int j = 0; j < repeatStr.length(); j++) {
                if (i + j >= msgLen) {
                    break;
                }

                sb.append(repeatStr.charAt(j));
            }
        }

        return sb.toString().getBytes();
    }

    public static byte[] createBinSphinxPacket(byte[] dest, byte[] message, SphinxParams params, byte[][] nodesRouting, ECPoint[] nodeKeys) throws IOException {
        DestinationAndMessage destinationAndMessage = new DestinationAndMessage(dest, message);
        HeaderAndDelta headerAndDelta = createForwardMessage(params, nodesRouting, nodeKeys, destinationAndMessage);
        ParamLengths paramLengths = new ParamLengths(params.getHeaderLength(), params.getBodyLength());
        SphinxPacket sphinxPacket = new SphinxPacket(paramLengths, headerAndDelta);
        return packMessage(sphinxPacket);
    }

    public static byte[][] splitIntoSphinxPackets(byte[] dest, byte[] message, SphinxParams params, byte[][] nodesRouting, ECPoint[] nodeKeys) throws IOException {
        int payloadSize = 832;

        int total = (int) Math.ceil((double) message.length / payloadSize);
        byte[] uuid = newUUID();

        byte[][] packets = new byte[total][];

        for (int i = 0; i < total; i++) {
            byte[] payload = copyNum(message, payloadSize * i, payloadSize);
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putInt(total).putInt(i);
            byte[] sphinxPayload = concatByteArrays(uuid, byteBuffer.array(), payload);
            packets[i] = createBinSphinxPacket(dest, sphinxPayload, params, nodesRouting, nodeKeys);
        }

        return packets;
    }

    private static byte[] newUUID() {
        UUID uuid = UUID.randomUUID();
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        return ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
    }

    private static byte[] copyNum(byte[] source, int offset, int numBytes) {
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
