package com.xlab.vbrowser.search;

import com.xlab.vbrowser.search.data.SearchSuggestionItem;

/**
 * Created by nguyenducthuan on 3/7/18.
 */

public interface ISearchSuggestionCallback {
    void onFailure(String message);
    void onSuccess(SearchSuggestionItem[] items, String searchTerm);
}
