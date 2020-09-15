/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * A CoordinatorLayout implementation that resizes dynamically based on whether a keyboard is visible or not.
 */
public class ResizableKeyboardLinearLayout extends LinearLayout {
    private final ResizableKeyboardViewDelegate delegate;

    public ResizableKeyboardLinearLayout(Context context) {
        this(context, null);
    }

    public ResizableKeyboardLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizableKeyboardLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        delegate = new ResizableKeyboardViewDelegate(this, attrs);
    }
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        delegate.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        delegate.onDetachedFromWindow();
    }

    public void reset() {
        delegate.reset();
    }
}
