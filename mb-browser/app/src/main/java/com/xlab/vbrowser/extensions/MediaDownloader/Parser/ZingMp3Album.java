package com.xlab.vbrowser.extensions.MediaDownloader.Parser;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.xlab.vbrowser.extensions.MediaDownloader.Parser.Data.ZingMp3.AlbumItem;

import com.xlab.vbrowser.extensions.MediaDownloader.IMediaParserCallback;
import com.xlab.vbrowser.extensions.MediaDownloader.MediaItem;
import com.xlab.vbrowser.extensions.MediaDownloader.Parser.Data.ZingMp3.Album;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by nguyenducthuan on 2/6/18.
 */

public class ZingMp3Album {
    public static final String SOURCE = "ZingMP3Album";
    private final String ZINGMP3_XHR ="https://m.mp3.zing.vn/xhr";
    private IMediaParserCallback callback;

    public ZingMp3Album(IMediaParserCallback callback) {
        this.callback = callback;
    }

    public void run(String url) {
        url = ZINGMP3_XHR + url;
        OkHttpClient client = new OkHttpClient();

        // Initialize a new Request
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure(response.message());

                    return;
                }

                ResponseBody body = response.body();

                if (body == null) {
                    callback.onFailure("NoData");

                    return;
                }

                final String data = body.string();
                ArrayList<MediaItem> results = prepareData(data);

                if (results == null || results.size() < 1) {
                    callback.onFailure("NoData");

                    return;
                }

                callback.onSuccess(results);
            }
        });
    }

    private ArrayList<MediaItem> prepareData(String data) {
        Album album = null;
        try {
            Gson gson = new Gson();
            album = gson.fromJson(data, Album.class);
        }
        catch(JsonSyntaxException e) {
            return null;
        }

        if (album == null || album.data == null || album.data.items == null || album.data.items.length < 1) {
            return null;
        }

        ArrayList<MediaItem> mediaItems = new ArrayList<>();

        for (AlbumItem albumItem: album.data.items) {
            if (albumItem.type == null || !albumItem.type.equals("audio")
                    || albumItem.source == null) {
                continue;
            }

            MediaItem mediaItem = new MediaItem();
            mediaItem.title = albumItem.title;
            mediaItem.ext = "mp3";

            if (albumItem.source.high != null && !TextUtils.isEmpty(albumItem.source.high)) {
                mediaItem.quality = "320k";
                mediaItem.namefile = "Audio-"+ mediaItem.title + "-" + mediaItem.quality + "."+ mediaItem.ext;

                if (!albumItem.source.high.startsWith("http:")) {
                    mediaItem.url = "http:" + albumItem.source.high;
                }
                else {
                    mediaItem.url = albumItem.source.high;
                }

            }
            else if (albumItem.source.low != null && !TextUtils.isEmpty(albumItem.source.low)) {
                mediaItem.quality = "128k";
                mediaItem.namefile = "Audio-"+ mediaItem.title + "-" + mediaItem.quality + "."+ mediaItem.ext;

                if (!albumItem.source.low.startsWith("http:")) {
                    mediaItem.url = "http:" + albumItem.source.low;
                }
                else {
                    mediaItem.url = albumItem.source.low;
                }
            }
            else {
                continue;
            }

            mediaItems.add(mediaItem);
        }

        return mediaItems;
    }
}
