package com.xlab.vbrowser.extensions.ReaderMode.Ui;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.menu.context.WebContextMenu;
import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;

public class ReaderModeBottomSheetDialog extends BottomSheetDialogFragment{
    private View mContentView;
    private String mHtml;
    private String mUrl;
    private IWebView.Callback mBrowserFragmentCallback;
    private IWebView webView;
    private View nightModeView;
    private ProgressBar progressView;
    private FrameLayout videoContainer;

    private IWebView.FullscreenCallback fullscreenCallback;

    public void setData(String html, String url, IWebView.Callback callback) {
        this.mHtml = html;
        this.mUrl = url;
        this.mBrowserFragmentCallback = callback;
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        //super.setupDialog(dialog, style);
        //Get the content View
        mContentView = View.inflate(getContext(), R.layout.layout_readermode_bottomsheet, null);
        dialog.setContentView(mContentView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.5f);
        }

        configDialog();

        webView = mContentView.findViewById(R.id.webview);
        nightModeView = mContentView.findViewById(R.id.nightModeView);
        progressView = mContentView.findViewById(R.id.progress);
        videoContainer = mContentView.findViewById(R.id.video_container);

        loadNightmode();

        webView.setCallback(createCallback());
        webView.loadDataWithBaseURL(mUrl, mHtml, "text/html", "UTF-8", null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (fullscreenCallback == null) {
            //Only configDialog if not showing fullscreen video
            configDialog();
        }
    }

    private void configDialog() {
        if (mContentView == null) {
            return;
        }
        int heightPixel = mContentView.getContext().getResources().getDisplayMetrics().heightPixels;

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

    private IWebView.Callback createCallback() {
        return new IWebView.Callback() {
            @Override
            public void onPageStarted(final String url) {
                //If user navigate to another page
                if (url != null && !url.equals(mUrl)) {
                    webView.stopLoading();
                    SessionManager.getInstance().getOpenUrlEvent().setValue(url);
                    dismiss();
                }

                progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageCommitVisible(final String url) {}

            @Override
            public void onPageFinished(boolean isSecure) {
                progressView.setProgress(progressView.getMax());
                progressView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onProgress(int progress) {
                progressView.setProgress(progress);
            }

            @Override
            public void onDownloadStart(Download download) {
                mBrowserFragmentCallback.onDownloadStart(download);
            }

            @Override
            public void onLongPress(IWebView.HitTarget hitTarget) {
                Session currentSession = SessionManager.getInstance().getCurrentSession();
                WebContextMenu.show(mContentView.getContext(), this, hitTarget, currentSession != null && currentSession.isBlockingEnabled());
            }

            @Override
            public void onURLChanged(String url, boolean isPageFinised) {}

            @Override
            public void onRequest(boolean isTriggeredByUserGesture) {}

            @Override
            public void onEnterFullScreen(@NonNull IWebView.FullscreenCallback callback, @Nullable View view) {
                fullscreenCallback = callback;

                if (view != null) {
                    // Add view to video container and make it visible
                    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    videoContainer.addView(view, params);
                    videoContainer.setVisibility(View.VISIBLE);

                    // Switch to immersive mode: Hide system bars other UI controls
                    //WindowUtils.switchToImmersiveMode(getActivity());
                }
            }

            @Override
            public void onExitFullScreen() {
                Log.d("ReaderMode", "onExitFullScreen");
                // Remove custom video views and hide container
                videoContainer.removeAllViews();
                videoContainer.setVisibility(View.GONE);

                //WindowUtils.exitImmersiveModeIfNeeded(getActivity());

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback.fullScreenExited();
                    fullscreenCallback = null;
                }
            }

            @Override
            public void countBlockedTracker() {}

            @Override
            public void resetBlockedTrackers() {}

            @Override
            public void onBlockingStateChanged(boolean isBlockingEnabled) {}

            @Override
            public void requestNewTab(String url) {
            }

            @Override
            public void onReceivedTitle(String title) {}

            @Override
            public void onReceivedIcon(Bitmap bitmap) {}

            @Override
            public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
                if (mContentView == null) {
                    return;
                }

                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) mContentView.getParent()).getLayoutParams();
                CoordinatorLayout.Behavior behavior = params.getBehavior();

                //Set callback
                if (behavior != null && behavior instanceof BottomSheetBehavior) {
                    if (l == 0 && t == 0) {
                        ((BottomSheetBehavior) behavior).setHideable(true);
                    }
                    else {
                        ((BottomSheetBehavior) behavior).setHideable(false);
                    }
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                return true;
            }
        };
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
