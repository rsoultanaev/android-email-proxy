package com.robertsoultanaev.sphinxproxy;

import java.util.Arrays;

public class SphinxPacketWithRouting {
    int firstNodeId;
    String firstNodeHost;
    int firstNodePort;
    byte[] binMessage;

    public SphinxPacketWithRouting(int firstNodeId, byte[] binMessage) {
        this.firstNodeId = firstNodeId;
        this.binMessage = binMessage;
        this.firstNodeHost = "localhost";
        this.firstNodePort = firstNodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SphinxPacketWithRouting that = (SphinxPacketWithRouting) o;

        if (firstNodeId != that.firstNodeId) return false;
        if (firstNodePort != that.firstNodePort) return false;
        if (!firstNodeHost.equals(that.firstNodeHost)) return false;
        return Arrays.equals(binMessage, that.binMessage);
    }
}
