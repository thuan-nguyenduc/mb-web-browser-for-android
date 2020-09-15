package com.xlab.vbrowser.styles;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.WindowManager;

import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.utils.Settings;

public class ScreenUtils {
    private static float getSystemBrightness(Context context) {
        try {
            float curBrightnessValue = android.provider.Settings.System.getInt(
                    context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);

            return curBrightnessValue / 255;
        } catch (android.provider.Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    /**
     * Get activity instance from desired context.
     */
    private static Activity getActivity(Context context) {
        try {
            if (context == null) return null;
            if (context instanceof Activity) return (Activity) context;
            if (context instanceof ContextWrapper)
                return getActivity(((ContextWrapper) context).getBaseContext());
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static void changeNightmodeBrightness(Settings settings, Context context) {
        if (settings == null) {
            settings = Settings.getInstance(context);
        }

        Activity activity = getActivity(context);

        if (activity == null) {
            return;
        }

        boolean isEnabledNightMode = settings.isEnabledNightMode();
        float systemBrightness = getSystemBrightness(context);
        Log.d("getSystemBrighness", systemBrightness + "");

        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();

        if (isEnabledNightMode) {
            int nightModeBrightness = settings.getNightModeBrighness();
            float fBrightness = nightModeBrightness / ((float)Constants.NIGHTMODE_BRIGHTNESS_MAX);

            if (systemBrightness > 0) {
                fBrightness = fBrightness * systemBrightness;
            }
            else if (fBrightness > 0.3) {
                fBrightness = 0.3f;
            }

            lp.screenBrightness = fBrightness;
            activity.getWindow().setAttributes(lp);
        }
        else {
            //-1, means using current brightness system
            lp.screenBrightness = -1;
            activity.getWindow().setAttributes(lp);
        }
    }
}
