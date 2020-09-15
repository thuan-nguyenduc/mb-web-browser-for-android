/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity.screenshots;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.xlab.vbrowser.activity.MainActivity;
import com.xlab.vbrowser.activity.TestHelper;
import com.xlab.vbrowser.activity.helpers.MainActivityFirstrunTestRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class HomeScreenScreenshots extends ScreenshotTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityFirstrunTestRule(false);

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Test
    public void takeScreenshotOfHomeScreen() {
        onView(ViewMatchers.withId(com.xlab.vbrowser.R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()));

        Screengrab.screenshot("Home_View");
    }

    @Test
    public void takeScreenshotOfMenu() {
        TestHelper.menuButton.perform(click());

        onView(ViewMatchers.withText(com.xlab.vbrowser.R.string.menu_whats_new))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("MainViewMenu");
    }
}
