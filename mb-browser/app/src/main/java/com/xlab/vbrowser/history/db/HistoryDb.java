package com.xlab.vbrowser.history.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.history.dao.HistoryDao;
import com.xlab.vbrowser.history.entity.History;

import static com.xlab.vbrowser.history.db.HistoryDb.DATABASE_VERSION;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Database(entities = {History.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class HistoryDb extends RoomDatabase {
    private static HistoryDb historyDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "historydb";

    public static HistoryDb getInstance(Context context) {
        try {
            if (historyDb == null) {
                historyDb = Room.databaseBuilder(context,
                        HistoryDb.class, DATABASE_NAME).build();
            }

            return historyDb;
        }
        catch(Exception ex) {
            return null;
        }
    }

    public static void destroyInstance() {
        if (historyDb == null) {
            return;
        }

        historyDb.close();
        historyDb = null;
    }

    public abstract HistoryDao historyDao();
}
