/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.menu.browser;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.utils.UrlUtils;

/* package-private */ class MenuItemViewHolder extends BrowserMenuViewHolder {
    /* package-private */ static final int LAYOUT_ID = com.xlab.vbrowser.R.layout.menu_item;

    private View entireView;
    private TextView titleView;
    private ImageButton actionButtonView;

    /* package-private */ MenuItemViewHolder(View itemView) {
        super(itemView);

        entireView = itemView;
        titleView = (TextView) itemView.findViewById(R.id.titleView);
        actionButtonView = (ImageButton) itemView.findViewById(R.id.actionButtonView);
    }

    /* package-private */ void bind(final BrowserMenuAdapter.MenuItem menuItem) {
        Context context = entireView.getContext();
        entireView.setId(menuItem.id);
        titleView.setText(menuItem.label);
        boolean wasSetIcon = false;

        final String title = browserFragment.getSession() != null ? browserFragment.getSession().getTitle().getValue() : "";
        String url = browserFragment.getInitialUrl();
        final boolean isUrl = url != null
                && UrlUtils.isUrl(url) && !UrlUtils.isBlankUrl(url) && !UrlUtils.isInternalErrorURL(url);

        if (entireView.getId() == com.xlab.vbrowser.R.id.add_to_homescreen && (title == null || TextUtils.isEmpty(title.trim()) || !isUrl)) {
            int color = getAttributeColor(entireView.getContext(), R.attr.inactiveTextColor);
            color = color == -1 ? browserFragment.getResources().getColor(com.xlab.vbrowser.R.color.colorTextInactive) : color;

            titleView.setTextColor(color);
            titleView.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(R.drawable.ic_home_disable), null, null, null);
            entireView.setEnabled(false);
            wasSetIcon = true;
        } else {
            entireView.setOnClickListener(this);
        }

        if (entireView.getId() == com.xlab.vbrowser.R.id.share && !isUrl) {
            int color = getAttributeColor(entireView.getContext(), R.attr.inactiveTextColor);
            color = color == -1 ? browserFragment.getResources().getColor(com.xlab.vbrowser.R.color.colorTextInactive) : color;
            titleView.setTextColor(color);
            titleView.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(R.drawable.ic_share_disable), null, null, null);
            entireView.setEnabled(false);
            wasSetIcon = true;
        }
        else {
            entireView.setOnClickListener(this);
        }

        if (!wasSetIcon) {
            titleView.setCompoundDrawablesWithIntrinsicBounds(menuItem.icon, null, null, null);
        }

        actionButtonView.setVisibility(menuItem.menuAction != null ? View.VISIBLE : View.GONE);

        if (menuItem.menuAction != null) {
            actionButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View var1) {
                    closeMenu();
                    menuItem.menuAction.onPerformAction();
                }
            });
        }
    }

    private int getAttributeColor(
            Context context,
            int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = context.getResources().getColor(colorRes);
        } catch (Resources.NotFoundException e) {
        }

        return color;
    }
}
