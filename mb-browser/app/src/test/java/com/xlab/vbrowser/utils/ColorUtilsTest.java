/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.utils;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ColorUtilsTest {
    @Test
    public void testGetReadableTextColor() {
        assertEquals(Color.BLACK, ColorUtils.getReadableTextColor(Color.WHITE));
        assertEquals(Color.WHITE, ColorUtils.getReadableTextColor(Color.BLACK));

        // Slack
        assertEquals(Color.BLACK, ColorUtils.getReadableTextColor(0xfff6f4ec));

        // Google+
        assertEquals(Color.WHITE, ColorUtils.getReadableTextColor(0xffdb4437));

        // Telegram
        assertEquals(Color.WHITE, ColorUtils.getReadableTextColor(0xff527da3));

        // IRCCloud
        assertEquals(Color.BLACK, ColorUtils.getReadableTextColor(0xfff2f7fc));

        // Yahnac
        assertEquals(Color.WHITE, ColorUtils.getReadableTextColor(0xfff57c00));
    }
}