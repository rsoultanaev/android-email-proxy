package com.robertsoultanaev.sphinxproxy;

import com.koushikdutta.async.AsyncServer;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Pop3Server {

    private InetAddress host;
    private int port;
    private DBQuery dbQuery;
    private String username;
    private String password;

    public Pop3Server(int port, String username, String password, DBQuery dbQuery) {
        try {
            this.host = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.port = port;
        this.dbQuery = dbQuery;
        this.username = username;
        this.password = password;
    }

    public void start() {
        Pop3Callback callback = new Pop3Callback(username, password, dbQuery);
        AsyncServer.getDefault().listen(host, port, callback);
    }

    public void stop() {
        AsyncServer.getDefault().stop();
    }
}
