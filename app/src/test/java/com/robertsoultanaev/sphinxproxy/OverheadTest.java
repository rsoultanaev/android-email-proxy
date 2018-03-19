package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.ECCGroup;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;
import com.robertsoultanaev.sphinxproxy.database.Recipient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OverheadTest {

    private class MeasuringMockAsyncTcpClient extends AsyncTcpClient {

        public int bytesSent;

        public MeasuringMockAsyncTcpClient() {
            bytesSent = 0;
        }

        @Override
        public void sendMessage(final SphinxPacketWithRouting sphinxPacketWithRouting) {
            bytesSent += sphinxPacketWithRouting.binMessage.length;
        }
    }

    @Mock
    private DBQuery dbQuery;

    @Test
    public void overheadTest() throws Exception {
        String recipientAddress = "mort@rsoultanaev.com";
        String encodedRecipientPublicKey = "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEI285LXjIgSV1pzc3STEtssSXo3hyQBJkHQl8O7TCvfIfykEUKZT27dXkhi0WAHU9T/hqL4kxzkM=";
        Recipient recipient = new Recipient(recipientAddress, encodedRecipientPublicKey);

        String from = "robert@sphinx.com";

        ArrayList<MixNode> mixNodeList = new ArrayList<>();
        mixNodeList.add(new MixNode(0, "host0", 8000, "02a99620da322bafd470d43087e217871f8aa47e9f310c1f64d1483f30"));
        mixNodeList.add(new MixNode(1, "host1", 8001, "034d85c1ba3ca7b17a10e16f6ba5287bc7bbe3ec23986ceb40ac855bfa"));
        mixNodeList.add(new MixNode(2, "host2", 8002, "02d1401530d832765ec664a9fb42af4f5f4396efc4559a583c13afb986"));
        mixNodeList.add(new MixNode(3, "host3", 8003, "02eaadcd423c8fd8cc2164e2fb5f5a3a7342013ff1ec78cce5ce8cd7e1"));
        mixNodeList.add(new MixNode(4, "host4", 8004, "03f3cd8c37977599a9a32a5b653aef617ddf89fe3367053077a934e39d"));

        when(dbQuery.getRecipient(recipientAddress)).thenReturn(recipient);

        int numUseMixes = 3;

        int largeTestSize = 200000;
        int[] testSizes = {500, 1000, 3000, 5000, 7000, 9000};
        int[] testBodyLengths = {1024, 4096, 8192};

        EndToEndCrypto endToEndCrypto = new EndToEndCrypto();

        for (int testBodyLength : testBodyLengths) {
            // Overall overhead
            String testLargeEmailStr = genRandomAlphanumericString(largeTestSize);
            byte[] testLargeEmail = testLargeEmailStr.getBytes();
            InputStream testLargeEmailStream = new ByteArrayInputStream(testLargeEmail);

            SphinxParams testParams = new SphinxParams(16, testBodyLength, 192, new ECCGroup());
            SphinxUtil sphinxUtil = new SphinxUtil(mixNodeList, numUseMixes, testParams);
            MeasuringMockAsyncTcpClient largeEmailOverheadClient = new MeasuringMockAsyncTcpClient();
            SmtpMessageHandler largeEmailOverheadHandler = new SmtpMessageHandler(sphinxUtil, largeEmailOverheadClient, dbQuery, endToEndCrypto);
            largeEmailOverheadHandler.deliver(from, recipientAddress, testLargeEmailStream);

            double overhead = (double) largeEmailOverheadClient.bytesSent / testLargeEmail.length;

            System.out.println("Body length: " + testBodyLength);
            System.out.println("Email size: " + largeTestSize);
            System.out.println("Overhead: " + overhead);

            for (int testSize : testSizes) {
                String emailStr = genRandomAlphanumericString(testSize);
                byte[] email = emailStr.getBytes();
                InputStream emailStream = new ByteArrayInputStream(email);

                MeasuringMockAsyncTcpClient asyncTcpClient = new MeasuringMockAsyncTcpClient();
                SmtpMessageHandler smtpMessageHandler = new SmtpMessageHandler(sphinxUtil, asyncTcpClient, dbQuery, endToEndCrypto);
                smtpMessageHandler.deliver(from, recipientAddress, emailStream);

                overhead = (double) asyncTcpClient.bytesSent / email.length;

                System.out.println("Email size: " + testSize);
                System.out.println("Overhead: " + overhead);
            }
        }
    }

    private String genRandomAlphanumericString(int targetLength) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder result = new StringBuilder();

        Random random = new Random();
        while (result.length() < targetLength) {
            int index = (int) (random.nextFloat() * chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();

    }

}
