/*Copyright by MonnyLab*/

package com.xlab.vbrowser.activity;

import android.os.SystemClock;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;

import com.xlab.vbrowser.activity.helpers.EspressoHelper;
import com.xlab.vbrowser.activity.helpers.MainActivityFirstrunTestRule;
import com.xlab.vbrowser.activity.helpers.WebViewFakeLongPress;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.web.IWebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;

/**
 * Open multiple sessions and verify that the UI looks like it should.
 */
@RunWith(AndroidJUnit4.class)
public class MultitaskingTest {
    @Rule
    public MainActivityFirstrunTestRule mActivityTestRule = new MainActivityFirstrunTestRule(false);

    private MockWebServer webServer;

    @Before
    public void startWebServer() throws Exception {
        webServer = new MockWebServer();

        webServer.enqueue(TestHelper.createMockResponseFromAsset("tab1.html"));
        webServer.enqueue(TestHelper.createMockResponseFromAsset("tab2.html"));
        webServer.enqueue(TestHelper.createMockResponseFromAsset("tab3.html"));

        webServer.enqueue(TestHelper.createMockResponseFromAsset("tab2.html"));

        webServer.start();
    }

    @After
    public void stopWebServer() throws Exception {
        webServer.shutdown();
    }

    @Test
    public void testVisitingMultipleSites() {
        {
            // Load website: Erase button visible, Tabs button not

            EspressoHelper.navigateToMockWebServer(webServer, "tab1.html");

            checkTabIsLoaded("Tab 1");

            EspressoHelper.onFloatingEraseButton()
                    .check(matches(isDisplayed()));

            EspressoHelper.onFloatingTabsButton()
                    .check(matches(not(isDisplayed())));
        }

        {
            // Open link in new tab: Erase button hidden, Tabs button visible

            longPressLink("tab2", "Tab 2", "tab2.html");

            openInNewTab();

            EspressoHelper.onFloatingEraseButton()
                    .check(matches(not(isDisplayed())));

            EspressoHelper.onFloatingTabsButton()
                    .check(matches(isDisplayed()))
                    .check(matches(withContentDescription(is("Tabs open: 2"))));
        }

        {
            // Open link in new tab: Tabs button updated, Erase button still hidden

            longPressLink("tab3", "Tab 3", "tab3.html");

            openInNewTab();

            EspressoHelper.onFloatingEraseButton()
                    .check(matches(not(isDisplayed())));

            EspressoHelper.onFloatingTabsButton()
                    .check(matches(isDisplayed()))
                    .check(matches(withContentDescription(is("Tabs open: 3"))));
        }

        {
            // Open tabs tray and switch to second tab.

            EspressoHelper.onFloatingTabsButton()
                    .perform(click());

            final String expectedUrl = webServer.getHostName() + "/tab2.html";

            onView(withText(expectedUrl))
                    .perform(click());

            onWebView()
                    .withElement(findElement(Locator.ID, "content"))
                    .check(webMatches(getText(), equalTo("Tab 2")));
        }

        {
            // Remove all tabs via the menu

            EspressoHelper.onFloatingTabsButton()
                    .perform(click());

            onView(ViewMatchers.withText(com.xlab.vbrowser.R.string.tabs_tray_action_erase))
                    .perform(click());

            assertFalse(SessionManager.getInstance().hasSession());
        }


        SystemClock.sleep(5000);
    }

    private void checkTabIsLoaded(String title) {
        onWebView()
                .withElement(findElement(Locator.ID, "content"))
                .check(webMatches(getText(), equalTo(title)));
    }

    private void longPressLink(String id, String label, String path) {
        onWebView()
                .withElement(findElement(Locator.ID, id))
                .check(webMatches(getText(), equalTo(label)));


        simulateLinkLongPress(path);
    }

    private void simulateLinkLongPress(String path) {
        onView(ViewMatchers.withId(com.xlab.vbrowser.R.id.webview))
                .perform(WebViewFakeLongPress.injectHitTarget(
                        new IWebView.HitTarget(true, webServer.url(path).toString(), false, null)));
    }

    private void openInNewTab() {
        onView(ViewMatchers.withText(com.xlab.vbrowser.R.string.contextmenu_open_in_new_tab))
                .perform(click());
    }
}
