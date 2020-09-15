package com.xlab.vbrowser.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

import com.xlab.vbrowser.BuildConfig;

import java.io.File;

/**
 * Created by nguyenducthuan on 3/20/18.
 */

public class FileExtUtils {
    private static final String FILE_PROVIDER_EXTENSION = ".fileprovider";

    private static String getExt(String file) {
        int last = file.lastIndexOf(".");

        if(last > 0 && last < file.length()) {
            return file.substring(last + 1);
        }

        return "*";
    }

    public static void openFile(Context context, String filePath) {
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                return;
            }

            final String fileExtension = FileExtUtils.getExt(filePath.toLowerCase());
            final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

            final Uri uriForFile = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + FILE_PROVIDER_EXTENSION, file);
            Intent openFileIntent = IntentUtils.createOpenFileIntent(uriForFile, mimeType);

            if (IntentUtils.activitiesFoundForIntent(context, openFileIntent)) {
                context.startActivity(openFileIntent);
            } else {
                openFileIntent = IntentUtils.createOpenFileIntent(uriForFile, "*/*");
                context.startActivity(openFileIntent);
            }
        }
        catch(Exception e) {

        }
    }
}
