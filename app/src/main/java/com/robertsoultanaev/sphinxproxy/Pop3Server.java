package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import java.io.IOException;
import java.net.ServerSocket;


public class Pop3Server {

    public static final String CRLF = "\r\n";

    private Pop3ServerThread serverThread;
    private ServerSocket serverSocket;

    private int port;
    private DBQuery dbQuery;
    private String username;
    private String password;

    public Pop3Server(int port, String username, String password, DBQuery dbQuery) {
        this.port = port;
        this.dbQuery = dbQuery;
        this.username = username;
        this.password = password;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create socket", ex);
        }

        this.serverThread = new Pop3ServerThread(this, serverSocket);
        this.serverThread.start();
    }

    public void stop() {
        serverThread.shutdown();
    }

    public void setDbQuery(DBQuery dbQuery) {
        this.dbQuery = dbQuery;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DBQuery getDbQuery() {
        return dbQuery;
    }
}
