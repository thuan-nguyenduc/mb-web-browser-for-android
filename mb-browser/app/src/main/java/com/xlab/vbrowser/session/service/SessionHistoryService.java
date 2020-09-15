package com.xlab.vbrowser.session.service;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.session.dao.SessionHistoryDao;
import com.xlab.vbrowser.session.db.SessionHistoryDb;
import com.xlab.vbrowser.session.entity.SessionHistory;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import java.util.List;

import static com.xlab.vbrowser.utils.BundleUtils.convertBundleToBytes;
import static com.xlab.vbrowser.utils.BundleUtils.convertBytesToBundle;

public class SessionHistoryService {
    public synchronized static void save(final Context context, final ISessionHistoryListener sessionHistoryListener) {
        Log.d("SessionHistoryService", "save");
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                SessionHistoryDb sessionHistoryDb = SessionHistoryDb.getInstance(context);

                if (sessionHistoryDb == null) {
                    return;
                }

                SessionHistoryDao sessionHistoryDao = sessionHistoryDb.sessionHistoryDao();

                SessionManager sessionManager = SessionManager.getInstance();
                List<Session> sessions = sessionManager.getSessions().getValue();
                SessionHistory[] sessionHistories = new SessionHistory[sessions.size()];

                int count = 0;

                for (Session session: sessions) {
                    SessionHistory sessionHistory = new SessionHistory();
                    sessionHistory.source = session.getSource().name();
                    sessionHistory.searchTerms = session.getSearchTerms() == null ? "" : session.getSearchTerms();
                    sessionHistory.accessTime = count;
                    sessionHistory.isBlockingEnabled = session.isBlockingEnabled();
                    sessionHistory.isSelectedSession = session == sessionManager.getCurrentSession();
                    sessionHistory.title = session.getTitle().getValue();
                    sessionHistory.url = session.getUrl().getValue();

                    if (session.hasWebViewState()) {
                        sessionHistory.webviewState = convertBundleToBytes(session.getWebViewState());
                    }

                    sessionHistories[count] = sessionHistory;
                    count++;
                }

                if (sessionHistories.length > 0) {
                    sessionHistoryDao.clear();
                }

                sessionHistoryDao.insert(sessionHistories);

                SessionHistoryDb.destroyInstance();
            }

            @Override
            public void onComplete() {
                sessionHistoryListener.onDone();
            }
        }).execute();
    }

    public static void load(final Context context, final ISessionHistoryListener sessionHistoryListener) {
        new BackgroundTask(new IBackgroundTask() {
            SessionHistory [] sessionHistories = null;
            Bundle [] bundles;
            @Override
            public void run() {
                SessionHistoryDb sessionHistoryDb = SessionHistoryDb.getInstance(context);

                if (sessionHistoryDb == null) {
                    return;
                }

                sessionHistories = sessionHistoryDb.sessionHistoryDao().load();

                if (sessionHistories == null) {
                    return;
                }

                bundles = new Bundle[sessionHistories.length];
                int count = 0;

                for (SessionHistory sessionHistory: sessionHistories) {
                    Bundle webViewStates = null;

                    if (sessionHistory.webviewState != null) {
                        webViewStates = convertBytesToBundle(sessionHistory.webviewState);
                    }

                    bundles[count++] = webViewStates;
                }
            }

            @Override
            public void onComplete() {
                if (sessionHistories == null || bundles == null) {
                    return;
                }

                SessionManager sessionManager = SessionManager.getInstance();
                Session currentSession = null;

                int count = 0;

                for (SessionHistory sessionHistory: sessionHistories) {
                    Session session = sessionManager.createSession(Source.valueOf(sessionHistory.source), sessionHistory.url, sessionHistory.title,
                            sessionHistory.isBlockingEnabled, sessionHistory.searchTerms, bundles[count++]);

                    if (currentSession == null || sessionHistory.isSelectedSession) {
                        currentSession = session;
                    }
                }

                if (currentSession != null) {
                    sessionManager.selectSession(currentSession);
                }

                sessionHistoryListener.onDone();
            }
        }).execute();
    }
}
