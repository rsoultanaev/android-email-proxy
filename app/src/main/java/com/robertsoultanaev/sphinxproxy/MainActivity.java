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
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.apache.commons.net.pop3.POP3SClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;

import javax.net.ssl.TrustManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        // Set default config
        Config.setKey(R.string.key_proxy_pop3_port, getString(R.string.default_proxy_pop3_port), context);
        Config.setKey(R.string.key_proxy_smtp_port, getString(R.string.default_proxy_smtp_port), context);
        Config.setKey(R.string.key_proxy_username, getString(R.string.default_proxy_username), context);
        Config.setKey(R.string.key_proxy_password, getString(R.string.default_proxy_password), context);
        Config.setKey(R.string.key_mailbox_hostname, getString(R.string.default_mailbox_hostname), context);
        Config.setKey(R.string.key_mailbox_port, getString(R.string.default_mailbox_port), context);
        Config.setKey(R.string.key_mailbox_username, getString(R.string.default_mailbox_username), context);
        Config.setKey(R.string.key_mailbox_password, getString(R.string.default_mailbox_password), context);

        // Read mix network configuration and save into database when first installed
        String sharedPreferencesFile = getString(R.string.key_preference_file);
        String setupDoneKey = getString(R.string.key_setup_done);
        final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(setupDoneKey, false)) {
            new Thread(new Runnable() {
                public void run() {
                    DB db = DB.getAppDatabase(context);
                    DBQuery dbQuery = db.getDao();

                    EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
                    KeyPair keyPair = endToEndCrypto.generateKeyPair();
                    Config.setKeyPair(keyPair, context);

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
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to read the mix network configuration", ex);
                    }

                    try {
                        String fileName = getString(R.string.recipient_keys_filename);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] splitLine = line.split(",");
                            String recipient = splitLine[0];
                            String encodedPublicKey = splitLine[1];
                            dbQuery.insertRecipient(new Recipient(recipient, encodedPublicKey));
                        }

                        reader.close();
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to read the recipient keys", ex);
                    }

                    try {
                        String fileName = getString(R.string.mailbox_cert_filename);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }

                        reader.close();

                        Config.setKey(R.string.key_mailbox_cert, sb.toString(), context);
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to read the recipient keys", ex);
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.key_setup_done), true);
                    editor.apply();
                }
            }).start();
        }
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

                DB db = DB.getAppDatabase(context);
                DBQuery dbQuery = db.getDao();

                TrustManager trustManager = Config.getTrustManager(context);
                POP3SClient pop3Client = new POP3SClient(true);
                pop3Client.setDefaultTimeout(60000);
                pop3Client.setTrustManager(trustManager);

                PrivateKey privateKey = Config.getPrivateKey(context);
                EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

                SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);

                Mailbox mailbox = new Mailbox(server, port, username, password, dbQuery, pop3Client, endToEndCrypto, privateKey, sphinxUtil);
                mailbox.updateMailbox();
            }
        }).start();
    }
}
