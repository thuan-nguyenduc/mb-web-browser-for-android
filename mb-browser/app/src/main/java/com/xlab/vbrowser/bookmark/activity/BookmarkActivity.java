package com.xlab.vbrowser.bookmark.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.bookmark.BookmarkActionListener;
import com.xlab.vbrowser.bookmark.adapter.BookmarkAdapter;
import com.xlab.vbrowser.bookmark.entity.Bookmark;
import com.xlab.vbrowser.bookmark.service.BookmarkService;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.styles.ThemeUtils;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.widget.EndlessRecyclerViewScrollListener;

/**
 * Created by nguyenducthuan on 3/21/18.
 */

public class BookmarkActivity extends LocaleAwareAppCompatActivity
                             implements BookmarkActionListener{
    private static final int NUMBER_ROW_PER_PAGE = 20;

    private View noDataView;
    private View dataView;
    private RecyclerView recyclerView;
    private View progressBarView;

    private EndlessRecyclerViewScrollListener scrollListener;
    private long lastAccessTime = Long.MAX_VALUE;
    private boolean isLoading = false;
    private String queryText = "";

    private BookmarkAdapter mBookmarkAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        noDataView = findViewById(R.id.noDataView);
        dataView = findViewById(R.id.dataView);
        progressBarView = findViewById(R.id.progressBarView);
        recyclerView = findViewById(R.id.recyclerView);
        progressBarView.setVisibility(View.VISIBLE);
        dataView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);

        View nightModeView = findViewById(R.id.nightModeView);
        ThemeUtils.loadNightmode(nightModeView, settings, this);

        setUpViews();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);

        applyLocale();

        loadData();

        GaReport.sendReportScreen(getBaseContext(), BookmarkActivity.class.getName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu) {
        getMenuInflater().inflate( R.menu.menu_bookmark, menu);

        //Search
        MenuItem myActionMenuItem = menu.findItem( R.id.bookmark_search);
        SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_bookmark_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                if (!queryText.equals(s)) {
                    queryText = s;
                    resetLoader();
                    loadData();
                }

                GaReport.sendReportEvent(getBaseContext(), "ON_SEARCH_BOOKMARK", BookmarkActivity.class.getName());

                return true;
            }
        });

        return true;
    }

    @Override
    public void applyLocale() {
        setTitle(getString(R.string.bookmark));
    }

    private void setUpViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.d("Bookmark onLoadMore", page + "_" + totalItemsCount );
                loadData();
            }
        };

        recyclerView.addOnScrollListener(scrollListener);

        mBookmarkAdapter = new BookmarkAdapter(this);
        recyclerView.setAdapter(mBookmarkAdapter);
    }

    private void resetLoader() {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }

        isLoading = false;
        lastAccessTime = Long.MAX_VALUE;

        if (mBookmarkAdapter != null) {
            mBookmarkAdapter.clearAll();
        }

        if (scrollListener != null) {
            scrollListener.resetState();
        }
    }

    private void clearBookmark() {
        if (mBookmarkAdapter == null) {
            return;
        }

        progressBarView.setVisibility(View.VISIBLE);
        dataView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);

        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                BookmarkService.clearAll(getBaseContext());
            }

            @Override
            public void onComplete() {
                resetLoader();
                //Notify client
                BookmarkService.notifyClearAllBookmarksEvent();
                progressBarView.setVisibility(View.GONE);
                dataView.setVisibility(View.GONE);
                noDataView.setVisibility(View.VISIBLE);
                GaReport.sendReportEvent(getBaseContext(), "ON_CLEAR_BOOKMARK", BookmarkActivity.class.getName());
            }
        }).execute();
    }

    private void loadData() {
        if (isLoading) {
            return;
        }

        if (lastAccessTime == Long.MAX_VALUE) {
            progressBarView.setVisibility(View.VISIBLE);
            noDataView.setVisibility(View.GONE);
            dataView.setVisibility(View.GONE);
        }

        isLoading = true;

        new BackgroundTask(new IBackgroundTask() {
            Bookmark[] bookmarks = null;

            @Override
            public void run() {
                bookmarks = BookmarkService.loadBookmarks(getBaseContext(), lastAccessTime, queryText, NUMBER_ROW_PER_PAGE);
            }

            @Override
            public void onComplete() {
                progressBarView.setVisibility(View.GONE);

                if (lastAccessTime == Long.MAX_VALUE) {
                    //If is in first loading
                    if (bookmarks == null || bookmarks.length < 1) {
                        noDataView.setVisibility(View.VISIBLE);
                        dataView.setVisibility(View.GONE);
                        return;
                    }

                    noDataView.setVisibility(View.GONE);
                    dataView.setVisibility(View.VISIBLE);
                }

                for (Bookmark bookmark : bookmarks) {
                    mBookmarkAdapter.addBookmark(bookmark);
                    lastAccessTime = bookmark.accessTime;
                }

                isLoading = false;
            }
        }).execute();
    }

    @Override
    public void onRemoveBookmark(final Bookmark bookmark) {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                BookmarkService.deleteBookmark(getBaseContext(), bookmark);
            }

            @Override
            public void onComplete() {
                if (mBookmarkAdapter.getItemCount() < 1) {
                    progressBarView.setVisibility(View.GONE);
                    noDataView.setVisibility(View.VISIBLE);
                    dataView.setVisibility(View.GONE);
                }
            }
        }).execute();
    }

    @Override
    public void onOpenBookmark(String url) {
        if (url == null || !UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        SessionManager.getInstance().openUrl(url);

        finish();
    }

    @Override
    public void onOpenBookmarkInNewTab(String url) {
        if (url == null || !UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        SessionManager.getInstance().createSession(Source.BOOKMARK, url);

        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetLoader();
    }
}
