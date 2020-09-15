package com.xlab.vbrowser.quickdial.service;

import android.content.Context;
import android.util.Log;

import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.quickdial.db.QuickDialDb;
import com.xlab.vbrowser.quickdial.entity.QuickDialItem;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.QuickDialWebsites;
import com.xlab.vbrowser.utils.Settings;

public class QuickDialService {
    public static QuickDialItem[] load(final Context context) {
        try {
            QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

            if (quickDialDb == null) {
                return null;
            }

            QuickDialItem[] items = quickDialDb.quickDialDao().load();

            if (items == null || items.length < 1) {
                Settings settings = Settings.getInstance(context);

                if (settings.isFirstTimeLoadingQuickDial()) {
                    items = buildDefault();
                    saveDefault(context, items);
                    settings.setFirstTimeLoadingQuickDial(false);
                } else {
                    //Quick access data is cleared, we need re-save default icon.
                    FaviconService.writeDefaultFavicon(context);
                }
            }

            return items;
        }
        catch (java.lang.IllegalStateException e) {
            return null;
        }
    }

    public static QuickDialItem[] load(final Context context, int maxRecords) {
        QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

        if (quickDialDb == null) {
            return null;
        }

        return quickDialDb.quickDialDao().load(maxRecords);
    }

    public static String getSuggestionUrl(final String url, final Context context) {
        QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

        if (quickDialDb == null) {
            return "";
        }

        String [] urls = quickDialDb.quickDialDao().getSuggestion(url + "%");

        if (urls != null && urls.length > 0) {
            return urls[0];
        }

        return "";
    }

    public static void checkUrl(final Context context) {
        final Session session = SessionManager.getInstance().getCurrentSession();

        if (context == null || session == null || session.getUrl() == null) {
            return;
        }

        final String url = session.getUrl().getValue();

        if (url == null) {
            return;
        }

        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

                if (quickDialDb == null) {
                    return;
                }

                boolean isAdded = quickDialDb.quickDialDao().load(url) > 0;
                Log.d("checkUrl", isAdded + url);
                session.setAddedToQuickAccess(isAdded);
            }

            @Override
            public void onComplete() {
            }
        }).execute();
    }

    public static void remove(final Context context, final QuickDialItem[] quickDialItems) {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

                if (quickDialDb == null) {
                    return;
                }

                quickDialDb.quickDialDao().delete(quickDialItems);
            }

            @Override
            public void onComplete() {

            }
        }).execute();
    }

    public static void clear(final Context context) {
        QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

        if (quickDialDb != null) {
            quickDialDb.quickDialDao().clear();
        }

        FaviconService.writeDefaultFavicon(context);

        //Recreate default quick dials
        Settings settings = Settings.getInstance(context);
        settings.setFirstTimeLoadingQuickDial(true);
    }

    public static void update(final Context context,final QuickDialItem... quickDialItems) {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

                if (quickDialDb != null) {
                    quickDialDb.quickDialDao().update(quickDialItems);
                }
            }

            @Override
            public void onComplete() {

            }
        }).execute();
    }

    public static long insert(final Context context, final QuickDialItem quickDialItem) {
        QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

        if (quickDialDb == null) {
            return 0;
        }

        return quickDialDb.quickDialDao().insert(quickDialItem);
    }

    private static QuickDialItem[] buildDefault() {
        QuickDialItem[] quickDialItems = new QuickDialItem[QuickDialWebsites.Sites.length];

        for (int i = 0; i< QuickDialWebsites.Sites.length; i++) {
            QuickDialItem quickDialItem = QuickDialItem.create(QuickDialWebsites.Sites[i][1], QuickDialWebsites.Sites[i][0]);
            quickDialItems[i] = quickDialItem;
        }

        return quickDialItems;
    }

    private static void saveDefault(final Context context, final QuickDialItem[] quickDialItems) {
        FaviconService.writeDefaultFavicon(context);

        QuickDialDb quickDialDb = QuickDialDb.getInstance(context);

        if (quickDialDb != null) {
            long[] results = quickDialDb.quickDialDao().insert(quickDialItems);

            for (int pos = 0; pos < results.length; pos++) {
                quickDialItems[pos].id = results[pos];
            }
        }
    }

}
