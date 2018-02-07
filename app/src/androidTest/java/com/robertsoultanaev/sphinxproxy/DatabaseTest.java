package com.robertsoultanaev.sphinxproxy;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.robertsoultanaev.sphinxproxy.database.AssembledMessage;
import com.robertsoultanaev.sphinxproxy.database.DB;
import com.robertsoultanaev.sphinxproxy.database.DBQuery;
import com.robertsoultanaev.sphinxproxy.database.Packet;
import com.robertsoultanaev.sphinxproxy.database.PacketCount;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private DB db;
    private DBQuery dbQuery;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, DB.class).build();
        dbQuery = db.getDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void addAndAssemblePacketsTest() throws Exception {
        String messageId = UUID.randomUUID().toString();
        String packet1Body = "hello\r\n";
        String packet2Body = "bye\r\n";

        Packet packet1 = new Packet(messageId, 0, 2, packet1Body.getBytes());
        Packet packet2 = new Packet(messageId, 1, 2, packet2Body.getBytes());

        dbQuery.addPacket(packet1);

        List<Packet> queryResult1 = dbQuery.getPackets(messageId);
        assertThat(queryResult1.size(), is(1));
        assertThat(queryResult1.get(0), is(packet1));

        List<String> queryResult2 = dbQuery.getReadyPacketIds();
        assertThat(queryResult2.isEmpty(), is(true));

        dbQuery.addPacket(packet2);

        List<Packet> queryResult3 = dbQuery.getPackets(messageId);
        assertThat(queryResult3.size(), is(2));
        assertThat(queryResult3.get(0), is(packet1));
        assertThat(queryResult3.get(1), is(packet2));

        List<String> queryResult4 = dbQuery.getReadyPacketIds();
        assertThat(queryResult4.size(), is(1));
        assertThat(queryResult4.get(0), is(messageId));

        AssembledMessage assembledMessage = new AssembledMessage(messageId, (packet1Body + packet2Body).getBytes());

        dbQuery.addAssembledMessage(assembledMessage);

        List<Packet> queryResult5 = dbQuery.getPackets(messageId);
        assertThat(queryResult5.isEmpty(), is(true));

        List<String> queryResult6 = dbQuery.getReadyPacketIds();
        assertThat(queryResult6.isEmpty(), is(true));

        List<AssembledMessage> queryResult7 = dbQuery.getAssembledMessages();
        assertThat(queryResult7.size(), is(1));
        assertThat(queryResult7.get(0), is(assembledMessage));
    }
}

