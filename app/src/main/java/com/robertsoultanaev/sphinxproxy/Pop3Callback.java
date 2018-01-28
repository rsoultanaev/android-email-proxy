package com.robertsoultanaev.sphinxproxy;

import android.content.Context;

import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Pop3Callback implements ListenCallback {

    private static final String CRLF = "\r\n";

    private enum State {
        AUTHORIZATION,
        TRANSACTION
    }

    private SortedMap<Integer, AssembledMessage> numberToMsg;
    private Set<String> markedForDeletion;
    private Context context;
    private State sessionState;
    private String user;
    private String pass;
    private String providedUser;

    public Pop3Callback(Context context) {
        this.numberToMsg = new TreeMap<>();
        this.markedForDeletion = new HashSet<>();
        this.context = context;
        this.sessionState = State.AUTHORIZATION;

        this.user = "proxyuser";
        this.pass = "12345";
        this.providedUser = null;
    }

    @Override
    public void onAccepted(final AsyncSocket socket) {
        System.out.println("[POP3] New Connection " + socket.toString());

        numberToMsg.clear();
        markedForDeletion.clear();

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
                update();

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
                update();

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

    private void update() {
        sessionState = State.AUTHORIZATION;

        DB db = DB.getAppDatabase(context);
        DBQuery dao = db.getDao();

        for (String uuid : markedForDeletion) {
            dao.deleteAssembledMessage(uuid);
        }

        numberToMsg.clear();
        markedForDeletion.clear();
    }

    private String getResponse(String queryStr) {
        String[] args = queryStr.split("[\\r\\n ]");
        String command = args[0].toUpperCase();

        String response = "-ERR unsupported command";

        switch (command) {
            case "QUIT":
            case "NOOP":
                response = "+OK";
                break;
            case "USER":
                response = handleUser(args);
                break;
            case "PASS":
                response = handlePass(args);
                break;
            case "STAT":
                response = handleStat();
                break;
            case "LIST":
                response = handleList(args);
                break;
            case "UIDL":
                response = handleUidl(args);
                break;
            case "RETR":
                response = handleRetr(args);
                break;
            case "DELE":
                response = handleDele(args);
                break;
            case "RSET":
                response = handleRset();
                break;
            case "TOP":
                response = handleTop(args);
                break;
        }

        return response + CRLF;
    }

    private String handleStat() {
        int numMessages = numberToMsg.size();
        int totalLength = 0;
        for (AssembledMessage msg : numberToMsg.values()) {
            totalLength += msg.messageBody.length;
        }
        return "+OK" + " " + numMessages + " " + totalLength;
    }

    private String handleList(String[] args) {
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

            if (markedForDeletion.contains(numberToMsg.get(argNum).uuid)) {
                return "-ERR message deleted";
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

    private String handleUidl(String[] args) {
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

            if (markedForDeletion.contains(numberToMsg.get(argNum).uuid)) {
                return "-ERR message deleted";
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

    private String handleRetr(String[] args) {
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

        if (markedForDeletion.contains(numberToMsg.get(argNum).uuid)) {
            return "-ERR message deleted";
        }

        AssembledMessage message = numberToMsg.get(argNum);
        String messageStr = new String(message.messageBody);
        return "+OK" + " " + message.messageBody.length + CRLF + messageStr + ".";
    }

    private String handleDele(String[] args) {
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

        markedForDeletion.add(numberToMsg.get(argNum).uuid);

        return "+OK";
    }

    private String handleRset() {
        markedForDeletion.clear();

        return "+OK";
    }

    private String handleTop(String[] args) {
        if (args.length < 3) {
            return "-ERR command expects more arguments";
        }

        int argNum;
        int argLines;
        try {
            argNum = Integer.parseInt(args[1]);
            argLines = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            return "-ERR failed to parse arguments";
        }

        if (!numberToMsg.containsKey(argNum)) {
            return "-ERR no such message";
        }

        String messageStr = new String(numberToMsg.get(argNum).messageBody);
        BufferedReader reader = new BufferedReader(new StringReader(messageStr));

        StringBuilder response = new StringBuilder();
        response.append("+OK").append(CRLF);

        try {
            String line = reader.readLine();

            // Append the message headers
            while (line != null) {
                response.append(line).append(CRLF);

                if (line.isEmpty()) {
                    break;
                }

                line = reader.readLine();
            }

            // Append requested lines
            line = reader.readLine();
            for (int i = 0; line != null && i < argLines; i++) {
                response.append(line).append(CRLF);
                line = reader.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read the message", ex);
        }

        response.append(".");

        return response.toString();
    }

    private String handleUser(String[] args) {
        if (args.length < 2) {
            return "-ERR command expects more arguments";
        }

        providedUser = args[1];

        return "+OK";
    }

    private String handlePass(String[] args) {
        if (args.length < 2) {
            return "-ERR command expects more arguments";
        }

        String providedPass = args[1];

        if (providedUser.equals(user) && providedPass.equals(pass)) {
            sessionState = State.TRANSACTION;
            return "+OK";
        } else {
            sessionState = State.AUTHORIZATION;
            return "-ERR authentication failed";
        }
    }
}
