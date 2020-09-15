/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity.screenshots;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.xlab.vbrowser.activity.MainActivity;
import com.xlab.vbrowser.activity.TestHelper;
import com.xlab.vbrowser.activity.helpers.MainActivityFirstrunTestRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FirstRunScreenshots extends ScreenshotTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityFirstrunTestRule(true);

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Test
    public void takeScreenshotsOfFirstrun() throws UiObjectNotFoundException {
        assertTrue(device.findObject(new UiSelector()
                .text(getString(com.xlab.vbrowser.R.string.firstrun_defaultbrowser_title))
                .enabled(true)
        ).waitForExists(waitingTime));

        Screengrab.screenshot("Onboarding_1_View");
        TestHelper.nextBtn.click();

        assertTrue(device.findObject(new UiSelector()
                .text(getString(com.xlab.vbrowser.R.string.firstrun_search_title))
                .enabled(true)
        ).waitForExists(waitingTime));

        Screengrab.screenshot("Onboarding_2_View");
        TestHelper.nextBtn.click();

        assertTrue(device.findObject(new UiSelector()
                .text(getString(com.xlab.vbrowser.R.string.firstrun_shortcut_title))
                .enabled(true)
        ).waitForExists(waitingTime));

        Screengrab.screenshot("Onboarding_3_View");
        TestHelper.nextBtn.click();

        assertTrue(device.findObject(new UiSelector()
                .text(getString(com.xlab.vbrowser.R.string.firstrun_privacy_title))
                .enabled(true)
        ).waitForExists(waitingTime));

        Screengrab.screenshot("Onboarding_last_View");
    }
}
