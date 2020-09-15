package com.xlab.vbrowser.downloadmanagers;

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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.UpApplication;
import com.xlab.vbrowser.downloadmanagers.adapter.FileTypeAdapter;
import com.xlab.vbrowser.downloadmanagers.data.FileTypeItem;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.styles.ThemeUtils;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.widget.EndlessRecyclerViewScrollListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.util.FileTypeValue;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DownloadManagerActivity extends LocaleAwareAppCompatActivity implements DownloadActionListener {
    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;
    private static final int NUMBER_ROW_PER_PAGE = 20;

    private DownloadManagerAdapter fileAdapter;
    private Fetch fetch;
    private View noDataView;
    private View dataView;
    private  RecyclerView recyclerView;
    private View progressBarView;
    private Spinner fileTypeView;
    private ImageView noDataImageView;

    private EndlessRecyclerViewScrollListener scrollListener;
    private long lastCreateDate = Long.MAX_VALUE;
    private boolean isLoading = false;
    private String queryText = "";
    private FileTypeItem selectedFileTypeItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);
        noDataView = findViewById(R.id.noDataView);
        dataView = findViewById(R.id.dataView);
        progressBarView = findViewById(R.id.progressBarView);
        recyclerView = findViewById(R.id.recyclerView);
        fileTypeView = findViewById(R.id.fileTypeView);
        noDataImageView = findViewById(R.id.noDataImageView);
        progressBarView.setVisibility(View.VISIBLE);
        dataView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);

        View nightModeView = findViewById(R.id.nightModeView);
        ThemeUtils.loadNightmode(nightModeView, settings, this);

        Toolbar toolbar = findViewById(com.xlab.vbrowser.R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        actionBar.setDisplayShowHomeEnabled(true);

        setUpViews();
        fetch = ((UpApplication) getApplication()).getFetch();

        applyLocale();

        loadData();
        GaReport.sendReportScreen(getBaseContext(), DownloadManagerActivity.class.getName());
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
        getMenuInflater().inflate( R.menu.menu_download_manager, menu);

        MenuItem myActionMenuItem = menu.findItem( R.id.dm_search);
        SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_download_manager_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                if (!s.equals(queryText)) {
                    queryText = s;
                    resetLoader();
                    loadData();
                    GaReport.sendReportEvent(getBaseContext(), "ON_SEARCH_DOWNLOADS", DownloadManagerActivity.class.getName());
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public void applyLocale() {
        setTitle("");
    }

    private void setUpViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.d("DM onLoadMore", page + "_" + totalItemsCount );
                loadData();
            }
        };

        recyclerView.addOnScrollListener(scrollListener);

        fileAdapter = new DownloadManagerAdapter(this);
        recyclerView.setAdapter(fileAdapter);

        final List<FileTypeItem> fileTypeItems = buildFileTypes();
        FileTypeAdapter fileTypeAdapter = new FileTypeAdapter(getBaseContext(), 0, fileTypeItems);
        fileTypeView.setAdapter(fileTypeAdapter);
        fileTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
               FileTypeItem selected = fileTypeItems.get(i);

               if (selectedFileTypeItem == null) {
                   selectedFileTypeItem = selected;
               }
               else if (selected != selectedFileTypeItem) {
                   selectedFileTypeItem = selected;
                   resetLoader();
                   loadData();
               }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private List<FileTypeItem> buildFileTypes() {
        List<FileTypeItem> items = new ArrayList<>();

        for(FileTypeValue fileTypeValue: FileTypeValue.values()) {
            FileTypeItem item = new FileTypeItem();

            switch(fileTypeValue) {
                case FILETYPE_NONE:
                    item.fileTypeValue = FileTypeValue.FILETYPE_NONE;
                    item.icon = getDrawable(R.drawable.filetype_download);
                    item.title = getString(R.string.download_manager);
                    break;

                case FILETYPE_DOC:
                    item.fileTypeValue = FileTypeValue.FILETYPE_DOC;
                    item.title = getString(R.string.filetype_doc);
                    item.icon = getDrawable(R.drawable.filetype_document);
                    break;

                case FILETYPE_VIDEO:
                    item.fileTypeValue = FileTypeValue.FILETYPE_VIDEO;
                    item.title = getString(R.string.filetype_video);
                    item.icon = getDrawable(R.drawable.filetype_video);
                    break;

                case FILETYPE_MUSIC:
                    item.fileTypeValue = FileTypeValue.FILETYPE_MUSIC;
                    item.title = getString(R.string.filetype_music);
                    item.icon = getDrawable(R.drawable.filetype_music);
                    break;

                case FILETYPE_PROGRAM:
                    item.fileTypeValue = FileTypeValue.FILETYPE_PROGRAM;
                    item.title = getString(R.string.filetype_program);
                    item.icon = getDrawable(R.drawable.filetype_android);
                    break;

                case FILETYPE_IMAGE:
                    item.fileTypeValue = FileTypeValue.FILETYPE_IMAGE;
                    item.title = getString(R.string.filetype_image);
                    item.icon = getDrawable(R.drawable.filetype_image);
                    break;

                case FILETYPE_ARCHIVE:
                    item.fileTypeValue = FileTypeValue.FILETYPE_ARCHIVE;
                    item.title = getString(R.string.filetype_archive);
                    item.icon = getDrawable(R.drawable.filetype_archive);
                    break;

                case FILETYPE_OTHER:
                    item.fileTypeValue = FileTypeValue.FILETYPE_OTHER;
                    item.title = getString(R.string.filetype_other);
                    item.icon = getDrawable(R.drawable.filetype_file);
                    break;

                default:
                        break;
            }

            items.add(item);
        }

        return items;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (fetch != null) {
            fetch.addListener(fetchListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (fetch != null && !fetch.isClosed()) {
                fetch.removeListener(fetchListener);
            }
        }catch (Exception e) {}

        resetLoader();
    }

    private void resetLoader() {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }

        isLoading = false;
        lastCreateDate = Long.MAX_VALUE;

        if (fileAdapter != null) {
            fileAdapter.clearAll();
        }

        if (scrollListener != null) {
            scrollListener.resetState();
        }
    }

    private void loadData() {
        if (isLoading) {
            return;
        }

        final FileTypeValue selectedFileType = selectedFileTypeItem != null ? selectedFileTypeItem.fileTypeValue : FileTypeValue.FILETYPE_NONE;

        if (lastCreateDate == Long.MAX_VALUE) {
            progressBarView.setVisibility(View.VISIBLE);
            noDataView.setVisibility(View.GONE);
            dataView.setVisibility(View.GONE);
        }

        isLoading = true;

        if (fetch == null) {
            return;
        }

        fetch.getDownloadsLimit(NUMBER_ROW_PER_PAGE, lastCreateDate, queryText, selectedFileType, new Func<List<? extends Download>>() {
            @Override
            public void call(List<? extends Download> downloads) {
                progressBarView.setVisibility(View.GONE);
                if (lastCreateDate == Long.MAX_VALUE) {
                    //If is in first loading
                    if (downloads == null || downloads.size() < 1) {
                        Log.d("DM", "No Data " + queryText + " _ " + selectedFileType.toString());
                        noDataImageView.setImageDrawable(selectedFileTypeItem != null ? selectedFileTypeItem.icon : getDrawable(R.drawable.filetype_download));
                        noDataView.setVisibility(View.VISIBLE);
                        dataView.setVisibility(View.GONE);
                        return;
                    }

                    noDataView.setVisibility(View.GONE);
                    dataView.setVisibility(View.VISIBLE);
                }

                for (Download download : downloads) {
                    fileAdapter.addDownload(download);
                    lastCreateDate = download.getCreated();
                }

                isLoading = false;
            }
        });
    }

    private void checkDataAvailable() {
        if (fileAdapter == null) {
            return;
        }

        if (fileAdapter.getItemCount() < 1) {
            progressBarView.setVisibility(View.GONE);
            noDataView.setVisibility(View.VISIBLE);
            dataView.setVisibility(View.GONE);
        }
        else {
            progressBarView.setVisibility(View.GONE);
            noDataView.setVisibility(View.GONE);
            dataView.setVisibility(View.VISIBLE);
        }
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onQueued(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onError(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
            onRemoveDownload(download.getId());
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
            onRemoveDownload(download.getId());
        }
    };

    @Override
    public void onPauseDownload(int id) {
        if (fetch == null) {
            return;
        }

        fetch.pause(id);
    }

    @Override
    public void onResumeDownload(int id) {
        if (fetch == null) {
            return;
        }

        fetch.resume(id);
    }

    @Override
    public void onRemoveDownload(int id) {
        if (fetch == null) {
            return;
        }

        fetch.delete(id);
    }

    @Override
    public void onRetryDownload(int id) {
        if (fetch == null) {
            return;
        }

        fetch.retry(id);
    }

    @Override
    public void onRemovedDownload(int id) {
        checkDataAvailable();
    }

    @Override
    public void onFinish() {
        finish();
    }

}