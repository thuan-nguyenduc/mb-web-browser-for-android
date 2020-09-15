/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.menu.browser;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.xlab.vbrowser.fragment.BrowserFragment;

public abstract class BrowserMenuViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private BrowserMenu menu;
    protected BrowserFragment browserFragment;

    public BrowserMenuViewHolder(View itemView) {
        super(itemView);
    }

    public void setMenu(BrowserMenu menu) {
        this.menu = menu;
    }

    public BrowserMenu getMenu() {
        return menu;
    }

    public void setOnClickListener(BrowserFragment browserFragment) {
        this.browserFragment = browserFragment;
    }

    @Override
    public void onClick(View view) {
        if (menu != null) {
            menu.dismiss();
        }

        if (browserFragment != null) {
            browserFragment.onClick(view);
        }
    }

    public void closeMenu() {
        if (menu != null) {
            menu.dismiss();
        }
    }
}
