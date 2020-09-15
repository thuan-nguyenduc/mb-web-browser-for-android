/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser;

import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.xlab.vbrowser.locale.LocaleAwareApplication;
import com.xlab.vbrowser.search.SearchEngineManager;
import com.xlab.vbrowser.session.VisibilityLifeCycleCallback;
import com.xlab.vbrowser.utils.AppConstants;
import com.tonyodev.fetch2.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;

import okhttp3.OkHttpClient;

public class UpApplication extends LocaleAwareApplication {
    private VisibilityLifeCycleCallback visibilityLifeCycleCallback;
    private Fetch fetch;
    private final int ConcurrentDownloadTask = 3;
    private final String FetchNamespace = "UpBrowserFetch";
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        sAnalytics = GoogleAnalytics.getInstance(this);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        enableStrictMode();

        SearchEngineManager.getInstance().init(this);

        registerActivityLifecycleCallbacks(visibilityLifeCycleCallback = new VisibilityLifeCycleCallback(this));
    }

    synchronized public Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker(R.xml.global_tracker);
        }

        return sTracker;
    }

    public VisibilityLifeCycleCallback getVisibilityLifeCycleCallback() {
        return visibilityLifeCycleCallback;
    }

    private void enableStrictMode() {
        // Android/WebView sometimes commit strict mode violations, see e.g.
        // https://github.com/mozilla-mobile/focus-android/issues/660
        if (AppConstants.isReleaseBuild()) {
            return;
        }

        final StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder().detectAll();
        final StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder().detectAll();

        threadPolicyBuilder.penaltyDialog();
        vmPolicyBuilder.penaltyLog();

        StrictMode.setThreadPolicy(threadPolicyBuilder.build());
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }

    private void buildFetch() {
        if (fetch == null || fetch.isClosed()) {
            final OkHttpClient client = new OkHttpClient.Builder().build();
            final Downloader okHttpDownloader = new OkHttpDownloader(client);
            fetch = new Fetch.Builder(this, FetchNamespace)
                    .setDownloader(okHttpDownloader)
                    .setDownloadConcurrentLimit(ConcurrentDownloadTask)
                    .enableLogging(false)
                    .enableRetryOnNetworkGain(true)
                    .build();
        }
    }

    public synchronized Fetch getFetch() {
        try {
            if (fetch == null) {
                buildFetch();
            }

            return fetch;
        }
        catch(Exception e) {
            return  null;
        }
    }

    public synchronized void closeFetch() {
        if (fetch != null && !fetch.isClosed()) {
            fetch.close();
            fetch = null;
        }
    }
}
