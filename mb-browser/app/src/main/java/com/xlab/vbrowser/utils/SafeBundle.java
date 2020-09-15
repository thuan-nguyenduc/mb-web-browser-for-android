/*
 Copyright by MonnyLab
 */

package com.xlab.vbrowser.utils;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

/**
 * See SafeIntent for more background: applications can put garbage values into Bundles. This is primarily
 * experienced when there's garbage in the Intent's Bundle. However that Bundle can contain further bundles,
 * and we need to handle those defensively too.
 *
 * See bug 1090385 for more.
 */
public class SafeBundle {
    private static final String LOGTAG = SafeBundle.class.getSimpleName();

    private final Bundle bundle;

    public SafeBundle(final Bundle bundle) {
        this.bundle = bundle;
    }

    public String getString(final String name) {
        try {
            return bundle.getString(name);
        } catch (OutOfMemoryError e) {
            Log.w(LOGTAG, "Couldn't get bundle items: OOM. Malformed?");
            return null;
        } catch (RuntimeException e) {
            Log.w(LOGTAG, "Couldn't get bundle items.", e);
            return null;
        }
    }

    public <T extends Parcelable> T getParcelable(final String name) {
        try {
            return bundle.getParcelable(name);
        } catch (OutOfMemoryError e) {
            Log.w(LOGTAG, "Couldn't get bundle items: OOM. Malformed?");
            return null;
        } catch (RuntimeException e) {
            Log.w(LOGTAG, "Couldn't get bundle items.", e);
            return null;
        }
    }
}
