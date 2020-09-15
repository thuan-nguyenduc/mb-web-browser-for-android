package com.xlab.vbrowser.extensions.MediaDownloader.Ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.extensions.MediaDownloader.MediaItem;
import com.xlab.vbrowser.fragment.BrowserFragment;
import com.xlab.vbrowser.permission.IRequestPermissionResult;
import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.web.Download;

import java.util.ArrayList;

/**
 * Created by nguyenducthuan on 1/29/18.
 */

@SuppressLint("ValidFragment")
public class MediaParserBottomSheet extends BottomSheetDialogFragment
                                    implements View.OnClickListener, IMediaSelectEvent {
    private View errorView;
    private TextView txtYoutubeWarning;
    private ProgressBar progressBar;
    private RecyclerView downloadList;
    private View downloadView;
    private Button btnCancel;
    private Button btnDownload;
    private CheckBox allView;
    private View noDataView;
    private View hasDataView;
    private View mContentView;
    private View nightModeView;

    private MediaParserAdapter adapter;
    private BrowserFragment browserFragment;

    @SuppressLint("ValidFragment")
    public MediaParserBottomSheet(BrowserFragment browserFragment){
        this.browserFragment = browserFragment;
    }


    @Override
    public void setupDialog(Dialog dialog, int style) {
        //super.setupDialog(dialog, style);
        //Get the content View
        mContentView = View.inflate(getContext(), R.layout.layout_mediaparser_bottomsheet, null);
        mContentView.setBackground(getContext().getDrawable(R.drawable.background_dialog));

        dialog.setContentView(mContentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getMinHeight()));
        dialog.getWindow().setDimAmount(0.5f);

        nightModeView = mContentView.findViewById(R.id.nightModeView);
        loadNightmode();

        errorView = mContentView.findViewById(R.id.viewError);
        txtYoutubeWarning = mContentView.findViewById(R.id.txtYoutubeWarning);
        progressBar = mContentView.findViewById(R.id.progressBar);
        downloadList = mContentView.findViewById(R.id.downloadList);
        downloadView = mContentView.findViewById(R.id.downloadView);
        btnCancel = mContentView.findViewById(R.id.btnCancel);
        btnDownload = mContentView.findViewById(R.id.btnDownload);
        allView = mContentView.findViewById(R.id.allView);
        noDataView = mContentView.findViewById(R.id.noDataView);
        hasDataView = mContentView.findViewById(R.id.hasDataView);

        Button btnReport = mContentView.findViewById(R.id.btnReport);

        btnCancel.setOnClickListener(this);
        btnDownload.setOnClickListener(this);
        btnReport.setOnClickListener(this);

        allView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapter == null) {
                    return;
                }

                if (allView.isChecked()) {
                    adapter.selectAll();
                }
                else {
                    adapter.clearSelected();
                }
            }
        });

        configDialog();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        configDialog();
    }

    private int getMinHeight() {
        DisplayMetrics displayMetrics = mContentView.getContext().getResources().getDisplayMetrics();
        int minHeight = Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels) - 100;
        minHeight = Math.min(minHeight, mContentView.getContext().getResources().getDimensionPixelSize(R.dimen.bottomsheet_min_height));

        return minHeight;
    }

    private void configDialog() {
        if (mContentView == null) {
            return;
        }

        int heightPixel = getMinHeight();

        mContentView.setMinimumHeight(heightPixel);
        //Set the coordinator layout behavior
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) mContentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        //Set callback
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setState(BottomSheetBehavior.STATE_EXPANDED);
            ((BottomSheetBehavior) behavior).setHideable(true);
            ((BottomSheetBehavior) behavior).setPeekHeight(heightPixel);
        }
    }

    public void showResult(ArrayList<MediaItem> results) {
        if (allView != null) {
            allView.setEnabled(true);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (txtYoutubeWarning != null) {
            txtYoutubeWarning.setVisibility(View.GONE);
        }

        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }

        if (downloadView != null) {
            downloadView.setVisibility(View.VISIBLE);
        }

        if (noDataView != null && hasDataView != null) {
            if (results != null && results.size() > 1) {
                noDataView.setVisibility(View.GONE);
                hasDataView.setVisibility(View.VISIBLE);
            } else {
                noDataView.setVisibility(View.VISIBLE);
                hasDataView.setVisibility(View.GONE);
            }
        }

        if (downloadList != null) {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            downloadList.setHasFixedSize(true);

            // use a linear layout manager
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.browserFragment.getContext());
            downloadList.setLayoutManager(layoutManager);
            downloadList.setItemAnimator(new DefaultItemAnimator());
            //downloadList.addItemDecoration(new DividerItemDecoration(browserFragment.getContext(), LinearLayoutManager.VERTICAL));

            adapter = new MediaParserAdapter(results, this);
            downloadList.setAdapter(adapter);
        }

        GaReport.sendReportEvent(getContext(), "SHOW_RESULT", "MEDIA_PARSER", String.valueOf(results != null ? results.size() : 0) + " url: " + this.browserFragment.getUrl());
    }

    public void showError() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (errorView != null) {
            errorView.setVisibility(View.VISIBLE);
        }

        if (txtYoutubeWarning != null) {
            txtYoutubeWarning.setVisibility(View.GONE);
        }

        if (downloadView != null) {
            downloadView.setVisibility(View.GONE);
        }

        if (this.browserFragment != null && this.browserFragment.getUrl() != null) {
            GaReport.sendReportEvent(getContext(), "SHOW_ERROR", "MEDIA_PARSER", "url:" + this.browserFragment.getUrl());
        }
    }

    public void showYoutubeWarning() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }

        if (txtYoutubeWarning != null) {
            txtYoutubeWarning.setVisibility(View.VISIBLE);
        }

        if (downloadView != null) {
            downloadView.setVisibility(View.GONE);
        }

        GaReport.sendReportEvent(getContext(), "SHOW_YOUTUBE_WARNING", "MEDIA_PARSER");
    }

    @Override
    public void onClick(View view) {
        Context context = this.browserFragment.getContext();

        switch (view.getId()) {
            case R.id.btnCancel:
                this.dismiss();
                GaReport.sendReportEvent(getContext(), "DOWNLOAD_BOTTOMSHEET_CANCEL_CLICK", "MEDIA_PARSER",
                        " url: " + this.browserFragment.getUrl());
                break;

            case R.id.btnDownload:
                browserFragment.setPermissionCallback(new IRequestPermissionResult() {
                    @Override
                    public void onReceivePermission() {
                        dismiss();
                        startDownload(adapter.getDownloadSelected());
                    }
                });

                browserFragment.requestWriteExtenalStoragePermission();

                break;

            case R.id.btnReport:
                String mesageEmail = context.getString(R.string.mediaparser_error_reportemail_message);

                if (this.browserFragment != null && this.browserFragment.getUrl() != null) {
                    mesageEmail = mesageEmail.replace("%s", this.browserFragment.getUrl() );
                }

                GaReport.sendReportEvent(context, "MEDIA_DOWNLOAD_REPORT", "MEDIA_PARSER", mesageEmail);
                this.dismiss();
                Toast.makeText(context, context.getString(R.string.thank_you_media_report), Toast.LENGTH_LONG). show();
                break;

            default:
                break;
        }
    }

    private void startDownload(ArrayList<MediaItem> items) {
        if (this.browserFragment == null || this.browserFragment.getWebView() == null
                || items == null || items.size() < 1 || this.browserFragment.getUrl() == null) {
            return;
        }

        Download[] downloads = new Download[items.size()];
        String [] fileNames = new String[items.size()];
        String userAgent = this.browserFragment.getWebView().getUserAgent();
        int pos = 0;
        for (MediaItem item: items) {
            Log.d("Download Link", item.url);

            downloads[pos] = new Download(item.url, userAgent, "", "", 0, Environment.DIRECTORY_DOWNLOADS);
            fileNames[pos] = item.namefile;

            pos++;
        }

        this.browserFragment.startDownload(downloads, fileNames);

        GaReport.sendReportEvent(getContext(), "DOWNLOAD_BOTTOMSHEET_DOWNLOAD_CLICK", "MEDIA_PARSER", "Number downloads: " + items.size()
                                    + " url: " + this.browserFragment.getUrl());
    }

    @Override
    public void onSelectAll() {
        allView.setChecked(true);
    }

    @Override
    public void onRemoveItem() {
        allView.setChecked(false);
    }

    @Override
    public void onClickItem() {
        if (adapter == null || adapter.getDownloadSelected() == null) {
            return;
        }

        int size = adapter.getDownloadSelected().size();

        btnDownload.setEnabled(size > 0);
        btnDownload.setAlpha(size > 0 ? 1f : 0.5f);
    }

    private void loadNightmode() {
        Settings settings = Settings.getInstance(getContext());
        boolean enableNightMode = settings.isEnabledNightMode();
        nightModeView.setVisibility(enableNightMode ? View.VISIBLE : View.GONE);

        int nightModeBrightness = settings.getNightModeBrighness();
        float fBrightness = (Constants.NIGHTMODE_BRIGHTNESS_MAX - nightModeBrightness) / ((float)Constants.NIGHTMODE_BRIGHTNESS_MAX * 1.1f);
        nightModeView.setAlpha(fBrightness);
    }
}
