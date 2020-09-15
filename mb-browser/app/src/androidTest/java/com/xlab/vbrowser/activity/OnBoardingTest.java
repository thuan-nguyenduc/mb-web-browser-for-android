/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.xlab.vbrowser.fragment.FirstrunFragment.FIRSTRUN_PREF;

@RunWith(AndroidJUnit4.class)
public class OnBoardingTest {

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
                    .putBoolean(FIRSTRUN_PREF, false)
                    .apply();
        }
    };

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void OnBoardingTest() throws InterruptedException, UiObjectNotFoundException {

        // Let's search for something
        TestHelper.firstSlide.waitForExists(TestHelper.waitingTime);
        Assert.assertTrue(TestHelper.firstSlide.exists());
        TestHelper.nextBtn.click();
        TestHelper.secondSlide.waitForExists(TestHelper.waitingTime);
        Assert.assertTrue(TestHelper.secondSlide.exists());
        TestHelper.nextBtn.click();
        TestHelper.thirdSlide.waitForExists(TestHelper.waitingTime);
        Assert.assertTrue(TestHelper.thirdSlide.exists());
        TestHelper.nextBtn.click();
        TestHelper.lastSlide.waitForExists(TestHelper.waitingTime);
        Assert.assertTrue(TestHelper.lastSlide.exists());
        TestHelper.finishBtn.click();

        TestHelper.inlineAutocompleteEditText.waitForExists(TestHelper.waitingTime);
        Assert.assertTrue(TestHelper.inlineAutocompleteEditText.exists());
    }
}
