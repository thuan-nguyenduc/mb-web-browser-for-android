/*Copyright by MonnyLab*/

package com.xlab.vbrowser.utils;

import android.content.Context;

public class HardwareUtils {
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(com.xlab.vbrowser.R.bool.is_tablet);
    }
}
