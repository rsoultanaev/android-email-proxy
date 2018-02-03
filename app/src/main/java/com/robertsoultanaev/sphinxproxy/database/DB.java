package com.robertsoultanaev.sphinxproxy.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Packet.class, PacketCount.class, AssembledMessage.class, MixNode.class, Recipient.class}, version = 1)
public abstract class DB extends RoomDatabase {

    private static DB INSTANCE;

    public abstract DBQuery getDao();

    public static DB getAppDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), DB.class, "sphinx-database").build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
