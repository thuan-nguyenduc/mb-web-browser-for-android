package com.xlab.vbrowser.history.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.history.dao.MostVisistedDao;
import com.xlab.vbrowser.history.entity.MostVisited;

import static com.xlab.vbrowser.history.db.MostVisitedDb.DATABASE_VERSION;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Database(entities = {MostVisited.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class MostVisitedDb extends RoomDatabase {
    private static MostVisitedDb mostVisitedDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "mostvisiteddb";

    public static MostVisitedDb getInstance(Context context) {
        try {
            if (mostVisitedDb == null) {
                mostVisitedDb = Room.databaseBuilder(context,
                        MostVisitedDb.class, DATABASE_NAME).build();
            }

            return mostVisitedDb;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static void destroyInstance() {
        if (mostVisitedDb == null) {
            return;
        }

        mostVisitedDb.close();
        mostVisitedDb = null;
    }

    public abstract MostVisistedDao mostVisitedDao();
}
