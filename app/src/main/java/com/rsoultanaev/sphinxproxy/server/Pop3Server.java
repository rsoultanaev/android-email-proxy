package com.rsoultanaev.sphinxproxy.server;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.ListenCallback;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Pop3Server {

    private InetAddress host;
    private int port;

    public Pop3Server(String host, int port) {
        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.port = port;
    }

    public void start() {
        AsyncServer.getDefault().listen(host, port, new ListenCallback() {

            @Override
            public void onAccepted(final AsyncSocket socket) {
                System.out.println("[Server] New Connection " + socket.toString());

                final String serverGreeting = "+OK Hello there\n";
                sendResponse(socket, serverGreeting);

                socket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        byte[] receivedBytes = bb.getAllByteArray();
                        String receivedString = new String(receivedBytes);

                        System.out.println("[Server] Received Message Length: " + receivedString.length() + "\n");
                        System.out.println("[Server] Received Message: " + receivedString + "\n");

                        final String response = respondToCommand(receivedString);
                        sendResponse(socket, response);
                    }
                });

                socket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            if (ex instanceof java.net.SocketException) {
                                java.net.SocketException socketException = (java.net.SocketException) ex;
                                System.out.println("[Server] Socket exception while closing connection:\n");
                                System.out.println(socketException.getMessage());
                            } else {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            System.out.println("[Server] Successfully closed connection");
                        }
                    }
                });

                socket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            if (ex instanceof java.net.SocketException) {
                                java.net.SocketException socketException = (java.net.SocketException) ex;
                                System.out.println("[Server] Socket exception while ending connection:\n");
                                System.out.println(socketException.getMessage());
                            } else {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            System.out.println("[Server] Successfully end connection");
                        }
                    }
                });
            }

            @Override
            public void onListening(AsyncServerSocket socket) {
                System.out.println("[Server] Server started listening for connections");
            }

            @Override
            public void onCompleted(Exception ex) {
                if(ex != null) throw new RuntimeException(ex);
                System.out.println("[Server] Successfully shutdown server");
            }
        });
    }

    private String respondToCommand(String command) {
        if (command.length() >= 4) {
            command = command.substring(0, 4);
        } else {
            return "-ERR unsupported command\n";
        }

        switch (command) {
            case "USER":
            case "PASS":
            case "UIDL":
            case "QUIT":
                return "+OK\n";
            case "STAT":
                return "+OK 0 0\n";
            default:
                return "-ERR unsupported command\n";
        }
    }

    private void sendResponse(final AsyncSocket socket, final String command) {
        Util.writeAll(socket, command.getBytes(), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) throw new RuntimeException(ex);
                System.out.println("[Server] Successfully wrote message: " + command + "\n");
            }
        });
    }
}
