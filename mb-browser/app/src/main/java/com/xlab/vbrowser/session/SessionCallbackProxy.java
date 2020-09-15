/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;

public class SessionCallbackProxy implements IWebView.Callback {
    /* package */ static final int MINIMUM_PROGRESS = 5;

    private final Session session;
    private final IWebView.Callback delegate;

    public SessionCallbackProxy(Session session, IWebView.Callback delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onPageStarted(String url) {
        session.setLoading(true);
        session.setSecure(false);

        // We are always setting the progress to 5% when a new page starts loading. Otherwise it might
        // look like the browser is doing nothing (on a slow network) until we receive a progress
        // from the WebView.
        session.setProgress(MINIMUM_PROGRESS);

        session.setUrl(url);

        delegate.onPageStarted(url);
    }

    @Override
    public void onPageCommitVisible(String url) {
        delegate.onPageCommitVisible(url);
    }

    @Override
    public void onPageFinished(boolean isSecure) {
        session.setLoading(false);
        session.setSecure(isSecure);
        delegate.onPageFinished(isSecure);
    }

    @Override
    public void onProgress(int progress) {
        session.setLoading(true);
        // We do not want the progress to go backwards - so we always set it to at least the minimum.
        progress = Math.max(MINIMUM_PROGRESS, progress);

        // We do not want to show to show a progress that 100% because this will make the progress
        // bar disappear.
        progress = Math.min(99, progress);

        session.setProgress(progress);
    }

    @Override
    public void onURLChanged(String url, boolean isPageFinised) {
        session.setUrl(url);
        delegate.onURLChanged(url, isPageFinised);
    }

    @Override
    public void onRequest(boolean isTriggeredByUserGesture) {
        if (isTriggeredByUserGesture && session.isSearch()) {
            // The user actively navigated away (no redirect) from the search page. Clear the
            // search terms.
            session.clearSearchTerms();
        }

        delegate.onRequest(isTriggeredByUserGesture);
    }

    @Override
    public void countBlockedTracker() {
        //noinspection ConstantConditions - The value is never null
        session.setTrackersBlocked(session.getBlockedTrackers().getValue() + 1);
    }

    @Override
    public void resetBlockedTrackers() {
        session.setTrackersBlocked(0);
    }

    @Override
    public void onBlockingStateChanged(boolean isBlockingEnabled) {
        session.setBlockingEnabled(isBlockingEnabled);
    }

    @Override
    public void onDownloadStart(Download download) {
        // TODO: Replace with session property
        delegate.onDownloadStart(download);
    }

    @Override
    public void onLongPress(IWebView.HitTarget hitTarget) {
        // TODO: Replace with session property
        delegate.onLongPress(hitTarget);
    }

    @Override
    public void onEnterFullScreen(@NonNull IWebView.FullscreenCallback callback, @Nullable View view) {
        // TODO: Replace with session property
        delegate.onEnterFullScreen(callback, view);
    }

    @Override
    public void onExitFullScreen() {
        // TODO: Replace with session property
        delegate.onExitFullScreen();
    }

    @Override
    public void requestNewTab(String url) {
        delegate.requestNewTab(url);
    }

    @Override
    public void onReceivedTitle(String title) {
        session.setTitle(title);
        delegate.onReceivedTitle(title);
    }

    @Override
    public void onReceivedIcon(Bitmap bitmap) {
        delegate.onReceivedIcon(bitmap);
    }

    @Override
    public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        delegate.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        return delegate.onShowFileChooser(webView, filePathCallback, fileChooserParams);
    }
}
