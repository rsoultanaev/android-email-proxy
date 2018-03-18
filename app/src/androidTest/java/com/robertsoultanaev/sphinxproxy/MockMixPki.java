package com.robertsoultanaev.sphinxproxy;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class MockMixPki {
    BigInteger priv;
    ECPoint pub;

    public MockMixPki(BigInteger priv, ECPoint pub) {
        this.priv = priv;
        this.pub = pub;
    }
}
