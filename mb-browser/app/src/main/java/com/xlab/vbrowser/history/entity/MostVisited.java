package com.xlab.vbrowser.history.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Entity
public class MostVisited {
    @PrimaryKey
    @NonNull
    public String url = "";
    public String title;
    public long count;
    public int isRemoved = 0;
}
