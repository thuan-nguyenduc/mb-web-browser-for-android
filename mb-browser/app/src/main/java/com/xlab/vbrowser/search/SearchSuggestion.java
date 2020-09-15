package com.xlab.vbrowser.search;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.xlab.vbrowser.history.entity.MostVisited;
import com.xlab.vbrowser.history.service.HistoryService;
import com.xlab.vbrowser.quickdial.entity.QuickDialItem;
import com.xlab.vbrowser.search.data.SearchSuggestionItem;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.UrlUtils;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by nguyenducthuan on 3/7/18.
 */

public class SearchSuggestion {
    private final static String SEARCH_URL = "https://www.google.com/complete/search?client=chrome&q={searchTerms}";
    private final static int MAX_ROWS = 7;
    private final static OkHttpClient client = new OkHttpClient();


    public static void requestSuggestion(final String searchTerm, final ISearchSuggestionCallback callback) {
        String url = SEARCH_URL.replace("{searchTerms}", searchTerm);

        // Initialize a new Request
        Request request = new Request.Builder()
                .url(url)
                .build();

        Call theCall = client.newCall(request);
        theCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    callback.onFailure(e.getMessage());
                }
                catch(IllegalArgumentException ise) {
                    //Fix bug for Google Play report
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();

                if (body == null) {
                    callback.onFailure("NoData");
                    return;
                }

                if (!response.isSuccessful()) {
                    body.close();
                    callback.onFailure(response.message());

                    return;
                }

                final String data = body.string();
                body.close();

                new BackgroundTask(new IBackgroundTask() {
                    SearchSuggestionItem[] results;
                    @Override
                    public void run() {
                        results = prepareData(searchTerm, data);
                    }

                    @Override
                    public void onComplete() {
                        if (results == null || results.length < 1) {
                            callback.onFailure("NoData");

                            return;
                        }

                        callback.onSuccess(results, searchTerm);
                    }
                }).execute();

            }
        });
    }

    public static void loadSuggestionFromDisk(final Context context, final ISearchSuggestionCallback callback) {
        new BackgroundTask(new IBackgroundTask() {
            SearchSuggestionItem[] results;
            @Override
            public void run() {
                MostVisited [] mostVisiteds = HistoryService.loadMostVisisted(context, MAX_ROWS);

                if (mostVisiteds != null && mostVisiteds.length > 0) {
                    results = prepareData(mostVisiteds);
                }
            }

            @Override
            public void onComplete() {
                if (results == null || results.length < 1) {
                    callback.onFailure("NoData");

                    return;
                }

                callback.onSuccess(results, "");
            }
        }).execute();
    }

    private static SearchSuggestionItem[] prepareData(MostVisited[] mostVisiteds) {
        if (mostVisiteds == null || mostVisiteds.length < 1) {
            return null;
        }

        SearchSuggestionItem[] suggestionItems = new SearchSuggestionItem[mostVisiteds.length];

        for (int index = 0; index < mostVisiteds.length; index++) {
            suggestionItems[index] = new SearchSuggestionItem();
            String hint = mostVisiteds[index].title;
            String term = mostVisiteds[index].url;

            if (TextUtils.isEmpty(term)) {
                continue;
            }

            suggestionItems[index].searchTerm = term;
            suggestionItems[index].isUrl = false;
            /*if (UrlUtils.isUrl(term) && !TextUtils.isEmpty(hint)) {
                suggestionItems[index].isUrl = true;
                suggestionItems[index].urlTitle = hint;
            }*/
        }

        return suggestionItems;
    }

    private static SearchSuggestionItem[] prepareData(QuickDialItem[] quickDialItems) {
        if (quickDialItems == null || quickDialItems.length < 1) {
            return null;
        }

        SearchSuggestionItem[] suggestionItems = new SearchSuggestionItem[quickDialItems.length];

        for (int index = 0; index < quickDialItems.length; index++) {
            suggestionItems[index] = new SearchSuggestionItem();
            String hint = quickDialItems[index].title;
            String term = quickDialItems[index].url;

            if (TextUtils.isEmpty(term)) {
                continue;
            }

            suggestionItems[index].searchTerm = term;

            if (UrlUtils.isUrl(term) && !TextUtils.isEmpty(hint)) {
                suggestionItems[index].isUrl = true;
                //suggestionItems[index].urlTitle = hint;
            }
        }

        return suggestionItems;
    }

    private static SearchSuggestionItem[] prepareData(String searchTerm, String data) {
        Object [] results = null;

        try {
            Gson gson = new Gson();
            results = gson.fromJson(data, Object[].class);

            if (results == null || results.length < 3) {
                return null;
            }
        }
        catch (Exception e) {
            return null;
        }

        if (results[0] == null ||  !(results[0] instanceof  String)
                && !(results[0].toString().equals(searchTerm))) {
            return null;
        }

        if (results[1] == null || results[2] == null) {
            return null;
        }

        if (!(results[1] instanceof ArrayList) || !(results[2] instanceof ArrayList)) {
            return null;
        }

        ArrayList<String> terms = (ArrayList<String>)results[1];
        ArrayList<String> hints = (ArrayList<String>)results[2];

        if (terms == null || hints == null || terms.size() < 1 || terms.size() != hints.size()) {
            return null;
        }

        int length = terms.size();
        length =  length > MAX_ROWS ? MAX_ROWS : length;
        SearchSuggestionItem[] suggestionItems = new SearchSuggestionItem[length];

        for (int index = 0; index < length; index ++) {
            suggestionItems[index] = new SearchSuggestionItem();
            String term = terms.get(index);
            String hint = hints.get(index);

            if (TextUtils.isEmpty(term)) {
                continue;
            }

            suggestionItems[index].searchTerm = term;

            if (UrlUtils.isHttpOrHttps(term) && !TextUtils.isEmpty(hint)) {
                suggestionItems[index].isUrl = true;
                suggestionItems[index].urlTitle = hint;
            }
        }

        return suggestionItems;
    }
}
