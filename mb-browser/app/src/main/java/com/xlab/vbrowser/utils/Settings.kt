/*Copyright by MonnyLab*/

package com.xlab.vbrowser.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager

import com.xlab.vbrowser.R
import com.xlab.vbrowser.prefs.Constants
import com.xlab.vbrowser.search.SearchEngine

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
class Settings private constructor(context: Context?) {
    companion object {
        private var instance: Settings? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context?): Settings {
            if (instance == null) {
                instance = Settings(context?.applicationContext)
            }
            return instance ?: throw AssertionError("Instance cleared")
        }
    }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources: Resources? = context?.resources

    val defaultSearchEngineName: String?
        get() = preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null)

    fun shouldBlockImages(): Boolean =
             preferences.getBoolean(
                    resources?.getString(R.string.pref_key_performance_block_images),
                    false)

    fun isSetDefault(): Boolean =
            preferences.getBoolean(
                    resources?.getString(R.string.pref_key_default_browser),
                    false)

    fun setDefault(isDefault: Boolean) {
        preferences.edit()
                .putBoolean(resources?.getString(R.string.pref_key_default_browser), isDefault)
                .apply()
    }

    fun shouldShowFirstrun(): Boolean =
            !preferences.getBoolean(Constants.FIRSTRUN_PREF, false)

    fun setFirstRun(firstRun: Boolean) {
        preferences.edit()
                .putBoolean(Constants.FIRSTRUN_PREF, firstRun)
                .apply()
    }

    fun shouldUseSecureMode(): Boolean =
            preferences.getBoolean(getPreferenceKey(R.string.pref_key_secure), false)

    fun shouldLogoutWhenRemovingTask(): Boolean =
            preferences.getBoolean(getPreferenceKey(R.string.pref_key_logout_when_removing_task), false)

    fun setDefaultSearchEngine(searchEngine: SearchEngine) {
        preferences.edit()
                .putString(getPreferenceKey(R.string.pref_key_search_engine), searchEngine.name)
                .apply()
    }

    fun shouldAutocompleteFromShippedDomainList() =
            preferences.getBoolean(
                    getPreferenceKey(R.string.pref_key_autocomplete_preinstalled),
                    true)

    fun shouldAutocompleteFromCustomDomainList() =
            preferences.getBoolean(
                    getPreferenceKey(R.string.pref_key_autocomplete_custom),
                    false)

    //Speed mode
    fun shouldEnterSpeedMode(): Boolean =
            preferences.getBoolean(Constants.PREF_SPEEDMODE_ENABLED_KEY,
                    false)

    fun enableSpeedMode(isEnabled: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_SPEEDMODE_ENABLED_KEY, isEnabled)
                .apply()
    }

    //Request desktop site
    fun shouldRequestDesktopSite(): Boolean =
            preferences.getBoolean(Constants.PREF_REQUEST_DESKTOP_SITE_KEY,
                    false)

    fun setRequestDesktopSite(isRequestedDesktopSite: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_REQUEST_DESKTOP_SITE_KEY, isRequestedDesktopSite)
                .apply()
    }

    //User agent
    fun getMobileUserAgentString(): String =
            preferences.getString(Constants.PREF_MOBILE_USER_AGENT_KEY,
                    "")

    fun setMobileUserAgentString(userAgent: String) {
        preferences.edit()
                .putString(Constants.PREF_MOBILE_USER_AGENT_KEY, userAgent)
                .apply()
    }

    //Download media tooltip
    fun isShownDownloadMediaTooltip(): Boolean =
            preferences.getBoolean(Constants.PREF_IS_SHOWN_DOWNLOAD_MEDIA_TOOLTIP_KEY,
                    false)

    fun setShownDownloadMediaTooltip(isShown: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_IS_SHOWN_DOWNLOAD_MEDIA_TOOLTIP_KEY, isShown)
                .apply()
    }

    //Reader mode tooltip
    fun isShownReaderModeTooltip(): Boolean =
            preferences.getBoolean(Constants.PREF_IS_SHOWN_READER_MODE_TOOLTIP_KEY,
                    false)

    fun setShownReaderModeTooltip(isShown: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_IS_SHOWN_READER_MODE_TOOLTIP_KEY, isShown)
                .apply()
    }

    //Nightmode
    fun isShownFirstTimeNightModeDialog(): Boolean =
            preferences.getBoolean(Constants.PREF_IS_FIRSTTIME_SHOWN_NIGHTMODE_DIALOG_KEY,
                    true)

    fun setShownFirstTimeNightModeDialog(isShown: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_IS_FIRSTTIME_SHOWN_NIGHTMODE_DIALOG_KEY, isShown)
                .apply()
    }

    fun isEnabledNightMode(): Boolean =
            preferences.getBoolean(Constants.PREF_NIGHTMODE_ENABLED_KEY,
                    false)

    fun setEnabledNightMode(isEnabled: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_NIGHTMODE_ENABLED_KEY, isEnabled)
                .apply()
    }

    fun getNightModeBrighness(): Int =
            preferences.getInt(Constants.PREF_NIGHTMODE_BRIGHTNESS_KEY,
                    Constants.DEFAULT_NIGHTMODE_BRIGHTNESS)

    fun setNightModeBrightness(brightness: Int) {
        preferences.edit()
                .putInt(Constants.PREF_NIGHTMODE_BRIGHTNESS_KEY, brightness)
                .apply()
    }

    //Quick dial
    fun isFirstTimeLoadingQuickDial(): Boolean =
            preferences.getBoolean(Constants.PREF_IS_FIRSTTIME_LOADING_QUICK_DIAL_KEY,
                    true)

    fun setFirstTimeLoadingQuickDial(isFirstTime: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_IS_FIRSTTIME_LOADING_QUICK_DIAL_KEY, isFirstTime)
                .apply()
    }

    //Incognito
    fun isIncognitoEnabled(): Boolean =
            preferences.getBoolean(Constants.PREF_INCOGNITO_ENABLED_KEY,
                    false)

    fun setIncognitoEnabled(isEnabled: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_INCOGNITO_ENABLED_KEY, isEnabled)
                .apply()
    }

    private fun getPreferenceKey(resourceId: Int): String? =
            resources?.getString(resourceId)

    //Rate
    fun isRated(): Boolean =
            preferences.getBoolean(Constants.PREF_IS_RATED_KEY,
                    false)

    fun setRated(isRated: Boolean) {
        preferences.edit()
                .putBoolean(Constants.PREF_IS_RATED_KEY, isRated)
                .apply()
    }
}
