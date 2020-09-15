/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.xlab.vbrowser.fragment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.v7.widget.LinearLayoutManager
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.FrameLayout
import com.xlab.vbrowser.R
import com.xlab.vbrowser.activity.InfoActivity
import com.xlab.vbrowser.autocomplete.UrlAutoCompleteFilter
import com.xlab.vbrowser.events.IItemClickListener
import com.xlab.vbrowser.history.service.HistoryService
import com.xlab.vbrowser.locale.LocaleAwareAppCompatActivity
import com.xlab.vbrowser.locale.LocaleAwareFragment
import com.xlab.vbrowser.permission.IRequestPermissionResult
import com.xlab.vbrowser.search.ISearchSuggestionCallback
import com.xlab.vbrowser.search.SearchEngine
import com.xlab.vbrowser.search.SearchEngineManager
import com.xlab.vbrowser.search.SearchSuggestion
import com.xlab.vbrowser.search.adapter.SearchEngineAdapter
import com.xlab.vbrowser.search.adapter.SearchSuggestionAdapter
import com.xlab.vbrowser.search.data.SearchSuggestionItem
import com.xlab.vbrowser.session.NullSession
import com.xlab.vbrowser.session.Session
import com.xlab.vbrowser.session.SessionManager
import com.xlab.vbrowser.session.Source
import com.xlab.vbrowser.trackers.GaReport
import com.xlab.vbrowser.utils.*
import com.xlab.vbrowser.widget.InlineAutocompleteEditText
import kotlinx.android.synthetic.main.fragment_urlinput.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

/**
 * Fragment for displaying he URL input controls.
 */
class UrlInputFragment : LocaleAwareFragment(), View.OnClickListener, InlineAutocompleteEditText.OnCommitListener,
        InlineAutocompleteEditText.OnFilterListener,
        EasyPermissions.PermissionCallbacks{
    companion object {
        @JvmField
        val FRAGMENT_TAG = "url_input"

        private val ARGUMENT_ANIMATION = "animation"
        private val ARGUMENT_X = "x"
        private val ARGUMENT_Y = "y"
        private val ARGUMENT_WIDTH = "width"
        private val ARGUMENT_HEIGHT = "height"

        private val ARGUMENT_SESSION_UUID = "sesssion_uuid"

        private val ANIMATION_BROWSER_SCREEN = "browser_screen"

        private val PLACEHOLDER = "5981086f-9d45-4f64-be99-7d2ffa03befb";

        private val ANIMATION_DURATION = 200

        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        private const val RESULT_SPEECH = 1

        @JvmStatic
        fun createWithoutSession(): UrlInputFragment {
            val arguments = Bundle()

            val fragment = UrlInputFragment()
            fragment.arguments = arguments

            return fragment
        }

        @JvmStatic
        fun createWithSession(session: Session, urlView: View): UrlInputFragment {
            val arguments = Bundle()

            arguments.putString(ARGUMENT_SESSION_UUID, session.uuid)
            arguments.putString(ARGUMENT_ANIMATION, ANIMATION_BROWSER_SCREEN)

            val screenLocation = IntArray(2)
            urlView.getLocationOnScreen(screenLocation)

            arguments.putInt(ARGUMENT_X, screenLocation[0])
            arguments.putInt(ARGUMENT_Y, screenLocation[1])
            arguments.putInt(ARGUMENT_WIDTH, urlView.width)
            arguments.putInt(ARGUMENT_HEIGHT, urlView.height)

            val fragment = UrlInputFragment()
            fragment.arguments = arguments

            return fragment
        }

        /**
         * Create a new UrlInputFragment with a gradient background (and the Focus logo). This configuration
         * is usually shown if there's no content to be shown below (e.g. the current website).
         */
        @JvmStatic
        fun createWithBackground(): UrlInputFragment {
            val arguments = Bundle()

            val fragment = UrlInputFragment()
            fragment.arguments = arguments

            return fragment
        }
    }

    private val urlAutoCompleteFilter: UrlAutoCompleteFilter = UrlAutoCompleteFilter()

    @Volatile private var isAnimating: Boolean = false

    private var session: Session? = null

    private var currentSearchTerm: String? = null

    private val isOverlay: Boolean
        get() = session != null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get session from session manager if there's a session UUID in the fragment's arguments
        arguments?.getString(ARGUMENT_SESSION_UUID)?.let {
            try {
                session = SessionManager.getInstance().getSessionByUUID(it)
            }
            catch(ex: java.lang.IllegalAccessError) {
                session = NullSession()
                //Fix https://play.google.com/apps/publish/?account=6112520402376869375#AndroidMetricsErrorsPlace:p=com.xlab.vbrowser&appid=4972283308480494280&appVersion&clusterName=apps/com.xlab.vbrowser/clusters/a40f834e&detailsSpan=7
            }
        }

        context?.let {
            urlAutoCompleteFilter.initialize(it.applicationContext)
        }

        GaReport.sendReportScreen(context, UrlInputFragment::class.java.name)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_urlinput, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listOf(dismissView, clearView, searchView, voiceView).forEach { it.setOnClickListener(this) }

        urlView.setOnFilterListener(this)
        urlView.imeOptions = urlView.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING

        urlInputContainerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                urlInputContainerView.viewTreeObserver.removeOnPreDrawListener(this)

                animateFirstDraw()

                return true
            }
        })

        if (isOverlay) {
            //keyboardLinearLayout.visibility = View.GONE
        } else {
            backgroundView.setBackgroundResource(R.drawable.background_primary)

            dismissView.visibility = View.GONE
            toolbarBackgroundView.visibility = View.GONE
        }

        urlView.setOnCommitListener(this)

        //Process Search Engines
        if (SearchEngineManager.getInstance().searchEngines != null &&
                SearchEngineManager.getInstance().searchEngines.size > 0) {

            val adapter = SearchEngineAdapter(view.context, 0,
                    SearchEngineManager.getInstance().searchEngines)
            searchEnginesView.adapter = adapter

            var selectedEngine: SearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(view.context)

            var selectedIndex = SearchEngineManager.getInstance().searchEngines.indexOf(selectedEngine)

            if (selectedIndex <= 0) {
                selectedIndex = 0;
                selectedEngine = SearchEngineManager.getInstance().searchEngines.get(0)
            }

            searchEnginesView.setSelection(selectedIndex)
            adapter.setSelectedEngine(selectedEngine)

            searchEnginesView.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    var engine = SearchEngineManager.getInstance().searchEngines.get(p2)

                    if (engine == null) {
                        return
                    }

                    adapter.setSelectedEngine(engine)

                    if (!engine.equals(Settings.getInstance(p1?.context).defaultSearchEngineName)) {
                        Settings.getInstance(p1?.context).setDefaultSearchEngine(engine)
                        GaReport.sendReportEvent(p1?.context, "CHANGE_SEARCH_ENGINE", UrlInputFragment::class.java.name)
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {
                    // your code here
                }
            }

            searchEnginesView.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, m: MotionEvent): Boolean {
                    // Perform tasks here
                    var urlView = activity?.findViewById<View>(R.id.urlView)

                    if (urlView != null) {
                        ViewUtils.hideKeyboard(urlView)
                    }
                    return false
                }
            })

            // use a linear layout manager
            val layoutManager = LinearLayoutManager(context)
            searchSuggestionView.setLayoutManager(layoutManager)
        }

        session?.let {
            var url = if (UrlUtils.isBlankUrl(it.url.value)) "" else it.url.value
            url = if (it.isSearch) it.searchTerms else url
            urlView.setText(url)

            if (!TextUtils.isEmpty(url)) {
                clearView.visibility = View.VISIBLE
            }
        }
    }

    override fun applyLocale() {
        if (isOverlay) {
            activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.container, createWithSession(session!!, urlView), FRAGMENT_TAG)
                    ?.commit()
        } else {
            activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.container, createWithBackground(), FRAGMENT_TAG)
                    ?.commit()
        }
    }

    fun onBackPressed(): Boolean {
        if (isOverlay) {
            animateAndDismiss()
            return true
        }

        return false
    }

    override fun onStart() {
        super.onStart()

        if (context == null) return

        showKeyboard()
    }

    override fun onStop() {
        super.onStop()
    }

    fun showKeyboard() {
        ViewUtils.showKeyboard(urlView)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clearView -> clear()

            R.id.searchView -> onSearch()

            R.id.dismissView -> if (isOverlay) {
                animateAndDismiss()
            } else {
                clear()
            }

            R.id.whats_new -> context?.let {
                SessionManager.getInstance()
                        .createSession(Source.MENU, SupportUtils.getWhatsNewUrl(context))
            }

            R.id.settings -> (activity as LocaleAwareAppCompatActivity).openPreferences()

            R.id.help -> {
                val helpIntent = InfoActivity.getHelpIntent(activity)
                startActivity(helpIntent)
            }

            R.id.voiceView -> {
                startVoiceRecord()
            }

            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    private fun clear() {
        session?.searchTerms = ""
        urlView.setText("")
        urlView.requestFocus()
    }

    override fun onDetach() {
        super.onDetach()
    }

    private fun animateFirstDraw() {
        if (ANIMATION_BROWSER_SCREEN == arguments?.getString(ARGUMENT_ANIMATION)) {
            playVisibilityAnimation(false)
        }
    }

    private fun animateAndDismiss() {
        ThreadUtils.assertOnUiThread()

        if (isAnimating) {
            // We are already animating some state change. Ignore all other requests.
            return
        }

        // Don't allow any more clicks: dismissView is still visible until the animation ends,
        // but we don't want to restart animations and/or trigger hiding again (which could potentially
        // cause crashes since we don't know what state we're in). Ignoring further clicks is the simplest
        // solution, since dismissView is about to disappear anyway.
        dismissView.isClickable = false

        if (ANIMATION_BROWSER_SCREEN == arguments?.getString(ARGUMENT_ANIMATION)) {
            playVisibilityAnimation(true)
        } else {
            dismiss()
        }
    }

    /**
     * This animation is quite complex. The 'reverse' flag controls whether we want to show the UI
     * (false) or whether we are going to hide it (true). Additionally the animation is slightly
     * different depending on whether this fragment is shown as an overlay on top of other fragments
     * or if it draws its own background.
     */
    private fun playVisibilityAnimation(reverse: Boolean) {
        if (isAnimating) {
            // We are already animating, let's ignore another request.
            return
        }

        isAnimating = true

        val xyOffset = (if (isOverlay)
            (urlInputContainerView.layoutParams as FrameLayout.LayoutParams).bottomMargin
        else
            0).toFloat()

        val width = urlInputBackgroundView.width.toFloat()
        val height = urlInputBackgroundView.height.toFloat()

        val widthScale = if (isOverlay)
            (width + 2 * xyOffset) / width
        else
            1f

        val heightScale = if (isOverlay)
            (height + 2 * xyOffset) / height
        else
            1f

        if (!reverse) {
            //urlInputBackgroundView.pivotX = 0f
            //urlInputBackgroundView.pivotY = 0f
            //urlInputBackgroundView.scaleX = widthScale
            //urlInputBackgroundView.scaleY = heightScale
            //urlInputBackgroundView.translationX = -xyOffset
            //urlInputBackgroundView.translationY = -xyOffset

            clearView.alpha = 0f
            voiceView.alpha = 0f
            searchEnginesView.alpha = 0f
            searchViewContainer.alpha = 0f
        }

        // Let the URL input use the full width/height and then shrink to the actual size
        urlInputBackgroundView.animate()
                .setDuration(ANIMATION_DURATION.toLong())
                //.scaleX(if (reverse) widthScale else 1f)
                //.scaleY(if (reverse) heightScale else 1f)
                .alpha((if (reverse && isOverlay) 0 else 1).toFloat())
                //.translationX(if (reverse) -xyOffset else 0f)
                //.translationY(if (reverse) -xyOffset else 0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        try {
                            if (reverse) {
                                clearView.alpha = 0f
                                voiceView.alpha = 0f
                                searchEnginesView.alpha = 0f
                                searchViewContainer.alpha = 0f
                            }
                        }
                        catch(ex: IllegalStateException) {
                            //A exception occurs here, we ignore it.
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            if (reverse) {
                                if (isOverlay) {
                                    dismiss()
                                }
                            } else {
                                clearView?.alpha = 1f
                                voiceView?.alpha = 1f
                                searchEnginesView?.alpha = 1f
                                searchViewContainer?.alpha = 1f
                            }

                            isAnimating = false
                        }
                        catch(ex: IllegalStateException) {
                            //A exception occurs here, we ignore it.
                        }
                    }
                })

        // We only need to animate the toolbar if we are an overlay.
        if (isOverlay) {
            val screenLocation = IntArray(2)
            urlView.getLocationOnScreen(screenLocation)

            val leftDelta = arguments!!.getInt(ARGUMENT_X) - screenLocation[0] - urlView.paddingLeft

            if (!reverse) {
                urlView.pivotX = 0f
                urlView.pivotY = 0f
                urlView.translationX = leftDelta.toFloat()
            }

            // The URL moves from the right (at least if the lock is visible) to it's actual position
            urlView.animate()
                    .setDuration(ANIMATION_DURATION.toLong())
                    .translationX((if (reverse) leftDelta else 0).toFloat())
        }

        if (!reverse) {
            //toolbarBackgroundView.alpha = 0f
            clearView.alpha = 0f
            voiceView.alpha = 0f
            searchEnginesView.alpha = 0f
            searchViewContainer.alpha = 0f
        }

        // The darker background appears with an alpha animation
        /*toolbarBackgroundView.animate()
                .setDuration(ANIMATION_DURATION.toLong())
                .alpha((if (reverse) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        toolbarBackgroundView.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (reverse) {
                            toolbarBackgroundView.visibility = View.GONE

                            if (!isOverlay) {
                                dismissView.visibility = View.GONE
                                menuView.visibility = View.VISIBLE
                            }
                        }
                    }
                })*/
    }

    private fun dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        activity?.supportFragmentManager
                ?.beginTransaction()
                ?.remove(this)
                ?.commitAllowingStateLoss()
    }

    override fun onCommit() {
        submitInput()
    }

    fun submitInput() {
        val input = urlView.text.toString()
        if (!input.trim { it <= ' ' }.isEmpty()) {
            ViewUtils.hideKeyboard(urlView)

            val isUrl = UrlUtils.isUrl(input)

            val url = if (isUrl)
                UrlUtils.normalize(input)
            else
                UrlUtils.createSearchUrl(context, input)

            val searchTerms = if (isUrl)
                null
            else
                input.trim { it <= ' ' }

            openUrl(url, searchTerms)
        }
    }


    private fun onSearch() {
        val searchTerms = urlView.originalText
        val searchUrl = UrlUtils.createSearchUrl(context, searchTerms)

        openUrl(searchUrl, searchTerms)
    }

    private fun onSearch(searchTerm: String) {
        var theSearchTerm = searchTerm
        var url: String

        if(!UrlUtils.isUrl(theSearchTerm)) {
            url = UrlUtils.createSearchUrl(context, theSearchTerm)
        }
        else {
            url = theSearchTerm
        }

        openUrl(url, theSearchTerm)
    }

    private fun openUrl(url: String, searchTerms: String?) {
        if (searchTerms != null && !UrlUtils.isUrl(searchTerms) && !Settings.getInstance(context).isIncognitoEnabled() ) {
            BackgroundTask(object: IBackgroundTask{
                override fun run() {
                    HistoryService.insertSearchTerm(context, searchTerms);
                }

                override fun onComplete() {
                }
            }).execute()
        }

        session?.searchTerms = searchTerms

        val fragmentManager = activity!!.supportFragmentManager

        // Replace all fragments with a fresh browser fragment. This means we either remove the
        // HomeFragment with an UrlInputFragment on top or an old BrowserFragment with an
        // UrlInputFragment.
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG)

        if (browserFragment != null && browserFragment is BrowserFragment && browserFragment.isVisible) {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFragment.loadUrl(url)

            // And this fragment can be removed again.
            fragmentManager.beginTransaction()
                    .remove(this)
                    .commit()
        } else {
            if (!TextUtils.isEmpty(searchTerms)) {
                SessionManager.getInstance().createSearchSession(Source.USER_ENTERED, url, searchTerms)
            } else {
                SessionManager.getInstance().createSession(Source.USER_ENTERED, url)
            }
        }
    }

    override fun onFilter(searchText: String, view: InlineAutocompleteEditText?) {
        // If the UrlInputFragment has already been hidden, don't bother with filtering. Because of the text
        // input architecture on Android it's possible for onFilter() to be called after we've already
        // hidden the Fragment, see the relevant bug for more background:
        // https://github.com/mozilla-mobile/focus-android/issues/441#issuecomment-293691141
        if (!isVisible) {
            return
        }

        currentSearchTerm = searchText

        urlAutoCompleteFilter.onFilter(searchText, view)

        val itemClickListener= object: IItemClickListener {
            override fun onItemClickListener(vararg data: Any) {
                if (data.size == 1) {
                    onSearch(data[0].toString())
                }
                else if (data.size == 2) {
                    val str = data[0].toString()
                    urlView.setText(str)
                    urlView.setSelection(str .length)
                }
            }
        }

        if (searchText.trim { it <= ' ' }.isEmpty()) {
            clearView.visibility = View.GONE
            searchView.visibility = View.GONE
            searchSeperator.visibility = View.GONE

            //Load the latest histories
            SearchSuggestion.loadSuggestionFromDisk(context, object: ISearchSuggestionCallback {
                override fun onFailure(message: String) {}

                override fun onSuccess(items: Array<SearchSuggestionItem>, searchTerm: String) {
                    activity?.runOnUiThread(object: Runnable{
                        override fun run() {
                            searchScrollView.scrollTo(0, 0)
                            searchSuggestionView.adapter = SearchSuggestionAdapter(context, items, itemClickListener)
                        }
                    })
                }
            })

            if (!isOverlay) {
                playVisibilityAnimation(true)
            }
        } else {
            clearView.visibility = View.VISIBLE

            if (!isOverlay && dismissView.visibility != View.VISIBLE) {
                playVisibilityAnimation(false)
                dismissView.visibility = View.VISIBLE
            }

            // LTR languages sometimes have grammar where the search terms are displayed to the left
            // of the hint string. To take care of LTR, RTL, and special LTR cases, we use a
            // placeholder to know the start and end indices of where we should bold the search text
            val hint = getString(R.string.search_hint, PLACEHOLDER)
            val start = hint.indexOf(PLACEHOLDER)

            val content = SpannableString(hint.replace(PLACEHOLDER, searchText))
            content.setSpan(StyleSpan(Typeface.BOLD), start, start + searchText.length, 0)

            searchView.text = content
            searchView.visibility = View.VISIBLE
            searchSeperator.visibility = View.VISIBLE

            //Load search suggestions
            SearchSuggestion.requestSuggestion(searchText, object: ISearchSuggestionCallback {
                override fun onFailure(message: String) {}

                override fun onSuccess(items: Array<SearchSuggestionItem>, searchTerm: String) {
                    if (searchSuggestionView != null && currentSearchTerm?.equals(searchTerm) == false) {
                        return
                    }

                    activity?.runOnUiThread(object: Runnable{
                        override fun run() {
                            searchScrollView.scrollTo(0, 0)
                            searchSuggestionView.adapter = SearchSuggestionAdapter(context, items, itemClickListener)
                        }
                    })
                }
            })
        }
    }

    /************* Voice record *****************/
    fun startVoiceRecord() {
        setPermissionCallback(object: IRequestPermissionResult {
            override fun onReceivePermission() {
                //get the recognize intent
                var intent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                //Specify the calling package to identify your application
                //intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,context?.packageName);
                //Given an hint to the recognizer about what the user is going to say
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                //specify the max number of results
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
                //User of SpeechRecognizer to "send" the intent.

                try {
                    startActivityForResult(intent, RESULT_SPEECH)
                }
                catch(e: ActivityNotFoundException) {
                    ViewUtils.showBrandedSnackbar(urlView, R.string.no_voice_recognizer, 500);
                }

            }
        })

        requestRecordAudioPermission()
    }

    /**********************Permission */
    private var requestPerssionResult: IRequestPermissionResult? = null

    fun setPermissionCallback(requestPerssionResult: IRequestPermissionResult) {
        this.requestPerssionResult = requestPerssionResult
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return
        }

        // The actual download dialog will be shown from onResume(). If this activity/fragment is
        // getting restored then we need to 'resume' first before we can show a dialog (attaching
        // another fragment).
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun hasRecordAudioPermission(): Boolean {
        return EasyPermissions.hasPermissions(context!!, Manifest.permission.RECORD_AUDIO)
    }

    @AfterPermissionGranted(REQUEST_RECORD_AUDIO_PERMISSION)
    fun requestRecordAudioPermission(): Boolean {
        if (hasRecordAudioPermission()) {
            requestPerssionResult!!.onReceivePermission()
            return true
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.notify_grant_record_audio_permission),
                    REQUEST_RECORD_AUDIO_PERMISSION,
                    Manifest.permission.RECORD_AUDIO)

            return false
        }
    }


    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (hasRecordAudioPermission()) {
                if (this.requestPerssionResult != null) {
                    this.requestPerssionResult!!.onReceivePermission()
                }

            }
        }
        else if (requestCode == RESULT_SPEECH && data != null && resultCode == RESULT_OK) {
            var matches: ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (matches.size < 1) {
                return;
            }

            urlView.setText(matches.get(0))

            submitInput()
        }
    }
}
