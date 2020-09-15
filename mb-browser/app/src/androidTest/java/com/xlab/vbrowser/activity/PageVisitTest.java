/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;

import com.xlab.vbrowser.activity.helpers.EspressoHelper;
import com.xlab.vbrowser.activity.helpers.SessionLoadedIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.xlab.vbrowser.R;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withTitleText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static com.xlab.vbrowser.fragment.FirstrunFragment.FIRSTRUN_PREF;

// This test visits each page and checks whether some essential elements are being displayed
@RunWith(AndroidJUnit4.class)
public class PageVisitTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {

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

    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        loadingIdlingResource = new SessionLoadedIdlingResource();
        IdlingRegistry.getInstance().register(loadingIdlingResource);
    }

    @After
    public void tearDown() throws Exception {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void visitPagesTest() throws InterruptedException, UiObjectNotFoundException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // What's new page

        EspressoHelper.openMenu();

        clickMenuItem(R.id.whats_new);
        assertWebsiteUrlContains("support.mozilla.org");

        pressBack();

        // Help page

        EspressoHelper.openMenu();

        clickMenuItem(R.id.help);
        EspressoHelper.assertToolbarMatchesText(R.string.menu_help);

        pressBack();

        // Go to settings

        EspressoHelper.openMenu();
        clickMenuItem(R.id.settings);

        // "About" page

        final String aboutLabel = context.getString(R.string.preference_about, context.getString(R.string.app_name));

        onData(withTitleText(aboutLabel))
                .check(matches(isDisplayed()))
                .perform(click());

        EspressoHelper.assertToolbarMatchesText(R.string.menu_about);

        pressBack();

        // "Your rights" page

        onData(withTitleText(context.getString(R.string.your_rights)))
                .check(matches(isDisplayed()))
                .perform(click());

        EspressoHelper.assertToolbarMatchesText(R.string.your_rights);
    }

    private void clickMenuItem(@IdRes int id) {
        onView(withId(id))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private void assertWebsiteUrlContains(String substring) {
        onView(withId(R.id.display_url))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString(substring))));
    }
}
