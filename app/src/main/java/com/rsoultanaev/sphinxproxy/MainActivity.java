package com.rsoultanaev.sphinxproxy;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.rsoultanaev.sphinxproxy.server.MessageListener;
import com.rsoultanaev.sphinxproxy.server.Pop3Server;
import com.rsoultanaev.sphinxproxy.server.SmtpServer;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

public class MainActivity extends AppCompatActivity {

    private static class Pop3Task extends AsyncTask<Integer, Void, Void> {
        protected Void doInBackground(Integer... urls) {
            int port = urls[0];
            String host = "localhost";
            Pop3Server pop3Server = new Pop3Server(host, port);
            pop3Server.start();

            return null;
        }
    }

    private static class SmtpTask extends AsyncTask<Integer, Void, Void> {
        protected Void doInBackground(Integer... urls) {
            int port = urls[0];
            String host = "localhost";

            MessageListener messageListener = new MessageListener();
            SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(messageListener));
            smtpServer.setPort(port);
            smtpServer.start();

//            SmtpServer smtpServer = new SmtpServer(host, port);
//            smtpServer.start();

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

        SmtpTask smtpTask = new SmtpTask();
        smtpTask.execute(28000);
    }
}
