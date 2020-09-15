/*Copyright by MonnyLab*/

package com.xlab.vbrowser.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

import java.util.HashSet;
import java.util.Set;

public class MultiselectSearchEngineListPreference extends SearchEngineListPreference {

    public MultiselectSearchEngineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiselectSearchEngineListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getItemResId() {
        return com.xlab.vbrowser.R.layout.search_engine_checkbox_button;
    }

    @Override
    protected void updateDefaultItem(CompoundButton defaultButton) {
        // Hide default engine so it can't be removed
        defaultButton.setVisibility(View.GONE);
    }

    public Set<String> getCheckedEngineIds() {
        final Set<String> engineIdSet = new HashSet<>();

        for (int i = 0; i < searchEngineGroup.getChildCount(); i++) {
            final CompoundButton engineButton = (CompoundButton) searchEngineGroup.getChildAt(i);
            if (engineButton.isChecked()) {
                engineIdSet.add((String) engineButton.getTag());
            }
        }
        return engineIdSet;
    }
}
