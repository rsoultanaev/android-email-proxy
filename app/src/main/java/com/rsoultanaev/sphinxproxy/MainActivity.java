package com.rsoultanaev.sphinxproxy;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.rsoultanaev.sphinxproxy.server.Pop3Server;

public class MainActivity extends AppCompatActivity {

    private boolean serviceStarted;

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
    }

    public void startProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        proxyIntent.putExtra("smtpPort", 28000);
        startService(proxyIntent);
    }

    public void stopProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        stopService(proxyIntent);
    }
}
