package com.rsoultanaev.sphinxproxy.server;

/**
 * Created by rsoultanaev on 30/12/17.
 */

public class SmtpMessage {
    private final byte[] data;
    private final String sender;
    private final String receiver;

    public SmtpMessage(byte[] data, String sender, String receiver) {
        this.data = data;
        this.sender = sender;
        this.receiver = receiver;
    }

    public byte[] getData() {
        return data;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }
}
