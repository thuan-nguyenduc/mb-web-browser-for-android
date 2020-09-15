/*Copyright by MonnyLab*/

package com.xlab.vbrowser.fragment;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import com.xlab.vbrowser.R;
import com.xlab.vbrowser.UpApplication;
import com.xlab.vbrowser.activity.MainActivity;
import com.xlab.vbrowser.architecture.NonNullObserver;
import com.xlab.vbrowser.bookmark.service.BookmarkService;
import com.xlab.vbrowser.events.IActionDone;
import com.xlab.vbrowser.events.IConfirmDialogResult;
import com.xlab.vbrowser.events.IItemClickListener;
import com.xlab.vbrowser.events.IPromptDialogResult;
import com.xlab.vbrowser.events.IQuickDialgItemClickListener;
import com.xlab.vbrowser.extensions.BaseExtension;
import com.xlab.vbrowser.extensions.ExtensionUtils;
import com.xlab.vbrowser.extensions.MediaDownloader.MediaParser;
import com.xlab.vbrowser.extensions.ReaderMode.ReaderModeParser;
import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.history.service.HistoryService;
import com.xlab.vbrowser.history.service.IHistoryServiceAction;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.menu.browser.BrowserMenu;
import com.xlab.vbrowser.menu.browser.DialogUtils;
import com.xlab.vbrowser.menu.context.WebContextMenu;
import com.xlab.vbrowser.permission.IRequestPermissionResult;
import com.xlab.vbrowser.prefs.Constants;
import com.xlab.vbrowser.quickdial.adapter.QuickDialAdapter;
import com.xlab.vbrowser.quickdial.entity.QuickDialItem;
import com.xlab.vbrowser.quickdial.service.QuickDialService;
import com.xlab.vbrowser.session.NullSession;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionCallbackProxy;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.session.ui.SessionsSheetFragment;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.AppUtils;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.DownloadUtils;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.ISnackbarAction;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlConstants;
import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.utils.ViewUtils;
import com.xlab.vbrowser.utils.WindowUtils;
import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;
import com.xlab.vbrowser.webview.WebViewUpload;
import com.xlab.vbrowser.widget.AnimatedProgressBar;
import com.xlab.vbrowser.widget.dialog.AddToHomeScreenDialog;
import com.xlab.vbrowser.widget.dialog.ConfirmDialog;
import com.xlab.vbrowser.widget.dialog.PromptDialog;
import com.xlab.vbrowser.widget.touchhelper.OnStartDragListener;
import com.xlab.vbrowser.widget.touchhelper.SimpleItemTouchHelperCallback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends WebFragment implements View.OnClickListener,
        EasyPermissions.PermissionCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private ArrayList<BaseExtension> extensions = new ArrayList<>();
    public static final String FRAGMENT_TAG = "browser";
    private final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    private static final String ARGUMENT_SESSION_UUID = "sessionUUID";
    private final int MOST_VISISTED_NUMBER_ROWS = 2;

    public static BrowserFragment createForSession(Session session) {
        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_SESSION_UUID, session.getUUID());
        BrowserFragment fragment = new BrowserFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private TextView urlView;
    private AnimatedProgressBar progressView;
    private FrameLayout blockView;
    private ImageView lockView;
    private ImageButton menuView;
    private SwipeRefreshLayout swipeRefresh;
    private View homePageView;
    private RecyclerView mostVisitedView;
    private RecyclerView quickDialView;
    private WeakReference<BrowserMenu> menuWeakReference = new WeakReference<>(null);
    private TextSwitcher adBlockedView;
    private AppBarLayout appBar;
    private ImageButton bookmarkView;
    private ImageView earthView;
    private TextView mostVisistedSeperatorHeader;

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private ViewGroup videoContainer;

    /**
     * Container containing the browser chrome and web content.
     */
    private View browserContainer;

    /**
     * MenuItem of BottomNavigationBar
     */
    private View backButtonView;
    private View forwardButtonView;
    private View homeButtonView;
    private View tabsButtonView;
    private View incognitoImageView;
    private TextView tabsCountView;
    private View refreshButton;
    private View stopButton;
    private View downloadMediaButton;
    private View readerModeButton;

    private IWebView.FullscreenCallback fullscreenCallback;

    private Download pendingDownload;

    private SessionManager sessionManager;
    private Session session;

    private Fetch fetch;

    private SharedPreferences prefs;

    private Settings settings;

    private ItemTouchHelper itemTouchHelper;

    /*
    *Support upload file
     */
    WebViewUpload webViewUpload = new WebViewUpload(this);

    public BrowserFragment() {
        sessionManager = SessionManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String sessionUUID = getArguments().getString(ARGUMENT_SESSION_UUID);

        if (sessionUUID == null) {
            throw new IllegalAccessError("No session exists");
        }

        session = sessionManager.hasSessionWithUUID(sessionUUID)
                ? sessionManager.getSessionByUUID(sessionUUID)
                : new NullSession();

        session.getBlockedTrackers().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer blockedTrackers) {
                //(thuan): Because we don't have space to show '100', so we limits this.
                if (menuWeakReference == null || blockedTrackers == null || blockedTrackers > 99 ) {
                    return;
                }

                final BrowserMenu menu = menuWeakReference.get();

                if (menu != null) {
                    //noinspection ConstantConditions - Not null
                    menu.updateTrackers(blockedTrackers);
                }

                if (adBlockedView != null) {
                    Animation slideTextInAnimation =  AnimationUtils.
                            loadAnimation(getContext(),   R.anim.slide_text_in);

                    Animation slideTextOutAnimation =  AnimationUtils.
                            loadAnimation(getContext(),   R.anim.slide_text_out);

                    adBlockedView.setInAnimation(slideTextInAnimation);
                    adBlockedView.setOutAnimation(slideTextOutAnimation);

                    adBlockedView.setText(String.valueOf(blockedTrackers));
                }
            }
        });

        fetch = ((UpApplication) getActivity().getApplication()).getFetch();

        settings = Settings.getInstance(getContext());

        GaReport.sendReportScreen(getContext(), BrowserFragment.class.getName());
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String getInitialUrl() {
        return session.getUrl().getValue();
    }

    @Override
    public void onPause() {
        super.onPause();

        final BrowserMenu menu = menuWeakReference.get();

        if (menu != null) {
            menu.dismiss();

            menuWeakReference.clear();
        }
    }

    @Override
    public View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(com.xlab.vbrowser.R.layout.fragment_browser, container, false);

        videoContainer = view.findViewById(com.xlab.vbrowser.R.id.video_container);
        browserContainer = view.findViewById(com.xlab.vbrowser.R.id.browser_container);

        urlView = view.findViewById(com.xlab.vbrowser.R.id.display_url);

        progressView = view.findViewById(com.xlab.vbrowser.R.id.progress);

        swipeRefresh = view.findViewById(com.xlab.vbrowser.R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(com.xlab.vbrowser.R.color.colorAccent);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reload();
            }
        });

        homePageView = view.findViewById(com.xlab.vbrowser.R.id.homePageView);
        mostVisitedView = view.findViewById(com.xlab.vbrowser.R.id.mostVisitedView);
        quickDialView = view.findViewById(R.id.quickDialView);
        appBar = view.findViewById(com.xlab.vbrowser.R.id.appbar);
        mostVisistedSeperatorHeader = view.findViewById(R.id.mostVisistedSeperatorHeader);

        session.getUrl().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String url) {
                String text = UrlUtils.getHost(url);
                urlView.setText(TextUtils.isEmpty(text) ? "" : url);

                if (TextUtils.isEmpty(text)) {
                    final Drawable leftDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_urlview_search);
                    urlView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null);
                }
                else {
                    urlView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }

                //(thuan)When not URI is inputted into URL, we will hide bookmarkView
                if (TextUtils.isEmpty(url) || !UrlUtils.isHttpOrHttps(url)) {
                    bookmarkView.setVisibility(View.GONE);
                    earthView.setVisibility(View.GONE);
                }
            }
        });

        setBlockingEnabled(session.isBlockingEnabled());

        session.getLoading().observe(this, new NonNullObserver<Boolean>() {
            @Override
            public void onValueChanged(@NonNull Boolean loading) {
                updateBlockingBadging(session.isBlockingEnabled());

                updateBottombarButtonStates(loading);
            }
        });

        if ((refreshButton = view.findViewById(com.xlab.vbrowser.R.id.refresh)) != null) {
            refreshButton.setOnClickListener(this);
        }

        if ((stopButton = view.findViewById(com.xlab.vbrowser.R.id.stop)) != null) {
            stopButton.setOnClickListener(this);
        }

        downloadMediaButton = view.findViewById(com.xlab.vbrowser.R.id.downloadMedia);
        readerModeButton = view.findViewById(R.id.readerModeView);

        adBlockedView = view.findViewById(com.xlab.vbrowser.R.id.adblocked);


        final ImageView blockIcon = view.findViewById(com.xlab.vbrowser.R.id.block_image);
        blockIcon.setImageResource(com.xlab.vbrowser.R.drawable.ic_tracking_protection_disabled);

        blockView = view.findViewById(com.xlab.vbrowser.R.id.block);

        lockView = view.findViewById(com.xlab.vbrowser.R.id.lock);
        session.getSecure().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean secure) {
                lockView.setVisibility(secure ? View.VISIBLE : View.GONE);
            }
        });

        session.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                progressView.setProgress(progress);
            }
        });

        menuView = view.findViewById(com.xlab.vbrowser.R.id.menuView);
        menuView.setOnClickListener(this);

        bookmarkView = view.findViewById(R.id.bookmarkView);
        bookmarkView.setOnClickListener(this);

        earthView = view.findViewById(R.id.earthView);

        if (!session.isCustomTab()) {
            initialiseNormalBrowserUi(view);
        }

        backButtonView = view.findViewById(R.id.backButtonView);
        forwardButtonView = view.findViewById(R.id.forwardButtonView);
        homeButtonView = view.findViewById(R.id.homeButtonView);
        tabsButtonView = view.findViewById(R.id.tabsButtonView);
        tabsCountView = view.findViewById(R.id.tabsCountView);
        incognitoImageView = view.findViewById(R.id.incognitoImageView);

        backButtonView.setOnClickListener(this);
        forwardButtonView.setOnClickListener(this);
        homeButtonView.setOnClickListener(this);
        tabsButtonView.setOnClickListener(this);

        startListenEvents();

        //Nightmode
        prefs= PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);

        //Resiger extensions
        registerExtensions();

        //Incognito
        updateTabsButtonView();

        return view;
    }

    public AnimatedProgressBar getProgressView() {
        return progressView;
    }

    private void startListenEvents() {
        SessionManager.getInstance().getOpenUrlEvent().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newUrl) {
                if (!getUrl().equals(newUrl) && !TextUtils.isEmpty(newUrl) && UrlUtils.isHttpOrHttps(newUrl)) {
                    loadUrl(newUrl);
                }

                if(!TextUtils.isEmpty(newUrl)) {
                    SessionManager.getInstance().getOpenUrlEvent().setValue("");
                }
            }
        });

        //Listen History service
        HistoryService.getClearHistoryEvent().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                //If history is cleared, then reload MostVisited
                Log.d("BrowserFragment", "Reload MostVisisted and Quick access due to clearing history");
                if (isShowingHomepage()) {
                    loadHomeData(false);
                }
                else {
                    clearHomeData();
                }
            }
        });

        //Listen Reload MostVisisted service
        HistoryService.getReloadMostVisitedEvent().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                //If history is cleared, then reload MostVisited
                Log.d("BrowserFragment", "Reload MostVisisted due to clearing history");
                if (isShowingHomepage()) {
                    loadHomeData(true);
                }
                else {
                    clearHomeData();
                }
            }
        });

        //Listen Bookmark service
        BookmarkService.getClearAllBookmarksEvent().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                //If bookmark is cleared
                if (bookmarkView != null) {
                    bookmarkView.setTag(false);
                    bookmarkView.setImageResource(R.drawable.ic_bookmark);
                }
            }
        });

        BookmarkService.getClearBookmarkEvent().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String str) {
                //If bookmark is cleared
                if (bookmarkView != null && !TextUtils.isEmpty(str) && str.equals(getUrl())) {
                    bookmarkView.setTag(false);
                    bookmarkView.setImageResource(R.drawable.ic_bookmark);
                }
            }
        });

    }

    @Override
    public void applyLocale() {
        TextView displayUrl = getActivity().findViewById(com.xlab.vbrowser.R.id.display_url);
        displayUrl.setHint(getString(com.xlab.vbrowser.R.string.urlbar_hint));

        mostVisistedSeperatorHeader.setText(getString(R.string.homepage_mostvisited_header));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (isShowingHomepage()) {
            loadHomeData(false);
        } else {
            clearHomeData();
        }
    }

    private void initialiseNormalBrowserUi(final @NonNull View view) {
        urlView.setOnClickListener(this);

        sessionManager.getSessions().observe(this, new NonNullObserver<List<Session>>() {
            @Override
            protected void onValueChanged(@NonNull List<Session> sessions) {
                updateTabsTitle(sessions.size());
                //eraseButton.updateSessionsCount(sessions.size());
            }
        });
    }

    private void updateTabsTitle(final int sessionNumber) {
        if (tabsCountView == null) {
            return;
        }

        //Animation slideTextInAnimation =  AnimationUtils.
          //      loadAnimation(getContext(),   R.anim.slide_text_in);

        //Animation slideTextOutAnimation =  AnimationUtils.
        //        loadAnimation(getContext(),   R.anim.slide_text_out);

        //tabsCountView.setInAnimation(slideTextInAnimation);
        //tabsCountView.setOutAnimation(slideTextOutAnimation);

        tabsCountView.setText(String.valueOf(sessionNumber));

        ExtensionUtils.showBlink(getContext(), tabsCountView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public IWebView.Callback createCallback() {
        return new SessionCallbackProxy(session, new IWebView.Callback() {
            private boolean wasStartedLoading = false;

            @Override
            public void onPageStarted(final String url) {
                session.setAddedToQuickAccess(true);
                //Swipe Refresh is disabled by default, if WebView's scrollbar is activated and scroll to top, then Swipe Refresh is enabled.
                //This fix a bug with some sites as https://gatenotes.com when topbar is sticky.
                swipeRefresh.setEnabled(false);

                showAppbar(true);
                hideHomepage(url);

                wasStartedLoading = true;
                progressView.setProgress(5);
                progressView.setVisibility(View.VISIBLE);


                final BrowserMenu menu = menuWeakReference.get();
                if (menu != null) {
                    menu.updateLoading(true);
                }

                updateToolbarButtonStates(true);

                /**
                 * Execute extensions
                 */
                for (BaseExtension extension: getExtensions()) {
                    extension.onPageStarted();
                }
            }

            @Override
            public void onPageCommitVisible(String url) {
                //This event is raised after onRequest or onPageStarted, and then before onPageFinished
                //It is called one time, it is diffenrence to onPageFinished, which may be called manytimes
                //for example: amazon.com
                loadBookmark(url);

                if (readerModeButton != null) {
                    readerModeButton.setEnabled(false);
                    readerModeButton.setAlpha(0.5f);
                }

                /**
                 * Execute extensions
                 */
                for (BaseExtension extension: getExtensions()) {
                    extension.onPageCommitVisible();
                }
            }

            @Override
            public void onPageFinished(boolean isSecure) {
                //(thuan): When loading about:blank page, webview does not raise onPageStarted,
                //so we need call setTrackersBlocked every loading about:blank
                String url = getUrl();

                showHomepage(url);

                if (url != null && UrlUtils.isBlankUrl(url)) {
                    session.setTrackersBlocked(0);
                    session.setAddedToQuickAccess(true);
                }
                else {
                    /**
                     * Check url exist or not in Quick acesss
                     */
                    QuickDialService.checkUrl(getContext());
                }

                //When loading about:blank, webview will not call onPageStarted, so we
                //need this as a work around.
                if (!wasStartedLoading) {
                    //backgroundTransitionGroup.resetTransition();
                    progressView.setProgress(5);
                    progressView.setVisibility(View.VISIBLE);
                }

                wasStartedLoading = false;

                if (progressView.getVisibility() == View.VISIBLE) {
                    // We start a transition only if a page was just loading before
                    // allowing to avoid issue #1179
                    //backgroundTransitionGroup.startTransition(ANIMATION_DURATION);
                    progressView.setProgress(progressView.getMax());
                    progressView.setVisibility(View.GONE);
                }

                swipeRefresh.setRefreshing(false);
                swipeRefresh.setEnabled(getWebView().canScrollDownVertically());

                final BrowserMenu menu = menuWeakReference.get();
                if (menu != null) {
                    menu.updateLoading(false);
                }

                updateToolbarButtonStates(false);

                //We call this method double times for making sure the bookmark info is loaded correctly
                loadBookmark(url);

                //TODO(thuan): This callback is called double times when switching tabs
                //because of in SystemWebView.restoreWebViewState, we called reload() after
                //restoring navigation history
                /**
                 * Execute extensions
                 */
                for (BaseExtension extension: getExtensions()) {
                    extension.onPageFinished();
                }

                if (!settings.isIncognitoEnabled()) {
                    /**
                     * Save history
                     */
                    HistoryService.updateHistoryAsync(getTitle(), url, getContext());
                }
            }

            @Override
            public void onURLChanged(final String url, boolean isPageFinished) {
            }

            @Override
            public void onRequest(boolean isTriggeredByUserGesture) {
                if (bookmarkView == null || earthView == null) {
                    return;
                }

                earthView.setVisibility(View.VISIBLE);
                bookmarkView.setVisibility(View.GONE);
            }

            @Override
            public void onProgress(int progress) {}

            @Override
            public void countBlockedTracker() {}

            @Override
            public void resetBlockedTrackers() {}

            @Override
            public void onBlockingStateChanged(boolean isBlockingEnabled) {}

            @Override
            public void onLongPress(final IWebView.HitTarget hitTarget) {
                WebContextMenu.show(getActivity(), this, hitTarget, session.isBlockingEnabled());
            }

            @Override
            public void onEnterFullScreen(@NonNull final IWebView.FullscreenCallback callback, @Nullable View view) {
                fullscreenCallback = callback;

                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer.setVisibility(View.INVISIBLE);

                    // Add view to video container and make it visible
                    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    videoContainer.addView(view, params);
                    videoContainer.setVisibility(View.VISIBLE);


                    // Switch to immersive mode: Hide system bars other UI controls
                    WindowUtils.switchToImmersiveMode(getActivity());
                }
            }

            @Override
            public void onExitFullScreen() {
                // Remove custom video views and hide container
                videoContainer.removeAllViews();
                videoContainer.setVisibility(View.GONE);

                // Show browser UI and web content again
                browserContainer.setVisibility(View.VISIBLE);

                WindowUtils.exitImmersiveModeIfNeeded(getActivity());

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback.fullScreenExited();
                    fullscreenCallback = null;
                }
            }

            @Override
            public void onDownloadStart(Download download) {
                if (hasWriteStoragePermission()) {
                    startDownload(download, null);
                    GaReport.sendReportEvent(getContext(), "onDownloadStart_hasWriteStoragePermission", BrowserFragment.class.getName());
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start the
                    // download from onRequestPermissionsResult().
                    final Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    pendingDownload = download;

                    setPermissionCallback(new IRequestPermissionResult() {
                        @Override
                        public void onReceivePermission() {
                            if (pendingDownload != null) {
                                startDownload(pendingDownload, null);
                                GaReport.sendReportEvent(getContext(), "onDownloadStart_onReceivePermission", BrowserFragment.class.getName());
                                pendingDownload = null;
                            }
                        }
                    });

                    requestWriteExtenalStoragePermission();
                }

                GaReport.sendReportEvent(getContext(), "onDownloadStart", BrowserFragment.class.getName());
            }

            @Override
            public void requestNewTab(String url) {
                if (TextUtils.isEmpty(url))
                {
                    return;
                }

                openNewTab(url);
            }

            @Override
            public void onReceivedTitle(String title) { }

            @Override
            public void onReceivedIcon(Bitmap bitmap) {
                if (bitmap == null) {
                    return;
                }

                String url = getUrl();

                FaviconService.writeFavicon(getContext(), url, bitmap);

                session.setReceivedFavicon(System.nanoTime());
            }

            @Override
            public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
                //swipeRefresh.setEnabled(l < 10 && t < 10);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
               return webViewUpload.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });
    }

    private void loadBookmark(String url) {
        if (bookmarkView != null && url != null && UrlUtils.isHttpOrHttps(url)) {
            BookmarkService.loadBookmark(getContext(), url, bookmarkView, earthView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // This fragment might get destroyed before the user left immersive mode (e.g. by opening another URL from an app).
        // In this case let's leave immersive mode now when the fragment gets destroyed.
        WindowUtils.exitImmersiveModeIfNeeded(getActivity());

        try {
            if (fetch != null && !fetch.isClosed()) {
                fetch.removeListener(fetchListener);
            }
        }catch (Exception e) {}

        unregisterExtensions();

        //Close Quick Access if needed
        closeQuickAccessActionMode();
    }

    void showAddToHomescreenDialog(String url, String title) {
        final FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager == null || fragmentManager.findFragmentByTag(AddToHomeScreenDialog.FRAGMENT_TAG) != null) {
            // We are already displaying a homescreen dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return;
        }

        final AddToHomeScreenDialog addToHomescreenDialog = AddToHomeScreenDialog.newInstance(url, title, session.isBlockingEnabled());
        addToHomescreenDialog.setTargetFragment(BrowserFragment.this, 300);

        try {
            addToHomescreenDialog.show(fragmentManager, addToHomescreenDialog.FRAGMENT_TAG);
        } catch (IllegalStateException e) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in adding to homescreen now.
        }
    }

    @Override
    public void onCreateViewCalled() {
    }

    @Override
    public void onResume() {
        super.onResume();
        applyLocale();

        if (fetch != null) {
            fetch.addListener(fetchListener);
        }
    }

    public boolean onBackPressed() {
        if (canGoBack()) {
            // Go back in web history
            goBack();
        } else {
            // Just go back to the home screen or remove current tab
            if (session == null) {
                showExitDialog();
                return true;
            }

            String previousSessionUUID = session.getPreviousSessionUUID();

            if (previousSessionUUID != null && SessionManager.getInstance().hasSessionWithUUID(previousSessionUUID)) {
                SessionManager.getInstance().removeCurrentSessionAndSelectSession(previousSessionUUID);
            }
            else {
                showExitDialog();
            }
        }

        return true;
    }

    private void showExitDialog() {
        if (settings.isRated()) {
            if (getActivity() != null) {
                getActivity().finish();
            }

            return;
        }

        final FragmentManager fragmentManager = this.getActivity().getSupportFragmentManager();

        ConfirmDialog.newInstance(getString(R.string.app_name), getString(R.string.exit_message), getString(R.string.menu_rate_app), getString(R.string.exit),
                new IConfirmDialogResult() {
                    @Override
                    public void onCancel() {
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }

                    @Override
                    public void onOk() {
                        settings.setRated(true);
                        AppUtils.openApp(getActivity(), getActivity().getPackageName());
                    }
                }).show(fragmentManager, BrowserFragment.class.getName());
    }

    private void openInputUrl() {
        final Fragment urlFragment = UrlInputFragment
                .createWithSession(session, urlView);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(com.xlab.vbrowser.R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.xlab.vbrowser.R.id.menuView:
                BrowserMenu menu = new BrowserMenu(getActivity(), this, session.getCustomTabConfig());
                menu.show(menuView);

                menuWeakReference = new WeakReference<>(menu);
                GaReport.sendReportEvent(getContext(), "menuView", "ACTION_" + BrowserFragment.class.getName());

                break;

            case com.xlab.vbrowser.R.id.display_url:
                openInputUrl();
                GaReport.sendReportEvent(getContext(), "openInputUrl", "ACTION_" + BrowserFragment.class.getName());
                break;


            case com.xlab.vbrowser.R.id.refresh: {
                reload();
                GaReport.sendReportEvent(getContext(), "refresh", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            case com.xlab.vbrowser.R.id.stop: {
                stop();
                GaReport.sendReportEvent(getContext(), "stop", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            case com.xlab.vbrowser.R.id.share: {
                final String url = getInitialUrl();
                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                // Use title from webView if it's content matches the url
                final IWebView webView = getWebView();
                if (webView != null) {
                    final String contentUrl = webView.getUrl();
                    if (contentUrl != null && contentUrl.equals(url)) {
                        final String contentTitle = getTitle();
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, contentTitle);
                    }
                }
                startActivity(Intent.createChooser(shareIntent, getString(com.xlab.vbrowser.R.string.share_dialog_title)));
                GaReport.sendReportEvent(getContext(), "share", "ACTION_" + BrowserFragment.class.getName());

                break;
            }

            case com.xlab.vbrowser.R.id.settings:
                ((LocaleAwareAppCompatActivity) getActivity()).openPreferences();
                GaReport.sendReportEvent(getContext(), "openPreferences", "ACTION_" + BrowserFragment.class.getName());

                break;

            case R.id.rateApp:
                AppUtils.openApp(getContext(), getActivity().getPackageName());

                break;

            case com.xlab.vbrowser.R.id.add_to_homescreen:
                final IWebView webView = getWebView();
                if (webView == null) {
                    break;
                }

                final String url = webView.getUrl();
                final String title = getTitle();
                showAddToHomescreenDialog(url, title);
                GaReport.sendReportEvent(getContext(), "add_to_homescreen", "ACTION_" + BrowserFragment.class.getName());

                break;

            case R.id.download_manager:
                ((LocaleAwareAppCompatActivity) getActivity()).openDownloadManager();
                GaReport.sendReportEvent(getContext(), "openDownloadManager", "ACTION_" + BrowserFragment.class.getName());

                break;

            case R.id.history:
                ((LocaleAwareAppCompatActivity) getActivity()).openHistory();
                GaReport.sendReportEvent(getContext(), "openHistory", "ACTION_" + BrowserFragment.class.getName());
                break;

            case R.id.bookmarkView:
                BookmarkService.addOrRemoveBookmark(getContext(), getTitle(), getInitialUrl(), bookmarkView);
                GaReport.sendReportEvent(getContext(), "addOrRemoveBookmark", "ACTION_" + BrowserFragment.class.getName());
                break;

            case R.id.bookmarkActivity:
                ((LocaleAwareAppCompatActivity) getActivity()).openBookmark();
                GaReport.sendReportEvent(getContext(), "openBookmark", "ACTION_" + BrowserFragment.class.getName());

                break;

            case R.id.nightMode:
                ((MainActivity)getActivity()).processNightmode();

                if (settings.isShownFirstTimeNightModeDialog()) {
                    DialogUtils.showNightmodeConfigDialog(getContext(), false);
                    settings.setShownFirstTimeNightModeDialog(false);
                }

                GaReport.sendReportEvent(getContext(), "processNightmode", "ACTION_" + BrowserFragment.class.getName());
                break;

            case R.id.speedMode:
                boolean isEnabled =  settings.shouldEnterSpeedMode();
                settings.enableSpeedMode(!isEnabled);
                GaReport.sendReportEvent(getContext(), "speedMode_"+String.valueOf(!isEnabled), "ACTION_SPEED_MODE");
                break;

            case R.id.requestDesktopSite:
                if (getWebView() != null) {
                    settings.setRequestDesktopSite(!settings.shouldRequestDesktopSite());
                    getWebView().requestDesktopSite();
                    loadUrl(getInitialUrl());
                    GaReport.sendReportEvent(getContext(), String.valueOf(!settings.shouldRequestDesktopSite()), "ACTION_REQUEST_DESKTOP");
                }
                break;

            case R.id.addToQuickAccess:
                final QuickDialItem quickDialItem = QuickDialItem.create(getInitialUrl(), getTitle().trim());
                final QuickDialAdapter quickDialAdapter = (QuickDialAdapter)quickDialView.getAdapter();

                addQuickDialItem(quickDialItem, quickDialAdapter);

                getSession().setAddedToQuickAccess(true);
                GaReport.sendReportEvent(getContext(), "ADD_QUICK_DIAL_BY_MENU", BrowserFragment.class.getName());
                break;

            case R.id.backButtonView: {
                closeQuickAccessActionMode();
                goBack();
                GaReport.sendReportEvent(getContext(), "bottomBarGoBack", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            case R.id.forwardButtonView: {
                closeQuickAccessActionMode();
                goForward();
                GaReport.sendReportEvent(getContext(), "bottomBarGoForward", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            case R.id.homeButtonView: {
                closeQuickAccessActionMode();
                goHome();
                GaReport.sendReportEvent(getContext(), "bottomBarGoHome", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            case R.id.tabsButtonView: {
                closeQuickAccessActionMode();
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .add(com.xlab.vbrowser.R.id.container, new SessionsSheetFragment(), SessionsSheetFragment.FRAGMENT_TAG)
                        .commit();
                GaReport.sendReportEvent(getContext(), "bottomBarOpenTabsManager", "ACTION_" + BrowserFragment.class.getName());
                break;
            }

            default:
                throw new IllegalArgumentException("Unhandled menu item in BrowserFragment");
        }
    }

    private void addQuickDialItem(final QuickDialItem quickDialItem, final QuickDialAdapter quickDialAdapter) {
        if (quickDialItem == null) {
            return;
        }

        new BackgroundTask(new IBackgroundTask() {
            long newId = 0;
            @Override
            public void run() {
                newId = QuickDialService.insert(getContext(), quickDialItem);
            }

            @Override
            public void onComplete() {
                if (newId > 0 && quickDialAdapter != null) {
                    quickDialItem.id = newId;
                    quickDialAdapter.addItem(quickDialItem);
                }

                if (newId > 0) {
                    Toast.makeText(getContext(), getString(R.string.added_to_quick_access), Toast.LENGTH_SHORT).show();
                }
            }
        }).execute();
    }

    private void updateToolbarButtonStates(boolean isLoading) {
        if (refreshButton == null || stopButton == null) {
            return;
        }

        refreshButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void updateBottombarButtonStates(boolean isLoading) {
        final IWebView webView = getWebView();
        if (webView == null || forwardButtonView == null || backButtonView == null) {
            return;
        }

        final boolean canGoForward = webView.canGoForward();
        final boolean canGoBack = webView.canGoBack();

        forwardButtonView.setEnabled(canGoForward);
        backButtonView.setEnabled(canGoBack);
        forwardButtonView.setAlpha(canGoForward ? 1f : 0.5f);
        backButtonView.setAlpha(canGoBack ? 1f : 0.5f);
    }

    @NonNull
    public String getUrl() {
        try {
            // getUrl() is used for things like sharing the current URL. We could try to use the webview,
            // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
            // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
            // user is probably expecting to share, so lets use that here:
            if (getWebView() != null) {
                String url = getWebView().getUrl();

                if (url != null) {
                    return url;
                }
            }

            return urlView.getText().toString();
        }
        catch (Exception e) {}

        return "";
    }

    public boolean canGoForward() {
        final IWebView webView = getWebView();
        return webView != null && webView.canGoForward();
    }

    public boolean canGoBack() {
        final IWebView webView = getWebView();
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        final IWebView webView = getWebView();
        if (webView != null && canGoBack()) {
            webView.goBack();
        }
    }

    public void goForward() {
        final IWebView webView = getWebView();
        if (webView != null && canGoForward()) {
            webView.goForward();
        }
    }

    public void loadUrl(String url) {
        final IWebView webView = getWebView();
        if (webView != null && !TextUtils.isEmpty(url)) {
            url = UrlUtils.normalize(url);
            webView.loadUrl(url);
        }
    }

    private void stop() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.stopLoading();
        }
    }

    public void reload() {
        //(thuan): We don't use reload() here because on 7.1, after load failed and call this method
        // chrome return a empty page.
        //loadUrl(getInitialUrl());
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.reload();
        }
    }

    public void setBlockingEnabled(boolean enabled) {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.setBlockingEnabled(enabled);
        }
    }

    // In the future, if more badging icons are needed, this should be abstracted
    public void updateBlockingBadging(boolean enabled) {
        blockView.setVisibility(enabled ? View.GONE : View.VISIBLE);
        adBlockedView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void openNewTab(String url) {
        SessionManager.getInstance().createSession(Source.PAGE_REQUEST_NEWTAB, url,
                session.getUUID(), getSession() != null && getSession().isBlockingEnabled());
    }

    private void goHome() {
        stop();
        loadUrl(UrlConstants.getHomeUrl());
    }

    /**
     * Sometimes when opening new window, webview is destroyed before we getTitle, so we need capture exception here
     * in the future, if we seperate each tab using another webview, we will not crea this.
     * @return current title of webview
     */
    private String getTitle() {
        if (getSession() != null) {
            return getSession().getTitle().getValue();
        }

        return "";
    }

    /********************* Downloader ***************************/
    /**
     * Use Fetch's Download Manager to queue this download.
     */
    private void queueDownload(Download download, String fileName) {
        final Context context = getContext();

        if (fetch == null || download == null || context == null) {
            return;
        }

        final String cookie = CookieManager.getInstance().getCookie(download.getUrl());

        if (fileName == null) {
            fileName = DownloadUtils.guessFileName(download);
        }

        fileName = fileName.replace("/", "");
        String originalFileName = fileName;

        String [] files = DownloadUtils.getFilePath(context, fileName);
        String filePath = files[0];
        fileName = files[1];

        if (TextUtils.isEmpty(fileName)) {
            return;
        }

        List<Request> requests = new ArrayList<Request>();
        Request request = new Request(download.getUrl(), fileName, originalFileName, getUrl(), filePath);

        if (download.getUserAgent() != null) {
            request.addHeader("User-Agent", download.getUserAgent());
        }

        if (cookie != null) {
            request.addHeader("Cookie", cookie);
        }

        if (getUrl() != null) {
            request.addHeader("Referer", getUrl());
        }

        requests.add(request);

        fetch.enqueue(requests, new Func<List<? extends com.tonyodev.fetch2.Download>>() {
            @Override
            public void call(List<? extends com.tonyodev.fetch2.Download> downloads) {
            }
        }, new Func<Error>() {
            @Override
            public void call(Error error) {
                Log.d("DownloadListActivity", "Error: " + error.toString());
            }
        });
    }

    public void startDownload(Download [] downloads, String fileNames[]) {
        ViewUtils.showSnackbarInfo(browserContainer, R.string.download_started, 300, getString(R.string.action_view), new ISnackbarAction() {
            @Override
            public void onOk() {
                ((MainActivity)getActivity()).openDownloadManager();
            }
        });

        for(int pos = 0; pos < downloads.length; pos++) {
            queueDownload(downloads[pos], fileNames[pos]);
        }
    }

    public void startDownload(Download download, String fileName) {
        ViewUtils.showSnackbarInfo(browserContainer, R.string.download_started, 300, getString(R.string.action_view), new ISnackbarAction() {
            @Override
            public void onOk() {
                ((MainActivity)getActivity()).openDownloadManager();
            }
        });

        queueDownload(download, fileName);
    }

    /**********************Permission*********************************/
    private IRequestPermissionResult requestPerssionResult;

    public void setPermissionCallback(IRequestPermissionResult requestPerssionResult) {
        this.requestPerssionResult = requestPerssionResult;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE_STORAGE_PERMISSION) {
            return;
        }

        // The actual download dialog will be shown from onResume(). If this activity/fragment is
        // getting restored then we need to 'resume' first before we can show a dialog (attaching
        // another fragment).
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private boolean hasWriteStoragePermission() {
        return EasyPermissions.hasPermissions(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @AfterPermissionGranted(REQUEST_CODE_STORAGE_PERMISSION)
    public boolean requestWriteExtenalStoragePermission() {
        if (hasWriteStoragePermission()) {
            requestPerssionResult.onReceivePermission();
            return true;
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                    this,
                    getString(com.xlab.vbrowser.R.string.notify_grant_write_storage_permission),
                    REQUEST_CODE_STORAGE_PERMISSION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            return false;
        }
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
           if (hasWriteStoragePermission()) {
               if (this.requestPerssionResult != null) {
                   this.requestPerssionResult.onReceivePermission();
               }
           }
        }

        //Handle upload file.
        webViewUpload.handleChooseFileResult(requestCode, resultCode, data);
    }


    /******************* Home Page *********************/
    private void showHomepage(String currentUrl) {
        if (currentUrl == null) {
            return;
        }

        if (UrlUtils.isBlankUrl(currentUrl.trim().toLowerCase())) {
            swipeRefresh.setVisibility(View.GONE);
            homePageView.setVisibility(View.VISIBLE);

            if (mostVisitedView.getAdapter() == null || quickDialView.getAdapter() == null) {
                loadHomeData(false);
            }

            showAppbar(true);
        }
    }

    private void hideHomepage(String currentUrl) {
        if (currentUrl == null || UrlUtils.isBlankUrl(currentUrl.trim().toLowerCase())) {
            return;
        }

        swipeRefresh.setVisibility(View.VISIBLE);
        homePageView.setVisibility(View.GONE);
    }

    private boolean isShowingHomepage() {
        return (homePageView.getVisibility() == View.VISIBLE)
                && (swipeRefresh.getVisibility() == View.GONE);
    }

    private void loadHomeData(boolean onlyLoadMostVisisted) {
        final int numberOfColumns = calcNumberOfMostVisitedColumns();
        final int numberOfRows = numberOfColumns * MOST_VISISTED_NUMBER_ROWS;
        mostVisistedSeperatorHeader.setVisibility(View.GONE);

        final IItemClickListener itemClickListener = new IItemClickListener() {
            @Override
            public void onItemClickListener(Object... data) {
                String url = data != null && data.length > 0 ? data[0].toString() : null;

                if (url == null) {
                    return;
                }

                closeQuickAccessActionMode();
                hideHomepage(url);

                if (UrlUtils.isUrl(url)) {
                    loadUrl(url);
                }
                else {
                    url = UrlUtils.createSearchUrl(getContext(), url);
                    loadUrl(url);
                }

                GaReport.sendReportEvent(getContext(), "MOST_VISITED_ITEM_CLICK", BrowserFragment.class.getName());
            }
        };

        IQuickDialgItemClickListener quickDialgItemClickListener = new IQuickDialgItemClickListener() {
            @Override
            public void onItemClickListener(Object... data) {
                String url = data != null && data.length > 0 ? data[0].toString() : null;

                if (url == null) {
                    return;
                }

                closeQuickAccessActionMode();
                hideHomepage(url);

                if (UrlUtils.isUrl(url)) {
                    loadUrl(url);
                }
                else {
                    url = UrlUtils.createSearchUrl(getContext(), url);
                    loadUrl(url);
                }

                GaReport.sendReportEvent(getContext(), "QUICK_DIAL_ITEM_CLICK", BrowserFragment.class.getName());
            }

            @Override
            public void onAddItemClickListener() {
                addQuickDialItem();
            }
        };

        final IHistoryServiceAction historyServiceAction = new IHistoryServiceAction() {
            @Override
            public void onLoadComplete() {
            }
        };

        quickDialView.setLayoutManager(new GridLayoutManager(getContext(),numberOfColumns));

        if (quickDialView.getAdapter() == null && !onlyLoadMostVisisted) {
            loadQuickDials(getContext(), quickDialView, quickDialgItemClickListener, new IActionDone() {
                @Override
                public void done() {
                    mostVisitedView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
                    HistoryService.loadMostVisitedAsync(getContext(), mostVisitedView, mostVisistedSeperatorHeader, numberOfRows, itemClickListener, historyServiceAction);
                }
            });
        }
        else {
            mostVisitedView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
            HistoryService.loadMostVisitedAsync(getContext(), mostVisitedView, mostVisistedSeperatorHeader, numberOfRows, itemClickListener, historyServiceAction);
        }
    }

    private void loadQuickDials(final Context context, final RecyclerView quickDialView,
                                final IQuickDialgItemClickListener quickDialgItemClickListener, final IActionDone actionDone) {
        new BackgroundTask(new IBackgroundTask() {
            QuickDialItem[] quickDialItems = null;

            @Override
            public void run() {
                quickDialItems = QuickDialService.load(context);
            }

            @Override
            public void onComplete() {
                if (quickDialItems == null) {
                    return;
                }

                quickDialView.setAlpha(0f);

                //binding here
                QuickDialAdapter adapter = new QuickDialAdapter(context, Arrays.asList(quickDialItems), quickDialgItemClickListener, new OnStartDragListener() {
                    @Override
                    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                        itemTouchHelper.startDrag(viewHolder);
                    }
                });
                quickDialView.setAdapter(adapter);

                ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
                itemTouchHelper = new ItemTouchHelper(callback);
                itemTouchHelper.attachToRecyclerView(quickDialView);

                //animate this
                quickDialView.animate()
                        .alpha(1.0f)
                        .setDuration(500);

                actionDone.done();
            }
        }).execute();
    }

    private void closeQuickAccessActionMode() {
        //Notify QuickDialAdapter
        QuickDialAdapter quickDialAdapter = (QuickDialAdapter) quickDialView.getAdapter();

        if (quickDialAdapter != null) {
            quickDialAdapter.closeActionMode();
        }
    }

    private void addQuickDialItem() {
        final FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager.findFragmentByTag(PromptDialog.FRAGMENT_TAG) != null) {
            // We are already displaying a homescreen dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return;
        }

        final String textHint = getString(R.string.add_quick_dial_text_hint);
        final String title = getString(R.string.add_quick_dial_title);
        final String actionOk = getString(R.string.action_add);
        final String actionCancel = getString(R.string.action_cancel);


        final PromptDialog promptDialog = PromptDialog.newInstance(textHint, title, actionOk, actionCancel, new IPromptDialogResult() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onOk(String result) {
                if (quickDialView == null || quickDialView.getAdapter() == null || result == null || TextUtils.isEmpty(result.trim())) {
                    return;
                }


                QuickDialAdapter quickDialAdapter = (QuickDialAdapter)quickDialView.getAdapter();

                if (quickDialAdapter == null) {
                    return;
                }

                result = result.trim();

                QuickDialItem quickDialItem = QuickDialItem.create(result, result);
                addQuickDialItem(quickDialItem, quickDialAdapter);

                GaReport.sendReportEvent(getContext(), "ADD_QUICK_DIAL_ON_HOME_PAGE", BrowserFragment.class.getName());
            }
        });

        promptDialog.setTargetFragment(BrowserFragment.this, 300);

        try {
            promptDialog.show(fragmentManager, promptDialog.FRAGMENT_TAG);
        } catch (IllegalStateException e) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in adding to homescreen now.
        }
    }

    private void clearHomeData() {
        mostVisitedView.setAdapter(null);
        quickDialView.setAdapter(null);
    }

    private int calcNumberOfMostVisitedColumns() {
        Resources resources = getActivity().getResources();
        float density = resources.getDisplayMetrics().density;
        int screenWidthDp = resources.getConfiguration().screenWidthDp;
        float padding = resources.getDimensionPixelSize(com.xlab.vbrowser.R.dimen.mostvisited_item_padding) / density;
        float mostVisistedItemWidth = resources.getDimensionPixelSize(com.xlab.vbrowser.R.dimen.mostvisisited_item_width) / density + padding;

        return (int)((screenWidthDp - padding) / mostVisistedItemWidth);
    }

    private void showAppbar(boolean shouldShow) {
        appBar.setExpanded(shouldShow, true);
    }

    /*************************************Fetch****************************************/
    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onQueued(@NotNull com.tonyodev.fetch2.Download download) {
        }

        @Override
        public void onCompleted(@NotNull com.tonyodev.fetch2.Download download) {
            ViewUtils.showSnackbarForDownloadingCompletely(getView(), download.getFileName(), download.getFile());
        }

        @Override
        public void onError(@NotNull com.tonyodev.fetch2.Download download) {
        }

        @Override
        public void onProgress(@NotNull com.tonyodev.fetch2.Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        }

        @Override
        public void onPaused(@NotNull com.tonyodev.fetch2.Download download) {
        }

        @Override
        public void onResumed(@NotNull com.tonyodev.fetch2.Download download) {
        }

        @Override
        public void onCancelled(@NotNull com.tonyodev.fetch2.Download download) {
        }

        @Override
        public void onRemoved(@NotNull com.tonyodev.fetch2.Download download) {

        }

        @Override
        public void onDeleted(@NotNull com.tonyodev.fetch2.Download download) {

        }
    };



    /*********************** Shared Preference ***************************************/
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch(s) {
            case Constants.PREF_NIGHTMODE_BRIGHTNESS_KEY:
                ((MainActivity)getActivity()).changeBrightness();
                GaReport.sendReportEvent(getContext(), String.valueOf(settings.getNightModeBrighness()) , "ACTION_CHANGE_NIGHT_MODE_BRIGHTNESS" );
                break;

            case Constants.PREF_INCOGNITO_ENABLED_KEY:
                updateTabsButtonView();
                break;

            default:
                break;
        }

        if (s.equals(getString(R.string.pref_key_logout_when_removing_task))) {
            boolean shouldLogout = settings.shouldLogoutWhenRemovingTask();
            GaReport.sendReportEvent(getContext(), "SHOULD_LOGOUT_AUTOMATICALLY_" + shouldLogout, "SETTINGS", String.valueOf(shouldLogout));
        }
        else  if (s.equals(getString(R.string.pref_key_secure))) {
            boolean shouldUseSecureMode = settings.shouldUseSecureMode();
            GaReport.sendReportEvent(getContext(), "SHOULD_STEALTH_MODE_AUTOMATICALLY_" + shouldUseSecureMode, "SETTINGS", String.valueOf(shouldUseSecureMode));
        }
        else  if (s.equals(getString(R.string.pref_key_performance_block_images))) {
            boolean shouldBlockImages = settings.shouldBlockImages();
            GaReport.sendReportEvent(getContext(), "SHOULD_BLOCK_IMAGE_AUTOMATICALLY_" + shouldBlockImages, "SETTINGS", String.valueOf(shouldBlockImages));
        }
    }


    /***********************Extension**********************************************/
    private void registerExtensions() {
        if (extensions == null) {
            extensions = new ArrayList<>();
        }

        extensions.clear();

        extensions.add(new MediaParser(new WeakReference<View>(downloadMediaButton), this));
        extensions.add(new ReaderModeParser(new WeakReference<View>(readerModeButton), this));
    }

    private void unregisterExtensions() {
        if (extensions == null) {
            return;
        }

        for(BaseExtension extension: extensions) {
            extension.onDestroy();
        }

        extensions.clear();
    }

    public ArrayList<BaseExtension> getExtensions() {
        return extensions;
    }

    private void updateTabsButtonView() {
        tabsCountView.setBackground(settings.isIncognitoEnabled() ? getContext().getDrawable(R.drawable.tabs_background_incognito_on)
                                        : getContext().getDrawable(R.drawable.tabs_background_incognito_off));
        incognitoImageView.setVisibility(settings.isIncognitoEnabled() ? View.VISIBLE : View.GONE);
    }
}
