/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;

import com.xlab.vbrowser.utils.Settings;

/**
 * Preference for setting the default search engine.
 */
public class SearchEnginePreference extends Preference implements SharedPreferences.OnSharedPreferenceChangeListener {
    final Context context;

    public SearchEnginePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public SearchEnginePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    @Override
    protected void onAttachedToActivity() {
        setTitle(SearchEngineManager.getInstance().getDefaultSearchEngine(getContext()).getName());
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onAttachedToActivity();
    }

    @Override
    protected void onPrepareForRemoval() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(context.getResources().getString(com.xlab.vbrowser.R.string.pref_key_search_engine))) {
            setTitle(Settings.getInstance(context).getDefaultSearchEngineName());
        }
    }
}
