package com.rsoultanaev.sphinxproxy.server;

import android.content.Context;

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

import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Pop3Callback implements ListenCallback {

    SortedMap<Integer, AssembledMessage> numberToMsg;
    Set<String> markedForDeletion;
    Context context;

    public Pop3Callback(Context context) {
        this.numberToMsg = new TreeMap<>();
        this.markedForDeletion = new HashSet<>();
        this.context = context;
    }

    @Override
    public void onAccepted(final AsyncSocket socket) {
        System.out.println("[POP3] New Connection " + socket.toString());

        clearState();

        DB db = DB.getAppDatabase(context);
        DBQuery dao = db.getDao();
        List<AssembledMessage> assembledMessages = dao.getAssembledMessages();
        for (int i = 1; i <= assembledMessages.size(); i++) {
            numberToMsg.put(i, assembledMessages.get(i - 1));
        }

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
                clearState();

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
                clearState();

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

    private String respondToCommand(String argString) {
        String[] args = argString.split("[\\r\\n ]");
        String command = args[0].toUpperCase();

        DB db = DB.getAppDatabase(context);
        DBQuery dao = db.getDao();

        switch (command) {
            case "USER":
            case "PASS":
            case "QUIT":
                return "+OK\r\n";
            case "STAT":
                int numMessages = numberToMsg.size();
                int totalLength = 0;
                for (AssembledMessage msg : numberToMsg.values()) {
                    totalLength += msg.message.length;
                }
                return "+OK" + " " + numMessages + " " + totalLength + "\r\n";
            case "LIST":
                StringBuilder listResponse = new StringBuilder();
                for (int number : numberToMsg.keySet()) {
                    listResponse.append(number + " " + numberToMsg.get(number).message.length + "\r\n");
                }
                listResponse.append(".\r\n");
                return "+OK\r\n" + listResponse.toString();
            case "UIDL":
                StringBuilder uidlResponse = new StringBuilder();
                for (int number : numberToMsg.keySet()) {
                    uidlResponse.append(number + " " + numberToMsg.get(number).uuid + "\r\n");
                }
                uidlResponse.append(".\r\n");
                return "+OK\r\n" + uidlResponse.toString();
            case "RETR":
                int retrNum = Integer.parseInt(args[1]);
                AssembledMessage message = numberToMsg.get(retrNum);
                String messageStr = new String(message.message);
                return "+OK " + message.message.length + "\r\n" + messageStr + ".\r\n";
            default:
                return "-ERR unsupported command\r\n";
        }
    }

    private void sendResponse(final AsyncSocket socket, final String response) {
        Util.writeAll(socket, response.getBytes(), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) throw new RuntimeException(ex);
                System.out.println("[Server] Successfully wrote message: " + response + "\n");
            }
        });
    }

    private void clearState() {
        numberToMsg.clear();
        markedForDeletion.clear();
    }
}
