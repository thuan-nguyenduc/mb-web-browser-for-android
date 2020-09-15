/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.webview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import com.xlab.vbrowser.BuildConfig;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.utils.AppConstants;
import com.xlab.vbrowser.utils.FileUtils;
import com.xlab.vbrowser.utils.ThreadUtils;
import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.utils.ViewUtils;
import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;
import com.xlab.vbrowser.web.WebViewProvider;

import java.util.HashMap;
import java.util.Map;

public class SystemWebView extends NestedWebView implements IWebView, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WebkitView";

    private Callback callback;
    private FocusWebViewClient client;
    private final LinkHandler linkHandler;

    public SystemWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        client = new FocusWebViewClient(context);
        client.setSystemWebView(this);

        setWebViewClient(client);
        setWebChromeClient(createWebChromeClient());
        setDownloadListener(createDownloadListener());

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }

        setLongClickable(true);
        linkHandler = new LinkHandler(this);
        setOnLongClickListener(linkHandler);
    }

    @Override
    public Callback getCallback() {
        return callback;
    }

    @Override
    public boolean canScrollDownVertically() {
        return this.canScrollVertically(1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection connection = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions |= ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING;
        return connection;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        WebViewProvider.applyAppSettings(getContext(), getSettings());
    }

    @Override
    public void restoreWebViewState(Session session) {
        final Bundle stateData = session.getWebViewState();

        final String desiredURL = session.getUrl().getValue();

        client.notifyCurrentURL(desiredURL);
        client.restoreState(stateData);

        final WebBackForwardList backForwardList = stateData != null
                ? super.restoreState(stateData)
                : null;

        // Pages are only added to the back/forward list when loading finishes. If a new page is
        // loading when the Activity is paused/killed, then that page won't be in the list,
        // and needs to be restored separately to the history list. We detect this by checking
        // whether the last fully loaded page (getCurrentItem()) matches the last page that the
        // WebView was actively loading (which was retrieved during onSaveInstanceState():
        // WebView.getUrl() always returns the currently loading or loaded page).
        // If the app is paused/killed before the initial page finished loading, then the entire
        // list will be null - so we need to additionally check whether the list even exists.

        if (backForwardList != null &&
                backForwardList.getCurrentItem().getUrl().equals(desiredURL)) {
            // restoreState doesn't actually load the current page, it just restores navigation history,
            // so we also need to explicitly reload in this case:
            //TODO(thuan): Need to recheck this, if remove this, we still see no problem.
            //reload();
        } else {
            loadUrl(desiredURL);
        }
    }

    @Override
    public void saveWebViewState(@NonNull Session session) {
        // We store the actual state into another bundle that we will keep in memory as long as this
        // browsing session is active. The data that WebView stores in this bundle is too large for
        // Android to save and restore as part of the state bundle.
        final Bundle stateData = new Bundle();

        super.saveState(stateData);
        client.saveState(this, stateData);

        session.saveWebViewState(stateData);
    }

    @Override
    public void setBlockingEnabled(boolean enabled) {
        client.setBlockingEnabled(enabled);

        if (callback != null) {
            callback.onBlockingStateChanged(enabled);
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
        client.setCallback(callback);
        linkHandler.setCallback(callback);
    }

    @Override
    public void requestDesktopSite() {
        WebViewProvider.requestDesktopSite(getContext(), super.getSettings());
    }

    public void loadUrl(String url) {
        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        if (!client.shouldOverrideUrlLoading(this, url)) {
            final Map<String, String> additionalHeaders = new HashMap<>();
            additionalHeaders.put("X-Requested-With", "");

            super.loadUrl(url, additionalHeaders);
        }

        client.notifyCurrentURL(url);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
        client.notifyCurrentURL(baseUrl);
    }


    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void cleanup() {
        clearFormData();
        clearHistory();
        clearMatches();
        clearSslPreferences();
        clearCache(true);

        // We don't care about the callback - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null);

        WebStorage.getInstance().deleteAllData();

        final WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getContext());
        // It isn't entirely clear how this differs from WebView.clearFormData()
        webViewDatabase.clearFormData();
        webViewDatabase.clearHttpAuthUsernamePassword();

        deleteContentFromKnownLocations(getContext());
    }

    @Override
    public void runScript(String script, ValueCallback<String> result) {
        evaluateJavascript(script, result);
    }

    @Override
    public String getUserAgent() {
        return super.getSettings().getUserAgentString();
    }

    @Override
    public void enableLoadingImage(boolean isEnabled) {
        WebViewProvider.enableLoadingImage(getContext(), super.getSettings(), isEnabled);
    }

    public static void cleanup(WebView webView) {
        webView.clearFormData();
        webView.clearHistory();
        webView.clearMatches();
        webView.clearSslPreferences();
        webView.clearCache(true);

        // We don't care about the callback - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null);

        final WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(webView.getContext());
        // It isn't entirely clear how this differs from WebView.clearFormData()
        webViewDatabase.clearFormData();
        webViewDatabase.clearHttpAuthUsernamePassword();

        deleteContentFromKnownLocations(webView.getContext());
    }

    public static void deleteContentFromKnownLocations(final Context context) {
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                // We call all methods on WebView to delete data. But some traces still remain
                // on disk. This will wipe the whole webview directory.
                FileUtils.deleteWebViewDirectory(context);

                // WebView stores some files in the cache directory. We do not use it ourselves
                // so let's truncate it.
                FileUtils.truncateCacheDirectory(context);
            }
        });
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (callback != null) {
                    // This is the earliest point where we might be able to confirm a redirected
                    // URL: we don't necessarily get a shouldInterceptRequest() after a redirect,
                    // so we can only check the updated url in onProgressChanges(), or in onPageFinished()
                    // (which is even later).
                    final String viewURL = view.getUrl();
                    if (!UrlUtils.isInternalErrorURL(viewURL) && viewURL != null) {
                        callback.onURLChanged(viewURL, false);
                    }
                    callback.onProgress(newProgress);
                }
            }

            @Override
            public void onShowCustomView(View view, final CustomViewCallback webviewCallback) {
                final FullscreenCallback fullscreenCallback = new FullscreenCallback() {
                    @Override
                    public void fullScreenExited() {
                        webviewCallback.onCustomViewHidden();
                    }
                };

                if (callback != null) {
                    callback.onEnterFullScreen(fullscreenCallback, view);
                }
            }

            @Override
            public void onHideCustomView() {
                if (callback != null) {
                    callback.onExitFullScreen();
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                //TODO(thuan): We don't support multi webview now, so disable this will help to prevent some crashes,
                //for example: accessing phimnhanh.com, it opens 2 webpages parralelly, so app crashed due to
                //one webview is destroyed before some actions.

                /*if (isDialog || !isUserGesture) {
                    //(thuan): Prevent popup.
                    return false;
                }

                final WebView newView = new WebView(baseContext);
                newView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        try {
                            //some webpages as phimnhanh.com usually open 2 new windows, so our webview crashed.
                            if (callback != null && UrlUtils.isHttpOrHttps(url)) {
                                callback.requestNewTab(url);
                            }

                            newView.stopLoading();
                        }
                        catch(Exception e) {}
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newView);
                resultMsg.sendToTarget();*/

                return true;
            }

            @Override
            public void onReceivedTitle(WebView webView, String title) {
                if (callback != null) {
                    callback.onReceivedTitle(title);
                }
            }

            @Override
            public void onReceivedIcon(WebView webView, Bitmap icon) {
                if (icon == null || callback == null) {
                    return;
                }

                callback.onReceivedIcon(icon);
            }

            @Override
            //handling input[type="file"] requests for android API 21+
            public boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (callback == null) {
                    return true;
                }

                return callback.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        };
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);

        if (callback != null) {
            callback.onScrollChanged(l, t, oldl, oldt);
        }
    }

    private DownloadListener createDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (!AppConstants.supportsDownloadingFiles()) {
                    return;
                }

                final String scheme = Uri.parse(url).getScheme();
                if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                    // We are ignoring everything that is not http or https. This is a limitation of
                    // Android's download manager. There's no reason to show a download dialog for
                    // something we can't download anyways.
                    Log.w(TAG, "Ignoring download from non http(s) URL: " + url);
                    return;
                }

                if (callback != null) {
                    final Download download = new Download(url, userAgent, contentDisposition, mimetype, contentLength, Environment.DIRECTORY_DOWNLOADS);
                    callback.onDownloadStart(download);
                }
            }
        };
    }
}