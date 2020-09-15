package com.xlab.vbrowser.bookmark.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.xlab.vbrowser.bookmark.dao.BookmarkDao;
import com.xlab.vbrowser.bookmark.entity.Bookmark;

import static com.xlab.vbrowser.bookmark.db.BookmarkDb.DATABASE_VERSION;

/**
 * Created by nguyenducthuan on 3/25/18.
 */

@Database(entities = {Bookmark.class}, version = DATABASE_VERSION, exportSchema = false)
public abstract class BookmarkDb extends RoomDatabase {
    private static BookmarkDb bookmarkDb;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "bookmarkDb";

    public static BookmarkDb getInstance(Context context) {
        try {
            if (bookmarkDb == null) {
                bookmarkDb = Room.databaseBuilder(context,
                        BookmarkDb.class, DATABASE_NAME).build();
            }

            return bookmarkDb;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static void destroyInstance() {
        if (bookmarkDb == null) {
            return;
        }

        bookmarkDb.close();
        bookmarkDb = null;
    }

    public abstract BookmarkDao bookmarkDao();
}
