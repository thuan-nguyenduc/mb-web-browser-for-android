package com.xlab.vbrowser.styles;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.Settings;


public class ThemeUtils {
    public static void loadNightmode(View nightModeView, Settings settings, AppCompatActivity parentActivity) {
        if (nightModeView == null) {
            return;
        }

        if (settings == null) {
            settings = Settings.getInstance(nightModeView.getContext());
        }

        boolean enableNightMode = settings.isEnabledNightMode();
        nightModeView.setVisibility(enableNightMode ? View.VISIBLE : View.GONE);

        changeBrightness(nightModeView, settings, parentActivity);
    }

    public static void changeBrightness(View nightModeView, Settings settings, AppCompatActivity parentActivity) {
        if (nightModeView == null) {
            return;
        }

        if (settings == null) {
            settings = Settings.getInstance(nightModeView.getContext());
        }

        int nightModeBrightness = settings.getNightModeBrighness();
        float fBrightness = (Constants.NIGHTMODE_BRIGHTNESS_MAX - nightModeBrightness) / ((float)Constants.NIGHTMODE_BRIGHTNESS_MAX * 1.1f);

        nightModeView.setAlpha(fBrightness);

        WindowManager.LayoutParams lp = parentActivity.getWindow().getAttributes();
        if (settings.isEnabledNightMode()) {
            //Change brightness screen
            float brightness = nightModeBrightness / (float) 150;
            lp.screenBrightness = brightness;
        }
        else {
            //Using system brightness
            lp.screenBrightness = -1;
        }

        parentActivity.getWindow().setAttributes(lp);
    }

    public static void processNightmode(View nightModeView, Settings settings, AppCompatActivity parentActivity) {
        if (nightModeView == null || settings == null) {
            return;
        }

        if (settings == null) {
            settings = Settings.getInstance(nightModeView.getContext());
        }

        boolean enableNightMode = settings.isEnabledNightMode();
        enableNightMode = !enableNightMode;
        settings.setEnabledNightMode(enableNightMode);
        nightModeView.setVisibility(enableNightMode ? View.VISIBLE : View.GONE);

        changeBrightness(nightModeView, settings, parentActivity);

        GaReport.sendReportEvent(nightModeView.getContext(), String.valueOf(enableNightMode) , "ACTION_ENABLE_NIGHT_MODE" );
    }
}
