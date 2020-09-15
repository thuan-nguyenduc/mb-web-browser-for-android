package com.xlab.vbrowser.utils;

import android.os.AsyncTask;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

public class BackgroundTask extends AsyncTask<Void, Void, Void> {
    private final IBackgroundTask backgroundTask;

    public BackgroundTask(IBackgroundTask backgroundTask) {
        this.backgroundTask = backgroundTask;
    }

    @Override
    protected Void doInBackground(final Void... params) {
        this.backgroundTask.run();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        this.backgroundTask.onComplete();
    }
}
