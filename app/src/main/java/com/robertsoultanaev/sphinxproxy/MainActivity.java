package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Test Config key setting
        Context context = getApplicationContext();
        Config.setKey(R.string.key_proxy_pop3_port,  "27000",     context);
        Config.setKey(R.string.key_proxy_smtp_port,  "28000",     context);
        Config.setKey(R.string.key_proxy_username,   "proxyuser", context);
        Config.setKey(R.string.key_proxy_password,   "12345",     context);
        Config.setKey(R.string.key_mailbox_hostname, "localhost", context);
        Config.setKey(R.string.key_mailbox_port,     "11000",     context);
        Config.setKey(R.string.key_mailbox_username, "mort",      context);
        Config.setKey(R.string.key_mailbox_password, "1234",      context);
    }

    public void startProxy(View view) {
        Context context = getApplicationContext();
        int pop3Port = Integer.parseInt(Config.getKey(R.string.key_proxy_pop3_port, context));
        int smtpPort = Integer.parseInt(Config.getKey(R.string.key_proxy_smtp_port, context));

        Intent proxyIntent = new Intent(this, ProxyService.class);
        proxyIntent.putExtra("pop3Port", pop3Port);
        proxyIntent.putExtra("smtpPort", smtpPort);
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
