package com.rsoultanaev.sphinxproxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.rsoultanaev.sphinxproxy.server.SmtpMessageHandler;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

public class ProxyService extends Service {

    private SMTPServer smtpServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int smtpPort = intent.getIntExtra("smtpPort", 28000);

        // Initialising SMTPServer blocks, so we need to do it in a separate thread
        new Thread(new Runnable() {
            public void run() {
                SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler();
                smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));
                smtpServer.setPort(smtpPort);
                smtpServer.start();
            }
        }).start();

        // TODO: investigate which return is best here
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        smtpServer.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
