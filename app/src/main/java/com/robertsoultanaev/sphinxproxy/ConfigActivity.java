package com.robertsoultanaev.sphinxproxy;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        final Context context = getApplicationContext();

        String pop3Port = Integer.toString(Config.getIntValue(R.string.key_proxy_pop3_port, context));
        String smtpPort = Integer.toString(Config.getIntValue(R.string.key_proxy_smtp_port, context));
        String proxyUsername = Config.getStringValue(R.string.key_proxy_username, context);
        String proxyPassword = Config.getStringValue(R.string.key_proxy_password, context);
        String mailboxHostname = Config.getStringValue(R.string.key_mailbox_hostname, context);
        String mailboxPort = Integer.toString(Config.getIntValue(R.string.key_mailbox_port, context));
        String mailboxUsername = Config.getStringValue(R.string.key_mailbox_username, context);
        String mailboxPassword = Config.getStringValue(R.string.key_mailbox_password, context);

        final EditText editTextPop3Port = findViewById(R.id.editTextPop3Port);
        final EditText editTextSmtpPort = findViewById(R.id.editTextSmtpPort);
        final EditText editTextProxyUsername = findViewById(R.id.editTextProxyUsername);
        final EditText editTextProxyPassword = findViewById(R.id.editTextProxyPassword);
        final EditText editTextMailboxHostname = findViewById(R.id.editTextMailboxHostname);
        final EditText editTextMailboxPort = findViewById(R.id.editTextMailboxPort);
        final EditText editTextMailboxUsername = findViewById(R.id.editTextMailboxUsername);
        final EditText editTextMailboxPassword = findViewById(R.id.editTextMailboxPassword);

        editTextPop3Port.setText(pop3Port);
        editTextSmtpPort.setText(smtpPort);
        editTextProxyUsername.setText(proxyUsername);
        editTextProxyPassword.setText(proxyPassword);
        editTextMailboxHostname.setText(mailboxHostname);
        editTextMailboxPort.setText(mailboxPort);
        editTextMailboxUsername.setText(mailboxUsername);
        editTextMailboxPassword.setText(mailboxPassword);
    }

    public void saveConfig(View view) {
        final Context context = getApplicationContext();

        final EditText editTextPop3Port = findViewById(R.id.editTextPop3Port);
        final EditText editTextSmtpPort = findViewById(R.id.editTextSmtpPort);
        final EditText editTextProxyUsername = findViewById(R.id.editTextProxyUsername);
        final EditText editTextProxyPassword = findViewById(R.id.editTextProxyPassword);
        final EditText editTextMailboxHostname = findViewById(R.id.editTextMailboxHostname);
        final EditText editTextMailboxPort = findViewById(R.id.editTextMailboxPort);
        final EditText editTextMailboxUsername = findViewById(R.id.editTextMailboxUsername);
        final EditText editTextMailboxPassword = findViewById(R.id.editTextMailboxPassword);

        int pop3Port = Integer.parseInt(editTextPop3Port.getText().toString());
        int smtpPort = Integer.parseInt(editTextSmtpPort.getText().toString());
        String proxyUsername = editTextProxyUsername.getText().toString();
        String proxyPassword = editTextProxyPassword.getText().toString();
        String mailboxHostname = editTextMailboxHostname.getText().toString();
        int mailboxPort = Integer.parseInt(editTextMailboxPort.getText().toString());
        String mailboxUsername = editTextMailboxUsername.getText().toString();
        String mailboxPassword = editTextMailboxPassword.getText().toString();

        Config.setIntValue(R.string.key_proxy_pop3_port, pop3Port, context);
        Config.setIntValue(R.string.key_proxy_smtp_port, smtpPort, context);
        Config.setStringValue(R.string.key_proxy_username, proxyUsername, context);
        Config.setStringValue(R.string.key_proxy_password, proxyPassword, context);
        Config.setStringValue(R.string.key_mailbox_hostname, mailboxHostname, context);
        Config.setIntValue(R.string.key_mailbox_port, mailboxPort, context);
        Config.setStringValue(R.string.key_mailbox_username, mailboxUsername, context);
        Config.setStringValue(R.string.key_mailbox_password, mailboxPassword, context);

        setResult(Activity.RESULT_OK);
        finish();
    }
}
