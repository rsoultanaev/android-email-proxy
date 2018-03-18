package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.HeaderAndDelta;
import com.robertsoultanaev.javasphinx.ProcessedPacket;
import com.robertsoultanaev.javasphinx.SphinxClient;
import com.robertsoultanaev.javasphinx.SphinxException;
import com.robertsoultanaev.javasphinx.SphinxNode;
import com.robertsoultanaev.javasphinx.SphinxParams;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class MockMixnetwork {

    final ServerSocket serverSocket;
    Socket socket;
    volatile byte[] desphinxedMessage;

    public MockMixnetwork(final HashMap<Integer, MockMixPki> pki, final int port) {

        desphinxedMessage = null;

        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception ex) {
            throw new RuntimeException();
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte buffer[] = new byte[1024];
                    int bytesRead = -1;
                    while( ( bytesRead = in.read(buffer) ) != -1 ) {
                        baos.write(buffer, 0, bytesRead );
                    }

                    byte[] receivedSphinxPacket = baos.toByteArray();

                    SphinxParams params = new SphinxParams();

                    HeaderAndDelta headerAndDelta = SphinxClient.unpackMessage(receivedSphinxPacket).headerAndDelta;

                    // Try all the keys to find which node was the first
                    BigInteger currentPrivKey = null;
                    for (int nodeId : pki.keySet()) {
                        currentPrivKey = pki.get(nodeId).priv;
                        try {
                            SphinxNode.sphinxProcess(params, currentPrivKey, headerAndDelta);
                            break;
                        } catch (SphinxException ex) {}
                    }

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

                            desphinxedMessage = SphinxClient.receiveForward(params, ret.macKey, ret.headerAndDelta.delta).message;

                            break;
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException();
                }
            }
        }).start();
    }

    public byte[] getDesphinxedMessage() {
        return desphinxedMessage;
    }
}
