package com.rsoultanaev.sphinxproxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.rsoultanaev.sphinxproxy.server.SmtpMessageHandler;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

public class ProxyService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int smtpPort = intent.getIntExtra("smtpPort", 28000);

        Runnable runSmtpServer = new Runnable() {
            public void run() {
                SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler();
                SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));
                smtpServer.setPort(smtpPort);
                smtpServer.start();
            }
        };

        Thread smtpThread = new Thread(runSmtpServer);
        smtpThread.start();

        // TODO: investigate which return is best here
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
