package com.xlab.vbrowser.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

public class AppUtils {

    public static void shareContent(Context context, String message, String title) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message );
        context.startActivity(Intent.createChooser(intent, title));
    }
    public static void openUrl(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse(url));
        context.startActivity(i);
    }

    public static void openAppOrMarket(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

        if (launchIntent == null) {
            //App is not installed yet
            AppUtils.openApp(context, packageName);
        }
        else {
            //App is installed yet, open it
            context.startActivity(launchIntent);
        }
    }

    public static void openApp(Context context, String packageName)
    {
        try
        {
            Intent rateIntent = openIntentForUrl(context,"market://details", packageName);
            context.startActivity(rateIntent);
        }
        catch (ActivityNotFoundException e)
        {
            Intent rateIntent = openIntentForUrl(context,"https://play.google.com/store/apps/details", packageName);
            context.startActivity(rateIntent);
        }
    }

    private static Intent openIntentForUrl(Context context, String url, String packageName)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, packageName)));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21)
        {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        }
        else
        {
            //noinspection deprecation
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
}
