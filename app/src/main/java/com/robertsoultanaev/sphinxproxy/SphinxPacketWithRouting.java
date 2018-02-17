package com.robertsoultanaev.sphinxproxy;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class SphinxPacketWithRouting {
    int firstNodeId;
    InetSocketAddress firstNodeAddress;
    byte[] binMessage;

    public SphinxPacketWithRouting(int firstNodeId, InetSocketAddress firstNodeAddress, byte[] binMessage) {
        this.firstNodeId = firstNodeId;
        this.firstNodeAddress = firstNodeAddress;
        this.binMessage = binMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SphinxPacketWithRouting that = (SphinxPacketWithRouting) o;

        if (firstNodeId != that.firstNodeId) return false;
        if (!firstNodeAddress.equals(that.firstNodeAddress)) return false;
        return Arrays.equals(binMessage, that.binMessage);
    }
}
