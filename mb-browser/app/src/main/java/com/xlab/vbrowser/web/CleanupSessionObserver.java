/*Copyright by MonnyLab*/

package com.xlab.vbrowser.web;


import android.content.Context;
import android.support.annotation.NonNull;

import com.xlab.vbrowser.architecture.NonNullObserver;
import com.xlab.vbrowser.session.Session;

import java.util.List;

public class CleanupSessionObserver extends NonNullObserver<List<Session>> {
    private final Context context;

    public CleanupSessionObserver(Context context) {
        this.context = context;
    }

    @Override
    protected void onValueChanged(@NonNull List<Session> sessions) {
        if (sessions.isEmpty()) {
            // Make sure no browsing data remains on the device if there's no active session (anymore).
            //WebViewProvider.performCleanup(context);
        }
    }
}
