package com.xlab.vbrowser.extensions.MediaDownloader;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ValueCallback;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.xlab.vbrowser.R;
import com.xlab.vbrowser.extensions.BaseExtension;
import com.xlab.vbrowser.extensions.ExtensionUtils;
import com.xlab.vbrowser.extensions.MediaDownloader.Parser.ZingMp3Album;
import com.xlab.vbrowser.extensions.MediaDownloader.Ui.MediaParserBottomSheet;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlUtils;

import com.xlab.vbrowser.fragment.BrowserFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by nguyenducthuan on 1/22/18.
 */

public final class MediaParser extends BaseExtension implements IMediaParserCallback {
    private static String script;
    private final int PATH_SCRIPT = com.xlab.vbrowser.R.raw.media;
    private MediaParser baseMediaParser;
    private MediaParserBottomSheet bottomSheetDialog;

    private void loadScript(Context context) {
        if (TextUtils.isEmpty(script)) {
            script = ExtensionUtils.loadScript(context, PATH_SCRIPT);
        }
    }

    public MediaParser(WeakReference<View> actionView, BrowserFragment browserFragment) {
        super(actionView, browserFragment);
    }

    @Override
    public void onPageStarted() {
        super.onPageStarted();

        //If user is accessing YouTube, hide actionView
        if (getActionView() != null && mBrowserFragment != null && mBrowserFragment.getInitialUrl() != null
                && UrlUtils.isYoutube(mBrowserFragment.getInitialUrl())) {
            getActionView().setVisibility(View.GONE);

            return;
        }

        /**
         * Disable download_media button
         */
        if (getActionView() != null) {
            getActionView().setVisibility(View.VISIBLE);
            getActionView().setEnabled(false);
            getActionView().setAlpha(0.5f);
        }
    }

    @Override
    public void onPageFinished() {
        super.onPageFinished();

        /**
         * Enable download_media button
         */
        if (getActionView() != null && mBrowserFragment != null && mBrowserFragment.getWebView() != null) {
            String url = mBrowserFragment.getUrl();

            if (url != null) {
                boolean isYoutube = UrlUtils.isYoutube(url);
                getActionView().setVisibility(isYoutube ? View.GONE : View.VISIBLE);

                if (isYoutube) {
                    return;
                }
            }

            if (url != null && !TextUtils.isEmpty(url) && !UrlUtils.isBlankUrl(url)) {
                getActionView().setEnabled(true);
                getActionView().setAlpha(1f);

                Context context = mBrowserFragment.getContext();
                Settings settings = Settings.getInstance(context);

                if (!settings.isShownDownloadMediaTooltip()) {
                    ExtensionUtils.showToolTipDialogBox(context, getActionView(), mBrowserFragment.getString(R.string.download_media_tooltip));
                    settings.setShownDownloadMediaTooltip(true);
                }
            }
            else {
                getActionView().setEnabled(false);
                getActionView().setAlpha(0.5f);
            }
        }

    }

    @Override
    public void runAction() {
        if (mBrowserFragment == null || mBrowserFragment.getWebView() == null) {
            return;
        }

        if (mBrowserFragment.getUrl() != null && !TextUtils.isEmpty(mBrowserFragment.getUrl())) {
            GaReport.sendReportEvent(mBrowserFragment.getContext(), "USER_TAP_DOWNLOAD_MEDIA_BUTTON", "MEDIA_PARSER", mBrowserFragment.getUrl());
        }

        this.baseMediaParser = this;
        loadScript(mBrowserFragment.getContext());

        if (TextUtils.isEmpty(script)) {
            return;
        }

        StringBuilder outBuilder = new StringBuilder("(function() { ");
        outBuilder.append(script);
        outBuilder.append(" return run();})();");

        bottomSheetDialog = new MediaParserBottomSheet(mBrowserFragment);
        bottomSheetDialog.show(mBrowserFragment.getActivity().getSupportFragmentManager(), bottomSheetDialog.getClass().getName());

        mBrowserFragment.getWebView().runScript(outBuilder.toString(), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (TextUtils.isEmpty(s)) {
                    bottomSheetDialog.showError();
                    return;
                }

                if (s.equals("\"isYoutube\"")) {
                    bottomSheetDialog.showYoutubeWarning();
                    return;
                }

                MediaData[] mediaData = parseMediaData(s);

                if (mediaData != null) {
                    if (mediaData.length < 1) {
                        bottomSheetDialog.showError();

                        return;
                    }

                    ArrayList<MediaItem> mediaItems = prepareData(mediaData);

                    if (mediaItems == null || mediaItems.size() < 1) {
                        bottomSheetDialog.showError();

                        return;
                    }

                    bottomSheetDialog.showResult(mediaItems);
                }
                else{
                    MediaAlbum [] mediaAlbums = parseMediaAlbum(s);

                    if (mediaAlbums == null || mediaAlbums.length < 1) {
                        bottomSheetDialog.showError();

                        return;
                    }

                    boolean hasAlbum = false;

                    for(MediaAlbum mediaAlbum: mediaAlbums) {
                        if (mediaAlbum == null || mediaAlbum.source == null || mediaAlbum.link == null) {
                            continue;
                        }

                        if (mediaAlbum.source.equals(ZingMp3Album.SOURCE)) {

                            ZingMp3Album zingMp3Album = new ZingMp3Album(baseMediaParser);

                            zingMp3Album.run(mediaAlbum.link);

                            hasAlbum = true;
                        }
                    }

                    if (!hasAlbum) {
                        bottomSheetDialog.showError();

                        return;
                    }
                }
            }
        });
    }

    @Override
    public void onFailure(String error) {
        if (mBrowserFragment == null || mBrowserFragment.getActivity() == null) {
            return;
        }

        mBrowserFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bottomSheetDialog.showError();
            }
        });
    }

    @Override
    public void onSuccess(final ArrayList<MediaItem> results) {
        if (mBrowserFragment == null || mBrowserFragment.getActivity() == null) {
            return;
        }

        mBrowserFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bottomSheetDialog.showResult(results);
            }
        });
    }

    private MediaData[] parseMediaData(String s) {
        try {
            Gson gson = new Gson();
            MediaData[] results = gson.fromJson(s, MediaData[].class);

            if (results == null) {
                return null;
            }

            for( MediaData mediaData: results) {
                if (mediaData.title != null) {
                    return results;
                }
            }

            return null;
        }
        catch(JsonSyntaxException e) {
            return null;
        }
    }

    private MediaAlbum[] parseMediaAlbum(String s) {
        try {
            Gson gson = new Gson();
            MediaAlbum[] results = gson.fromJson(s, MediaAlbum[].class);

            if (results == null) {
                return null;
            }

            for (MediaAlbum mediaAlbum: results) {
                if (mediaAlbum.source != null && mediaAlbum.link != null) {
                    return results;
                }
            }

            return null;
        }
        catch(JsonSyntaxException e) {
            return null;
        }
    }

    private ArrayList<MediaItem> prepareData(MediaData [] myDataset) {
        ArrayList<MediaItem> mDataset = new ArrayList<>();

        for (MediaData mediaData: myDataset){
            String title = mediaData.title;

            if (mediaData.audio != null) {
                for (MediaItem audio : mediaData.audio) {
                    if (audio.url == null || !UrlUtils.isHttpOrHttps(audio.url)) {
                        // We are ignoring everything that is not http or https. This is a limitation of
                        // Android's download manager. There's no reason to show a download dialog for
                        // something we can't download anyways.
                        continue;
                    }

                    String nameFile = "";

                    if (!TextUtils.isEmpty(audio.resolution)) {
                        nameFile = "Music-"+ audio.resolution + "-" + title + "." + audio.ext;
                    }
                    else {
                        nameFile = "Music-" + title + "." + audio.ext;
                    }

                    audio.namefile = nameFile;
                    audio.title = title;
                    mDataset.add(audio);
                }
            }

            if (mediaData.video != null) {
                for (MediaItem video : mediaData.video) {
                    if (video.url == null || !UrlUtils.isHttpOrHttps(video.url)) {
                        // We are ignoring everything that is not http or https. This is a limitation of
                        // Android's download manager. There's no reason to show a download dialog for
                        // something we can't download anyways.
                        continue;
                    }

                    String nameFile = "";

                    if (!TextUtils.isEmpty(video.resolution)) {
                        nameFile = "Video-" + video.resolution + "-" + title  + "." + video.ext;
                    }
                    else {
                        nameFile = "Video-" + title + "." + video.ext;
                    }

                    video.namefile = nameFile;
                    video.title = title;

                    mDataset.add(video);
                }
            }
        }

        return mDataset;
    }
}

