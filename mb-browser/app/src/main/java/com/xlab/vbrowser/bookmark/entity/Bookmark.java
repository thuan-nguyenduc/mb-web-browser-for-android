package com.xlab.vbrowser.bookmark.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by nguyenducthuan on 3/25/18.
 */

@Entity
public class Bookmark {
    @PrimaryKey
    @NonNull
    public String url;
    public String title;
    public long accessTime;

    public int parentId;
    public boolean isFolder;
}
