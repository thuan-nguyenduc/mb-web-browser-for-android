/*Copyright by MonnyLab*/

package com.xlab.vbrowser.autocomplete

import android.app.Fragment
import com.xlab.vbrowser.R
import com.xlab.vbrowser.settings.SettingsFragment

class AutocompleteAddDomainFragment : Fragment() {
    override fun onResume() {
        super.onResume()

        val updater = activity as SettingsFragment.ActionBarUpdater
        updater.updateTitle(R.string.preference_autocomplete_title_add)
        updater.updateIcon(R.drawable.ic_close)
    }
}
