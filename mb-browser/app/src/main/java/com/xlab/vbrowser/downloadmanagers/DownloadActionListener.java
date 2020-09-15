package com.xlab.vbrowser.downloadmanagers;

/**
 * Created by nguyenducthuan on 3/15/18.
 */

public interface DownloadActionListener {
    void onPauseDownload(int id);

    void onResumeDownload(int id);

    void onRemoveDownload(int id);

    void onRetryDownload(int id);

    void onRemovedDownload(int id);

    void onFinish();
}
