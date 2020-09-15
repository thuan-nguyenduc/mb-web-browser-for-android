package com.tonyodev.fetch2.util;

import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadUtils {
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
    public static String parseContentDisposition(String contentDisposition) {
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

    public static String addAdditionalIntoFilename(String fileName) {
        int lastDotIndexOf = fileName.lastIndexOf(".");
        String noExtFileName = fileName;
        String ext = "";

        if (lastDotIndexOf > 0) {
            noExtFileName = fileName.substring(0, lastDotIndexOf);

            if (lastDotIndexOf + 1 < fileName.length()) {
                ext = fileName.substring(lastDotIndexOf + 1);
            }
        }

        return noExtFileName + "-" + (new Date()).getTime() + "." + ext;
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
}
