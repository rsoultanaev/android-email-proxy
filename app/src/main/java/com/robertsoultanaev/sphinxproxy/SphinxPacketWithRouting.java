package com.robertsoultanaev.sphinxproxy;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class SphinxPacketWithRouting {
    InetSocketAddress firstNodeAddress;
    byte[] binMessage;

    public SphinxPacketWithRouting(InetSocketAddress firstNodeAddress, byte[] binMessage) {
        this.firstNodeAddress = firstNodeAddress;
        this.binMessage = binMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SphinxPacketWithRouting that = (SphinxPacketWithRouting) o;

        if (firstNodeAddress != null ? !firstNodeAddress.equals(that.firstNodeAddress) : that.firstNodeAddress != null)
            return false;
        return Arrays.equals(binMessage, that.binMessage);
    }
}
