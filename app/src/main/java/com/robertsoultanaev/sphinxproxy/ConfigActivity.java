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

        String pop3Port = Config.getKey(R.string.key_proxy_pop3_port, context);
        String smtpPort = Config.getKey(R.string.key_proxy_smtp_port, context);

        final EditText editTextPop3Port = findViewById(R.id.editTextPop3Port);
        final EditText editTextSmtpPort = findViewById(R.id.editTextSmtpPort);

        editTextPop3Port.setText(pop3Port);
        editTextSmtpPort.setText(smtpPort);

//        Config.getKey(R.string.key_proxy_username, context);
//        Config.getKey(R.string.key_proxy_password, context);
//        Config.getKey(R.string.key_mailbox_hostname, context);
//        Config.getKey(R.string.key_mailbox_port, context);
//        Config.getKey(R.string.key_mailbox_username, context);
//        Config.getKey(R.string.key_mailbox_password, context);
    }

    public void saveConfig(View view) {
        final Context context = getApplicationContext();

        final EditText editTextPop3Port = findViewById(R.id.editTextPop3Port);
        final EditText editTextSmtpPort = findViewById(R.id.editTextSmtpPort);

        String pop3Port = editTextPop3Port.getText().toString();
        Config.setKey(R.string.key_proxy_pop3_port, pop3Port, context);

        String smtpPort = editTextSmtpPort.getText().toString();
        Config.setKey(R.string.key_proxy_smtp_port, smtpPort, context);

        setResult(Activity.RESULT_OK);
        finish();
    }
}
