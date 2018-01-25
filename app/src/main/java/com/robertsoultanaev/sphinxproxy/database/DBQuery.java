package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public abstract class DBQuery {
    @Insert
    protected abstract void insertPacket(Packet packet);

    @Insert
    protected abstract void insertPacketCount(PacketCount packetCount);

    @Update
    protected abstract void updatePacketCount(PacketCount packetCount);

    @Query("DELETE FROM packet WHERE uuid=:uuid")
    protected abstract void deletePackets(String uuid);

    @Query("DELETE FROM packetcount WHERE uuid=:uuid")
    protected abstract void deletePacketCount(String uuid);

    @Query("DELETE FROM packet")
    protected abstract void deleteAllPackets();

    @Query("DELETE FROM PacketCount")
    protected abstract void deleteAllPacketCounts();

    @Query("DELETE FROM AssembledMessage")
    protected abstract void deleteAllAssembledMessages();

    @Query("SELECT * FROM packetcount WHERE uuid=:uuid")
    protected abstract PacketCount getPacketCount(String uuid);


    @Insert
    public abstract void insertAssembledMessage(AssembledMessage assembledMessage);

    @Query("DELETE FROM assembledmessage WHERE uuid=:uuid")
    public abstract void deleteAssembledMessage(String uuid);

    @Query("SELECT * FROM packet WHERE uuid=:uuid ORDER BY sequenceNumber")
    public abstract List<Packet> getPackets(String uuid);

    @Query("SELECT * FROM assembledmessage")
    public abstract List<AssembledMessage> getAssembledMessages();

    @Query("SELECT uuid FROM packetcount WHERE packetsInMessage=packetsReceived")
    public abstract List<String> getReadyPacketIds();

    @Transaction
    public void addPacket(Packet packet) {
        // TODO: Think about receiving duplicates
        insertPacket(packet);
        PacketCount packetCount = getPacketCount(packet.uuid);
        if (packetCount == null) {
            packetCount = new PacketCount(packet.uuid, packet.packetsInMessage, 1);
            insertPacketCount(packetCount);
        } else {
            ++packetCount.packetsInMessage;
            updatePacketCount(packetCount);
        }
    }

    @Transaction
    public void addAssembledMessage(AssembledMessage assembledMessage) {
        insertAssembledMessage(assembledMessage);
        deletePackets(assembledMessage.uuid);
        deletePacketCount(assembledMessage.uuid);
    }

    @Transaction
    public void deleteEverything() {
        deleteAllAssembledMessages();
        deleteAllPacketCounts();
        deleteAllPackets();
    }
}