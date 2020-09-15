/*Copyright by MonnyLab*/

package com.xlab.vbrowser.fragment;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;

public class InfoFragment extends WebFragment {
    private ProgressBar progressView;
    private View webView;

    private static final String ARGUMENT_URL = "url";

    public static InfoFragment create(String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);

        InfoFragment fragment = new InfoFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    @NonNull
    @Override
    public View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(com.xlab.vbrowser.R.layout.fragment_info, container, false);
        progressView = view.findViewById(com.xlab.vbrowser.R.id.progress);
        webView = view.findViewById(com.xlab.vbrowser.R.id.webview);

        final String url = getInitialUrl();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            // Hide webview until content has loaded, if we're loading built in about/rights/etc
            // pages: this avoid a white flash (on slower devices) between the screen appearing,
            // and the about/right/etc content appearing. We don't do this for SUMO and other
            // external pages, because they are both light-coloured, and significantly slower loading.
            webView.setVisibility(View.INVISIBLE);
        }

        applyLocale();

        return view;
    }

    @Override
    public void onCreateViewCalled() {}

    @Override
    public IWebView.Callback createCallback() {
        return new IWebView.Callback() {
            @Override
            public void onPageStarted(final String url) {
                progressView.announceForAccessibility(getString(com.xlab.vbrowser.R.string.accessibility_announcement_loading));

                progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageCommitVisible(final String url) {}

            @Override
            public void onPageFinished(boolean isSecure) {
                progressView.announceForAccessibility(getString(com.xlab.vbrowser.R.string.accessibility_announcement_loading_finished));

                progressView.setVisibility(View.INVISIBLE);

                if (webView.getVisibility() != View.VISIBLE) {
                    webView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProgress(int progress) {
                progressView.setProgress(progress);
            }

            @Override
            public void onDownloadStart(Download download) {}

            @Override
            public void onLongPress(IWebView.HitTarget hitTarget) {}

            @Override
            public void onURLChanged(String url, boolean isPageFinised) {}

            @Override
            public void onRequest(boolean isTriggeredByUserGesture) {}

            @Override
            public void onEnterFullScreen(@NonNull IWebView.FullscreenCallback callback, @Nullable View view) {}

            @Override
            public void onExitFullScreen() {}

            @Override
            public void countBlockedTracker() {}

            @Override
            public void resetBlockedTrackers() {}

            @Override
            public void onBlockingStateChanged(boolean isBlockingEnabled) {}

            @Override
            public void requestNewTab(String url) {}

            @Override
            public void onReceivedTitle(String title) {}

            @Override
            public void onReceivedIcon(Bitmap bitmap) {}

            @Override
            public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                return true;
            }
        };
    }

    @Override
    public Session getSession() {
        return null;
    }

    @Nullable
    @Override
    public String getInitialUrl() {
        return getArguments().getString(ARGUMENT_URL);
    }
}
