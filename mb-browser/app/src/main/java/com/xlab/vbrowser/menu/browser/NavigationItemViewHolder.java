/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.menu.browser;

import android.view.View;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.fragment.BrowserFragment;

public class NavigationItemViewHolder extends BrowserMenuViewHolder {
    public static final int LAYOUT_ID = R.layout.menu_navigation;

    final View refreshButton;
    final View stopButton;

    public NavigationItemViewHolder(View itemView, BrowserFragment fragment) {
        super(itemView);

        refreshButton = itemView.findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);

        stopButton = itemView.findViewById(R.id.stop);
        stopButton.setOnClickListener(this);

        updateLoading(fragment.getSession().getLoading().getValue());

        final View forwardView = itemView.findViewById(R.id.forward);
        if (!fragment.canGoForward()) {
            forwardView.setEnabled(false);
            forwardView.setAlpha(0.5f);
        } else {
            forwardView.setOnClickListener(this);
        }
    }

    public void updateLoading(boolean loading) {
        refreshButton.setVisibility(loading ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
