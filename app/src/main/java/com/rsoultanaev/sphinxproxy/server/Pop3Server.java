package com.rsoultanaev.sphinxproxy.server;

import android.content.Context;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.rsoultanaev.sphinxproxy.database.AssembledMessage;
import com.rsoultanaev.sphinxproxy.database.DB;
import com.rsoultanaev.sphinxproxy.database.DBQuery;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

public class Pop3Server {

    private InetAddress host;
    private int port;
    private Context context;

    public Pop3Server(int port, Context context) {
        try {
            this.host = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.port = port;
        this.context = context;
    }

    public void start() {
        AsyncServer.getDefault().listen(host, port, new ListenCallback() {

            @Override
            public void onAccepted(final AsyncSocket socket) {
                System.out.println("[POP3] New Connection " + socket.toString());

                final String serverGreeting = "+OK Hello there\n";
                sendResponse(socket, serverGreeting);

                socket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        byte[] receivedBytes = bb.getAllByteArray();
                        String receivedString = new String(receivedBytes);

                        System.out.println("[POP3] Received Message: " + receivedString + "\n");

                        final String response = respondToCommand(receivedString);
                        sendResponse(socket, response);
                    }
                });

                socket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            if (ex instanceof SocketException) {
                                System.out.println("[POP3] Socket exception while closing connection:\n");
                                System.out.println(ex.getMessage());
                            } else {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            System.out.println("[POP3] Successfully closed connection");
                        }
                    }
                });

                socket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            if (ex instanceof SocketException) {
                                System.out.println("[POP3] Socket exception while ending connection:\n");
                                System.out.println(ex.getMessage());
                            } else {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            System.out.println("[POP3] Successfully end connection");
                        }
                    }
                });
            }

            @Override
            public void onListening(AsyncServerSocket socket) {
                System.out.println("[POP3] Server started listening for connections");
            }

            @Override
            public void onCompleted(Exception ex) {
                if(ex != null) throw new RuntimeException(ex);
                System.out.println("[POP3] Successfully shutdown server");
            }
        });
    }

    public void stop() {
        AsyncServer.getDefault().stop();
    }

    private String respondToCommand(String command) {
        command = command.split("[\\r\\n ]")[0];

        DB db = DB.getAppDatabase(context);
        DBQuery dao = db.getDao();

        AssembledMessage message = dao.getAssembledMessages().get(0);
        String email = new String(message.message);

        switch (command) {
            case "USER":
            case "PASS":
            case "QUIT":
                return "+OK\r\n";
            case "STAT":
                return "+OK 1 " + email.length() + "\r\n";
            case "LIST":
                return "+OK\r\n" + "1 " + email.length() + "\r\n" + ".\r\n";
            case "UIDL":
                return "+OK\r\n" + "1 " + generateEmailId() + "\r\n" + ".\r\n";
            case "RETR":
                return "+OK " + email.length() + "\r\n" + email + ".\r\n";
            default:
                return "-ERR unsupported command\r\n";
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

    private String generateEmailId() {
        int leftLimit = 0x21;
        int rightLimit = 0x7e;
        int targetStringLength = 50;

        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);

        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }

        return buffer.toString();
    }
}
