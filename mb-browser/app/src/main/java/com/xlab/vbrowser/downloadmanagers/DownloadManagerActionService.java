package com.xlab.vbrowser.downloadmanagers;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xlab.vbrowser.UpApplication;
import com.tonyodev.fetch2.Fetch;

/**
 * Created by nguyenducthuan on 3/20/18.
 */

public class DownloadManagerActionService extends IntentService {
    public final static String PAUSE_ACTION = "pause";
    public final static String RESUME_ACTION = "resume";
    public final static String DELETE_ACTION = "delete";
    public final static String RETRY_ACTION = "retry";

    public DownloadManagerActionService() {
        super(DownloadManagerActionService.class.getName());
    }

    public DownloadManagerActionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Fetch fetch = ((UpApplication)getApplication()).getFetch();

        if (fetch == null) {
            return;
        }

        String action = intent.getAction();
        String data = "";
        int idDownload = 0;
        Log.d("onHandleIntent", action);
        switch (action) {
            case PAUSE_ACTION:
                data = intent.getDataString();
                idDownload = Integer.parseInt(data);
                fetch.pause(idDownload);
                break;

            case RESUME_ACTION:
                data = intent.getDataString();
                idDownload = Integer.parseInt(data);
                fetch.resume(idDownload);
                break;

            case RETRY_ACTION:
                data = intent.getDataString();
                idDownload = Integer.parseInt(data);
                fetch.retry(idDownload);
                break;

            case DELETE_ACTION:
                data = intent.getDataString();
                idDownload = Integer.parseInt(data);
                fetch.delete(idDownload);
                break;

                default:
                    break;
        }
    }
}
