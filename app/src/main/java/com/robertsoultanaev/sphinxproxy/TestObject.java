package com.robertsoultanaev.sphinxproxy;

import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.MixNode;

import java.util.List;

public class TestObject {
    private DBQuery dbQuery;

    public TestObject(DBQuery dbQuery) {
        this.dbQuery = dbQuery;
    }

    public String doThing() {
        List<MixNode> mixNodeList = dbQuery.getMixNodes();
        StringBuilder sb = new StringBuilder();
        for (MixNode node : mixNodeList) {
            sb.append(node.port + " " + node.encodedPublicKey + "\n");
        }

        return sb.toString();
    }
}
