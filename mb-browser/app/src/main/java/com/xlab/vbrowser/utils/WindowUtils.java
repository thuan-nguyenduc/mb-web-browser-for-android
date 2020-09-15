package com.xlab.vbrowser.utils;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class WindowUtils {
    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    public static void switchToImmersiveMode(final Activity activity) {
        if (activity == null) {
            return;
        }

        final Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Show the system bars again.
     */
    public static void exitImmersiveModeIfNeeded(final Activity activity) {
        if (activity == null) {
            return;
        }

        if ((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON & activity.getWindow().getAttributes().flags) == 0) {
            // We left immersive mode already.
            return;
        }

        final Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public static void setStatusBarColor(final Activity activity, @ColorRes final int colorResId,
                                         final boolean isDarkTheme) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        final Window window = activity.getWindow();
        final int backgroundColor = ContextCompat.getColor(activity, colorResId);
        window.setStatusBarColor(backgroundColor);

        final View view = window.getDecorView();
        int flags = view.getSystemUiVisibility();
        if (isDarkTheme) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        view.setSystemUiVisibility(flags);
    }

}
