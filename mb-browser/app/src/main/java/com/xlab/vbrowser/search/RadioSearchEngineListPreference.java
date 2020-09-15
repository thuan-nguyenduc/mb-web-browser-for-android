/*Copyright by MonnyLab*/

package com.xlab.vbrowser.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.xlab.vbrowser.utils.Settings;

public class RadioSearchEngineListPreference extends SearchEngineListPreference implements RadioGroup.OnCheckedChangeListener {

    public RadioSearchEngineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RadioSearchEngineListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final View view = super.onCreateView(parent);
        searchEngineGroup.setOnCheckedChangeListener(this);
        return view;
    }

    @Override
    protected int getItemResId() {
        return com.xlab.vbrowser.R.layout.search_engine_radio_button;
    }

    @Override
    protected void updateDefaultItem(CompoundButton defaultButton) {
        defaultButton.setChecked(true);
    }


    @Override
    public void onCheckedChanged (RadioGroup group, int checkedId) {
        Settings.getInstance(group.getContext()).setDefaultSearchEngine(searchEngines.get(checkedId));
    }
}
