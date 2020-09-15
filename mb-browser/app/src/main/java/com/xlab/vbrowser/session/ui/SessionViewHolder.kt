/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.xlab.vbrowser.R
import com.xlab.vbrowser.session.Session
import com.xlab.vbrowser.session.SessionManager
import com.xlab.vbrowser.favicon.FaviconService
import com.xlab.vbrowser.trackers.GaReport
import com.xlab.vbrowser.utils.UrlUtils
import java.io.File
import java.lang.ref.WeakReference

class SessionViewHolder internal constructor(private val fragment: SessionsSheetFragment, private val relativeLayout: RelativeLayout) : RecyclerView.ViewHolder(relativeLayout), View.OnClickListener, LifecycleOwner {
    override fun getLifecycle(): Lifecycle {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        @JvmField
        internal val LAYOUT_ID = R.layout.item_session
    }

    private var sessionReference: WeakReference<Session> = WeakReference<Session>(null)
    var titleView: TextView
    var urlView: TextView
    var btnRemove: ImageButton
    val iconImageView: ImageView


    init {
        titleView = relativeLayout.findViewById<TextView>(R.id.title)
        urlView = relativeLayout.findViewById<TextView>(R.id.url)
        btnRemove = relativeLayout.findViewById<ImageButton>(R.id.btnRemove)
        iconImageView = relativeLayout.findViewById<ImageView>(R.id.iconImageView)
        relativeLayout.setOnClickListener(this)
        btnRemove.setOnClickListener(this)
    }

    fun bind(session: Session, position: Int) {
        this.sessionReference = WeakReference(session)

        session.url.observe(fragment, Observer { url ->
            updateUrl(session, position)
        })

        session.title.observe(fragment, Observer { url ->
            updateUrl(session, position)
        })

        session.receivedFavicon.observe(fragment, Observer { favicon ->
            updateUrl(session, position)
        })

        updateUrl(session, position)

        val isCurrentSession = SessionManager.getInstance().isCurrentSession(session)

        updateTextColor(isCurrentSession)
    }

    private fun updateTextColor(isCurrentSession: Boolean) {
        val context = titleView.context;
        val selectedColor = ContextCompat.getColor(context, R.color.colorSelectedSession)
        val normalColor = ContextCompat.getColor(context, R.color.colorNormalSession)

        titleView.setTextColor(if (isCurrentSession) selectedColor else normalColor)
        urlView.setTextColor(if (isCurrentSession) selectedColor else normalColor)
        relativeLayout.background = if (isCurrentSession) context.getDrawable(R.drawable.background_list_selected_item_session)
                                    else context.getDrawable(R.drawable.background_list_item_session)
        btnRemove.setImageDrawable(if (isCurrentSession) context.getDrawable(R.drawable.ic_remove_light)
                                    else context.getDrawable(R.drawable.ic_remove))
    }

    private fun updateUrl(session: Session, position: Int) {
        urlView.text = session.url.value;
        val context = titleView.context

        if (UrlUtils.isBlankUrl(session.url.value)) {
            titleView.text = (position + 1).toString() + ". " + context.getString(R.string.home)
            iconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_earth))
            return
        }

        val faviconPath = FaviconService.getFavicon(context, session.url.value)

        if (faviconPath == null) {
            iconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_earth))
        } else {
            iconImageView.setImageURI(Uri.fromFile(File(faviconPath)))
        }

        val title = session.title.value;
        if (title != null && title.trim() != "" && !UrlUtils.isBlankUrl(title)) {
            titleView.text = (position + 1).toString() + ". " + title;
        }
        else {
            titleView.text = (position + 1).toString() + ". " + context.getString(R.string.home);
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.itemSessionLayout) {
            val session = sessionReference.get() ?: return
            selectSession(session)
            GaReport.sendReportEvent(view.context, "ON_SELECT_SESSION", SessionsSheetFragment::class.java.name)
        }
        else if (view.id == R.id.btnRemove) {
            val session = sessionReference.get() ?: return
            SessionManager.getInstance().removeSession(session)
            GaReport.sendReportEvent(view.context, "DELETE_SESSION_BY_BUTTON", SessionsSheetFragment::class.java.name)
        }
    }

    private fun selectSession(session: Session) {
        fragment.animateAndDismiss().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                try {
                    SessionManager.getInstance().selectSession(session)
                }
                catch(ex: IllegalStateException) {
                    //Ignore this know exception
                }
            }
        })
    }
}
