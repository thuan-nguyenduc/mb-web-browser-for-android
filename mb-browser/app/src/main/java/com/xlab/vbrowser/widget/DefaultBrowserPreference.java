/*Copyright by MonnyLab*/

package com.xlab.vbrowser.widget;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.Browsers;
import com.xlab.vbrowser.utils.SupportUtils;

public class DefaultBrowserPreference extends Preference {
    private Switch switchView;

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_default_browser);

        final String appName = getContext().getResources().getString(R.string.app_name);
        final String title = getContext().getResources().getString(R.string.preference_default_browser2, appName);

        setTitle(title);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        switchView = view.findViewById(R.id.switch_widget);

        update();
    }

    public void update() {
        if (switchView != null) {
            final Browsers browsers = new Browsers(getContext(), Browsers.TRADITIONAL_BROWSER_URL);
            com.xlab.vbrowser.utils.Settings settings = com.xlab.vbrowser.utils.Settings.getInstance(getContext());
            boolean isDefault = browsers.isDefaultBrowser(getContext());
            switchView.setChecked(isDefault);

            if (settings.isSetDefault() != isDefault) {
                settings.setDefault(isDefault);
                GaReport.sendReportEvent(getContext(), isDefault ? "SET_BROWSER_DEAULT" : "UNSET_BROWSER_DEFAULT", "SETTINGS");
            }
        }
    }

    @Override
    protected void onClick() {
        final Context context = getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openDefaultAppsSettings(context);
        } else {
            openSumoPage(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void openDefaultAppsSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // In some cases, a matching Activity may not exist (according to the Android docs).
            openSumoPage(context);
        }
    }

    private void openSumoPage(Context context) {
    }
}
