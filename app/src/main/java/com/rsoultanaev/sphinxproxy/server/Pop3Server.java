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
import java.net.SocketException;
import java.util.Random;

public class Pop3Server {

    private InetAddress host;
    private int port;

    public Pop3Server(int port) {
        try {
            this.host = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.port = port;
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

        String email =
                  "+OK\r\n"
                + "Date: Sun, 31 Dec 2017 12:59:51 +0200\r\n"
                + "User-Agent: K-9 Mail for Android\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/alternative; boundary=\"----MZ1WRL6YXU0MHXZS0DBVZ18PC8NKEE\"\r\n"
                + "Content-Transfer-Encoding: 7bit\r\n"
                + "Subject: This is subject\r\n"
                + "To: robert@sphinx.com\r\n"
                + "From: Eric <eric@m.com>\r\n"
                + "Message-ID: <F7EDCFAA-F670-429B-9A9C-000DE53AF856@m.com>\r\n"
                + "------MZ1WRL6YXU0MHXZS0DBVZ18PC8NKEE\r\n"
                + "Content-Type: text/plain;\r\n"
                + " charset=utf-8\r\n"
                + "Content-Transfer-Encoding: quoted-printable\r\n"
                + "This is message\r\n"
                + "Dave\r\n"
                + "--=20\r\n"
                + "Sent from my Android device with K-9 Mail=2E Please excuse my brevity=2E\r\n"
                + "------MZ1WRL6YXU0MHXZS0DBVZ18PC8NKEE\r\n"
                + "Content-Type: text/html;\r\n"
                + " charset=utf-8\r\n"
                + "Content-Transfer-Encoding: quoted-printable\r\n"
                + "This is message<br>\r\n"
                + "<br>\r\n"
                + "Dave<br>\r\n"
                + "-- <br>\r\n"
                + "Sent from my Android device with K-9 Mail=2E Please excuse my brevity=2E\r\n"
                + "------MZ1WRL6YXU0MHXZS0DBVZ18PC8NKEE--\r\n";

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
                return email + ".\r\n";
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
