package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class Pop3ServerTest {

    private int pop3Port = 27000;
    private String username = "proxyuser";
    private String password = "12345";
    private String host = "localhost";
    private Pop3Server pop3Server;

    @Mock
    private DBQuery dbQuery;

    @Before
    public void before() throws Exception  {
        pop3Port = 27000;
        username = "proxyuser";
        password = "12345";
        host = "localhost";

        pop3Server = new Pop3Server(pop3Port, username, password, dbQuery);
        pop3Server.start();
    }

    @After
    public void after() throws Exception  {
        pop3Server.stop();
    }

    @Test
    public void retrSessionTest() throws Exception {
        String msg1Body = "hello" + Pop3Server.CRLF;
        String msg2Body = "bye" + Pop3Server.CRLF;
        AssembledMessage msg1 = new AssembledMessage("1", msg1Body.getBytes());
        AssembledMessage msg2 = new AssembledMessage("2", msg2Body.getBytes());

        List<AssembledMessage> msgList = new ArrayList<AssembledMessage>();
        msgList.add(msg1);
        msgList.add(msg2);

        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        pop3Server.setDbQuery(dbQuery);

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);

        // USER & PASS
        boolean loginSuccessful = pop3Client.login(username, password);

        assertThat(loginSuccessful, is(equalTo(true)));

        // STAT
        POP3MessageInfo status = pop3Client.status();

        int expectedNumber = msgList.size();
        int expectedSize = 0;
        for (AssembledMessage msg : msgList) {
            expectedSize += msg.messageBody.length;
        }

        assertThat(status.number, is(equalTo(expectedNumber)));
        assertThat(status.size, is(equalTo(expectedSize)));

        // LIST
        POP3MessageInfo[] msgInfo = pop3Client.listMessages();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].size, is(equalTo(msgList.get(i).messageBody.length)));
        }

        // UIDL
        msgInfo = pop3Client.listUniqueIdentifiers();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].identifier, is(equalTo(msgList.get(i).uuid)));
        }

        // RETR
        List<String> retrievedMessages = new ArrayList<String>();
        for (int i = 1; i <= msgList.size(); i++) {
            BufferedReader reader = (BufferedReader) pop3Client.retrieveMessage(i);
            retrievedMessages.add(IOUtils.toString(reader));
        }

        assertThat(retrievedMessages.size(), is(equalTo(msgList.size())));
        for (int i = 0; i < retrievedMessages.size(); i++) {
            String expectedMsg = new String(msgList.get(i).messageBody);
            assertThat(retrievedMessages.get(i), is(equalTo(expectedMsg)));
        }

        pop3Client.logout();

        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }

    @Test
    public void topSessionTest() throws Exception {
        int topLines = 10;

        StringBuilder fullMessage = new StringBuilder();
        StringBuilder expectedTopMessageSb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            String line = "header" + Pop3Server.CRLF;
            fullMessage.append(line);
            expectedTopMessageSb.append(line);
        }
        fullMessage.append(Pop3Server.CRLF);
        expectedTopMessageSb.append(Pop3Server.CRLF);
        for (int i = 0; i < 20; i++) {
            String line = "body" + Pop3Server.CRLF;
            fullMessage.append(line);
            if (i < topLines) {
                expectedTopMessageSb.append(line);
            }
        }

        String expectedTopMessage = expectedTopMessageSb.toString();

        List<AssembledMessage> msgList = new ArrayList<AssembledMessage>();
        msgList.add(new AssembledMessage("1", fullMessage.toString().getBytes()));

        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        pop3Server.setDbQuery(dbQuery);

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);

        // USER & PASS
        boolean loginSuccessful = pop3Client.login(username, password);

        assertThat(loginSuccessful, is(equalTo(true)));

        // STAT
        POP3MessageInfo status = pop3Client.status();

        int expectedNumber = msgList.size();
        int expectedSize = 0;
        for (AssembledMessage msg : msgList) {
            expectedSize += msg.messageBody.length;
        }

        assertThat(status.number, is(equalTo(expectedNumber)));
        assertThat(status.size, is(equalTo(expectedSize)));

        // LIST
        POP3MessageInfo[] msgInfo = pop3Client.listMessages();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].size, is(equalTo(msgList.get(i).messageBody.length)));
        }

        // UIDL
        msgInfo = pop3Client.listUniqueIdentifiers();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].identifier, is(equalTo(msgList.get(i).uuid)));
        }

        // TOP
        List<String> retrievedMessages = new ArrayList<String>();
        for (int i = 1; i <= msgList.size(); i++) {
            BufferedReader reader = (BufferedReader) pop3Client.retrieveMessageTop(i, topLines);
            retrievedMessages.add(IOUtils.toString(reader));
        }

        assertThat(retrievedMessages.size(), is(equalTo(msgList.size())));
        for (int i = 0; i < retrievedMessages.size(); i++) {
            assertThat(retrievedMessages.get(i), is(equalTo(expectedTopMessage)));
        }

        pop3Client.logout();

        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }

    @Test
    public void deleSessionTest() throws Exception {
        String msg1Body = "hello" + Pop3Server.CRLF;
        String msg2Body = "bye" + Pop3Server.CRLF;
        AssembledMessage msg1 = new AssembledMessage("1", msg1Body.getBytes());
        AssembledMessage msg2 = new AssembledMessage("2", msg2Body.getBytes());

        List<AssembledMessage> msgList = new ArrayList<AssembledMessage>();
        msgList.add(msg1);
        msgList.add(msg2);

        doNothing().when(dbQuery).deleteAssembledMessage(msg1.uuid);
        doNothing().when(dbQuery).deleteAssembledMessage(msg2.uuid);
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        pop3Server.setDbQuery(dbQuery);

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);

        // USER & PASS
        boolean loginSuccessful = pop3Client.login(username, password);

        assertThat(loginSuccessful, is(equalTo(true)));

        // STAT
        POP3MessageInfo status = pop3Client.status();

        int expectedNumber = msgList.size();
        int expectedSize = 0;
        for (AssembledMessage msg : msgList) {
            expectedSize += msg.messageBody.length;
        }

        assertThat(status.number, is(equalTo(expectedNumber)));
        assertThat(status.size, is(equalTo(expectedSize)));

        // LIST
        POP3MessageInfo[] msgInfo = pop3Client.listMessages();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].size, is(equalTo(msgList.get(i).messageBody.length)));
        }

        // UIDL
        msgInfo = pop3Client.listUniqueIdentifiers();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].identifier, is(equalTo(msgList.get(i).uuid)));
        }

        // DELE
        for (int i = 1; i <= msgList.size(); i++) {
            pop3Client.deleteMessage(i);
        }

        pop3Client.logout();

        verify(dbQuery, times(1)).deleteAssembledMessage(msg1.uuid);
        verify(dbQuery, times(1)).deleteAssembledMessage(msg2.uuid);
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }

    @Test
    public void rsetSessionTest() throws Exception {
        String msg1Body = "hello" + Pop3Server.CRLF;
        String msg2Body = "bye" + Pop3Server.CRLF;
        AssembledMessage msg1 = new AssembledMessage("1", msg1Body.getBytes());
        AssembledMessage msg2 = new AssembledMessage("2", msg2Body.getBytes());

        List<AssembledMessage> msgList = new ArrayList<AssembledMessage>();
        msgList.add(msg1);
        msgList.add(msg2);

        doNothing().when(dbQuery).deleteAssembledMessage(msg1.uuid);
        doNothing().when(dbQuery).deleteAssembledMessage(msg2.uuid);
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        pop3Server.setDbQuery(dbQuery);

        POP3Client pop3Client = new POP3Client();

        pop3Client.connect(host, pop3Port);

        // USER & PASS
        boolean loginSuccessful = pop3Client.login(username, password);

        assertThat(loginSuccessful, is(equalTo(true)));

        // STAT
        POP3MessageInfo status = pop3Client.status();

        int expectedNumber = msgList.size();
        int expectedSize = 0;
        for (AssembledMessage msg : msgList) {
            expectedSize += msg.messageBody.length;
        }

        assertThat(status.number, is(equalTo(expectedNumber)));
        assertThat(status.size, is(equalTo(expectedSize)));

        // LIST
        POP3MessageInfo[] msgInfo = pop3Client.listMessages();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].size, is(equalTo(msgList.get(i).messageBody.length)));
        }

        // UIDL
        msgInfo = pop3Client.listUniqueIdentifiers();

        expectedNumber = msgList.size();

        assertThat(msgInfo.length, is(equalTo(expectedNumber)));
        for (int i = 0; i < msgInfo.length; i++) {
            assertThat(msgInfo[i].number, is(equalTo(i + 1)));
            assertThat(msgInfo[i].identifier, is(equalTo(msgList.get(i).uuid)));
        }

        // DELE
        for (int i = 1; i <= msgList.size(); i++) {
            pop3Client.deleteMessage(i);
        }

        // RSET
        pop3Client.reset();

        pop3Client.logout();

        verify(dbQuery, never()).deleteAssembledMessage(anyString());
        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }

    @Test
    public void multipleSessionsTest() throws Exception {
        List<AssembledMessage> msgList = new ArrayList<AssembledMessage>();

        doNothing().when(dbQuery).deleteAssembledMessage(anyString());
        when(dbQuery.getAssembledMessages()).thenReturn(msgList);

        pop3Server.setDbQuery(dbQuery);

        POP3Client pop3Client = new POP3Client();
        boolean loginSuccessful = false;
        POP3MessageInfo msgInfo = null;

        pop3Client.connect(host, pop3Port);
        loginSuccessful = pop3Client.login(username, password);
        assertThat(loginSuccessful, is(equalTo(true)));
        msgInfo = pop3Client.status();
        assertThat(msgInfo, is(notNullValue()));
        pop3Client.logout();

        pop3Client.connect(host, pop3Port);
        loginSuccessful = pop3Client.login(username, password);
        assertThat(loginSuccessful, is(equalTo(true)));
        msgInfo = pop3Client.status();
        assertThat(msgInfo, is(notNullValue()));
        pop3Client.logout();

        pop3Client.connect(host, pop3Port);
        loginSuccessful = pop3Client.login(username, password);
        assertThat(loginSuccessful, is(equalTo(true)));
        msgInfo = pop3Client.status();
        assertThat(msgInfo, is(notNullValue()));
        pop3Client.logout();

        verifyNoMoreInteractions(ignoreStubs(dbQuery));
    }
}
