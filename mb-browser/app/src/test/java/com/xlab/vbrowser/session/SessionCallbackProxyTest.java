/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import org.junit.Test;
import org.junit.runner.RunWith;
import com.xlab.vbrowser.architecture.NonNullMutableLiveData;
import com.xlab.vbrowser.web.IWebView;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SessionCallbackProxyTest {
    private static final String TEST_URL = "https://github.com/mozilla-mobile/focus-android/";

    @Test
    public void testOnPageStarted() {
        final Session session = mock(Session.class);
        final SessionCallbackProxy proxy = new SessionCallbackProxy(session, mock(IWebView.Callback.class));

        proxy.onPageStarted(TEST_URL);

        verify(session).setUrl(TEST_URL);
        verify(session).setLoading(true);
        verify(session).setSecure(false);
        verify(session).setProgress(SessionCallbackProxy.MINIMUM_PROGRESS);
    }

    @Test
    public void testOnPageFinished() {
        final Session session = mock(Session.class);
        final SessionCallbackProxy proxy = new SessionCallbackProxy(session, mock(IWebView.Callback.class));

        proxy.onPageFinished(true);

        verify(session).setLoading(false);
        verify(session).setSecure(true);
    }

    @Test
    public void testOnProgress() {
        final Session session = mock(Session.class);
        final SessionCallbackProxy proxy = new SessionCallbackProxy(session, mock(IWebView.Callback.class));

        proxy.onProgress(1);

        verify(session).setProgress(SessionCallbackProxy.MINIMUM_PROGRESS);

        proxy.onProgress(42);

        verify(session).setProgress(42);
    }

    @Test
    public void testOnUrlChanged() {
        final Session session = mock(Session.class);
        final SessionCallbackProxy proxy = new SessionCallbackProxy(session, mock(IWebView.Callback.class));

        proxy.onURLChanged(TEST_URL);

        verify(session).setUrl(TEST_URL);
    }

    @Test
    public void testBlockedTrackerCounter() {
        final NonNullMutableLiveData<Integer> blockedTrackerCounter = new NonNullMutableLiveData<>(50);

        final Session session = mock(Session.class);
        doReturn(blockedTrackerCounter).when(session).getBlockedTrackers();

        final SessionCallbackProxy proxy = new SessionCallbackProxy(session, mock(IWebView.Callback.class));

        proxy.countBlockedTracker();

        verify(session).getBlockedTrackers();
        verify(session).setTrackersBlocked(51);

        proxy.resetBlockedTrackers();

        verify(session).setTrackersBlocked(0);
    }
}