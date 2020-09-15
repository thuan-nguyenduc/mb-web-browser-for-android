package com.xlab.vbrowser.favicon;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.xlab.vbrowser.utils.UrlUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by nguyenducthuan on 3/1/18.
 */

public class FaviconService {
    private final static String FAVICON_DIR_NAME = "favicon";

    public static void writeDefaultFavicon(Context context) {
        try {
            String [] favicons = context.getAssets().list("favicons");
            File faviconDir = new File(context.getNoBackupFilesDir(), FAVICON_DIR_NAME);

            if (!faviconDir.exists()) {
                faviconDir.mkdir();
            }

            for(String favicon: favicons) {
                AssetFileDescriptor assets = context.getAssets().openFd("favicons/" + favicon);

                Bitmap bitmap = BitmapFactory.decodeStream(assets.createInputStream());
                assets.close();

                File fileIcon = new File(faviconDir, favicon);

                //If file is exist, do nothing;
                if (fileIcon.exists()) {
                    continue;
                }

                //If not, write file
                FileOutputStream fos = new FileOutputStream(fileIcon);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
        }
        catch(Exception e) {
        }
    }

    public static void writeFavicon(Context context, String url, Bitmap bitmap) {
        try {
            url = UrlUtils.getMainHost(url);

            if (url == null) {
                return;
            }

            File faviconDir = new File(context.getNoBackupFilesDir(), FAVICON_DIR_NAME);

            //Create favicon dir if not exist
            if (!faviconDir.exists()) {
                faviconDir.mkdir();
            }

            File fileIcon = new File(faviconDir, url + ".png");

            //If file is exist, do nothing;
            if (fileIcon.exists()) {
                return;
            }

            //If not, write file
            FileOutputStream fos = new FileOutputStream(fileIcon);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearFaviconDir(Context context) {
        try {
            File faviconDir = new File(context.getNoBackupFilesDir(), FAVICON_DIR_NAME);

           if (faviconDir.exists()) {
               faviconDir.delete();
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFavicon(Context context, String url) {
        try {
            url = UrlUtils.getMainHost(url);

            if (url == null) {
                return null;
            }

            File faviconDir = new File(context.getNoBackupFilesDir(), FAVICON_DIR_NAME);
            File fileIcon = new File(faviconDir, url + ".png");

            //If the file is not exist
            if (!fileIcon.exists()) {
                return null;
            }

            return fileIcon.getAbsolutePath();
        }
        catch(Exception e) {
            return null;
        }
    }
}
