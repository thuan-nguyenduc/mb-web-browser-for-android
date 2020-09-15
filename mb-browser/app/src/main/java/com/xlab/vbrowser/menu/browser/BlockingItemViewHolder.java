/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.menu.browser;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.ThreadUtils;
import com.xlab.vbrowser.fragment.BrowserFragment;

/* package */ class BlockingItemViewHolder extends BrowserMenuViewHolder implements CompoundButton.OnCheckedChangeListener {
    /* package */ static final int LAYOUT_ID = com.xlab.vbrowser.R.layout.menu_blocking_switch;

    private TextView trackerCounter;
    private TextView trackerTitle;
    private BrowserFragment fragment;

    /* package */ BlockingItemViewHolder(View itemView, final BrowserFragment fragment) {
        super(itemView);

        this.fragment = fragment;

        final Switch switchView = itemView.findViewById(com.xlab.vbrowser.R.id.blocking_switch);
        switchView.setChecked(fragment.getSession().isBlockingEnabled());
        switchView.setOnCheckedChangeListener(this);

        trackerCounter = itemView.findViewById(com.xlab.vbrowser.R.id.trackers_count);
        trackerTitle = itemView.findViewById(R.id.trackers_title);

        updateTrackers(fragment.getSession().getBlockedTrackers().getValue());
    }

    /* package */ void updateTrackers(int trackers) {
        if (fragment.getSession().isBlockingEnabled()) {
            updateTrackingCount(trackers);
        } else {
            disableTrackingCount();
        }
    }

    private void updateTrackingCount(final int count) {
        ThreadUtils.postToMainThread(new Runnable() {
            @Override
            public void run() {
                trackerCounter.setText(String.valueOf(count));
            }
        });
    }

    private void disableTrackingCount() {
        ThreadUtils.postToMainThread(new Runnable() {
            @Override
            public void run() {
                trackerCounter.setText(com.xlab.vbrowser.R.string.content_blocking_disabled);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        fragment.setBlockingEnabled(isChecked);

        // Delay closing the menu and reloading the website a bit so that the user can actually see
        // the switch change its state.
        ThreadUtils.postToMainThreadDelayed(new Runnable() {
            @Override
            public void run() {
                getMenu().dismiss();

                fragment.reload();
            }
        }, /* Switch.THUMB_ANIMATION_DURATION */ 250);

        GaReport.sendReportEvent(fragment.getContext(), isChecked ? "ON_BLOCKING_ADS" : "OFF_BLOCKING_ADS", BlockingItemViewHolder.class.getName(),
                String.valueOf(isChecked));
    }
}
