/*Copyright by MonnyLab*/

package com.xlab.vbrowser.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class FloatingSessionsButton extends FloatingActionButton {
    /**
     * The Answer to the Ultimate Question of Life, the Universe, and Everything. And the number of
     * tabs that is just too many.
     */
    private static final int TOO_MANY_TABS = 42;
    private static final String TOO_MANY_TABS_SYMBOL = ":(";

    private TextPaint textPaint;
    private int tabCount;

    public FloatingSessionsButton(Context context) {
        super(context);
        init();
    }

    public FloatingSessionsButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingSessionsButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        final int textSize = getResources().getDimensionPixelSize(com.xlab.vbrowser.R.dimen.tabs_button_text_size);

        textPaint = new TextPaint(paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSize);
    }

    public void updateSessionsCount(int tabCount) {
        this.tabCount = tabCount;

        setContentDescription(
            getResources().getString(com.xlab.vbrowser.R.string.content_description_tab_counter, String.valueOf(tabCount)));

        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        final FloatingActionButtonBehavior behavior = (FloatingActionButtonBehavior) params.getBehavior();

        final boolean shouldBeVisible = tabCount >= 2;

        if (behavior != null) {
            behavior.setEnabled(shouldBeVisible);
        }

        if (shouldBeVisible) {
            setVisibility(View.VISIBLE);
            invalidate();
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float x = canvas.getWidth() / 2f;
        final float y = (canvas.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);

        final String text = tabCount < TOO_MANY_TABS ? String.valueOf(tabCount) : TOO_MANY_TABS_SYMBOL;

        canvas.drawText(text, x, y, textPaint);
    }
}
