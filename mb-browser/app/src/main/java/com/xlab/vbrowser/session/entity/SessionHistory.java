package com.xlab.vbrowser.session.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

@Entity
public class SessionHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String source;
    public String searchTerms;
    public String url;
    public String title;
    public boolean isSelectedSession;
    public boolean isBlockingEnabled;
    public long accessTime;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] webviewState;
}
