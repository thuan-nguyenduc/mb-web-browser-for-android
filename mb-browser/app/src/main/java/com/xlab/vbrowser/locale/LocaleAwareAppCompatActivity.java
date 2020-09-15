/*Copyright by MonnyLab*/

package com.xlab.vbrowser.locale;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.activity.SettingsActivity;
import com.xlab.vbrowser.bookmark.activity.BookmarkActivity;
import com.xlab.vbrowser.downloadmanagers.DownloadManagerActivity;
import com.xlab.vbrowser.history.activity.HistoryActivity;
import com.xlab.vbrowser.utils.Settings;

import java.util.Locale;

public abstract class LocaleAwareAppCompatActivity
        extends AppCompatActivity{

    private volatile Locale mLastLocale;

    protected Settings settings;

    /**
     * Is called whenever the application locale has changed. Your Activity must either update
     * all localised Strings, or replace itself with an updated version.
     */
    public abstract void applyLocale();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = Settings.getInstance(this);
        applyCurrentTheme();
        Locales.initializeLocale(this);

        mLastLocale = LocaleManager.getInstance().getCurrentLocale(getApplicationContext());

        LocaleManager.getInstance().updateConfiguration(this, mLastLocale);

        super.onCreate(savedInstanceState);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        final LocaleManager localeManager = LocaleManager.getInstance();

        localeManager.correctLocale(this, getResources(), getResources().getConfiguration());

        final Locale changed = localeManager.onSystemConfigurationChanged(this, getResources(), newConfig, mLastLocale);

        if (changed != null) {
            LocaleManager.getInstance().updateConfiguration(this, changed);
            applyLocale();
            setLayoutDirection(getWindow().getDecorView(), changed);
        }

        super.onConfigurationChanged(newConfig);
    }

    /**
     * Force set layout direction to RTL or LTR by Locale.
     *
     * @param view
     * @param locale
     */
    public static void setLayoutDirection(View view, Locale locale) {
        switch (TextUtilsCompat.getLayoutDirectionFromLocale(locale)) {
            case ViewCompat.LAYOUT_DIRECTION_RTL:
                ViewCompat.setLayoutDirection(view, ViewCompat.LAYOUT_DIRECTION_RTL);
                break;
            case ViewCompat.LAYOUT_DIRECTION_LTR:
            default:
                ViewCompat.setLayoutDirection(view, ViewCompat.LAYOUT_DIRECTION_LTR);
                break;
        }
    }

    /**
     * Open the app preferences. Activities must not open SettingsActivity themselves, or any
     * locale changes performed by SettingsActivity might not be correctly detected.
     */
    public void openPreferences() {
        final Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivityForResult(settingsIntent, 0);
    }

    public void openDownloadManager() {
        final Intent dmIntent = new Intent(this, DownloadManagerActivity.class);
        startActivity(dmIntent);
    }

    public void openHistory() {
        final Intent historyIntent = new Intent(this, HistoryActivity.class);
        startActivity(historyIntent);
    }

    public void openBookmark() {
        final Intent bookmarkIntent = new Intent(this, BookmarkActivity.class);
        startActivity(bookmarkIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        onConfigurationChanged(getResources().getConfiguration());

        if (resultCode == SettingsActivity.ACTIVITY_RESULT_LOCALE_CHANGED) {
            applyLocale();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((LocaleAwareApplication) getApplicationContext()).onActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((LocaleAwareApplication) getApplicationContext()).onActivityPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void applyCurrentTheme() {
        setTheme(settings.isEnabledNightMode() ? R.style.AppTheme_Dark : R.style.AppTheme);
    }
}
