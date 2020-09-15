package com.xlab.vbrowser.history;

import com.xlab.vbrowser.history.entity.History;

/**
 * Created by nguyenducthuan on 3/15/18.
 */

public interface HistoryActionListener {
    void onRemoveHistory(History history);

    void onOpenHistory(String url);

    void onOpenHistoryInNewTab(String url);
}
