package com.rsoultanaev.sphinxproxy;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;

import java.net.InetSocketAddress;

public class AsyncTcpClient {

    private String host;
    private int port;

    public AsyncTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendMessage(final byte[] message) {
        AsyncServer.getDefault().connectSocket(new InetSocketAddress(host, port), new ConnectCallback() {
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
