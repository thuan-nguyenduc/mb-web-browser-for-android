package com.xlab.vbrowser.extensions;

import android.view.View;
import com.xlab.vbrowser.fragment.BrowserFragment;
import java.lang.ref.WeakReference;

/**
 * Created by nguyenducthuan on 1/22/18.
 */

public abstract class BaseExtension {
    private WeakReference<View> actionViewReference;
    protected BrowserFragment mBrowserFragment;

    public BaseExtension(WeakReference<View> actionViewReference, BrowserFragment browserFragment) {
        this.actionViewReference = actionViewReference;
        this.mBrowserFragment = browserFragment;

        if (this.actionViewReference != null && this.actionViewReference.get() != null) {
            this.actionViewReference.get().setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    runAction();
                }
            });
        }
    }

    protected View getActionView() {
        if (actionViewReference == null) {
            return null;
        }

        return actionViewReference.get();
    }

    public void onDestroy() {
        actionViewReference = null;
        mBrowserFragment = null;
    }

    public void onPageStarted() {}
    public void onPageCommitVisible() {}
    public void onPageFinished() {}
    public void runAction() {}
}
