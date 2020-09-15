/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.xlab.vbrowser.customtabs.CustomTabConfig;
import com.xlab.vbrowser.utils.SafeIntent;
import com.xlab.vbrowser.architecture.NonNullLiveData;
import com.xlab.vbrowser.architecture.NonNullMutableLiveData;
import com.xlab.vbrowser.shortcut.HomeScreen;
import com.xlab.vbrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sessions are managed by this global SessionManager instance.
 */
public class SessionManager {
    private NonNullMutableLiveData<String> openUrlEvent;

    private static final SessionManager INSTANCE = new SessionManager();

    private NonNullMutableLiveData<List<Session>> sessions;
    private String currentSessionUUID;

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    private SessionManager() {
        this.sessions = new NonNullMutableLiveData<>(
                Collections.unmodifiableList(Collections.<Session>emptyList()));
        this.openUrlEvent = new NonNullMutableLiveData<>("");
    }

    /**
     * Handle this incoming intent (via onCreate()) and create a new session if required.
     */
    public void handleIntent(final Context context, final SafeIntent intent, final Bundle savedInstanceState) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // This Intent was launched from history (recent apps). Android will redeliver the
            // original Intent (which might be a VIEW intent). However if there's no active browsing
            // session then we do not want to re-process the Intent and potentially re-open a website
            // from a session that the user already "erased".
            return;
        }

        if (savedInstanceState != null) {
            // We are restoring a previous session - No need to handle this Intent.
            return;
        }

        createSessionFromIntent(context, intent);
    }

    /**
     * Handle this incoming intent (via onNewIntent()) and create a new session if required.
     */
    public void handleNewIntent(final Context context, final SafeIntent intent) {
        createSessionFromIntent(context, intent);
    }

    private void createSessionFromIntent(Context context, SafeIntent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            final String dataString = intent.getDataString();
            if (TextUtils.isEmpty(dataString)) {
                return; // If there's no URL in the Intent then we can't create a session.
            }

            if (intent.hasExtra(HomeScreen.ADD_TO_HOMESCREEN_TAG)) {
                final boolean blockingEnabled = intent.getBooleanExtra(HomeScreen.BLOCKING_ENABLED, true);
                createSession(context, Source.HOME_SCREEN, intent, intent.getDataString(), blockingEnabled);
            } else {
                createSession(context, Source.VIEW, intent, intent.getDataString());
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            final String dataString = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (TextUtils.isEmpty(dataString)) {
                return;
            }

            final boolean isSearch = !UrlUtils.isUrl(dataString);

            String url = isSearch
                    ? UrlUtils.createSearchUrl(context, dataString)
                    : dataString;

            if (isSearch) {
                createSearchSession(Source.SHARE, url, dataString);
            } else {
                url = UrlUtils.normalize(url);
                createSession(Source.SHARE, url);
            }
        }
    }

    /**
     * Is there at least one browsing session?
     */
    public boolean hasSession() {
        return !sessions.getValue().isEmpty();
    }

    /**
     * Get the current session. This method will throw an exception if there's no active session.
     */
    public Session getCurrentSession() {
        try {
            if (currentSessionUUID == null) {
                return null;
            }

            return getSessionByUUID(currentSessionUUID);
        }
        catch (IllegalAccessError e) {
            return null;
        }
    }

    public boolean isCurrentSession(@NonNull Session session) {
        return session.getUUID().equals(currentSessionUUID);
    }

    public boolean hasSessionWithUUID(@NonNull String uuid) {
        for (Session session : sessions.getValue()) {
            if (uuid.equals(session.getUUID())) {
                return true;
            }
        }

        return false;
    }

    public Session getSessionByUUID(@NonNull String uuid) {
        for (Session session : sessions.getValue()) {
            if (uuid.equals(session.getUUID())) {
                return session;
            }
        }

        throw new IllegalAccessError("There's no active session with UUID " + uuid);
    }

    public int getPositionOfCurrentSession() {
        if (currentSessionUUID == null) {
            return -1;
        }

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session session = this.sessions.getValue().get(i);

            if (session.getUUID().equals(currentSessionUUID)) {
                return i;
            }
        }

        return -1;
    }

    public NonNullLiveData<List<Session>> getSessions() {
        return sessions;
    }

    public void createSession(@NonNull Source source, @NonNull String url) {
        final Session session = new Session(source, url);
        addSession(session);
    }

    public Session createSession(@NonNull Source source, @NonNull String url, @NonNull String title,
                                 @NonNull boolean isBlockingEnabled, @NonNull String searchTerms, Bundle webViewStates) {
        final Session session = new Session(source, url);
        session.setSearchTerms(searchTerms);
        session.setBlockingEnabled(isBlockingEnabled);
        session.setTitle(title);
        session.saveWebViewState(webViewStates);
        addSession(session);

        return session;
    }

    public void createSession(@NonNull Source source, @NonNull String url, @NonNull String previousSessionUUID, final boolean isBlockingEnabled) {
        final Session session = new Session(source, url, previousSessionUUID);
        session.setBlockingEnabled(isBlockingEnabled);
        addSession(session);
    }

    public void createNextSession(@NonNull Source source, @NonNull String url, boolean isBlockingEnabled, boolean isInBackground) {
        final Session session = new Session(source, url, isBlockingEnabled);
        addNextSession(session, isInBackground);
    }

    public void createSearchSession(@NonNull Source source, @NonNull String url, String searchTerms) {
        final Session session = new Session(source, url);
        session.setSearchTerms(searchTerms);
        addSession(session);
    }

    private void createSession(Context context, Source source, SafeIntent intent, String url) {
        final Session session = CustomTabConfig.isCustomTabIntent(intent)
                ? new Session(url, CustomTabConfig.parseCustomTabIntent(context, intent))
                : new Session(source, url);
        addSession(session);
    }

    private void createSession(Context context, Source source, SafeIntent intent, String url, boolean blockingEnabled) {
        final Session session = CustomTabConfig.isCustomTabIntent(intent)
                ? new Session(url, CustomTabConfig.parseCustomTabIntent(context, intent))
                : new Session(source, url);
        session.setBlockingEnabled(blockingEnabled);
        addSession(session);
    }

    /**
     * Add session to next position
     * @param session
     */
    private void addNextSession(Session session, boolean isInBackground) {
        final List<Session> sessions = new ArrayList<>(this.sessions.getValue());
        Session currentSession = getCurrentSession();
        int posCurrentSession = sessions.indexOf(currentSession);

        if (currentSession == null || posCurrentSession < 0) {
            addSession(session);
            return;
        }

        if (!isInBackground) {
            currentSessionUUID = session.getUUID();
        }

        sessions.add(posCurrentSession + 1, session);

        this.sessions.setValue(Collections.unmodifiableList(sessions));
    }

    private void addSession(Session session) {
        currentSessionUUID = session.getUUID();

        final List<Session> sessions = new ArrayList<>(this.sessions.getValue());
        sessions.add(session);

        this.sessions.setValue(Collections.unmodifiableList(sessions));
    }

    public void selectSession(Session session) {
        if (session.getUUID().equals(currentSessionUUID)) {
            // This is already the selected session.
            return;
        }

        currentSessionUUID = session.getUUID();

        this.sessions.setValue(this.sessions.getValue());
    }

    public void selectSession(String sessionUUID) {
        if (sessionUUID != null && sessionUUID.equals(currentSessionUUID)) {
            // This is already the selected session.
            return;
        }

        currentSessionUUID = sessionUUID;

        this.sessions.setValue(this.sessions.getValue());
    }

    /**
     * Remove all sessions.
     */
    public void removeAllSessions() {
        currentSessionUUID = null;

        sessions.setValue(Collections.unmodifiableList(Collections.<Session>emptyList()));
    }

    /**
     * Remove the current (selected) session.
     */
    public void removeCurrentSession() {
        removeSession(currentSessionUUID);
    }

    public void removeSession(Session session) {
        if (currentSessionUUID == null || session == null) {
            return;
        }

        if (currentSessionUUID.equals(session.getUUID())) {
            removeCurrentSession();
            return;
        }

        final List<Session> sessions = new ArrayList<>();

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session currentSession = this.sessions.getValue().get(i);

            if (currentSession.getUUID().equals(session.getUUID())) {
                continue;
            }

            sessions.add(currentSession);
        }

        this.sessions.setValue(sessions);
    }

    public void removeCurrentSessionAndSelectSession(String sessionUUIDWantToSelect) {
        final List<Session> sessions = new ArrayList<>();

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session currentSession = this.sessions.getValue().get(i);

            if (currentSession.getUUID().equals(currentSessionUUID)) {
                continue;
            }

            sessions.add(currentSession);
        }

        if (sessions.isEmpty()) {
            currentSessionUUID = null;
        } else {
            selectSession(sessionUUIDWantToSelect);
        }

        this.sessions.setValue(sessions);
    }

    @VisibleForTesting void removeSession(String uuid) {
        final List<Session> sessions = new ArrayList<>();

        int removedFromPosition = -1;

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session currentSession = this.sessions.getValue().get(i);

            if (currentSession.getUUID().equals(uuid)) {
                removedFromPosition = i;
                continue;
            }

            sessions.add(currentSession);
        }

        if (removedFromPosition == -1) {
            return;
        }

        if (sessions.isEmpty()) {
            currentSessionUUID = null;
        } else {
            final Session currentSession = sessions.get(
                    Math.min(removedFromPosition, sessions.size() - 1));
            currentSessionUUID = currentSession.getUUID();
        }

        this.sessions.setValue(sessions);
    }

    public void openUrl(String url) {
        this.openUrlEvent.setValue(url);
    }

    public NonNullMutableLiveData<String> getOpenUrlEvent() {
        return this.openUrlEvent;
    }
}
