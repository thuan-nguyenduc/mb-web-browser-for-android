/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.action.ViewActions.click;
import static junit.framework.Assert.assertTrue;
import static com.xlab.vbrowser.activity.TestHelper.waitingTime;
import static com.xlab.vbrowser.fragment.FirstrunFragment.FIRSTRUN_PREF;

// This test opens share menu
@RunWith(AndroidJUnit4.class)
public class ShareDialogTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {

        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, true)
                    .apply();
        }
    };

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void shareTest() throws InterruptedException, UiObjectNotFoundException {

        UiObject shareBtn = TestHelper.mDevice.findObject(new UiSelector()
                .resourceId("com.xlab.vbrowser.debug:id/share")
                .enabled(true));

        /* Go to a webpage */
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("mozilla");
        TestHelper.hint.waitForExists(waitingTime);
        TestHelper.pressEnterKey();
        assertTrue(TestHelper.webView.waitForExists(waitingTime));

        /* Select share */
        TestHelper.menuButton.perform(click());
        shareBtn.waitForExists(waitingTime);
        shareBtn.click();

        // For simulators, where apps are not installed, it'll take to message app
        TestHelper.shareMenuHeader.waitForExists(waitingTime);
        assertTrue(TestHelper.shareMenuHeader.exists());
        assertTrue(TestHelper.shareAppList.exists());
        TestHelper.pressBackKey();
    }
}
