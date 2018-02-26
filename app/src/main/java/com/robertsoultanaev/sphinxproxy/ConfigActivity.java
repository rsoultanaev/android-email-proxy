package com.robertsoultanaev.sphinxproxy;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
        final EditText editTextNumUseMixes = findViewById(R.id.editTextNumUseMixes);

        String inputPop3Port = editTextPop3Port.getText().toString();
        String inputSmtpPort = editTextSmtpPort.getText().toString();
        String inputProxyUsername = editTextProxyUsername.getText().toString();
        String inputProxyPassword = editTextProxyPassword.getText().toString();
        String inputMailboxHostname = editTextMailboxHostname.getText().toString();
        String inputMailboxPort = editTextMailboxPort.getText().toString();
        String inputMailboxUsername = editTextMailboxUsername.getText().toString();
        String inputMailboxPassword = editTextMailboxPassword.getText().toString();
        String inputNumUseMixes = editTextNumUseMixes.getText().toString();

        int pop3Port;
        try {
            pop3Port = Integer.parseInt(inputPop3Port);
        } catch (NumberFormatException ex) {
            Toast.makeText(context, "Please enter a number for the POP3 port", Toast.LENGTH_SHORT).show();
            return;
        }

        int smtpPort;
        try {
            smtpPort = Integer.parseInt(inputSmtpPort);
        } catch (NumberFormatException ex) {
            Toast.makeText(context, "Please enter a number for the SMTP port", Toast.LENGTH_SHORT).show();
            return;
        }

        String proxyUsername;
        if (!inputProxyUsername.isEmpty()) {
            proxyUsername = inputProxyUsername;
        } else {
            Toast.makeText(context, "Please enter the proxy username", Toast.LENGTH_SHORT).show();
            return;
        }

        String proxyPassword;
        if (!inputProxyPassword.isEmpty()) {
            proxyPassword = inputProxyPassword;
        } else {
            Toast.makeText(context, "Please enter the proxy password", Toast.LENGTH_SHORT).show();
            return;
        }

        String mailboxHostname;
        if (!inputMailboxHostname.isEmpty()) {
            mailboxHostname = inputMailboxHostname;
        } else {
            Toast.makeText(context, "Please enter the mailbox hostname", Toast.LENGTH_SHORT).show();
            return;
        }

        int mailboxPort;
        try {
            mailboxPort = Integer.parseInt(inputMailboxPort);
        } catch (NumberFormatException ex) {
            Toast.makeText(context, "Please enter a number for the mailbox port", Toast.LENGTH_SHORT).show();
            return;
        }

        String mailboxUsername;
        if (!inputMailboxUsername.isEmpty()) {
            mailboxUsername = inputMailboxUsername;
        } else {
            Toast.makeText(context, "Please enter the mailbox username", Toast.LENGTH_SHORT).show();
            return;
        }

        String mailboxPassword;
        if (!inputMailboxPassword.isEmpty()) {
            mailboxPassword = inputMailboxPassword;
        } else {
            Toast.makeText(context, "Please enter the mailbox password", Toast.LENGTH_SHORT).show();
            return;
        }

        int numUseMixes;
        try {
            numUseMixes = Integer.parseInt(inputNumUseMixes);
        } catch (NumberFormatException ex) {
            Toast.makeText(context, "Please enter a number for the number of mixes used", Toast.LENGTH_SHORT).show();
            return;
        }

        int numTotalMixes = Config.getIntValue(R.string.key_num_total_mixes, context);
        if (numUseMixes > numTotalMixes) {
            Toast.makeText(context, "Number of mixes to be used cannot exceed the total number of mixes: " + numTotalMixes, Toast.LENGTH_SHORT).show();
            return;
        }

        Config.setIntValue(R.string.key_proxy_pop3_port, pop3Port, context);
        Config.setIntValue(R.string.key_proxy_smtp_port, smtpPort, context);
        Config.setStringValue(R.string.key_proxy_username, proxyUsername, context);
        Config.setStringValue(R.string.key_proxy_password, proxyPassword, context);
        Config.setStringValue(R.string.key_mailbox_hostname, mailboxHostname, context);
        Config.setIntValue(R.string.key_mailbox_port, mailboxPort, context);
        Config.setStringValue(R.string.key_mailbox_username, mailboxUsername, context);
        Config.setStringValue(R.string.key_mailbox_password, mailboxPassword, context);
        Config.setIntValue(R.string.key_num_use_mixes, numUseMixes, context);

        setResult(Activity.RESULT_OK);
        finish();
    }
}
