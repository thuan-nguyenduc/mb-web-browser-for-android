/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */
package com.xlab.vbrowser.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import com.xlab.vbrowser.web.IWebView;

import java.net.URISyntaxException;
import java.util.List;

public class IntentUtils {

    private static String MARKET_INTENT_URI_PACKAGE_PREFIX = "market://details?id=";
    private static String MARKET_WEBSITE_PREFIX = "https://play.google.com/store/apps/details?id=";
    private static String EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url";

    /**
     * Find and open the appropriate app for a given Uri. If appropriate, let the user select between
     * multiple supported apps. Returns a boolean indicating whether the URL was handled. A fallback
     * URL will be opened in the supplied WebView if appropriate (in which case the URL was handled,
     * and true will also be returned). If not handled, we should  fall back to webviews error handling
     * (which ends up calling our error handling code as needed).
     *
     * Note: this method "leaks" the target Uri to Android before asking the user whether they
     * want to use an external app to open the uri. Ultimately the OS can spy on anything we're
     * doing in the app, so this isn't an actual "bug".
     */
    public static boolean handleExternalUri(final Context context, final IWebView webView, final String uri) {
        // This code is largely based on Fennec's ExternalIntentDuringPrivateBrowsingPromptFragment.java
        final Intent intent;
        try {
            intent = Intent.parseUri(uri, 0);
        } catch (URISyntaxException e) {
            // Let the browser handle the url (i.e. let the browser show it's unsupported protocol /
            // invalid URL page).
            return false;
        }

        // Since we're a browser:
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        final PackageManager packageManager = context.getPackageManager();

        // This is where we "leak" the uri to the OS. If we're using the system webview, then the OS
        // already knows that we're opening this uri. Even if we're using GeckoView, the OS can still
        // see what domains we're visiting, so there's no loss of privacy here:
        final List<ResolveInfo> matchingActivities = packageManager.queryIntentActivities(intent, 0);

        if (matchingActivities.size() == 0) {
            return handleUnsupportedLink(context, webView, intent, uri);
        } else if (matchingActivities.size() == 1) {
            final ResolveInfo info;

            if (matchingActivities.size() == 1) {
                info = matchingActivities.get(0);
            } else {
                // Ordering isn't guaranteed if there is more than one available activity - hence
                // we fetch the default (this code isn't currently run because we handle the > 1
                // case separately, but would be needed if we ever decide to prefer the default
                // app for the > 1 case.
                info = packageManager.resolveActivity(intent, 0);
            }
            final CharSequence externalAppTitle = info.loadLabel(packageManager);

            showConfirmationDialog(context, intent, context.getString(com.xlab.vbrowser.R.string.external_app_prompt_title), com.xlab.vbrowser.R.string.external_app_prompt, externalAppTitle);
            return true;
        } else { // matchingActivities.size() > 1
            // By explicitly showing the chooser, we can avoid having a (default) app from opening
            // the link immediately. This isn't perfect - we'd prefer to highlight the default app,
            // but it's not clear if there's any way of doing that. An alternative
            // would be to reuse the same dialog as for the single-activity case, and offer
            // a "open in other app this time" button if we have more than one matchingActivity.
            final String chooserTitle = context.getString(com.xlab.vbrowser.R.string.external_multiple_apps_matched_exit);
            final Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
            context.startActivity(chooserIntent);

            return true;
        }
    }

    private static boolean handleUnsupportedLink(final Context context, final IWebView webView, final Intent intent, final String originalUri) {
        final String fallbackUrl = intent.getStringExtra(EXTRA_BROWSER_FALLBACK_URL);
        if (fallbackUrl != null) {
            webView.loadUrl(fallbackUrl);
            return true;
        }

        String packageName = intent.getPackage();

        if (packageName == null) {
            if(originalUri != null && originalUri.startsWith(MARKET_INTENT_URI_PACKAGE_PREFIX)) {
                packageName = originalUri.replace(MARKET_INTENT_URI_PACKAGE_PREFIX, "");
            }
        }

        if (packageName != null) {
            // The url included the target package:
            final String marketUri = MARKET_INTENT_URI_PACKAGE_PREFIX + packageName;
            final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUri));
            marketIntent.addCategory(Intent.CATEGORY_BROWSABLE);

            final PackageManager packageManager = context.getPackageManager();
            final ResolveInfo info = packageManager.resolveActivity(marketIntent, 0);

            CharSequence marketTitle = "";

            if (info != null) {
                marketTitle = info.loadLabel(packageManager);
                showConfirmationDialog(context, marketIntent,
                        context.getString(com.xlab.vbrowser.R.string.external_app_prompt_no_app_title),
                        com.xlab.vbrowser.R.string.external_app_prompt_no_app, marketTitle);
            }
            else {
                webView.loadUrl(MARKET_WEBSITE_PREFIX + packageName);
            }

            // Stop loading, we essentially have a result.
            return true;
        }
        else {
            //If there's really no way to handle this, we don' load anything
            return true;
        }
    }

    // We only need one param for both scenarios, hence we use just one "param" argument. If we ever
    // end up needing more or a variable number we can change this, but java varargs are a bit messy
    // so let's try to avoid that seeing as it's not needed right now.
    private static void showConfirmationDialog(final Context context, final Intent targetIntent, final String title, final @StringRes int messageResource, final CharSequence param) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, com.xlab.vbrowser.R.style.DialogStyle);

        final CharSequence ourAppName = context.getString(com.xlab.vbrowser.R.string.app_name);

        builder.setTitle(title);

        builder.setMessage(context.getResources().getString(messageResource, ourAppName, param));

        builder.setPositiveButton(com.xlab.vbrowser.R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                context.startActivity(targetIntent);
            }
        });

        builder.setNegativeButton(com.xlab.vbrowser.R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });

        // TODO: remove this, or enable it - depending on how we decide to handle the multiple-app/>1
        // case in future.
//            if (matchingActivities.size() > 1) {
//                builder.setNeutralButton(R.string.external_app_prompt_other, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        final String chooserTitle = activity.getResources().getString(R.string.external_multiple_apps_matched_exit);
//                        final Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
//                        activity.startActivity(chooserIntent);
//                    }
//                });
//            }

        builder.show();
    }

    public static boolean activitiesFoundForIntent(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
        return (resolveInfoList.size() > 0);
    }

    public static Intent createOpenFileIntent(Uri uriFile, String mimeType) {
        if (uriFile == null || mimeType == null) {
            return null;
        }
        final Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
        openFileIntent.setDataAndType(uriFile, mimeType);
        openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return openFileIntent;
    }
}
