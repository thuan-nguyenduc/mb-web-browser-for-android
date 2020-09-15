/*Copyright by MonnyLab*/

package com.xlab.vbrowser.ext

import android.content.Context

// Extension functions for the Context class

/**
 * The (visible) version name of the application, as specified by the <manifest> tag's versionName
 * attribute. E.g. "2.0".
 */
val Context.appVersionName: String?
    get() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName
    }
