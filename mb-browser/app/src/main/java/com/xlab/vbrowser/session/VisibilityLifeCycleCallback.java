/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.xlab.vbrowser.UpApplication;

/**
 * This ActivityLifecycleCallbacks implementations tracks if there is at least one activity in the
 * STARTED state (meaning some part of our application is visible).
 * Based on this information the current task can be removed if the app is not visible.
 */
public class VisibilityLifeCycleCallback implements Application.ActivityLifecycleCallbacks {
    /**
     * If all activities of this app are in the background then finish and remove all tasks. After
     * that the app won't show up in "recent apps" anymore.
     */
    /* package */ static void finishAndRemoveTaskIfInBackground(Context context) {
        ((UpApplication) context.getApplicationContext())
                .getVisibilityLifeCycleCallback()
                .finishAndRemoveTaskIfInBackground();
    }

    private Context context;

    /**
     * Activities are not stopped/started in an ordered way. So we are using
     */
    private int activitiesInStartedState;

    public VisibilityLifeCycleCallback(Context context) {
        this.context = context;
    }

    private void finishAndRemoveTaskIfInBackground() {
        if (activitiesInStartedState == 0) {
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return;
            }

            for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
                task.finishAndRemoveTask();
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activitiesInStartedState++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activitiesInStartedState--;
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
