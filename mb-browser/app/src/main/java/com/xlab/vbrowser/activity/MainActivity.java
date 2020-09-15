/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.xlab.vbrowser.BuildConfig;
import com.xlab.vbrowser.R;
import com.xlab.vbrowser.UpApplication;
import com.xlab.vbrowser.architecture.NonNullObserver;
import com.xlab.vbrowser.bookmark.db.BookmarkDb;
import com.xlab.vbrowser.events.IPurchasedResult;
import com.xlab.vbrowser.fragment.BrowserFragment;
import com.xlab.vbrowser.fragment.UrlInputFragment;
import com.xlab.vbrowser.history.db.HistoryDb;
import com.xlab.vbrowser.history.db.MostVisitedDb;
import com.xlab.vbrowser.history.db.SearchTermDb;
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity;
import com.xlab.vbrowser.menu.context.PaymentContextMenu;
import com.xlab.vbrowser.payment.utils.IabBroadcastReceiver;
import com.xlab.vbrowser.payment.utils.IabHelper;
import com.xlab.vbrowser.payment.utils.IabResult;
import com.xlab.vbrowser.payment.utils.Inventory;
import com.xlab.vbrowser.payment.utils.Purchase;
import com.xlab.vbrowser.quickdial.db.QuickDialDb;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.SessionNotificationService;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.session.service.ISessionHistoryListener;
import com.xlab.vbrowser.session.service.SessionHistoryService;
import com.xlab.vbrowser.session.ui.SessionsSheetFragment;
import com.xlab.vbrowser.styles.ThemeUtils;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.FileExtUtils;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.utils.SafeIntent;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlConstants;
import com.xlab.vbrowser.utils.ViewUtils;
import com.xlab.vbrowser.web.IWebView;
import com.xlab.vbrowser.web.WebViewProvider;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends LocaleAwareAppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener{
    public static final String ACTION_SIGNOUT = "signout";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_FILE_OPEN = "open_file";
    public static final String ACTION_OPEN_DOWNLOAD_MANAGER = "open_download_manager";
    public static final String EXTRA_TEXT_SELECTION = "text_selection";

    private static final String TAG = "MainActivity";

    //Google Billing
    private static final String SKU_PREMIUM = "premium";
    private static final String SKU_MONTHLY = "sku_monthly";
    private static final String SKU_YEARLY = "sku_yearly";
    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;
    // Does the user have the premium upgrade?
    private boolean isPremium = false;
    private boolean isMonthySubcription = false;
    private boolean isYearlySubcription = false;
    private String selectedSKU = "";
    private IPurchasedResult purchasedResult;

    // The helper object
    private IabHelper iabHelper;

    // Provides purchase notification while this app is running
    private IabBroadcastReceiver broadcastReceiver;


    private final SessionManager sessionManager;

    private View nightModeView;
    private View loadSessionProgressView;

    private Fetch fetch;
    private SafeIntent newIntent;

    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(FragmentManager fm, Fragment f, View v, Bundle savedInstanceState) {
                    super.onFragmentViewCreated(fm, f, v, savedInstanceState);

                    if (f instanceof BrowserFragment && loadSessionProgressView != null) {
                        loadSessionProgressView.setVisibility(View.GONE);
                    }
                }
            };

    public MainActivity() {
        sessionManager = SessionManager.getInstance();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        }
        catch (Exception e) {
            //Fix https://play.google.com/apps/publish/?account=6112520402376869375#AndroidMetricsErrorsPlace:p=com.xlab.vbrowser&appid=4972283308480494280&appVersion&clusterName=apps/com.xlab.vbrowser/clusters/498f31f1&detailsSpan=7
        }

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setContentView(com.xlab.vbrowser.R.layout.activity_main);
        nightModeView = findViewById(R.id.nightModeView);
        loadSessionProgressView = findViewById(R.id.load_sessions_progress_view);
        ThemeUtils.loadNightmode(nightModeView, settings, this);

        final SafeIntent intent = new SafeIntent(getIntent());

        checkIntent(intent);

        ISessionHistoryListener sessionHistoryListener = new ISessionHistoryListener() {
            @Override
            public void onDone() {
                sessionManager.handleIntent(MainActivity.this, intent, savedInstanceState);
                sessionManager.getSessions().observe(MainActivity.this,  new NonNullObserver<List<Session>>() {
                    @Override
                    public void onValueChanged(@NonNull final List<Session> sessions) {
                        if (sessions.isEmpty()) {
                            // There's no active session.
                            // start a new session.
                            openFirstBlankTab();
                        } else {
                            showBrowserScreenForCurrentSession();
                        }
                    }
                });
            }
        };

        //Register Fragment Lifescycle Callbacks.
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);

        //Load Session Histories here if sessions is null
        if (sessionManager.getSessions().getValue().size() <= 0) {
            SessionHistoryService.load(this, sessionHistoryListener);
        }
        else {
            sessionHistoryListener.onDone();
        }

        //Preload Ads block list
        WebViewProvider.preload(this);

        //Process Fetch Download Manager
        processDownloadManager();

        //Setup Google Billing
        setupPayment();

        GaReport.sendReportScreen(getBaseContext(), MainActivity.class.getName());
    }

    @Override
    protected void onNewIntent(Intent unsafeIntent) {
        newIntent = new SafeIntent(unsafeIntent);

        final String action = newIntent.getAction();

        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_SIGNOUT:
                Log.d("onNewIntent", action);
                processSignoutAction();
                break;

            case ACTION_OPEN_DOWNLOAD_MANAGER:
                openDownloadManager();
                GaReport.sendReportEvent(getBaseContext(), ACTION_OPEN_DOWNLOAD_MANAGER, "ACTION_" + MainActivity.class.getName());
                break;

            case ACTION_FILE_OPEN:
                openFile(newIntent);
                break;

            default:
                break;
        }
    }

    private synchronized void processNewIntent() {
        if (newIntent == null) {
            return;
        }

        sessionManager.handleNewIntent(this, newIntent);
        newIntent = null;
    }

    @Override
    public void applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        //Process NewIntent after 1s to make sure that all old session histories is loaded before process newIntent;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                processNewIntent();
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Save Session Histories
        SessionHistoryService.save(this, new ISessionHistoryListener() {
            @Override
            public void onDone() {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // very important:
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }

        // very important:
        if (iabHelper != null) {
            iabHelper.disposeWhenFinished();
            iabHelper = null;
        }

        //Close DB
        HistoryDb.destroyInstance();
        MostVisitedDb.destroyInstance();
        BookmarkDb.destroyInstance();
        SearchTermDb.destroyInstance();
        QuickDialDb.destroyInstance();

        //Signout of all websites if needed
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                WebViewProvider.signoutOfWebsitesIfNeeded(getBaseContext());
            }

            @Override
            public void onComplete() {

            }
        }).execute();

        //Unregister callbacks
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);

        //Unregister fetch
        try {
            fetch.removeListener(fetchListener);
        }
        catch (com.tonyodev.fetch2.exception.FetchException e) {
            //Sometime fetch is closed before and throwExceptionIfClosed is called.
        }

        //Signout of all websites if needed
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                WebViewProvider.signoutOfWebsitesIfNeeded(MainActivity.this);
            }

            @Override
            public void onComplete() {

            }
        }).execute();
    }

    private void checkIntent(SafeIntent safeIntent) {
        if (safeIntent == null) {
            return;
        }

        final String action = safeIntent.getAction();

        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_SIGNOUT:
                Log.d("checkIntent", action);
                processSignoutAction();
                break;

            case ACTION_OPEN_DOWNLOAD_MANAGER:
                openDownloadManager();
                GaReport.sendReportEvent(getBaseContext(), ACTION_OPEN_DOWNLOAD_MANAGER, "ACTION_" + MainActivity.class.getName());
                break;

            case ACTION_FILE_OPEN:
                openFile(safeIntent);
                break;

            default:
                break;
        }

    }

    private void openFile(SafeIntent intent) {
        String file = intent.getDataString();
        Log.d("openFile", file);

        if (TextUtils.isEmpty(file)) {
            return;
        }

        FileExtUtils.openFile(this, file);
        GaReport.sendReportEvent(getBaseContext(), ACTION_FILE_OPEN, "ACTION_" + MainActivity.class.getName());
    }

    private void processSignoutAction() {
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                WebViewProvider.signoutOfWebsites();
            }

            @Override
            public void onComplete() {
                SessionManager.getInstance().removeAllSessions();
                showEraseInfo(R.string.feedback_signout);
                GaReport.sendReportEvent(getBaseContext(), ACTION_SIGNOUT, "ACTION_" + MainActivity.class.getName());
            }
        }).execute();
    }

    public void showEraseInfo(int stringId) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager
                .findFragmentByTag(BrowserFragment.FRAGMENT_TAG);

        final boolean isShowingBrowser = browserFragment != null;

        if (isShowingBrowser) {
            ViewUtils.showBrandedSnackbar(findViewById(android.R.id.content),
                    stringId,
                    getResources().getInteger(R.integer.erase_snackbar_delay));
        }
    }

    private void openFirstBlankTab() {
        SessionManager.getInstance().createSession(Source.FIRST_BLANK_TAB, UrlConstants.getHomeUrl());
    }

    private void showBrowserScreenForCurrentSession() {
        try {
            final Session currentSession = sessionManager.getCurrentSession();

            if (currentSession == null) {
                return;
            }

            final FragmentManager fragmentManager = getSupportFragmentManager();

            final BrowserFragment fragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
            if (fragment != null && fragment.getSession().isSameAs(currentSession)) {
                // There's already a BrowserFragment displaying this session.
                return;
            }

            fragmentManager
                    .beginTransaction()
                    .replace(com.xlab.vbrowser.R.id.container,
                            BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                    .commit();
        }
        catch (IllegalStateException e) {
            //This exception raised when share a website from this browser to this browser on Android 5.1
            //This exception raised when fargmenetManager.commit() after onSavedInstance,
            //so we call fragmentManager.commitAllowingStateLoss() for adding new fragment without exception
            //however it exist a dangerous as description of commitAllowingStateLoss()
            showBrowserScreenForCurrentSessionAfterSavingSate();
        }
    }

    private void showBrowserScreenForCurrentSessionAfterSavingSate() {
        final Session currentSession = sessionManager.getCurrentSession();

        if (currentSession == null) {
            return;
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();

        final BrowserFragment fragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (fragment != null && fragment.getSession().isSameAs(currentSession)) {
            // There's already a BrowserFragment displaying this session.
            return;
        }

        fragmentManager
                .beginTransaction()
                .replace(com.xlab.vbrowser.R.id.container,
                        BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        if (name.equals(IWebView.class.getName())) {
            // Inject our implementation of IWebView from the WebViewProvider.
            return WebViewProvider.create(this, attrs);
        }

        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();

        final SessionsSheetFragment sessionsSheetFragment = (SessionsSheetFragment) fragmentManager.findFragmentByTag(SessionsSheetFragment.FRAGMENT_TAG);
        if (sessionsSheetFragment != null &&
                sessionsSheetFragment.isVisible() &&
                sessionsSheetFragment.onBackPressed()) {
            // SessionsSheetFragment handles back presses itself (custom animations).
            return;
        }

        final UrlInputFragment urlInputFragment = (UrlInputFragment) fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (urlInputFragment != null &&
                urlInputFragment.isVisible() &&
                urlInputFragment.onBackPressed()) {
            // The URL input fragment has handled the back press. It does its own animations so
            // we do not try to remove it from outside.
            return;
        }

        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (browserFragment != null &&
                browserFragment.isVisible() &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return;
        }

        super.onBackPressed();
    }

    public void processNightmode() {
        ThemeUtils.processNightmode(nightModeView, settings, this);
        applyCurrentTheme();
    }

    public void changeBrightness() {
        ThemeUtils.changeBrightness(nightModeView, settings, this);
    }

    private void processDownloadManager() {
        fetch = ((UpApplication)getApplication()).getFetch();
        fetch.addListener(fetchListener);
    }

    FetchListener fetchListener = new FetchListener() {
        @Override
        public void onQueued(@NotNull Download download) {
            Log.d("FetchListener", "onQueued");
            SessionNotificationService.start(MainActivity.this);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
        }

        @Override
        public void onError(@NotNull Download download) {
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            Log.d("FetchListener", "onProgress");
            SessionNotificationService.start(MainActivity.this);
        }

        @Override
        public void onPaused(@NotNull Download download) {
        }

        @Override
        public void onResumed(@NotNull Download download) {
            Log.d("FetchListener", "onResumed");
            SessionNotificationService.start(MainActivity.this);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
        }

        @Override
        public void onRemoved(@NotNull Download download) {
        }

        @Override
        public void onDeleted(@NotNull Download download) {
        }
    };

    /*
    *Google Billing
    */
    private void setupPayment() {
        iabHelper = new IabHelper(this, getString(R.string.app_public_key));

        // enable debug logging (for a production application, you should set this to false).
        iabHelper.enableDebugLogging(BuildConfig.DEBUG);

        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "Setup failed " + result.getMessage());
                    // Oh noes, there was a problem.
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (iabHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                broadcastReceiver = new IabBroadcastReceiver(MainActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(broadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    iabHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                }
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            isPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (isPremium ? "PREMIUM" : "NOT PREMIUM"));

            // First find out which subscription is auto renewing
            Purchase monthlyPackage = inventory.getPurchase(SKU_MONTHLY);
            Purchase yearlyPackage = inventory.getPurchase(SKU_YEARLY);
            if (monthlyPackage != null && monthlyPackage.isAutoRenewing()
                    && verifyDeveloperPayload(monthlyPackage)) {
                isMonthySubcription = true;
            } else if (yearlyPackage != null && yearlyPackage.isAutoRenewing()
                    && verifyDeveloperPayload(yearlyPackage)) {
                isYearlySubcription = true;
            } else {
                isMonthySubcription = false;
            }

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            iabHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            Log.d(TAG, "Error querying inventory. Another async operation in progress.");
        }
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return payload.equals(getString(R.string.app_payload));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (iabHelper == null || !iabHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (iabHelper == null) {
                if (purchasedResult != null) {
                    purchasedResult.onFailed();
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_FAILED_IAB_NULL", "ACTION_REQUEST_PAYMENTS");

                return;
            }

            if (result.isFailure()) {
                if (purchasedResult != null) {
                    purchasedResult.onFailed();
                }

                String message = result.getMessage();

                if (message == null) {
                    message = "";
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_FAILED_" + selectedSKU , "ACTION_REQUEST_PAYMENTS", message);

                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                if (purchasedResult != null) {
                    purchasedResult.onFailed();
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_VERIFY_FAILED", "ACTION_REQUEST_PAYMENTS");

                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                isPremium = true;

                if (purchasedResult != null) {
                    purchasedResult.onSuccess();
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_PREMIUM", "ACTION_REQUEST_PAYMENTS");
            }
            else if (purchase.getSku().equals(SKU_MONTHLY)) {
                Log.d(TAG, "Monthly subscription purchased.");
                isMonthySubcription = true;

                if (purchasedResult != null) {
                    purchasedResult.onSuccess();
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_MONTHLY_SUB", "ACTION_REQUEST_PAYMENTS");
            }
            else if (purchase.getSku().equals(SKU_YEARLY)) {
                Log.d(TAG, "Yearly subscription purchased.");
                isYearlySubcription = true;

                if (purchasedResult != null) {
                    purchasedResult.onSuccess();
                }

                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_PURCHASED_YEARLYY_SUB", "ACTION_REQUEST_PAYMENTS");
            }
        }
    };

    private void buySubctiptionPackage(String package_name) {
        if (iabHelper == null || !iabHelper.isSetupDone()) {
            return;
        }

        String payload = getString(R.string.app_payload);
        List<String> oldSkus = null;
        if (isMonthySubcription || isYearlySubcription) {
            // The user currently has a valid subscription, any purchase action is going to
            // replace that subscription
            oldSkus = new ArrayList<String>();

            if (isMonthySubcription) {
                oldSkus.add(SKU_MONTHLY);
            }

            if (isYearlySubcription) {
                oldSkus.add(SKU_YEARLY);
            }
        }

        try {
            iabHelper.launchPurchaseFlow(this, package_name, IabHelper.ITEM_TYPE_SUBS,
                    oldSkus, RC_REQUEST, purchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            if (this.purchasedResult != null) {
                this.purchasedResult.onFailed();
            }
        }
    }

    private void buyPremiumPackage() {
        if (iabHelper == null || !iabHelper.isSetupDone()) {
            return;
        }

        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = getString(R.string.app_payload);

        try {
            iabHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
                    purchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            if (this.purchasedResult != null) {
                this.purchasedResult.onFailed();
            }
        }
    }

    public void requestPayments(IPurchasedResult purchasedResult) {
        GaReport.sendReportEvent(MainActivity.this, "REQUEST_PAYMENTS", "ACTION_REQUEST_PAYMENTS");
        this.purchasedResult = purchasedResult;

        if (isMonthySubcription || isYearlySubcription || isPremium || iabHelper == null || !iabHelper.isSetupDone()) {
            purchasedResult.onSuccess();

            return;
        }

        PaymentContextMenu.show(this, getString(R.string.payment_title_dialog), new PaymentContextMenu.IActionMenu() {
            @Override
            public void onPremium() {
                selectedSKU = SKU_PREMIUM;
                buyPremiumPackage();
                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_START_BUY_PREMIUM", "ACTION_REQUEST_PAYMENTS");
            }

            @Override
            public void onMonthly() {
                selectedSKU = SKU_MONTHLY;
                buySubctiptionPackage(SKU_MONTHLY);
                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_START_BUY_MONTHLY_SUB", "ACTION_REQUEST_PAYMENTS");
            }

            @Override
            public void onYealy() {
                selectedSKU = SKU_YEARLY;
                buySubctiptionPackage(SKU_YEARLY);
                GaReport.sendReportEvent(MainActivity.this, "CATEGORY_START_BUY_YEARLY_SUB", "ACTION_REQUEST_PAYMENTS");
            }
        });

        GaReport.sendReportEvent(MainActivity.this, "CATEGORY_SHOWN_PAYMENT_METHOD_DIALOG", "ACTION_REQUEST_PAYMENTS");
    }
}
