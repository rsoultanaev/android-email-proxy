package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;

import org.apache.commons.net.pop3.POP3Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MailboxTest {

    @Mock
    private DBQuery dbQuery;

    @Mock
    private POP3Client pop3Client;

    @Test
    public void test() throws Exception {
    }
}

