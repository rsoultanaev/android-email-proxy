package com.rsoultanaev.sphinxproxy;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        proxyIntent.putExtra("pop3Port", 27000);
        proxyIntent.putExtra("smtpPort", 28000);
        startService(proxyIntent);
    }

    public void stopProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        stopService(proxyIntent);
    }

    public void triggerTest(View view) {
        new Thread(new Runnable() {
            public void run() {
                String server = "localhost";
                int port = 11000;
                String username = "mort";
                String password = "1234";

                Mailbox mailbox = new Mailbox(server, port, username, password, getApplicationContext());
                mailbox.updateMailbox();
            }
        }).start();
    }
}
