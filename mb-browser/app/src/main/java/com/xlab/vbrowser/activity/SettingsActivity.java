/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.settings.SettingsFragment;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.styles.ThemeUtils;
import com.xlab.vbrowser.trackers.GaReport;

public class SettingsActivity extends LocaleAwareAppCompatActivity implements SettingsFragment.ActionBarUpdater {
    public static final int ACTIVITY_RESULT_LOCALE_CHANGED = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.xlab.vbrowser.R.layout.activity_settings);

        View nightModeView = findViewById(R.id.nightModeView);
        ThemeUtils.loadNightmode(nightModeView, settings, this);

        Toolbar toolbar = findViewById(com.xlab.vbrowser.R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        actionBar.setDisplayShowHomeEnabled(true);

        final PreferenceFragment fragment = SettingsFragment.newInstance(getIntent().getExtras(), SettingsFragment.SettingsScreen.MAIN);

        getFragmentManager().beginTransaction()
                .replace(com.xlab.vbrowser.R.id.container, fragment)
                .commit();

        // Ensure all locale specific Strings are initialised on first run, we don't set the title
        // anywhere before now (the title can only be set via AndroidManifest, and ensuring
        // that that loads the correct locale string is tricky).
        applyLocale();

        GaReport.sendReportScreen(getBaseContext(), SettingsActivity.class.getName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void applyLocale() {
        setTitle(com.xlab.vbrowser.R.string.menu_settings);
    }

    @Override
    public void updateTitle(int titleResId) {
        setTitle(titleResId);
    }

    @Override
    public void updateIcon(int iconResId) {
        getSupportActionBar().setHomeAsUpIndicator(iconResId);
    }
}
