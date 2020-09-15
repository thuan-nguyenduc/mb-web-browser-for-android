package com.xlab.vbrowser.session.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.session.dao.SessionHistoryDao;
import com.xlab.vbrowser.session.entity.SessionHistory;

import static com.xlab.vbrowser.session.db.SessionHistoryDb.DATABASE_VERSION;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Database(entities = {SessionHistory.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class SessionHistoryDb extends RoomDatabase {
    private static SessionHistoryDb sessionHistoryDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "sessionhistorydb";

    public synchronized static SessionHistoryDb getInstance(Context context) {
        try {
            if (sessionHistoryDb == null) {
                sessionHistoryDb = Room.databaseBuilder(context,
                        SessionHistoryDb.class, DATABASE_NAME).build();
            }

            return sessionHistoryDb;
        }
        catch(Exception ex) {
            return null;
        }
    }

    public synchronized static void destroyInstance() {
        if (sessionHistoryDb == null) {
            return;
        }

        sessionHistoryDb.close();
        sessionHistoryDb = null;
    }

    public abstract SessionHistoryDao sessionHistoryDao();
}
