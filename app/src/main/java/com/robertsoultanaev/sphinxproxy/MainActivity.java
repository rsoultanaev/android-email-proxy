package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3SClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;

import javax.net.ssl.TrustManager;

public class MainActivity extends AppCompatActivity {

    public static final int EDIT_CONFIG_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        if (!Config.setupDone(context)) {
            setDefaultConfig(context);

            new Thread(new Runnable() {
                public void run() {
                    DBQuery dbQuery = DB.getAppDatabase(context).getDao();

                    generateAndSetKeyPair(context);

                    loadMixNetworkConfig(dbQuery, context);
                    loadRecipientPublicKeys(dbQuery);
                    loadMailboxCertificate(context);
                }
            }).start();

            Intent configIntent = new Intent(this, ConfigActivity.class);
            startActivityForResult(configIntent, EDIT_CONFIG_REQUEST);

            Config.setSetupDone(context, true);
        }
    }

    public void startProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        startService(proxyIntent);
    }

    public void stopProxy(View view) {
        Intent proxyIntent = new Intent(this, ProxyService.class);
        stopService(proxyIntent);
    }

    public void pullFromMailbox(View view) {
        new Thread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                String server = Config.getStringValue(R.string.key_mailbox_hostname, context);
                int port = Config.getIntValue(R.string.key_mailbox_port, context);
                String username = Config.getStringValue(R.string.key_mailbox_username, context);
                String password = Config.getStringValue(R.string.key_mailbox_password, context);

                DB db = DB.getAppDatabase(context);
                DBQuery dbQuery = db.getDao();

                TrustManager trustManager = Config.getTrustManager(context);
                POP3SClient pop3Client = new POP3SClient(true);
                pop3Client.setDefaultTimeout(60000);
                pop3Client.setTrustManager(trustManager);

                PrivateKey privateKey = Config.getPrivateKey(context);
                EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

                List<MixNode> mixNodes = dbQuery.getMixNodes();
                int numUseMixes = Config.getIntValue(R.string.key_num_use_mixes, context);
                SphinxUtil sphinxUtil = new SphinxUtil(mixNodes, numUseMixes);

                Mailbox mailbox = new Mailbox(server, port, username, password, dbQuery, pop3Client, endToEndCrypto, privateKey, sphinxUtil);
                mailbox.updateMailbox();
            }
        }).start();
    }

    public void editConfig(View view) {
        Intent configIntent = new Intent(this, ConfigActivity.class);
        startActivityForResult(configIntent, EDIT_CONFIG_REQUEST);
    }

    private void setDefaultConfig(Context context) {
        Config.setIntValue(R.string.key_proxy_pop3_port, Integer.parseInt(getString(R.string.default_proxy_pop3_port)), context);
        Config.setIntValue(R.string.key_proxy_smtp_port, Integer.parseInt(getString(R.string.default_proxy_smtp_port)), context);
        Config.setStringValue(R.string.key_proxy_username, getString(R.string.default_proxy_username), context);
        Config.setStringValue(R.string.key_proxy_password, getString(R.string.default_proxy_password), context);
        Config.setStringValue(R.string.key_mailbox_hostname, getString(R.string.default_mailbox_hostname), context);
        Config.setIntValue(R.string.key_mailbox_port, Integer.parseInt(getString(R.string.default_mailbox_port)), context);
        Config.setStringValue(R.string.key_mailbox_username, getString(R.string.default_mailbox_username), context);
        Config.setStringValue(R.string.key_mailbox_password, getString(R.string.default_mailbox_password), context);
        Config.setIntValue(R.string.key_num_use_mixes, Integer.parseInt(getString(R.string.default_num_use_mixes)), context);
    }

    private void loadMixNetworkConfig(DBQuery dbQuery, Context context) {
        String fileName = getString(R.string.mix_network_filename);
        String line;
        int numTotalMixes = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(",");
                int id = Integer.parseInt(splitLine[0]);
                String host = splitLine[1];
                int port = Integer.parseInt(splitLine[2]);
                String encodedPublicKey = splitLine[3];
                dbQuery.insertMixNode(new MixNode(id, host, port, encodedPublicKey));
                numTotalMixes++;
            }
            reader.close();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read the mix network configuration", ex);
        }

        Config.setIntValue(R.string.key_num_total_mixes, numTotalMixes, context);
    }

    private void loadRecipientPublicKeys(DBQuery dbQuery) {
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
    }

    private void loadMailboxCertificate(Context context) {
        try {
            String fileName = getString(R.string.mailbox_cert_filename);
            byte[] certBytes = IOUtils.toByteArray(getAssets().open(fileName));
            String certString = new String(certBytes);

            Config.setStringValue(R.string.key_mailbox_cert, certString, context);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read mailbox certificate", ex);
        }
    }

    private void generateAndSetKeyPair(Context context) {
        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();
        KeyPair keyPair = endToEndCrypto.generateKeyPair();
        Config.setKeyPair(keyPair, context);
    }
}
