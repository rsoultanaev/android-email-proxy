package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;


public class Pop3Server {

    public static final String CRLF = "\r\n";

    private Pop3ServerThread serverThread;

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
        synchronized (this) {
            this.serverThread = new Pop3ServerThread(this);
            this.serverThread.start();

            // Block until serverThread is listening for connections
            try {
                wait();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
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
