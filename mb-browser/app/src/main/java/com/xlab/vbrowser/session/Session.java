/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xlab.vbrowser.customtabs.CustomTabConfig;
import com.xlab.vbrowser.architecture.NonNullLiveData;
import com.xlab.vbrowser.architecture.NonNullMutableLiveData;

import java.util.UUID;

/**
 * Keeping track of state / data of a single browsing session (tab).
 */
public class Session {
    private final Source source;
    private final String uuid;
    private final NonNullMutableLiveData<String> url;
    private final NonNullMutableLiveData<Integer> progress;
    private final NonNullMutableLiveData<Boolean> secure;
    private final NonNullMutableLiveData<Boolean> loading;
    private final NonNullMutableLiveData<Integer> trackersBlocked;
    private final NonNullMutableLiveData<String> title;
    private final NonNullMutableLiveData<Long> receivedFavicon;
    private CustomTabConfig customTabConfig;
    private Bundle webviewState;
    private String searchTerms;
    private String searchUrl;
    private boolean isRecorded;
    private boolean isBlockingEnabled;

    //This flag to know current URL is added to quick access not not. This is used for checking to show/hide "Add to Quick access" in Menu
    private boolean wasAddedToQuickAccess;

    /**
     * This session UUID is used when opening new tab, and then back to previous tab.
     */
    private String previousSessionUUID;

    /* package */ Session(Source source, String url) {
        this.uuid = UUID.randomUUID().toString();
        this.source = source;

        this.url = new NonNullMutableLiveData<>(url);
        this.progress = new NonNullMutableLiveData<>(0);
        this.secure = new NonNullMutableLiveData<>(false);
        this.loading = new NonNullMutableLiveData<>(false);
        this.trackersBlocked = new NonNullMutableLiveData<>(0);
        this.title = new NonNullMutableLiveData<>("");
        this.receivedFavicon = new NonNullMutableLiveData<>(0l);

        this.isBlockingEnabled = true;
        this.isRecorded = false;
        this.wasAddedToQuickAccess = false;
    }

    /* package */ Session(Source source, String url, String previousSessionUUID) {
        this(source, url);
        this.previousSessionUUID = previousSessionUUID;
    }

    /* package */ Session(String url, @NonNull CustomTabConfig customTabConfig) {
        this(Source.CUSTOM_TAB, url);

        this.customTabConfig = customTabConfig;
    }

    /* package */ Session(Source source, String url, boolean isBlockingEnabled) {
        this(source, url);
        this.isBlockingEnabled = isBlockingEnabled;
    }

    public Source getSource() {
        return source;
    }

    public String getUUID() {
        return uuid;
    }

    /* package */ void setUrl(String url) {
        this.url.setValue(url);
    }

    public NonNullLiveData<String> getUrl() {
        return url;
    }

    /* package */ void setProgress(int progress) {
        this.progress.setValue(progress);
    }

    public NonNullLiveData<Integer> getProgress() {
        return progress;
    }

    /* package */ void setSecure(boolean secure) {
        this.secure.setValue(secure);
    }

    public NonNullLiveData<Boolean> getSecure() {
        return secure;
    }

    /* package */ void setLoading(boolean loading) {
        this.loading.setValue(loading);
    }

    public NonNullLiveData<Boolean> getLoading() {
        return loading;
    }

    public void setTrackersBlocked(int trackersBlocked) {
        this.trackersBlocked.postValue(trackersBlocked);
    }

    /* package */ void clearSearchTerms() {
        searchTerms = null;
    }

    public NonNullLiveData<Integer> getBlockedTrackers() {
        return trackersBlocked;
    }

    public void saveWebViewState(Bundle bundle) {
        this.webviewState = bundle;
    }

    public Bundle getWebViewState() {
        return webviewState;
    }

    public boolean hasWebViewState() {
        return webviewState != null;
    }

    public boolean isCustomTab() {
        return customTabConfig != null;
    }

    public CustomTabConfig getCustomTabConfig() {
        return customTabConfig;
    }

    public boolean isRecorded() {
        return isRecorded;
    }

    public void markAsRecorded() {
        isRecorded = true;
    }

    public boolean isSearch() {
        return !TextUtils.isEmpty(searchTerms);
    }

    public void setSearchTerms(String searchTerms) {
        this.searchTerms = searchTerms;
    }

    public String getSearchTerms() {
        return searchTerms;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public boolean isSameAs(@NonNull Session session) {
        return uuid.equals(session.getUUID());
    }

    public boolean isBlockingEnabled() {
        return isBlockingEnabled;
    }

    public void setBlockingEnabled(boolean blockingEnabled) {
        this.isBlockingEnabled = blockingEnabled;
    }

    public boolean wasAddedToQuickAccess() {
        return wasAddedToQuickAccess;
    }

    public void setAddedToQuickAccess(boolean wasAddedToQuickAccess) {
        this.wasAddedToQuickAccess = wasAddedToQuickAccess;
    }

    public void setTitle(String title) {
        this.title.setValue(title);
    }

    public NonNullLiveData<String> getTitle() {
        return title;
    }

    public String getPreviousSessionUUID() { return previousSessionUUID; }


    public NonNullMutableLiveData<Long> getReceivedFavicon() {
        return receivedFavicon;
    }

    public void setReceivedFavicon(Long receivedFavicon) {
        this.receivedFavicon.setValue(receivedFavicon);
    }
}
