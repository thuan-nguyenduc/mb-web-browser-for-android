package com.xlab.vbrowser.history.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.history.dao.SearchTermDao;
import com.xlab.vbrowser.history.entity.SearchTerm;

import static com.xlab.vbrowser.history.db.SearchTermDb.DATABASE_VERSION;


/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Database(entities = {SearchTerm.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class SearchTermDb extends RoomDatabase {
    private static SearchTermDb searchTermDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "searchtermdb";

    public static SearchTermDb getInstance(Context context) {
        try {
            if (searchTermDb == null) {
                searchTermDb = Room.databaseBuilder(context,
                        SearchTermDb.class, DATABASE_NAME).build();
            }

            return searchTermDb;
        }
        catch(Exception ex) {
            return null;
        }
    }

    public static void destroyInstance() {
        if (searchTermDb == null) {
            return;
        }

        searchTermDb.close();
        searchTermDb = null;
    }

    public abstract SearchTermDao searchTermDao();
}
