package com.xlab.vbrowser.trackers;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.xlab.vbrowser.BuildConfig;
import com.xlab.vbrowser.UpApplication;

public class GaReport {
    public static void sendReportScreen(Context context, String screenName) {
        try {
            if (BuildConfig.DEBUG) {
                return;
            }

            UpApplication application = (UpApplication) context.getApplicationContext();

            if (application == null) {
                return;
            }

            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(screenName);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
        catch (Exception ex) { }
    }

    public static void sendReportEvent(Context context, String event, String category) {
        try {
            if (BuildConfig.DEBUG) {
                return;
            }

            UpApplication application = (UpApplication) context.getApplicationContext();

            if (application == null) {
                return;
            }

            Tracker tracker = application.getDefaultTracker();
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(event)
                    .build());
        }
        catch (Exception e) {}
    }

    public static void sendReportEvent(Context context, String event, String category, String label) {
        try {
            if (BuildConfig.DEBUG) {
                return;
            }

            UpApplication application = (UpApplication) context.getApplicationContext();

            if (application == null) {
                return;
            }

            Tracker tracker = application.getDefaultTracker();
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(event)
                    .setLabel(label)
                    .build());
        }
        catch (Exception e) {}
    }
}
