/*Copyright by MonnyLab*/
package com.xlab.vbrowser.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.xlab.vbrowser.session.SessionManager;

public class EraseShortcutActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager.getInstance().removeAllSessions();

        finishAndRemoveTask();
    }
}
