package com.robertsoultanaev.sphinxproxy;

import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.javasphinx.ECCGroup;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
public class SendingTimingTest {

    @Test
    public void timeSendingMessages() throws Exception {
        int[] testSizes = {500, 1000, 3000, 5000, 7000, 9000};

        String mixHost = "";
        int mixPort = 0;
        String encodedMixPubKey = "";
        ArrayList<MixNode> mixNodeList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mixNodeList.add(new MixNode(i, mixHost, mixPort, encodedMixPubKey));
        }

        int numUseMixes = 3;
        SphinxUtil sphinxUtil = new SphinxUtil(mixNodeList, numUseMixes);
        
        String recipientAddress = "mort@rsoultanaev.com";
        ArrayList<SphinxPacketWithRouting[]> testMessages = new ArrayList<>();
        for (int testSize : testSizes) {
            String emailStr = genRandomAlphanumericString(testSize);
            byte[] email = emailStr.getBytes();
            SphinxPacketWithRouting[] sphinxPackets = sphinxUtil.splitIntoSphinxPackets(email, recipientAddress);
            testMessages.add(sphinxPackets);
        }

        int repetitions = 100;

        for (int i = 0; i < testSizes.length; i++) {
            int testSize = testSizes[i];
            SphinxPacketWithRouting[] testMessage = testMessages.get(i);

            long startTime = System.nanoTime();
            for (int j = 0; j < repetitions; j++) {
                sendSphinxSequence(testMessage);
            }
            long endTime = System.nanoTime();

            long timeTaken = (endTime - startTime) / repetitions;
            long timeTakenMillis = timeTaken / 1000000;

            System.out.println("Input size: " + testSize);
            System.out.println("Time taken to send sequence: " + timeTakenMillis + "ms");
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

    private void sendSphinxSequence(SphinxPacketWithRouting[] packets) throws Exception {
        for (SphinxPacketWithRouting packet : packets) {
            String hostname = packet.firstNodeAddress.getHostName();
            int port = packet.firstNodeAddress.getPort();
            byte[] message = packet.binMessage;

            Socket clientSocket = new Socket(hostname, port);
            OutputStream outToServer = clientSocket.getOutputStream();
            outToServer.write(message);
            clientSocket.close();
        }
    }
}
