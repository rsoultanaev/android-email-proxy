package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.javasphinx.SphinxParams;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SphinxUtilTimingTest {

    class PkiEntry {
        BigInteger priv;
        ECPoint pub;

        public PkiEntry(BigInteger priv, ECPoint pub) {
            this.priv = priv;
            this.pub = pub;
        }
    }

    @Mock
    private DBQuery dbQuery;

    @Test
    public void timeSplittingIntoPackets() throws Exception {
        int repetitions = 1000;
        int emailSize = 10000;

        SphinxParams params = new SphinxParams();

        int basePort = 8000;
        HashMap<Integer, PkiEntry> pki = new HashMap<Integer, PkiEntry>();
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();

        for (int i = 0; i < 3; i++) {
            BigInteger priv = params.getGroup().genSecret();
            ECPoint pub = params.getGroup().expon(params.getGroup().getGenerator(), priv);

            int port = basePort + i;
            String host = "host" + Integer.toString(i);
            pki.put(i,  new PkiEntry(priv, pub));
            nodeList.add(new MixNode(i, host, port, Hex.toHexString(pub.getEncoded(true))));
        }

        when(dbQuery.getMixNodes()).thenReturn(nodeList);

        SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);

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
