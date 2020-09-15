/*Copyright by MonnyLab*/

package com.xlab.vbrowser.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

public class FloatingEraseButton extends FloatingActionButton {
    private boolean keepHidden;

    public FloatingEraseButton(Context context) {
        super(context);
    }

    public FloatingEraseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingEraseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateSessionsCount(int tabCount) {
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        final FloatingActionButtonBehavior behavior = (FloatingActionButtonBehavior) params.getBehavior();

        keepHidden = tabCount != 1;

        if (behavior != null) {
            behavior.setEnabled(!keepHidden);
        }

        if (keepHidden) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (keepHidden && visibility == View.VISIBLE) {
            // There are multiple callbacks updating the visibility of the button. Let's make sure
            // we do not show the button if we do not want to.
            return;
        }

        super.setVisibility(visibility);
    }
}
