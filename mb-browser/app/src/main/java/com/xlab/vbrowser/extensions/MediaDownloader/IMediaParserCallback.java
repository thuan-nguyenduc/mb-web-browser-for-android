package com.xlab.vbrowser.extensions.MediaDownloader;

import java.util.ArrayList;

/**
 * Created by nguyenducthuan on 2/6/18.
 */

public interface IMediaParserCallback {
    void onFailure(String error);
    void onSuccess(ArrayList<MediaItem> results);
}
