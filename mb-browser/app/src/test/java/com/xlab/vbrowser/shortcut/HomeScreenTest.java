/*Copyright by MonnyLab*/

package com.xlab.vbrowser.shortcut;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class HomeScreenTest {
    @Test
    public void testGenerateTitleFromUrl() {
        assertEquals("mozilla.org", HomeScreen.generateTitleFromUrl("https://www.mozilla.org"));
        assertEquals("facebook.com", HomeScreen.generateTitleFromUrl("http://m.facebook.com/home"));
    }
}