package com.rsoultanaev.sphinxproxy.server;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.rsoultanaev.javasphinx.DestinationAndMessage;
import com.rsoultanaev.javasphinx.HeaderAndDelta;
import com.rsoultanaev.javasphinx.ParamLengths;
import com.rsoultanaev.javasphinx.SphinxClient;
import com.rsoultanaev.javasphinx.SphinxPacket;
import com.rsoultanaev.javasphinx.SphinxParams;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;

public class SmtpMessageHandler implements SimpleMessageListener {
    public boolean accept(String from, String recipient) {
        return true;
    }

    public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        data = new BufferedInputStream(data);

        int current;
        while ((current = data.read()) >= 0)
        {
            out.write(current);
        }

        byte[] bytes = out.toByteArray();
        String emailBody = new String(bytes);

        System.out.println("[SMTP] email body start");
        System.out.println(emailBody);
        System.out.println("[SMTP] email body end");
        System.out.println("[SMTP] email length: " + bytes.length);


        // ----------------------------

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
        nodeKeys[0] = com.rsoultanaev.javasphinx.Util.decodeECPoint(Hex.decode("036457e713498b559afe446158aaa08613530022b25e418c59b8b2a624"));
        nodeKeys[1] = com.rsoultanaev.javasphinx.Util.decodeECPoint(Hex.decode("039d95b858383fdeee0d493a1675d513c29671de322c367d23a08cd5bf"));
        nodeKeys[2] = com.rsoultanaev.javasphinx.Util.decodeECPoint(Hex.decode("02739a6205b940db5dd4c62c17fe568dc1b061a150322df9a45543898f"));
//        for (int i = 0; i < useNodes.length; i++) {
//            nodeKeys[i] = pkiPub.get(useNodes[i]).y;
//        }

        System.out.println("-------------------------");

        System.out.println("useNodes:");
        for (int n : useNodes) {
            System.out.println(n);
        }

        System.out.println("nodesRouting:");
        for (byte[] n : nodesRouting) {
            System.out.println(Hex.toHexString(n));
        }

        System.out.println("nodeKeys:");
        for (ECPoint n : nodeKeys) {
            System.out.println(Hex.toHexString(n.getEncoded(true)));
        }

        byte[] dest = "rsoultanaev@rsoultanaev.com".getBytes();
        byte[] message = "Message from sphinxproxy 2".getBytes();

        DestinationAndMessage destinationAndMessage = new DestinationAndMessage(dest, message);
        HeaderAndDelta headerAndDelta = SphinxClient.createForwardMessage(params, nodesRouting, nodeKeys, destinationAndMessage);
        ParamLengths paramLengths = new ParamLengths(params.getHeaderLength(), params.getBodyLength());
        SphinxPacket sphinxPacket = new SphinxPacket(paramLengths, headerAndDelta);
        byte[] binMessage = SphinxClient.packMessage(sphinxPacket);

        System.out.println("Sphinx packet:");
        System.out.println(Hex.toHexString(binMessage));


        // --------------------------


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


        new Client("localhost", 10000, binMessage);


    }
}
