package com.xlab.vbrowser.bookmark.service;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.architecture.NonNullLiveData;
import com.xlab.vbrowser.architecture.NonNullMutableLiveData;
import com.xlab.vbrowser.bookmark.db.BookmarkDb;
import com.xlab.vbrowser.bookmark.entity.Bookmark;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;

import java.util.Date;

/**
 * Created by nguyenducthuan on 3/25/18.
 */

public class BookmarkService {
    public static int ERROR_RESULT = -9999;
    //This value is used as a event to notify client when history is cleared
    private final static NonNullMutableLiveData<String> clearBookmarkEvent = new NonNullMutableLiveData<>("");
    private final static NonNullMutableLiveData<Long> clearAllBookmarksEvent = new NonNullMutableLiveData<>(0l);

    private static int loadBookmarkByUrl(Context context, String url) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return ERROR_RESULT;
        }

        return bookmarkDb.bookmarkDao().loadBookmarkByUrl(url);
    }

    private static long insertBookmark(Context context, Bookmark bookmark) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return ERROR_RESULT;
        }

        return bookmarkDb.bookmarkDao().insertBookmark(bookmark);
    }

    private static void deleteByUrl(Context context, String url) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return;
        }

        bookmarkDb.bookmarkDao().deleteByUrl(url);
    }

    public static void clearAll(final Context context) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return;
        }

        bookmarkDb.bookmarkDao().clear();
    }

    public static void deleteBookmark(final Context context, final Bookmark bookmark) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return;
        }

        bookmarkDb.bookmarkDao().deleteBookmarks(bookmark);
    }

    public static Bookmark[] loadBookmarks(final Context context, final long lastAccesstime, String queryText, final int limitRecords) {
        BookmarkDb bookmarkDb = BookmarkDb.getInstance(context);

        if (bookmarkDb == null) {
            return new Bookmark[0];
        }

        if (TextUtils.isEmpty(queryText.trim())) {
            return bookmarkDb.bookmarkDao().loadBookmarks(lastAccesstime, limitRecords);
        }
        else {
            queryText = "%" + queryText + "%";
            return bookmarkDb.bookmarkDao().loadBookmarks(lastAccesstime, queryText, limitRecords);
        }
    }

    /**
     * Determine the url is added to bookmark or not, and then update bookmarkView
     */
    public static void loadBookmark(final Context context, final String url, final ImageButton bookmarkView, final ImageView earthView) {
        bookmarkView.setEnabled(false);

        new BackgroundTask(new IBackgroundTask() {
            long mResult = -1;

            @Override
            public void run() {
                mResult = loadBookmarkByUrl(context, url);
             }

            @Override
            public void onComplete() {
                if (mResult == BookmarkService.ERROR_RESULT) {
                    bookmarkView.setVisibility(View.GONE);
                    bookmarkView.setEnabled(true);
                    return;
                }

                Session session = SessionManager.getInstance().getCurrentSession();

                if (session != null && session.getUrl() != null && url.equals(session.getUrl().getValue())) {
                    //This tag is used when user press the button to know bookmark is added or not yet.
                    bookmarkView.setTag(mResult > 0);
                    bookmarkView.setImageResource(mResult > 0 ? R.drawable.ic_bookmark_added : R.drawable.ic_bookmark);
                    bookmarkView.setVisibility(View.VISIBLE);
                    bookmarkView.setEnabled(true);
                    earthView.setVisibility(View.GONE);
                }
            }
        }).execute();
    }

    public static void addOrRemoveBookmark(final Context context, final String title, final String url, final ImageButton bookmarkView) {
        final boolean isAdded = (boolean) bookmarkView.getTag();
        bookmarkView.setEnabled(false);

        new BackgroundTask(new IBackgroundTask() {
            long result = - 1;

            @Override
            public void run() {
                if (isAdded) {
                    deleteByUrl(context, url);
                }
                else {
                    Bookmark bookmark = new Bookmark();
                    bookmark.accessTime = (new Date()).getTime();
                    bookmark.isFolder = false;
                    bookmark.parentId = 0;
                    bookmark.title = title;
                    bookmark.url = url;
                    result = insertBookmark(context, bookmark);
                }
            }

            @Override
            public void onComplete() {
                if (result == BookmarkService.ERROR_RESULT) {
                    return;
                }

                bookmarkView.setTag(!isAdded);
                bookmarkView.setEnabled(true);
                bookmarkView.setImageResource(!isAdded ? R.drawable.ic_bookmark_added : R.drawable.ic_bookmark);
                Toast.makeText(bookmarkView.getContext(),
                        !isAdded ? bookmarkView.getContext().getString(R.string.bookmarkAddedInfo)
                        : bookmarkView.getContext().getString(R.string.bookmarkRemovedInfo), Toast.LENGTH_SHORT).show();
            }
        }).execute();
    }

    public static void notifyClearBookmarkEvent(String url) {
        clearBookmarkEvent.setValue(url);
    }

    public static NonNullLiveData<String> getClearBookmarkEvent() {
        return clearBookmarkEvent;
    }

    public static void notifyClearAllBookmarksEvent() {
        clearAllBookmarksEvent.setValue(System.nanoTime());
    }

    public static NonNullLiveData<Long> getClearAllBookmarksEvent() {
        return clearAllBookmarksEvent;
    }
}
