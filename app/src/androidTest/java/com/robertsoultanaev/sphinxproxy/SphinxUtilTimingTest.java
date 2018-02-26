package com.robertsoultanaev.sphinxproxy;

import android.support.test.runner.AndroidJUnit4;

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
        SphinxParams params = new SphinxParams();

        int repetitions = 100;

        int emailSize = 1000;
        int numUseMixes = 5;

        int basePort = 8000;
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();

        for (int i = 0; i < numUseMixes; i++) {
            BigInteger priv = params.getGroup().genSecret();
            ECPoint pub = params.getGroup().expon(params.getGroup().getGenerator(), priv);

            int port = basePort + i;
            String host = "host" + Integer.toString(i);
            nodeList.add(new MixNode(i, host, port, Hex.toHexString(pub.getEncoded(true))));
        }

        SphinxUtil sphinxUtil = new SphinxUtil(nodeList, numUseMixes);

        String emailStr = genRandomAlphanumericString(emailSize);
        byte[] email = emailStr.getBytes();
        String recipient = "mort@rsoultanaev.com";

        long startTime = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            sphinxUtil.splitIntoSphinxPackets(email, recipient);
        }
        long endTime = System.nanoTime();

        long timeTaken = (endTime - startTime) / repetitions;
        long timeTakenMillis = timeTaken / 1000000;

        System.out.println("Time taken per split: " + timeTakenMillis + "ms");
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
