package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import org.apache.commons.net.pop3.POP3Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        // Read mix network configuration and save into database when first installed
        String sharedPreferencesFile = getString(R.string.key_preference_file);
        String setupDoneKey = getString(R.string.key_setup_done);
        final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(setupDoneKey, false)) {
            new Thread(new Runnable() {
                public void run() {
                    DB db = DB.getAppDatabase(context);
                    DBQuery dbQuery = db.getDao();

                    try {
                        String fileName = getString(R.string.mix_network_filename);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] splitLine = line.split(",");
                            int port = Integer.parseInt(splitLine[0]);
                            String encodedPublicKey = splitLine[1];
                            dbQuery.insertMixNode(new MixNode(port, encodedPublicKey));
                        }

                        reader.close();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.key_setup_done), true);
                        editor.apply();
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to read the mix network configuration", ex);
                    }

                    EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
                    KeyPair keyPair = endToEndCrypto.generateKeyPair();
                    Config.setKeyPair(keyPair, context);

                    // TODO: Read recipient public keys
                }
            }).start();
        }

        // Test Config key setting
        Config.setKey(R.string.key_proxy_pop3_port,   "27000",     context);
        Config.setKey(R.string.key_proxy_smtp_port,   "28000",     context);
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
                Context context = getApplicationContext();
                String server = Config.getKey(R.string.key_mailbox_hostname, context);
                int port = Integer.parseInt(Config.getKey(R.string.key_mailbox_port, context));
                String username = Config.getKey(R.string.key_mailbox_username, context);
                String password = Config.getKey(R.string.key_mailbox_password, context);

                DB db = DB.getAppDatabase(getApplicationContext());
                DBQuery dbQuery = db.getDao();
                POP3Client pop3Client = new POP3Client();
                pop3Client.setDefaultTimeout(60000);

                PrivateKey privateKey = Config.getPrivateKey(context);
                EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

                Mailbox mailbox = new Mailbox(server, port, username, password, dbQuery, pop3Client, endToEndCrypto, privateKey);
                mailbox.updateMailbox();
            }
        }).start();
    }
}
