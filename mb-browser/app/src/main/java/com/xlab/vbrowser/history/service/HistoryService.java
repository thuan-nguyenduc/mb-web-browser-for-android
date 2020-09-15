package com.xlab.vbrowser.history.service;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.xlab.vbrowser.architecture.NonNullLiveData;
import com.xlab.vbrowser.architecture.NonNullMutableLiveData;
import com.xlab.vbrowser.events.IItemClickListener;
import com.xlab.vbrowser.history.adapter.MostVisitedAdapter;
import com.xlab.vbrowser.history.dao.HistoryDao;
import com.xlab.vbrowser.history.dao.MostVisistedDao;
import com.xlab.vbrowser.history.db.HistoryDb;
import com.xlab.vbrowser.history.db.MostVisitedDb;
import com.xlab.vbrowser.history.db.SearchTermDb;
import com.xlab.vbrowser.history.entity.History;
import com.xlab.vbrowser.history.entity.MostVisited;
import com.xlab.vbrowser.history.entity.SearchTerm;
import com.xlab.vbrowser.quickdial.service.QuickDialService;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.UrlUtils;

import java.util.Date;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

public class HistoryService {
    //This value is used as a event to notify client when history is cleared
    private final static NonNullMutableLiveData<Long> clearHistoryEvent = new NonNullMutableLiveData<>(0l);

    //This value is used as a event to notify client when we need reload mostvisited
    private final static NonNullMutableLiveData<Long> reloadMostVisisted = new NonNullMutableLiveData<>(0l);

    //private static final long MILISECOND_OF_DAY = 86400000; //24 * 60 * 60 * 1000;
    private static final long MILISECOND_OF_HOUR = 3600000; //60 * 60 * 1000;

    public static void updateHistoryAsync(final String title, final String url, final Context context) {
        new BackgroundTask(new IBackgroundTask(){
            public void run() {
                try {
                    MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

                    if (!UrlUtils.isHttpOrHttps(url) || mostVisitedDb == null) {
                        return;
                    }

                    long accessTime = new Date().getTime();

                    final History history = new History();
                    history.title = title;
                    history.url = url;
                    history.accessTime = accessTime;

                    final MostVisited mostVisited = new MostVisited();
                    mostVisited.title = title;
                    mostVisited.url = UrlUtils.stripCommonSubdomains(UrlUtils.getHost(url));

                    if (!TextUtils.isEmpty(mostVisited.url)) {
                        MostVisistedDao mostVisistedDao = mostVisitedDb.mostVisitedDao();

                        MostVisited savedMostVisisted = mostVisistedDao.getMostVisited(mostVisited.url);

                        if (savedMostVisisted == null) {
                            mostVisited.count = 1;
                            mostVisistedDao.insertMostVisited(mostVisited);
                        } else {
                            mostVisited.count = savedMostVisisted.count + 1;
                            mostVisistedDao.updateMostVisited(mostVisited);
                        }
                    }

                    HistoryDb historyDb = HistoryDb.getInstance(context);

                    if (historyDb != null) {
                        HistoryDao historyDao = historyDb.historyDao();
                        //Check the url in day
                        long result = historyDao.loadMostVistedInDayByUrl(url, new Date().getTime(), MILISECOND_OF_HOUR);

                        if (result <= 0) {
                            //Insert history
                            historyDao.insertHistory(history);
                        }
                        //Or update accessTime of history
                        else {
                            historyDao.updateHistory(history);
                        }
                    }
                }
                catch(android.database.sqlite.SQLiteDatabaseLockedException e) {
                    //Ignore this exception what is reported to Google Play
                }
                catch (java.lang.IllegalStateException e) {
                    //Ignore this exception what is reported to Google Play
                }
            }

            public void onComplete() {}
        }).execute();
    }

    public static String getSuggestionUrl(final String url, final Context context) {
        MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

        if (mostVisitedDb == null) {
            return "";
        }

        MostVisistedDao mostVisistedDao = mostVisitedDb.mostVisitedDao();
        String [] urls = mostVisistedDao.getSuggestion(url + "%");

        if (urls != null && urls.length > 0) {
            return urls[0];
        }

        return "";
    }

    public static void loadMostVisitedAsync(final Context context, final RecyclerView mostVisistedView, final View mostVisistedSeperatorHeader,
                                            final int limitRecords, final IItemClickListener itemClickListener,
                                            final IHistoryServiceAction historyServiceAction) {
        new BackgroundTask(new IBackgroundTask() {
            MostVisited[] mostVisiteds = null;

            @Override
            public void run() {
                MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

                if (mostVisitedDb == null) {
                    return;
                }

                mostVisiteds = mostVisitedDb.mostVisitedDao().loadMostVisteds(limitRecords);
            }

            @Override
            public void onComplete() {
                if (mostVisiteds == null) {
                    return;
                }

                mostVisistedView.setAlpha(0f);
                mostVisistedSeperatorHeader.setAlpha(0f);
                mostVisistedSeperatorHeader.setVisibility(mostVisiteds.length > 0 ? View.VISIBLE : View.GONE);

                //binding here
                mostVisistedView.setAdapter(new MostVisitedAdapter(context, mostVisiteds, itemClickListener));

                mostVisistedSeperatorHeader.animate()
                        .alpha(1.0f)
                        .setDuration(500);

                //animate this
                mostVisistedView.animate()
                        .alpha(1.0f)
                        .setDuration(500);

                historyServiceAction.onLoadComplete();
            }
        }).execute();
    }

    public static MostVisited[] loadMostVisisted(final Context context, final int limitRecords) {
        MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

        if (mostVisitedDb == null) {
            return new MostVisited[0];
        }

        return mostVisitedDb.mostVisitedDao().loadMostVisteds(limitRecords);
    }

    public static void updateMostVisisteds(final Context context, final MostVisited... mostVisiteds) {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

                if (mostVisitedDb == null) {
                    return;
                }

                mostVisitedDb.mostVisitedDao().updateMostVisited(mostVisiteds);
            }

            @Override
            public void onComplete() {
                HistoryService.notifyReloadMostVisistedEvent();
            }
        }).execute();
    }

    public static History[] loadHistories(final Context context,final long lastAccesstime, String queryText, final int limitRecords) {
        HistoryDb historyDb = HistoryDb.getInstance(context);

        if (historyDb == null) {
            return new History[0];
        }

        if (TextUtils.isEmpty(queryText.trim())) {
            return historyDb.historyDao().loadHistories(lastAccesstime, limitRecords);
        }
        else {
            queryText = "%" + queryText + "%";
            return historyDb.historyDao().loadHistories(lastAccesstime, queryText, limitRecords);
        }
    }

    public static void deleteHistory(final  Context context, final History history) {
        HistoryDb historyDb = HistoryDb.getInstance(context);

        if (historyDb == null) {
            return;
        }

        historyDb.historyDao().deleteHistories(history);
    }

    public static void clearHistories(final Context context) {
        HistoryDb historyDb = HistoryDb.getInstance(context);

        if (historyDb != null) {
            historyDb.historyDao().clear();
        }

        MostVisitedDb mostVisitedDb = MostVisitedDb.getInstance(context);

        if (mostVisitedDb != null) {
            mostVisitedDb.mostVisitedDao().clear();
        }

        SearchTermDb searchTermDb = SearchTermDb.getInstance(context);

        if (searchTermDb != null) {
            searchTermDb.searchTermDao().clear();
        }

        QuickDialService.clear(context);
    }

    public static void insertSearchTerm(final Context context, final String term) {
        SearchTermDb searchTermDb = SearchTermDb.getInstance(context);

        if (searchTermDb == null) {
            return;
        }

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.term = term;
        searchTerm.accessTime = new Date().getTime();

        searchTermDb.searchTermDao().insert(searchTerm);
    }

    public static String getSearchTerm(String term, final Context context) {
        SearchTermDb searchTermDb = SearchTermDb.getInstance(context);

        if (searchTermDb == null) {
            return "";
        }

        term = term + "%";
        String [] terms = searchTermDb.searchTermDao().getTerm(term);

        if (terms != null && terms.length > 0) {
            return terms[0];
        }
        else
            return "";
    }

    public static void notifyClearHistoryEvent() {
        clearHistoryEvent.setValue(System.nanoTime());
    }

    public static NonNullLiveData<Long> getClearHistoryEvent() {
        return clearHistoryEvent;
    }

    private static void notifyReloadMostVisistedEvent() {
        reloadMostVisisted.setValue(System.nanoTime());
    }

    public static NonNullLiveData<Long> getReloadMostVisitedEvent() {
        return reloadMostVisisted;
    }
}

