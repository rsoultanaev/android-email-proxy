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

    private SortedMap<Integer, AssembledMessage> numberToMsg;
    private Set<String> markedForDeletion;
    private Context context;
    private static final String CRLF = "\r\n";

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

                final String response = getResponse(receivedString);
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

    private String getResponse(String queryStr) {
        String[] args = queryStr.split("[\\r\\n ]");
        String command = args[0].toUpperCase();

        String response = "-ERR unsupported command";

        switch (command) {
            case "USER":
            case "PASS":
            case "QUIT":
            case "NOOP":
                response = "+OK";
                break;
            case "STAT":
                response = getStatResponse();
                break;
            case "LIST":
                response = getListResponse(args);
                break;
            case "UIDL":
                response = getUidlResponse(args);
                break;
            case "RETR":
                response = getRetrResponse(args);
                break;
        }

        return response + CRLF;
    }

    private String getStatResponse() {
        int numMessages = numberToMsg.size();
        int totalLength = 0;
        for (AssembledMessage msg : numberToMsg.values()) {
            totalLength += msg.messageBody.length;
        }
        return "+OK" + " " + numMessages + " " + totalLength;
    }

    private String getListResponse(String[] args) {
        if (args.length > 1) {
            int argNum;
            try {
                argNum = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                return "-ERR failed to parse arguments";
            }

            if (!numberToMsg.containsKey(argNum)) {
                return "-ERR no such message";
            }

            return "+OK" + " " + argNum + " " + numberToMsg.get(argNum).messageBody.length;
        }

        StringBuilder response = new StringBuilder();
        response.append("+OK").append(CRLF);
        for (int number : numberToMsg.keySet()) {
            response.append(number + " " + numberToMsg.get(number).messageBody.length + CRLF);
        }
        response.append(".");
        return response.toString();
    }

    private String getUidlResponse(String[] args) {
        if (args.length > 1) {
            int argNum;
            try {
                argNum = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                return "-ERR failed to parse arguments";
            }

            if (!numberToMsg.containsKey(argNum)) {
                return "-ERR no such message";
            }

            return "+OK" + " " + argNum + " " + numberToMsg.get(argNum).uuid;
        }

        StringBuilder response = new StringBuilder();
        response.append("+OK").append(CRLF);
        for (int number : numberToMsg.keySet()) {
            response.append(number + " " + numberToMsg.get(number).uuid + CRLF);
        }
        response.append(".");
        return response.toString();
    }

    private String getRetrResponse(String[] args) {
        if (args.length < 2) {
            return "-ERR command expects more arguments";
        }

        int argNum;
        try {
            argNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            return "-ERR failed to parse arguments";
        }

        if (!numberToMsg.containsKey(argNum)) {
            return "-ERR no such message";
        }

        AssembledMessage message = numberToMsg.get(argNum);
        String messageStr = new String(message.messageBody);
        return "+OK" + " " + message.messageBody.length + CRLF + messageStr + ".";
    }
}