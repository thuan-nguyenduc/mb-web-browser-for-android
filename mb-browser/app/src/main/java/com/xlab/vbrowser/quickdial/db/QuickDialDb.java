package com.xlab.vbrowser.quickdial.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.quickdial.dao.QuickDialDao;
import com.xlab.vbrowser.quickdial.entity.QuickDialItem;

import static com.xlab.vbrowser.quickdial.db.QuickDialDb.DATABASE_VERSION;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Database(entities = {QuickDialItem.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class QuickDialDb extends RoomDatabase {
    private static QuickDialDb quickDialDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "quickdialdb";

    public static QuickDialDb getInstance(Context context) {
        try {
            if (quickDialDb == null) {
                quickDialDb = Room.databaseBuilder(context,
                        QuickDialDb.class, DATABASE_NAME).build();
            }

            return quickDialDb;
        }
        catch(Exception ex) {
            return null;
        }
    }

    public static void destroyInstance() {
        if (quickDialDb == null) {
            return;
        }

        quickDialDb.close();
        quickDialDb = null;
    }

    public abstract QuickDialDao quickDialDao();
}
