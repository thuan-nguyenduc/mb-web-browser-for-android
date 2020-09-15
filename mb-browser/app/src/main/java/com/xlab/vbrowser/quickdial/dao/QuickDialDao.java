package com.xlab.vbrowser.quickdial.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.xlab.vbrowser.quickdial.entity.QuickDialItem;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Dao
public interface QuickDialDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public long insert(QuickDialItem quickDialItems);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public long[] insert(QuickDialItem... quickDialItems);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    public void update(QuickDialItem... quickDialItems);

    @Query("SELECT * FROM quickdialitem order by sortOrder asc")
    public QuickDialItem[] load();

    @Query("SELECT * FROM quickdialitem order by sortOrder asc limit :limitRecords")
    public QuickDialItem[] load(int limitRecords);

    @Delete
    public void delete(QuickDialItem... quickDialItems);

    @Query("DELETE FROM quickdialitem")
    public void clear();

    @Query("SELECT id FROM quickdialitem where url = :url")
    public int load(String url);

    @Query("SELECT url FROM quickdialitem WHERE url like :url order by sortOrder asc")
    public String[] getSuggestion(String url);
}
