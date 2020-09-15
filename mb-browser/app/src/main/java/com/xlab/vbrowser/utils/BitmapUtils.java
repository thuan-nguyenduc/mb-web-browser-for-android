/*Copyright by MonnyLab*/
package com.xlab.vbrowser.utils;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class BitmapUtils {
    public static String getBase64EncodedDataUriFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        final String encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
        return "data:image/png;base64," + encodedImage;
    }
}
