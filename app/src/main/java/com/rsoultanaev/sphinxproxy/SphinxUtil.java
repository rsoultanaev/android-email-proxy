package com.rsoultanaev.sphinxproxy;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
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

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
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

        class Client {

            private String host;
            private int port;
            private byte[] message;

            public Client(String host, int port, byte[] message) {
                this.host = host;
                this.port = port;
                this.message = message;
                setup();
            }

            private void setup() {
                AsyncServer.getDefault().connectSocket(new InetSocketAddress(host, port), new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, final AsyncSocket socket) {
                        handleConnectCompleted(ex, socket);
                    }
                });
            }

            private void handleConnectCompleted(Exception ex, final AsyncSocket socket) {
                if(ex != null) throw new RuntimeException(ex);

                Util.writeAll(socket, message, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) throw new RuntimeException(ex);
                        System.out.println("[Client] Successfully wrote message");
                        System.out.println("[Client] Message length: " + message.length);
                        System.out.println(new String(message));
                    }
                });

                socket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if(ex != null) throw new RuntimeException(ex);
                        System.out.println("[Client] Successfully closed connection");
                    }
                });

                socket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if(ex != null) throw new RuntimeException(ex);
                        System.out.println("[Client] Successfully end connection");
                    }
                });

                socket.close();
            }
        }

        for (byte[] binMessage : splitMessage) {
            new Client("localhost", 10000, binMessage);
        }
    }

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
        int payloadSize = 1000;

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
