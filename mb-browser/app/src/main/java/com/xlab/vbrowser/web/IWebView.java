/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.web;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.xlab.vbrowser.session.Session;

public interface IWebView {
    class HitTarget {
        public final boolean isLink;
        public final String linkURL;

        public final boolean isImage;
        public final String imageURL;

        public HitTarget(final boolean isLink, final String linkURL, final boolean isImage, final String imageURL) {
            if (isLink && linkURL == null) {
                throw new IllegalStateException("link hittarget must contain URL");
            }

            if (isImage && imageURL == null) {
                throw new IllegalStateException("image hittarget must contain URL");
            }

            this.isLink = isLink;
            this.linkURL = linkURL;
            this.isImage = isImage;
            this.imageURL = imageURL;
        }
    }

    interface Callback {
        void onRequest(final boolean isTriggeredByUserGesture);

        void onPageStarted(String url);

        void onPageCommitVisible(final String url);

        void onPageFinished(boolean isSecure);
        void onProgress(int progress);

        void onURLChanged(final String url, boolean isPageFinished);

        void onDownloadStart(Download download);

        void onLongPress(final HitTarget hitTarget);

        /**
         * Notify the host application that the current page has entered full screen mode.
         *
         * The callback needs to be invoked to request the page to exit full screen mode.
         *
         * Some IWebView implementations may pass a custom View which contains the web contents in
         * full screen mode.
         */
        void onEnterFullScreen(@NonNull  FullscreenCallback callback, @Nullable View view);

        /**
         * Notify the host application that the current page has exited full screen mode.
         *
         * If a View was passed when the application entered full screen mode then this view must
         * be hidden now.
         */
        void onExitFullScreen();

        void countBlockedTracker();

        void resetBlockedTrackers();

        void onBlockingStateChanged(boolean isBlockingEnabled);

        void requestNewTab(String url);

        void onReceivedTitle(String title);

        void onReceivedIcon(Bitmap bitmap);

        void onScrollChanged(final int l, final int t, final int oldl, final int oldt);

        boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams);
    }

    interface FullscreenCallback {
        void fullScreenExited();
    }

    /**
     * Enable/Disable content blocking for this session (Only the blockers that are enabled in the app's settings will be turned on/off).
     */
    void setBlockingEnabled(boolean enabled);

    void setCallback(Callback callback);

    void onPause();

    void onResume();

    void destroy();

    void reload();

    void stopLoading();

    String getUrl();

    void loadUrl(String url);

    void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl);

    void cleanup();

    void goForward();

    void goBack();

    boolean canGoForward();

    boolean canGoBack();

    void restoreWebViewState(Session session);

    void saveWebViewState(@NonNull Session session);

    void addJavascriptInterface(Object object, String name);

     void removeJavascriptInterface(String name);

    /**
     * Get the title of the currently displayed website.
     */
    String getTitle();

    void runScript(String script, ValueCallback<String> result);

    String getUserAgent();

    void enableLoadingImage(boolean isEnabled);

    void requestDesktopSite();

    Callback getCallback();

    boolean canScrollDownVertically();
}