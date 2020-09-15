/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.utils;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.xlab.vbrowser.search.SearchEngineManager;
import com.xlab.vbrowser.search.SearchEngine;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtils {

    private final static String YT_HOST1 = "youtu.be";
    private final static String YT_HOST2 = "youtube.com";
    private final static String YT_HOST3 = "www.youtube.com";
    private final static String YT_HOST4 = "m.youtube.com";

    public static boolean isYoutube(String url) {
        if (url == null) {
            return false;
        }

        String host = getHost(url);

        if (host == null) {
            return false;
        }

        return host.equals(YT_HOST1) || host.equals(YT_HOST2) || host.equals(YT_HOST3) || host.equals(YT_HOST4);
    }


    public static Uri parse(String url) {
        try {
            return Uri.parse(url);
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static String normalize(@NonNull String input) {
        String trimmedInput = input.trim();
        Uri uri = Uri.parse(trimmedInput);

        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = Uri.parse("http://" + trimmedInput);
        }

        return uri.toString();
    }

    /**
     * Is the given string a URL or should we perform a search?
     *
     * TODO: This is a super simple and probably stupid implementation.
     */
    public static boolean isUrl(String url) {
        String trimmedUrl = url.trim();
        if (trimmedUrl.contains(" ")) {
            return false;
        }

        if (trimmedUrl.contains(":") && isHttpOrHttps(trimmedUrl)) {
            return true;
        }

        return trimmedUrl.contains(".");
    }

    public static boolean isValidSearchQueryUrl(String url) {
        String trimmedUrl = url.trim();
        if (!trimmedUrl.matches("^.+?://.+?")) {
            // UI hint url doesn't have http scheme, so add it if necessary
            trimmedUrl = "http://" + trimmedUrl;
        }

        if (!URLUtil.isNetworkUrl(trimmedUrl)) {
            return false;
        }

        if (!trimmedUrl.matches(".*%s$")) {
            return false;
        }

        return true;
    }

    public static boolean isHttpOrHttps(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        url = url.toLowerCase();

        return url.startsWith("http:") || url.startsWith("https:");
    }

    public static boolean isSearchQuery(String text) {
        return text.contains(" ");
    }

    public static String createSearchUrl(Context context, String searchTerm) {
        final SearchEngine searchEngine = SearchEngineManager.getInstance()
                .getDefaultSearchEngine(context);

        return searchEngine.buildSearchUrl(searchTerm);
    }

    public static String stripUserInfo(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        try {
            URI uri = new URI(url);

            final String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                return url;
            }

            // Strip the userInfo to minimise spoofing ability. This only affects what's shown
            // during browsing, this information isn't used when we start editing the URL:
            uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());

            return uri.toString();
        } catch (URISyntaxException e) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url;
        }
    }

    public static boolean isBlankUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        boolean result = url.trim().toLowerCase().equals(UrlConstants.AboutBlankUrl);

        return result;
    }

    /**
     * This method is used for checking all whitelist url, which is loaded without checking on shouldOverrideUrlLoading
     *
     */
    public static boolean isWhitelistUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        url = url.trim().toLowerCase();

        boolean result = url.equals(UrlConstants.AboutBlankUrl);

        return result;
    }

    public static String getHost(String url) {
        if (isBlankUrl(url) || TextUtils.isEmpty(url)) {
            return "";
        }

        try {
            URI uri = new URI(url);

            return uri.getHost();
        }
        catch (URISyntaxException e) {
            return url;
        }
    }

    public static boolean isPermittedResourceProtocol(@Nullable String scheme) {
        if (scheme == null) {
            return false;
        }

        scheme = scheme.toLowerCase();

        return scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data");
    }

    public static boolean isSupportedProtocol(@Nullable final String scheme) {
        return scheme != null && (isPermittedResourceProtocol(scheme) || scheme.startsWith("error"));
    }

    public static boolean isInternalErrorURL(final String url) {
        return "data:text/html;charset=utf-8;base64,".equals(url);
    }

    public static boolean urlsMatchExceptForTrailingSlash(final @NonNull String url1, final @NonNull String url2) {
        int lengthDifference = url1.length() - url2.length();

        if (lengthDifference == 0) {
            // The simplest case:
            return url1.equalsIgnoreCase(url2);
        } else if (lengthDifference == 1) {
            // url1 is longer:
            return url1.charAt(url1.length() - 1) == '/' &&
                    url1.regionMatches(true, 0, url2, 0, url2.length());
        } else if (lengthDifference == -1) {
            return url2.charAt(url2.length() - 1) == '/' &&
                    url2.regionMatches(true, 0, url1, 0, url1.length());
        }

        return false;
    }

    /**
     * For example: https://m.google.com, this function returns: google.com
     * @param url
     * @return
     */
    public static String getMainHost(String url) {
        if (UrlUtils.isHttpOrHttps(url)) {
            url = url != null ? UrlUtils.stripCommonSubdomains(UrlUtils.getHost(url)) : null;
        }

        return url;
    }

    public static String stripCommonSubdomains(@Nullable String host) {
        if (host == null) {
            return null;
        }

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        int start = 0;

        if (host.startsWith("www.")) {
            start = 4;
        } else if (host.startsWith("mobile.")) {
            start = 7;
        } else if (host.startsWith("m.")) {
            start = 2;
        }

        return host.substring(start);
    }

    public static String stripScheme(@Nullable String url) {
        if (url == null) {
            return null;
        }

        int start = 0;

        if (url.startsWith("http://")) {
            start = 7;
        } else if (url.startsWith("https://")) {
            start = 8;
        }

        return url.substring(start);
    }
}
