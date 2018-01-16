package com.rsoultanaev.sphinxproxy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.rsoultanaev.sphinxproxy.server.Pop3Server;
import com.rsoultanaev.sphinxproxy.server.SmtpMessageHandler;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.robertsoultanaev.javasphinx.SphinxParams;

public class ProxyService extends Service {

    private SMTPServer smtpServer;
    private Pop3Server pop3Server;
    private boolean running;

    @Override
    public void onCreate() {
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            final int smtpPort = intent.getIntExtra("smtpPort", 28000);
            final int pop3Port = intent.getIntExtra("pop3Port", 27000);

            // Initialising SMTPServer blocks, so we need to do it in a separate thread
            new Thread(new Runnable() {
                public void run() {
                    SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler();
                    smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));
                    smtpServer.setPort(smtpPort);
                    smtpServer.start();

                    pop3Server = new Pop3Server(pop3Port);
                    pop3Server.start();
                }
            }).start();

            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("My Awesome App")
                    .setContentText("Doing some work...")
                    .setContentIntent(pendingIntent).build();

            startForeground(1337, notification);

            running = true;
        }

        // TODO: investigate which return is best here
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        smtpServer.stop();
        pop3Server.stop();
        running = false;
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void func() {

    }
}
