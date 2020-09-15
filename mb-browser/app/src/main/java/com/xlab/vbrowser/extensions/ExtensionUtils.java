package com.xlab.vbrowser.extensions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import it.sephiroth.android.library.tooltip.Tooltip;

/**
 * Created by nguyenducthuan on 1/22/18.
 */

public class ExtensionUtils {
    public static String loadScript(@NonNull final Context context, @NonNull final @RawRes int resourceID) {
        try (final BufferedReader fileReader =
                     new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceID), StandardCharsets.UTF_8))) {

            final StringBuilder outputBuffer = new StringBuilder();

            String line;
            while ((line = fileReader.readLine()) != null) {
                outputBuffer.append(line).append("\n");
            }

            return outputBuffer.toString();
        } catch (final IOException e) {
            return "";
        }
    }

    /*********** Tooltips ********************/
    public static void showToolTipDialogBox(Context context, View view, String tip) {
        Tooltip.make(context,
                new Tooltip.Builder(101)
                        .anchor(view, Tooltip.Gravity.BOTTOM)
                        .closePolicy(new Tooltip.ClosePolicy()
                                .insidePolicy(true, false)
                                .outsidePolicy(true, false), 20000)
                        .activateDelay(900)
                        .showDelay(400)
                        .text(tip)
                        .maxWidth(600)
                        .withArrow(true)
                        .withOverlay(true).build()
        ).show();
    }

    public static void showBlink(Context context, View view) {
        /*Tooltip.make(context,
                new Tooltip.Builder(101)
                        .anchor(view, Tooltip.Gravity.BOTTOM)
                        .closePolicy(new Tooltip.ClosePolicy()
                                .insidePolicy(true, false)
                                .outsidePolicy(true, false), 2000)
                        .activateDelay(900)
                        .showDelay(400)
                        .text("")
                        .withOverlay(true)
                        .build()
        ).show();*/
    }
}
