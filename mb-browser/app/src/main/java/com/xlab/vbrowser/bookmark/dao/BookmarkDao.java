package com.xlab.vbrowser.bookmark.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.xlab.vbrowser.bookmark.entity.Bookmark;

/**
 * Created by nguyenducthuan on 3/25/18.
 */

@Dao
public interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public long insertBookmark(Bookmark bookmark);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    public void updateBookmark(Bookmark bookmark);

    @Delete
    public void deleteBookmarks(Bookmark... bookmarks);

    @Query("DELETE FROM bookmark where url = :url")
    public void deleteByUrl(String url);

    @Query("DELETE FROM bookmark")
    public void clear();

    @Query("SELECT * FROM bookmark order by accessTime desc")
    public Bookmark[] loadBookmarks();

    @Query("SELECT * FROM bookmark order by accessTime desc limit :limitRecords")
    public Bookmark[] loadBookmarks(int limitRecords);

    @Query("SELECT * FROM bookmark where accessTime < :lastAccessTime order by accessTime desc limit :limitRecords")
    public Bookmark[] loadBookmarks(long lastAccessTime, int limitRecords);

    @Query("SELECT * FROM bookmark where accessTime < :lastAccessTime and (url like :queryText or title like :queryText) order by accessTime desc limit :limitRecords")
    public Bookmark[] loadBookmarks(long lastAccessTime, String queryText, int limitRecords);

    @Query("SELECT count(url) FROM bookmark where url = :url")
    public int loadBookmarkByUrl(String url);
}
