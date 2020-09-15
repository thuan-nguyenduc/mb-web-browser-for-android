/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import com.xlab.vbrowser.utils.AppConstants;
import com.xlab.vbrowser.utils.Browsers;

/**
 * Helper activity that will open the Google Play store by following a redirect URL.
 */
public class InstallFirefoxActivity extends Activity {
    private static final String REDIRECT_URL = "https://app.adjust.com/gs1ao4";

    private WebView webView;

    public static ActivityInfo resolveAppStore(Context context) {
        final ResolveInfo resolveInfo = context.getPackageManager()
                .resolveActivity(createStoreIntent(), 0);

        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }

        if (!resolveInfo.activityInfo.exported) {
            // We are not allowed to launch this activity.
            return null;
        }

        return resolveInfo.activityInfo;
    }

    private static Intent createStoreIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + Browsers.KnownBrowser.FIREFOX.packageName));
    }

    public static void open(Context context) {
        if (AppConstants.isKlarBuild()) {
            // Redirect to Google Play directly
            context.startActivity(createStoreIntent());
        } else {
            // Start this activity to load the redirect URL in a WebView.
            final Intent intent = new Intent(context, InstallFirefoxActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        webView.loadUrl(REDIRECT_URL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (webView != null) {
            webView.onPause();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (webView != null) {
            webView.destroy();
        }
    }
}
