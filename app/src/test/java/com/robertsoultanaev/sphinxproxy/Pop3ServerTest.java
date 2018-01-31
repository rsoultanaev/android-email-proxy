package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class Pop3ServerTest {

    @Mock
    private DBQuery dbQuery;

    private List<AssembledMessage> msgList;
    private int pop3Port;
    private String username;
    private String password;
    private String host;

    @Before
    public void setup() throws Exception {
        String msg1Body = "hello";
        String msg2Body = "bye";
        AssembledMessage msg1 = new AssembledMessage("1", msg1Body.getBytes());
        AssembledMessage msg2 = new AssembledMessage("2", msg2Body.getBytes());

        msgList = new ArrayList<AssembledMessage>();
        msgList.add(msg1);
        msgList.add(msg2);

        pop3Port = 27000;
        username = "proxyuser";
        password = "12345";
        host = "localhost";
    }

    @Test
    public void statTest() throws Exception {
        doNothing().when(dbQuery).deleteAssembledMessage(anyString());
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        Pop3Server pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
        pop3Server.start();

        Thread.sleep(1000);

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);
        pop3Client.login(username, password);


        POP3MessageInfo status = pop3Client.status();
        pop3Client.logout();

        int expectedNumber = msgList.size();
        int expectedSize = 0;
        for (AssembledMessage msg : msgList) {
            expectedSize += msg.messageBody.length;
        }

        assertThat(status.number, is(equalTo(expectedNumber)));
        assertThat(status.size, is(equalTo(expectedSize)));

        pop3Server.stop();
    }

    @Test
    public void listTest() throws Exception {
        doNothing().when(dbQuery).deleteAssembledMessage(anyString());
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        Pop3Server pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
        pop3Server.start();

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);
        pop3Client.login(username, password);


        POP3MessageInfo[] msgInfo = pop3Client.listMessages();

        pop3Server.stop();

        int expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].size, is(equalTo(msgList.get(i).messageBody.length)));
        }
    }

    @Test
    public void uidlTest() throws Exception {
        doNothing().when(dbQuery).deleteAssembledMessage(anyString());
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        Pop3Server pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
        pop3Server.start();

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);
        pop3Client.login(username, password);


        POP3MessageInfo[] msgInfo = pop3Client.listUniqueIdentifiers();

        pop3Server.stop();

        int expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].identifier, is(equalTo(msgList.get(i).uuid)));
        }
    }

    @Test
    public void retrTest() throws Exception {
        doNothing().when(dbQuery).deleteAssembledMessage(anyString());
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        Pop3Server pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
        pop3Server.start();

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);
        pop3Client.login(username, password);


        List<String> retrievedMessages = new ArrayList<String>();
        for (int i = 0; i < msgList.size(); i++) {
            BufferedReader reader = (BufferedReader) pop3Client.retrieveMessage(i);
            retrievedMessages.add(IOUtils.toString(reader));
        }

        pop3Server.stop();

        assertThat(retrievedMessages.size(), is(equalTo(msgList.size())));
        for (int i = 0; i < retrievedMessages.size(); i++) {
            String expectedMsg = new String(msgList.get(i).messageBody);
            assertThat(retrievedMessages.get(i), is(equalTo(expectedMsg)));
        }
    }
}
