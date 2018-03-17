package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.subethamail.smtp.client.SmartClient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.subethamail.smtp.util.TextUtils;

@RunWith(AndroidJUnit4.class)
public class ProxyServiceTest {
    @Test
    public void test() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        Config.setIntValue(R.string.key_proxy_pop3_port, 27000, context);
        Config.setIntValue(R.string.key_proxy_smtp_port, 28000, context);
        Config.setStringValue(R.string.key_proxy_username, "proxyuser", context);
        Config.setStringValue(R.string.key_proxy_password, "12345", context);
        Config.setStringValue(R.string.key_mailbox_hostname, "ec2-35-178-56-77.eu-west-2.compute.amazonaws.com", context);
        Config.setIntValue(R.string.key_mailbox_port, 995, context);
        Config.setStringValue(R.string.key_mailbox_username, "mort", context);
        Config.setStringValue(R.string.key_mailbox_password, "1234", context);
        Config.setIntValue(R.string.key_num_use_mixes, 5, context);

        DB db = DB.getAppDatabase(context);
        DBQuery dbQuery = db.getDao();
        dbQuery.deleteEverything();

        String mixHostname = "ec2-35-176-102-252.eu-west-2.compute.amazonaws.com";
        dbQuery.insertMixNode(new MixNode(0, mixHostname, 8000, "039035bbb28c19631d39e98c0a9e5d7d3b75a8532ff0dff4d455b5a140"));
        dbQuery.insertMixNode(new MixNode(1, mixHostname, 8001, "035580996c2b1000da10f8bbe05436c420a0cc3b8e2ac6f097cb3120ab"));
        dbQuery.insertMixNode(new MixNode(2, mixHostname, 8002, "0202e293b9c1ca204746dc24e9c8d90cfc9e607076314d5ddda4a66f57"));
        dbQuery.insertMixNode(new MixNode(3, mixHostname, 8003, "020018d994afdf4ea61f47f659cdd28dedf9de2d109cc61b4560045d90"));
        dbQuery.insertMixNode(new MixNode(4, mixHostname, 8004, "020f98055cb829c3bef0b98b3f850e5d29a873f9ac5b1a7b6aad00829a"));
        dbQuery.insertMixNode(new MixNode(5, mixHostname, 8005, "0251761bd56a0d009ac88b108b658857b1947454cd7e11b2f8324e950e"));
        dbQuery.insertMixNode(new MixNode(6, mixHostname, 8006, "0221e0397b0509218dcfbf288520a0996629432d8684bbb0a74c08b540"));
        dbQuery.insertMixNode(new MixNode(7, mixHostname, 8007, "02b7434526d247e1c7274b3e515aa13ea7ce0813497cf5134405addaab"));
        dbQuery.insertMixNode(new MixNode(8, mixHostname, 8008, "03d9f463c92bfb9ac4d0bfef955c3194d4c17eaa375f5145d84e94cc46"));
        dbQuery.insertMixNode(new MixNode(9, mixHostname, 8009, "0388a8f02dff9caf33fbc60a858fa5e3be6b57e4b44a283aeef08adeea"));

        String recipientAddress = "mort@rsoultanaev.com";
        String encodedRecipientPublicKey = "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEI285LXjIgSV1pzc3STEtssSXo3hyQBJkHQl8O7TCvfIfykEUKZT27dXkhi0WAHU9T/hqL4kxzkM=";
        Recipient recipient = new Recipient(recipientAddress, encodedRecipientPublicKey);
        dbQuery.insertRecipient(recipient);

        Intent proxyIntent = new Intent(context, ProxyService.class);
        context.startService(proxyIntent);

        Thread.sleep(2000);

        SmartClient client = new SmartClient("localhost", 28000,"localhost");
        client.from("john@example.com");
        client.to(recipientAddress);
        client.dataStart();
        client.dataWrite(TextUtils.getAsciiBytes("body"), 4);
        client.dataEnd();
        client.quit();
    }
}
