package com.xlab.vbrowser.extensions.ReaderMode;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.extensions.BaseExtension;
import com.xlab.vbrowser.extensions.ExtensionUtils;
import com.xlab.vbrowser.extensions.ReaderMode.Ui.ReaderModeBottomSheetDialog;
import com.xlab.vbrowser.fragment.BrowserFragment;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlUtils;

import java.lang.ref.WeakReference;

/**
 * Created by nguyenducthuan on 3/27/18.
 */

public class ReaderModeParser extends BaseExtension {
    private static String mReaderModeCheckerScript;
    private static String mReaderModeStyle;
    private final String JsInterface = "ReaderModeParser";

    public ReaderModeParser(WeakReference<View> actionView, BrowserFragment browserFragment) {
        super(actionView, browserFragment);
    }

    @JavascriptInterface
    public void showHtml(final String html, final String uuid) {
        Session session = SessionManager.getInstance().getCurrentSession();
        if (session == null ||
                TextUtils.isEmpty(html) || mBrowserFragment == null)
        {
            return;
        }

        if (session.getUUID() != null && !session.getUUID().equals(uuid)) {
            return;
        }


        mBrowserFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if (mBrowserFragment.getWebView() == null || mBrowserFragment.getUrl() == null) {
                return;
            }

            new BackgroundTask(new IBackgroundTask() {
                String newHtml = "";
                @Override
                public void run() {
                    loadStyle(mBrowserFragment.getContext());
                    newHtml = html.replace("{upbrowser_readermode_style}", mReaderModeStyle);
                }

                @Override
                public void onComplete() {
                    try {
                        final ReaderModeBottomSheetDialog readerModeBottomSheetDialog = new ReaderModeBottomSheetDialog();
                        readerModeBottomSheetDialog.setData(newHtml, mBrowserFragment.getUrl(), mBrowserFragment.getWebView().getCallback());

                        readerModeBottomSheetDialog.show(mBrowserFragment.getFragmentManager(), "ReaderModeParser");
                        GaReport.sendReportEvent(mBrowserFragment.getContext(), "SHOW_READER_MODE_DIALOG", "READER_MODE");
                    }
                    catch (IllegalStateException e) {
                        //This exception is raised if current context is destroyed.
                    }
                }
            }).execute();
            }
        });
    }

    @Override
    public void onPageStarted() {
        super.onPageStarted();

        /**
         * Hide reader mode button
         */
        if (getActionView() != null) {
            getActionView().setEnabled(false);
            getActionView().setAlpha(0.5f);
        }

        if (mBrowserFragment != null) {
            mBrowserFragment.getWebView().removeJavascriptInterface(JsInterface);
            mBrowserFragment.getWebView().addJavascriptInterface(this, JsInterface);
        }
    }

    @Override
    public void onPageFinished() {
        super.onPageFinished();

        /**
         * Enable download_media button
         */
        if (getActionView() != null && mBrowserFragment != null && mBrowserFragment.getWebView() != null) {
            String url = mBrowserFragment.getUrl();
            if (getActionView() != null && (url == null || TextUtils.isEmpty(url) || UrlUtils.isBlankUrl(url))) {
                getActionView().setEnabled(false);
                getActionView().setAlpha(0.5f);
            }
        }

        checkReaderMode();
    }

    @Override
    public void runAction() {
        super.runAction();
        final Session session = SessionManager.getInstance().getCurrentSession();

        if (mBrowserFragment == null || mBrowserFragment.getWebView() == null || session == null) {
            return;
        }

        if (getActionView() != null) {
            getActionView().setClickable(false);
        }

        if (mBrowserFragment.getProgressView().getVisibility() != View.VISIBLE) {
            mBrowserFragment.getProgressView().setVisibility(View.VISIBLE);
            mBrowserFragment.getProgressView().setProgress(20);
        }

        new BackgroundTask(new IBackgroundTask() {
            StringBuilder outBuilder;
            @Override
            public void run() {
                String parserScript = loadParserScript(mBrowserFragment.getContext());

                if (TextUtils.isEmpty(parserScript)) {
                    return;
                }

                outBuilder = new StringBuilder("(function() { ");
                outBuilder.append(parserScript);
                outBuilder.append(" return parse('"+ session.getUUID() +"');})();");
            }

            @Override
            public void onComplete() {
                if (outBuilder == null || mBrowserFragment == null) {
                    return;
                }

                mBrowserFragment.getWebView().runScript(outBuilder.toString(), new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        mBrowserFragment.getProgressView().setProgress(mBrowserFragment.getProgressView().getMax());
                        mBrowserFragment.getProgressView().setVisibility(View.GONE);

                        if (getActionView() != null) {
                            getActionView().setClickable(true);
                        }
                    }
                });
            }
        }).execute();

        GaReport.sendReportEvent(mBrowserFragment.getContext(), "TAP_READER_MODE_BUTTON", "READER_MODE");
    }

    private void checkReaderMode() {
        if (mBrowserFragment == null || mBrowserFragment.getWebView() == null) {
            return;
        }

        final Context context = mBrowserFragment.getContext();

        loadCheckerScript(context);

        if (TextUtils.isEmpty(mReaderModeCheckerScript)) {
            return;
        }

        StringBuilder outBuilder = new StringBuilder("(function() { ");
        outBuilder.append(mReaderModeCheckerScript);
        outBuilder.append(" return isAvailable();})();");

        mBrowserFragment.getWebView().runScript(outBuilder.toString(), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (s != null && s.equals("1") && getActionView() != null) {
                    getActionView().setEnabled(true);
                    getActionView().setAlpha(1f);

                    Settings settings = Settings.getInstance(context);

                    if (!settings.isShownReaderModeTooltip() && settings.isShownDownloadMediaTooltip()) {
                        ExtensionUtils.showToolTipDialogBox(context, getActionView(), mBrowserFragment.getString(R.string.reader_mode_tooltip));
                        settings.setShownReaderModeTooltip(true);
                    }
                }
            }
        });
    }

    private void loadCheckerScript(Context context) {
        if (TextUtils.isEmpty(mReaderModeCheckerScript)) {
            mReaderModeCheckerScript = ExtensionUtils.loadScript(context, R.raw.readermode_checker);
        }
    }

    private String loadParserScript(Context context) {
        return ExtensionUtils.loadScript(context, R.raw.readermode_parser);
    }

    private void loadStyle(Context context) {
        if (TextUtils.isEmpty(mReaderModeStyle)) {
            mReaderModeStyle = ExtensionUtils.loadScript(context, R.raw.readermode_style);
        }
    }
}
