/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.content.Context;
import android.support.annotation.NonNull;

import com.xlab.vbrowser.architecture.NonNullObserver;

import java.util.List;

public class NotificationSessionObserver extends NonNullObserver<List<Session>> {
    private Context context;

    public NotificationSessionObserver(Context context) {
        this.context = context;
    }

    @Override
    protected void onValueChanged(@NonNull List<Session> sessions) {
        if (sessions.isEmpty()) {
            SessionNotificationService.stop(context);
        } else {
            SessionNotificationService.start(context);
        }
    }
}