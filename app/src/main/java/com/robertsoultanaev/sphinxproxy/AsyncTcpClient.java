package com.robertsoultanaev.sphinxproxy;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;

import java.net.InetSocketAddress;

public class AsyncTcpClient {

    public void sendMessage(final SphinxPacketWithRouting sphinxPacketWithRouting) {
        final InetSocketAddress firstNodeAddress = sphinxPacketWithRouting.firstNodeAddress;
        final byte[] message = sphinxPacketWithRouting.binMessage;

        AsyncServer.getDefault().connectSocket(firstNodeAddress, new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, final AsyncSocket socket) {
                if(ex != null) {
                    throw new RuntimeException(ex);
                }

                Util.writeAll(socket, message, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            throw new RuntimeException(ex);
                        }

                        System.out.println("[AsyncTcpClient] Successfully wrote message");
                        System.out.println("[AsyncTcpClient] Message length: " + message.length);
                        System.out.println(new String(message));
                    }
                });

                socket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            throw new RuntimeException(ex);
                        }

                        System.out.println("[AsyncTcpClient] Successfully closed connection");
                    }
                });

                socket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            throw new RuntimeException(ex);
                        }

                        System.out.println("[AsyncTcpClient] Successfully end connection");
                    }
                });

                socket.close();
            }
        });
    }
}
