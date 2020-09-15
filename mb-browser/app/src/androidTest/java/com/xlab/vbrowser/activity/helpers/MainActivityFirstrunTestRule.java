/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity.helpers;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.ThreadUtils;
import com.xlab.vbrowser.activity.MainActivity;

import static com.xlab.vbrowser.fragment.FirstrunFragment.FIRSTRUN_PREF;

public class MainActivityFirstrunTestRule extends ActivityTestRule<MainActivity> {
    private boolean showFirstRun;

    public MainActivityFirstrunTestRule(boolean showFirstRun) {
        super(MainActivity.class);

        this.showFirstRun = showFirstRun;
    }

    @CallSuper
    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();

        Context appContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();

        PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(FIRSTRUN_PREF, !showFirstRun)
                .apply();
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();

        getActivity().finishAndRemoveTask();

        ThreadUtils.postToMainThread(new Runnable() {
            @Override
            public void run() {
                SessionManager.getInstance().removeAllSessions();
            }
        });
    }
}
