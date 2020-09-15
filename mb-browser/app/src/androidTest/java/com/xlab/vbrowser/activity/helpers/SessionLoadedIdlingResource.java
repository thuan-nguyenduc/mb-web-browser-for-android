/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity.helpers;

import android.support.test.espresso.IdlingResource;

import com.xlab.vbrowser.session.SessionManager;

/**
 * An IdlingResource implementation that waits until the current session is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class SessionLoadedIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;

    @Override
    public String getName() {
        return SessionLoadedIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        final SessionManager sessionManager = SessionManager.getInstance();
        if (!sessionManager.hasSession()) {
            invokeCallback();
            return true;
        }

        if (sessionManager.getCurrentSession().getLoading().getValue()) {
            return false;
        } else {
            invokeCallback();
            return true;
        }
    }

    private void invokeCallback() {
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }
}
