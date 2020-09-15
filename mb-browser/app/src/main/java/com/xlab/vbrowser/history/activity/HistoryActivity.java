package com.xlab.vbrowser.history.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
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
import com.xlab.vbrowser.events.IConfirmDialogResult;
import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.history.HistoryActionListener;
import com.xlab.vbrowser.history.adapter.HistoryAdapter;
import com.xlab.vbrowser.history.entity.History;
import com.xlab.vbrowser.history.service.HistoryService;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.styles.ThemeUtils;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.web.WebViewProvider;
import com.xlab.vbrowser.widget.EndlessRecyclerViewScrollListener;
import com.xlab.vbrowser.widget.dialog.ConfirmDialog;

/**
 * Created by nguyenducthuan on 3/21/18.
 */

public class HistoryActivity extends LocaleAwareAppCompatActivity
                             implements HistoryActionListener{
    private static final int NUMBER_ROW_PER_PAGE = 20;

    private View noDataView;
    private View dataView;
    private RecyclerView recyclerView;
    private View progressBarView;
    private View clearButtonView;

    private EndlessRecyclerViewScrollListener scrollListener;
    private long lastAccessTime = Long.MAX_VALUE;
    private boolean isLoading = false;
    private String queryText = "";

    private HistoryAdapter mHistoryAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        noDataView = findViewById(R.id.noDataView);
        dataView = findViewById(R.id.dataView);
        progressBarView = findViewById(R.id.progressBarView);
        recyclerView = findViewById(R.id.recyclerView);
        clearButtonView = findViewById(R.id.clearButtonView);
        progressBarView.setVisibility(View.VISIBLE);
        dataView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);

        View nightModeView = findViewById(R.id.nightModeView);
        ThemeUtils.loadNightmode(nightModeView, settings, this);

        setUpViews();

        Toolbar toolbar = findViewById(com.xlab.vbrowser.R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        actionBar.setDisplayShowHomeEnabled(true);

        applyLocale();

        loadData();

        clearButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearHistory();
            }
        });

        GaReport.sendReportScreen(getBaseContext(), HistoryActivity.class.getName());
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
        getMenuInflater().inflate( R.menu.menu_history, menu);

        //Search
        MenuItem myActionMenuItem = menu.findItem( R.id.history_search);
        SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_history_hint));
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
                    GaReport.sendReportEvent(getBaseContext(), "ON_SEARCH_HISTORY", HistoryActivity.class.getName());
                }

                return true;
            }
        });

        return true;
    }

    @Override
    public void applyLocale() {
        setTitle(getString(R.string.history));
    }

    private void setUpViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.d("History onLoadMore", page + "_" + totalItemsCount );
                loadData();
            }
        };

        recyclerView.addOnScrollListener(scrollListener);

        mHistoryAdapter = new HistoryAdapter(this);
        recyclerView.setAdapter(mHistoryAdapter);
    }

    private void resetLoader() {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }

        isLoading = false;
        lastAccessTime = Long.MAX_VALUE;

        if (mHistoryAdapter != null) {
            mHistoryAdapter.clearAll();
        }

        if (scrollListener != null) {
            scrollListener.resetState();
        }
    }

    private void clearHistory() {
        if (mHistoryAdapter == null) {
            return;
        }

        final FragmentManager fragmentManager = this.getSupportFragmentManager();

        ConfirmDialog.newInstance(getString(R.string.app_name), getString(R.string.delete_all_history_title),
                getString(R.string.clear),
                getString(R.string.cancel),
                new IConfirmDialogResult() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onOk() {
                        progressBarView.setVisibility(View.VISIBLE);
                        dataView.setVisibility(View.GONE);
                        noDataView.setVisibility(View.GONE);
                        clearButtonView.setVisibility(View.GONE);

                        new BackgroundTask(new IBackgroundTask() {
                            @Override
                            public void run() {
                                FaviconService.clearFaviconDir(getBaseContext());
                                HistoryService.clearHistories(getBaseContext());
                            }

                            @Override
                            public void onComplete() {
                                SessionManager.getInstance().removeAllSessions();
                                WebViewProvider.performNewBrowserSessionCleanup(getBaseContext());
                                resetLoader();
                                HistoryService.notifyClearHistoryEvent();
                                progressBarView.setVisibility(View.GONE);
                                dataView.setVisibility(View.GONE);
                                noDataView.setVisibility(View.VISIBLE);
                                clearButtonView.setVisibility(View.VISIBLE);
                            }
                        }).execute();
                    }
                }).show(fragmentManager, HistoryActivity.class.getName());
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
            History[] histories = null;

            @Override
            public void run() {
                histories = HistoryService.loadHistories(getBaseContext(), lastAccessTime, queryText, NUMBER_ROW_PER_PAGE);
            }

            @Override
            public void onComplete() {
                progressBarView.setVisibility(View.GONE);

                if (lastAccessTime == Long.MAX_VALUE) {
                    //If is in first loading
                    if (histories == null || histories.length < 1) {
                        noDataView.setVisibility(View.VISIBLE);
                        dataView.setVisibility(View.GONE);
                        return;
                    }

                    noDataView.setVisibility(View.GONE);
                    dataView.setVisibility(View.VISIBLE);
                }

                for (History history : histories) {
                    mHistoryAdapter.addHistory(history);
                    lastAccessTime = history.accessTime;
                }

                isLoading = false;
            }
        }).execute();
    }

    @Override
    public void onRemoveHistory(final History history) {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                HistoryService.deleteHistory(getBaseContext(), history);
            }

            @Override
            public void onComplete() {
                if (mHistoryAdapter.getItemCount() < 1) {
                    progressBarView.setVisibility(View.GONE);
                    noDataView.setVisibility(View.VISIBLE);
                    dataView.setVisibility(View.GONE);
                }
            }
        }).execute();
    }

    @Override
    public void onOpenHistory(String url) {
        if (url == null || !UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        SessionManager.getInstance().openUrl(url);

        finish();
    }

    @Override
    public void onOpenHistoryInNewTab(String url) {
        if (url == null || !UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        SessionManager.getInstance().createSession(Source.HISTORY, url);

        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetLoader();
    }
}
