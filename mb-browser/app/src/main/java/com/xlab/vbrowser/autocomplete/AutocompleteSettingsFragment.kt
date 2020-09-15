/*Copyright by MonnyLab*/

package com.xlab.vbrowser.autocomplete

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import com.xlab.vbrowser.R
import com.xlab.vbrowser.settings.SettingsFragment

class AutocompleteSettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.autocomplete)
    }

    override fun onResume() {
        super.onResume()

        val updater = activity as SettingsFragment.ActionBarUpdater
        updater.updateTitle(R.string.preference_subitem_autocomplete)
        updater.updateIcon(R.drawable.ic_back)
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
        preference?.let {
            if (it.key == getString(R.string.pref_key_screen_custom_domains)) {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, AutocompleteCustomDomainsFragment())
                        .addToBackStack(null)
                        .commit()
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }
}
