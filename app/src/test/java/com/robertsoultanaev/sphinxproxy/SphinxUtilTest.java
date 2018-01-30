package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class SphinxUtilTest {

    @Mock
    private DBQuery dbQuery;

    @Test
    public void splitIntoSphinxPacketsTest() throws Exception {
        MixNode node1 = new MixNode(8000, "0221ad76d2fd0ec503ff72f55e5afb93605f8133c947b800bb2d386e5c");
        MixNode node2 = new MixNode(8001, "037d9959a0ce756953cfad237efa3531a23f297e2b66901862deefdd58");
        MixNode node3 = new MixNode(8002, "033e8682257991788cb911f54661f4f71ba769ae460f66a3c8f515447d");
        ArrayList<MixNode> nodeList = new ArrayList<MixNode>();
        nodeList.add(node1);
        nodeList.add(node2);
        nodeList.add(node3);

        when(dbQuery.getMixNodes()).thenReturn(nodeList);

        SphinxUtil sphinxUtil = new SphinxUtil(dbQuery);

        StringBuilder sb = new StringBuilder();
        while (sb.length() < 1500) {
            sb.append("0123456789");
        }
        byte[] email = sb.toString().getBytes();
        String recipient = "mort@rsoultanaev.com";

        sphinxUtil.splitIntoSphinxPackets(email, recipient);

        assertThat(true, is(false));
    }

    @Test
    public void assemblePacketsTest() throws Exception {
        assertThat(true, is(false));
    }
}
