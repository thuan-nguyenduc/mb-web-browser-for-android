/*Copyright by MonnyLab*/

package com.xlab.vbrowser.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.xlab.vbrowser.search.SearchEngineManager;
import com.xlab.vbrowser.utils.UrlUtils;

import java.util.Collections;

public class ManualAddSearchEngineSettingsFragment extends SettingsFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // We've checked that this cast is legal in super.onAttach.
        ((ActionBarUpdater) getActivity()).updateIcon(com.xlab.vbrowser.R.drawable.ic_close);
   }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(com.xlab.vbrowser.R.menu.menu_search_engine_manual_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case com.xlab.vbrowser.R.id.menu_save_search_engine:
                final View rootView = getView();
                final String engineName = ((EditText) rootView.findViewById(com.xlab.vbrowser.R.id.edit_engine_name)).getText().toString();
                final String searchQuery = ((EditText) rootView.findViewById(com.xlab.vbrowser.R.id.edit_search_string)).getText().toString();

                final SharedPreferences sharedPreferences = getSearchEngineSharedPreferences();
                if (!validateSearchFields(engineName, searchQuery, sharedPreferences)) {
                    Snackbar.make(rootView, com.xlab.vbrowser.R.string.search_add_error, Snackbar.LENGTH_SHORT).show();
                } else {
                    SearchEngineManager.addSearchEngine(sharedPreferences, getActivity(), engineName, searchQuery);
                    Snackbar.make(rootView, com.xlab.vbrowser.R.string.search_add_confirmation, Snackbar.LENGTH_SHORT).show();
                    getFragmentManager().popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static boolean validateSearchFields(String engineName, String searchString, SharedPreferences sharedPreferences) {
        if (TextUtils.isEmpty(engineName)) {
            return false;
        }

        if (sharedPreferences.getStringSet(SearchEngineManager.PREF_KEY_CUSTOM_SEARCH_ENGINES,
                Collections.<String>emptySet()).contains(engineName)) {
            return false;
        }

        return UrlUtils.isValidSearchQueryUrl(searchString);
    }
}
