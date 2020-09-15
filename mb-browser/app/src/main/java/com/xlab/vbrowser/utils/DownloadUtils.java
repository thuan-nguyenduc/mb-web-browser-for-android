/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License. */

package com.xlab.vbrowser.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.web.Download;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class DownloadUtils {

    private final static String DownloadListDir =  "/Downloads/";

    public static String requestFileName(Download download, String refererUrl) {
        try {
            OkHttpClient client = new OkHttpClient();
            final String cookie = CookieManager.getInstance().getCookie(refererUrl);

            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().head();

            if (download.getUserAgent() != null) {
                builder.addHeader("User-Agent", download.getUserAgent());
            }

            if (cookie != null) {
                builder.addHeader("Cookie", cookie);
            }

            if (refererUrl != null) {
                builder.addHeader("Referer", refererUrl);
            }

            okhttp3.Request request = builder
                    .url(download.getUrl())
                    .build();

            Response response = client.newCall(request).execute();
            response.close();

            String contentDisposition = response.header("content-disposition");

            if (contentDisposition == null) {
                return null;
            }

            return parseContentDisposition(contentDisposition);
        }
        catch(Exception e) {
            return null;
        }
    }

    /**
     * Guess the name of the file that should be downloaded.
     *
     * This method is largely identical to {@link android.webkit.URLUtil#guessFileName}
     * which unfortunately does not implement RfC 5987.
     *
     * @param download the Download object containing information about the request
     * @return file name including extension
     */
    public static String guessFileName(Download download) {
        String contentDisposition = download.getContentDisposition();
        String url = download.getUrl();
        String mimeType = download.getMimeType();

        String filename = null;
        String extension = null;

        // Extract file name from content disposition header field
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition);
            if (filename != null) {
                int index = filename.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = filename.substring(index);
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            String decodedUrl = Uri.decode(url);
            if (decodedUrl != null) {
                int queryIndex = decodedUrl.indexOf('?');
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex);
                }
                if (!decodedUrl.endsWith("/")) {
                    int index = decodedUrl.lastIndexOf('/') + 1;
                    if (index > 0) {
                        filename = decodedUrl.substring(index);
                    }
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = "downloadfile";
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        int dotIndex = filename.indexOf('.');
        if (dotIndex < 0) {
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null) {
                    extension = "." + extension;
                }
            }
            if (extension == null) {
                if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                    if (mimeType.equalsIgnoreCase("text/html")) {
                        extension = ".html";
                    } else {
                        extension = ".txt";
                    }
                } else {
                    extension = ".bin";
                }
            }
        } else {
            if (mimeType != null) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                int lastDotIndex = filename.lastIndexOf('.');
                String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        filename.substring(lastDotIndex + 1));
                if (typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        extension = "." + extension;
                    }
                }
            }
            if (extension == null) {
                extension = filename.substring(dotIndex);
            }
            filename = filename.substring(0, dotIndex);
        }

        return filename + extension;
    }

    /**
     * Format as defined in RFC 2616 and RFC 5987
     * Only the attachment type is supported.
     */
    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment\\s*;\\s*filename\\s*=\\s*" +
                            "(\"((?:\\\\.|[^\"\\\\])*)\"|[^;]*)\\s*" +
                            "(?:;\\s*filename\\*\\s*=\\s*(utf-8|iso-8859-1)'[^']*'(\\S*))?",
                    Pattern.CASE_INSENSITIVE);

    @Nullable
    private static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);

            if (m.find()) {
                // If escaped string is found, decode it using the given encoding.
                String encodedFileName = m.group(4);
                String encoding = m.group(3);

                if (encodedFileName != null) {
                    return decodeHeaderField(encodedFileName, encoding);
                }

                // Return quoted string if available and replace escaped characters.
                String quotedFileName = m.group(2);

                if (quotedFileName != null) {
                    return quotedFileName.replaceAll("\\\\(.)", "$1");
                }

                // Otherwise try to extract the unquoted file name
                return m.group(1);
            }
        } catch (IllegalStateException | UnsupportedEncodingException ex) {
            // This function is defined as returning null when it can't parse the header
        }

        return null;
    }

    /**
     * Definition as per RFC 5987, section 3.2.1. (value-chars)
     */
    private static final Pattern ENCODED_SYMBOL_PATTERN =
            Pattern.compile("%[0-9a-f]{2}|[0-9a-z!#$&+-.^_`|~]", Pattern.CASE_INSENSITIVE);

    private static String decodeHeaderField(String field, String encoding)
            throws UnsupportedEncodingException {
        Matcher m = ENCODED_SYMBOL_PATTERN.matcher(field);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        while (m.find()) {
            String symbol = m.group();

            if (symbol.startsWith("%")) {
                stream.write(Integer.parseInt(symbol.substring(1), 16));
            } else {
                stream.write(symbol.charAt(0));
            }
        }

        return stream.toString(encoding);
    }

    public static String getMimeType(final Context context, @NonNull final Uri uri) {
        final ContentResolver cR = context.getContentResolver();
        final MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(cR.getType(uri));
        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    public static void deleteFileAndContents(final File file) throws Exception {
        if (file == null) {
            return;
        }
        if (file.exists()) {
            if (file.isDirectory()) {
                final File[] contents = file.listFiles();
                if (contents != null) {
                    for (final File content : contents) {
                        deleteFileAndContents(content);
                    }
                }
            }
            file.delete();
        }
    }

    public static String getETAString(Context context, long etaInMilliSeconds) {
        if (etaInMilliSeconds < 0) {
            return "";
        }
        int seconds = (int) (etaInMilliSeconds / 1000);
        long hours = seconds / 3600;
        seconds -= hours * 3600;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        if (hours > 0) {
            return context.getString(R.string.download_eta_hrs, hours, minutes, seconds);
        } else if (minutes > 0) {
            return context.getString(R.string.download_eta_min, minutes, seconds);
        } else {
            return context.getString(R.string.download_eta_sec, seconds);
        }
    }

    public static String getDownloadLongString(Context context, long downloadedBytes) {
        if (downloadedBytes < 0) {
            return "";
        }
        double kb = (double) downloadedBytes / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return context.getString(R.string.download_mb, decimalFormat.format(mb));
        } else if (kb >= 1) {
            return context.getString(R.string.download_kb, decimalFormat.format(kb));
        } else {
            return context.getString(R.string.download_bytes, downloadedBytes);
        }
    }

    public static String getDownloadSpeedString(Context context, long downloadedBytesPerSecond) {
        if (downloadedBytesPerSecond < 0) {
            return "";
        }
        double kb = (double) downloadedBytesPerSecond / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return context.getString(R.string.download_speed_mb, decimalFormat.format(mb));
        } else if (kb >= 1) {
            return context.getString(R.string.download_speed_kb, decimalFormat.format(kb));
        } else {
            return context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond);
        }
    }

    public static String[] getFilePath(Context context, String fileName) {
        final String dir = getSaveDir(context);

        if (TextUtils.isEmpty(dir)) {
            return new String [] {"", ""};
        }

        String newFile = dir + DownloadListDir + fileName;
        boolean first = true;

        while((new File(newFile)).exists() || first) {
            first = false;
            int lastDotIndexOf = fileName.lastIndexOf(".");
            String noExtFileName = fileName;
            String ext = "";

            if (lastDotIndexOf > 0) {
                noExtFileName = fileName.substring(0, lastDotIndexOf);

                if (lastDotIndexOf + 1 < fileName.length()) {
                    ext = fileName.substring(lastDotIndexOf + 1);
                }
            }

            fileName = noExtFileName + "-" + (new Date()).getTime() + "." + ext;
            newFile = dir + DownloadListDir + fileName;
        }

        return new String [] {newFile, fileName};
    }

    private static String getSaveDir(Context context) {
        try {
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (file.exists()) {
                return file.toString() + "/" + context.getString(R.string.download_folder);
            } else {
                File file2 = context.getNoBackupFilesDir();
                return file2.toString() + "/" + context.getString(R.string.download_folder);
            }
        }
        catch (Exception e) {
            return "";
        }
    }
}
