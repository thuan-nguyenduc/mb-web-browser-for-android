/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import com.xlab.vbrowser.locale.Locales;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class SupportUtils {
    public static final String HELP_URL = "https://support.mozilla.org/kb/what-firefox-focus-android";
    public static final String DEFAULT_BROWSER_URL = "file:///android_asset/set_default_browser_guide.html";

    public static String getWhatsNewUrl(Context context) {
        return getSumoURLForTopic(context, "whats-new-focus-android-24");
    }

    public static String getSumoURLForTopic(final Context context, final String topic) {
        String escapedTopic;
        try {
            escapedTopic = URLEncoder.encode(topic, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("utf-8 should always be available", e);
        }

        final String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Focus", e);
        }

        final String osTarget = "Android";
        final String langTag = Locales.getLanguageTag(Locale.getDefault());

        return "https://support.mozilla.org/1/mobile/" + appVersion + "/" + osTarget + "/" + langTag + "/" + escapedTopic;
    }

    public static String getManifestoURL() {
        final String langTag = Locales.getLanguageTag(Locale.getDefault());
        return "https://www.mozilla.org/" + langTag + "/about/manifesto/";
    }
}
