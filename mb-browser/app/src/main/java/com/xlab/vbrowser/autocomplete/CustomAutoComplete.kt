/*Copyright by MonnyLab*/

package com.xlab.vbrowser.autocomplete

import android.content.Context
import android.content.SharedPreferences
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") // That's just how Kotlin singletons work...
object CustomAutoComplete {
    private const val PREFERENCE_NAME = "custom_autocomplete"
    private const val KEY_DOMAINS = "domains"

    suspend fun loadCustomAutoCompleteDomains(context: Context): Set<String> =
            preferences(context).getStringSet(KEY_DOMAINS, HashSet())

    fun saveDomains(context: Context, domains: Set<String>) {
        preferences(context)
                .edit()
                .putStringSet(KEY_DOMAINS, domains)
                .apply()
    }

    private fun preferences(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
}
