/*Copyright by MonnyLab*/

package com.xlab.vbrowser.ext

import android.content.res.AssetManager
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.json.JSONObject

// Extension functions for the AssetManager class

@SuppressFBWarnings(
        value = "SA_LOCAL_SELF_ASSIGNMENT",
        justification = "The Kotlin 1.1 compiler generates bytecode that contains unnecessary self assignments. This should be fixed in 1.2.")
fun AssetManager.readJSONObject(fileName: String) = JSONObject(open(fileName).bufferedReader().use {
    it.readText()
})
