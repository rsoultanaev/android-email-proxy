package com.rsoultanaev.sphinxproxy;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.rsoultanaev.sphinxproxy.database.DB;
import com.rsoultanaev.sphinxproxy.database.DBQuery;
import com.rsoultanaev.sphinxproxy.database.Packet;

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

                Pop3Puller pop3Puller = new Pop3Puller(server, port, username, password);

                Packet[] newMessages = pop3Puller.pullMessages(false);

                if (newMessages == null) {
                    System.out.println("Message pull failed");
                } else {
                    System.out.println("Messages for mort: " + newMessages.length);
                    for (Packet m : newMessages) {
                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                        System.out.println(m.uuid);
                        System.out.println(m.packetsInMessage);
                        System.out.println(m.sequenceNumber);
                        System.out.println(m.payload.length);
                        System.out.println(new String(m.payload));
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                    }
                }
            }
        }).start();
    }
}
