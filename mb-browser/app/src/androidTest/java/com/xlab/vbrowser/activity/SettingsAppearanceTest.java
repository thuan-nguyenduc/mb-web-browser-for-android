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

import com.xlab.vbrowser.activity.helpers.EspressoHelper;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static com.xlab.vbrowser.activity.TestHelper.waitingTime;
import static com.xlab.vbrowser.fragment.FirstrunFragment.FIRSTRUN_PREF;

// This test checks all the headings in the Settings menu are there
@RunWith(AndroidJUnit4.class)
public class SettingsAppearanceTest {

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
    public void settingsScreenTest() throws InterruptedException, UiObjectNotFoundException {

        UiObject SearchEngineSelection = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        UiObject searchHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Search")
                .resourceId("android:id/title"));
        UiObject privacyHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Privacy")
                .resourceId("android:id/title"));
        UiObject perfHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Performance")
                .resourceId("android:id/title"));
        UiObject mozHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Mozilla")
                .resourceId("android:id/title"));

        /* Go to Settings */
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);

        EspressoHelper.openSettings();
        SearchEngineSelection.waitForExists(waitingTime);

        /* Check the first element and other headings are present */
        assertTrue(SearchEngineSelection.isEnabled());
        assertTrue(searchHeading.exists());
        assertTrue(privacyHeading.exists());
        TestHelper.swipeUpScreen();
        assertTrue(perfHeading.exists());
        mozHeading.waitForExists(waitingTime);
        assertTrue(mozHeading.exists());
    }
}
