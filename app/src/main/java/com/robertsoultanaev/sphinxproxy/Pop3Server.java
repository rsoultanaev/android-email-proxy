package com.robertsoultanaev.sphinxproxy;

import android.content.Context;

import com.koushikdutta.async.AsyncServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
        Pop3Callback callback = new Pop3Callback(context);
        AsyncServer.getDefault().listen(host, port, callback);
    }

    public void stop() {
        AsyncServer.getDefault().stop();
    }
}
