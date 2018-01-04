package com.rsoultanaev.sphinxproxy;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rsoultanaev.sphinxproxy.server.Pop3Server;

public class MainActivity extends AppCompatActivity {

    private static class Pop3Task extends AsyncTask<Integer, Void, Void> {
        protected Void doInBackground(Integer... urls) {
            int port = urls[0];
            Pop3Server pop3Server = new Pop3Server(port);
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
        Pop3Task pop3Task = new Pop3Task();
        pop3Task.execute(27000);

        Intent startProxyIntent = new Intent(this, ProxyService.class);
        startProxyIntent.putExtra("smtpPort", 28000);
        startService(startProxyIntent);
    }
}
