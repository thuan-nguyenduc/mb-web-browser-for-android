package com.xlab.vbrowser.bookmark;

import com.xlab.vbrowser.bookmark.entity.Bookmark;

/**
 * Created by nguyenducthuan on 3/15/18.
 */

public interface BookmarkActionListener {
    void onRemoveBookmark(Bookmark history);

    void onOpenBookmark(String url);

    void onOpenBookmarkInNewTab(String url);
}
