package com.xlab.vbrowser.history.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Entity
public class History {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String url;
    public String title;
    public long accessTime;
}
