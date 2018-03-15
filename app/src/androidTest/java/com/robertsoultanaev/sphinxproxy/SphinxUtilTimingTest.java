package com.robertsoultanaev.sphinxproxy;

import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.javasphinx.ECCGroup;
import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
public class SphinxUtilTimingTest {

    @Test
    public void timeSplittingIntoPackets() throws Exception {
        int bodyLength = 1024;
        SphinxParams params = new SphinxParams(16, bodyLength, 192, new ECCGroup());

        int repetitions = 100;

        int[] testSizes = {1000, 3000, 5000, 7000, 9000};
        int totalMixes = 10;

        int basePort = 8000;
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();

        for (int i = 0; i < totalMixes; i++) {
            BigInteger priv = params.getGroup().genSecret();
            ECPoint pub = params.getGroup().expon(params.getGroup().getGenerator(), priv);

            int port = basePort + i;
            String host = "host" + Integer.toString(i);
            nodeList.add(new MixNode(i, host, port, Hex.toHexString(pub.getEncoded(true))));
        }

        byte[][] testEmails = new byte[testSizes.length][];
        for (int i = 0; i < testSizes.length; i++) {
            testEmails[i] = genRandomAlphanumericString(testSizes[i]).getBytes();
        }

        int numUseMixes = 5;
        String recipient = "mort@rsoultanaev.com";
        SphinxUtil sphinxUtil = new SphinxUtil(nodeList, numUseMixes, params);
        for (int i = 0; i < testSizes.length; i++) {
            long startTime = System.nanoTime();
            for (int j = 0; j < repetitions; j++) {
                sphinxUtil.splitIntoSphinxPackets(testEmails[i], recipient);
            }
            long endTime = System.nanoTime();

            long timeTaken = (endTime - startTime) / repetitions;
            long timeTakenMillis = timeTaken / 1000000;

            System.out.println("Email length: " + testEmails[i].length);
            System.out.println("Time taken to encrypt: " + timeTakenMillis + "ms");
        }

        int[] testMixNums = {3, 5, 7};
        byte[] testEmail = testEmails[2];
        System.out.println("Test email size: " + testEmail.length);
        for (int i = 0; i < testMixNums.length; i++) {
            SphinxUtil testSphinxUtil = new SphinxUtil(nodeList, testMixNums[i], params);

            long startTime = System.nanoTime();
            for (int j = 0; j < repetitions; j++) {
                testSphinxUtil.splitIntoSphinxPackets(testEmail, recipient);
            }
            long endTime = System.nanoTime();

            long timeTaken = (endTime - startTime) / repetitions;
            long timeTakenMillis = timeTaken / 1000000;

            System.out.println("Number of mixes used: " + testMixNums[i]);
            System.out.println("Time taken to encrypt: " + timeTakenMillis + "ms");
        }

        int[] testBodyLengths = {1024, 4096, 8192};
        for (int i = 0; i < testBodyLengths.length; i++) {
            SphinxParams testParams = new SphinxParams(16, testBodyLengths[i], 192, new ECCGroup());
            SphinxUtil testSphinxUtil = new SphinxUtil(nodeList, numUseMixes, testParams);
            System.out.println("Body length used: " + testBodyLengths[i]);

            for (int j = 0; j < testEmails.length; j++) {
                long startTime = System.nanoTime();
                for (int k = 0; k < repetitions; k++) {
                    testSphinxUtil.splitIntoSphinxPackets(testEmails[j], recipient);
                }
                long endTime = System.nanoTime();

                long timeTaken = (endTime - startTime) / repetitions;
                long timeTakenMillis = timeTaken / 1000000;

                System.out.println("Email length used: " + testEmails[j].length);
                System.out.println("Time taken to encrypt: " + timeTakenMillis + "ms");
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
