package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.robertsoultanaev.sphinxproxy.Pop3Server.CRLF;

public class Pop3ServerThread extends Thread {
    private enum State {
        AUTHORIZATION,
        TRANSACTION
    }

    private Pop3Server server;

    private volatile boolean shuttingDown;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private SortedMap<Integer, AssembledMessage> numberToMsg;
    private Set<String> markedForDeletion;
    private State sessionState;
    private String providedUsername;

    public Pop3ServerThread(Pop3Server server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;

        this.shuttingDown = false;
        this.numberToMsg = new TreeMap<>();
        this.markedForDeletion = new HashSet<>();
        this.sessionState = State.AUTHORIZATION;
    }

    @Override
    public void run() {
        while (!this.shuttingDown) {
            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine, output;

                numberToMsg.clear();
                markedForDeletion.clear();

                List<AssembledMessage> assembledMessages = server.getDbQuery().getAssembledMessages();
                for (int i = 1; i <= assembledMessages.size(); i++) {
                    numberToMsg.put(i, assembledMessages.get(i - 1));
                }

                output = "+OK Hello there" + "\r";
                out.println(output);

                while ((inputLine = in.readLine()) != null) {
                    String[] args = inputLine.split("[\\r\\n ]");
                    String command = args[0].toUpperCase();

                    output = handleCommand(command, args);
                    out.println(output);
                    if (command.equals("QUIT"))
                        break;
                }
            } catch (IOException ex) {
                // IOException happens when the server is closed, so we only
                // fail if exception happens without the thread being closed
                if (!this.shuttingDown) {
                    throw new RuntimeException("Pop3ServerThread failed", ex);
                }

                continue;
            }

            update();

            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to close the connection", ex);
            }
        }
    }

    public void shutdown() {
        this.shuttingDown = true;

        try {
            if (clientSocket != null) {
                in.close();
                out.close();
                clientSocket.close();
            }
            serverSocket.close();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to stop the thread", ex);
        }

        try {
            join();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interrupted while shutting down", ex);
        }
    }

    private void update() {
        sessionState = State.AUTHORIZATION;

        for (String uuid : markedForDeletion) {
            server.getDbQuery().deleteAssembledMessage(uuid);
        }

        numberToMsg.clear();
        markedForDeletion.clear();
    }

    private String handleCommand(String command, String[] args) {
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

        return response + "\r";
    }

    private String handleStat() {
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

        int numMessages = numberToMsg.size();
        int totalLength = 0;
        for (AssembledMessage msg : numberToMsg.values()) {
            totalLength += msg.messageBody.length;
        }
        return "+OK" + " " + numMessages + " " + totalLength;
    }

    private String handleList(String[] args) {
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

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
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

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
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

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
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

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
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

        markedForDeletion.clear();

        return "+OK";
    }

    private String handleTop(String[] args) {
        if (sessionState != State.TRANSACTION) {
            return "-ERR unauthorized access";
        }

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
        if (sessionState != State.AUTHORIZATION) {
            return "-ERR command only allowed in AUTHORIZATION state";
        }

        if (args.length < 2) {
            return "-ERR command expects more arguments";
        }

        providedUsername = args[1];

        return "+OK";
    }

    private String handlePass(String[] args) {
        if (sessionState != State.AUTHORIZATION) {
            return "-ERR command only allowed in AUTHORIZATION state";
        }

        if (args.length < 2) {
            return "-ERR command expects more arguments";
        }

        if (providedUsername == null) {
            return "-ERR username not specified";
        }

        String providedPassword = args[1];
        String response = "-ERR authentication failed";

        if (providedUsername.equals(server.getUsername()) && providedPassword.equals(server.getPassword())) {
            sessionState = State.TRANSACTION;
            return "+OK";
        }

        providedUsername = null;
        return response;
    }
}
