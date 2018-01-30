package com.robertsoultanaev.sphinxproxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class SmtpServerTest {

    @Mock
    private DBQuery dbQuery;

    @Test
    public void testTest() {
        MixNode node1 = new MixNode(1, "123");
        MixNode node2 = new MixNode(2, "456");

        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();
        nodeList.add(node1);
        nodeList.add(node2);

        String s = "1 123\n2 456\n";

        when(dbQuery.getMixNodes()).thenReturn(nodeList);
        TestObject t = new TestObject(dbQuery);
        String res = t.doThing();

        assertThat(res, is(s));
    }
}
