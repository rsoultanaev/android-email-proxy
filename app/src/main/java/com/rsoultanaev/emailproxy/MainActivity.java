package com.rsoultanaev.emailproxy;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rsoultanaev.emailproxy.server.Pop3Server;

public class MainActivity extends AppCompatActivity {

    private static class ServerTask extends AsyncTask<Integer, Void, Void> {
        protected Void doInBackground(Integer... urls) {
            int port = urls[0];
            String host = "localhost";
            Pop3Server pop3Server = new Pop3Server(host, port);
            pop3Server.start();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AsyncTask not recommended for long running tasks,
        // so this is for prototyping purposes
        ServerTask serverTask = new ServerTask();
        serverTask.execute(27000);
    }
}
