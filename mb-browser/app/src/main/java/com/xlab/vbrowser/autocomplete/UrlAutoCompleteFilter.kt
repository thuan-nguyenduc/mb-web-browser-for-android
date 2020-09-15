/*Copyright by MonnyLab*/

package com.xlab.vbrowser.autocomplete

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.xlab.vbrowser.history.service.HistoryService
import com.xlab.vbrowser.locale.LocaleManager
import com.xlab.vbrowser.locale.Locales
import com.xlab.vbrowser.quickdial.service.QuickDialService
import com.xlab.vbrowser.utils.BackgroundTask
import com.xlab.vbrowser.utils.IBackgroundTask
import com.xlab.vbrowser.utils.Settings
import com.xlab.vbrowser.widget.InlineAutocompleteEditText
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.util.*

class UrlAutoCompleteFilter : InlineAutocompleteEditText.OnFilterListener {
    companion object {
        private val LOG_TAG = "UrlAutoCompleteFilter"
    }

    private var settings : Settings? = null

    //private val customDomains = LinkedHashSet<String>()
    private val preInstalledDomains = LinkedHashSet<String>()

    override fun onFilter(rawSearchText: String, view: InlineAutocompleteEditText?) {
        if (view == null) {
            return
        }

        // Search terms are all lowercase already, we just need to lowercase the search text
        val searchText = rawSearchText.toLowerCase(Locale.US)

        /*if (!searchText.contains(".")) {
            return
        }*/

        settings?.let {
            /*if (it.shouldAutocompleteFromCustomDomainList()) {
                val autocomplete = tryToAutocomplete(searchText, customDomains)
                if (autocomplete != null) {
                    view.onAutocomplete(prepareAutocompleteResult(rawSearchText, autocomplete))
                    return
                }
            }*/

            BackgroundTask(object: IBackgroundTask{
                var autocomplete: String? = ""

                override fun run() {
                    //Query from MostVisited
                    autocomplete = HistoryService.getSuggestionUrl(searchText, view.context)
                    if (!TextUtils.isEmpty(autocomplete)) {
                        return
                    }

                    //Query from QuickDial
                    autocomplete = QuickDialService.getSuggestionUrl(searchText, view.context)
                    if (!TextUtils.isEmpty(autocomplete)) {
                        return
                    }

                    //Query from search term db
                    autocomplete = HistoryService.getSearchTerm(searchText, view.context)
                    if (!TextUtils.isEmpty(autocomplete)) {
                        return
                    }

                    //Query from shipped domain file
                    if (it.shouldAutocompleteFromShippedDomainList()) {
                        autocomplete = tryToAutocomplete(searchText, preInstalledDomains)
                    }

                }

                override fun onComplete() {
                    if (!TextUtils.isEmpty(autocomplete) && autocomplete!!.contains(rawSearchText)) {
                        view.onAutocomplete(prepareAutocompleteResult(rawSearchText, autocomplete!!))
                    }
                }
            }).execute()
        }
    }

    private fun tryToAutocomplete(searchText: String, domains: Set<String>): String? {
        domains.forEach {
            val wwwDomain = "www." + it
            if (wwwDomain.startsWith(searchText)) {
                return wwwDomain
            }

            if (it.startsWith(searchText)) {
                return it
            }
        }

        return null
    }

    internal fun onDomainsLoaded(domains: Set<String>/*, customDomains: Set<String>*/) {
        this.preInstalledDomains.addAll(domains)
        //this.customDomains.addAll(customDomains)
    }

    fun initialize(context: Context, loadDomainsFromDisk: Boolean = true) {
        settings = Settings.getInstance(context)

        if (loadDomainsFromDisk) {
            launch(UI) {
                //val domains = async(CommonPool) { loadDomains(context) }
                BackgroundTask(object: IBackgroundTask{
                    override fun run() {
                        var domains = loadDomains(context)
                        onDomainsLoaded(domains)
                    }

                    override fun onComplete() {

                    }
                }).execute()

                //val customDomains = async(CommonPool) { CustomAutoComplete.loadCustomAutoCompleteDomains(context) }


            }
        }
    }

    private fun loadDomains(context: Context): Set<String> {
        val domains = LinkedHashSet<String>()

        try {
            val availableLists = getAvailableDomainLists(context)

            // First load the country specific lists following the default locale order
            var currentLocale = LocaleManager.getInstance().getCurrentLocale(context)

            if (currentLocale == null) {
                currentLocale = Locale.getDefault()
                var country = currentLocale?.country?.toLowerCase(Locale.US)

                if (country != null) {
                    loadDomainsForLanguage(context, domains, country)
                }
            }
            else {
                var settingCountries = Locales.getCountriesFromLanguage(context, currentLocale.language?.toLowerCase(Locale.US));

                settingCountries
                        .asSequence()
                        .filter { availableLists.contains(it) }
                        .forEach {
                            loadDomainsForLanguage(context, domains, it)
                        }
            }
        } catch(e: Exception) {}

        // And then add domains from the global list
        loadDomainsForLanguage(context, domains, "global")

        return domains
    }

    private fun getAvailableDomainLists(context: Context): Set<String> {
        val availableDomains = HashSet<String>()

        val assetManager = context.assets

        try {
            Collections.addAll(availableDomains, *assetManager.list("domains"))
        } catch (e: IOException) {
            Log.w(LOG_TAG, "Could not list domain list directory")
        }

        return availableDomains
    }

    private fun loadDomainsForLanguage(context: Context, domains: MutableSet<String>, country: String) {
        val assetManager = context.assets

        try {
            domains.addAll(
                    assetManager.open("domains/" + country).bufferedReader().readLines())
        } catch (e: IOException) {
            Log.w(LOG_TAG, "Could not load domain list: " + country)
        }
    }

    /**
     * Our autocomplete list is all lower case, however the search text might be mixed case.
     * Our autocomplete EditText code does more string comparison, which fails if the suggestion
     * doesn't exactly match searchText (ie. if casing differs). It's simplest to just build a suggestion
     * that exactly matches the search text - which is what this method is for:
     */
    private fun prepareAutocompleteResult(rawSearchText: String, lowerCaseResult: String) =
            rawSearchText + lowerCaseResult.substring(rawSearchText.length)
}
