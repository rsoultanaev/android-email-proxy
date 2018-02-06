package com.robertsoultanaev.sphinxproxy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

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
                    Context context = getApplicationContext();
                    DB db = DB.getAppDatabase(getApplicationContext());
                    DBQuery dbQuery = db.getDao();

                    SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);
                    AsyncTcpClient asyncTcpClient = new AsyncTcpClient("localhost", 10000);
                    EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
                    SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient, dbQuery, endToEndCrypto);
                    smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(smtpMessageHandler));
                    smtpServer.setPort(smtpPort);
                    smtpServer.start();

                    String username = Config.getKey(R.string.key_proxy_username, context);
                    String password = Config.getKey(R.string.key_proxy_password, context);
                    pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
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
}
