package com.rsoultanaev.sphinxproxy;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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

        serviceStarted = false;

        // AsyncTask not recommended for long running tasks,
        // so this is for prototyping purposes
        Pop3Task pop3Task = new Pop3Task();
        pop3Task.execute(27000);
    }

    public void startProxy(View view) {
        if (!serviceStarted) {
            Intent proxyIntent = new Intent(this, ProxyService.class);
            proxyIntent.putExtra("smtpPort", 28000);

            startService(proxyIntent);
            serviceStarted = true;

            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Service already started", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopProxy(View view) {
        if (serviceStarted) {
            Intent proxyIntent = new Intent(this, ProxyService.class);

            stopService(proxyIntent);
            serviceStarted = false;

            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Service already stopped", Toast.LENGTH_SHORT).show();
        }
    }
}
