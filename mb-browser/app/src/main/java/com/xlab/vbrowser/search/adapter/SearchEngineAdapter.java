package com.xlab.vbrowser.search.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.search.SearchEngine;

import java.util.List;

/**
 * Created by nguyenducthuan on 3/5/18.
 */

public class SearchEngineAdapter extends ArrayAdapter {
    private List<SearchEngine> searchEngines;
    private Context context;
    private SearchEngine selectedEngine;

    public SearchEngineAdapter(@NonNull Context context, int resourceId, List<SearchEngine> searchEngineList) {
        super(context, resourceId);
        this.searchEngines = searchEngineList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return searchEngines.size();
    }

    @Override
    public Object getItem(int i) {
        return searchEngines.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        // inflate the layout for each list row
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_view_search_engine_item, viewGroup, false);
        }

        // get current item to be displayed
        SearchEngine currentItem = (SearchEngine) getItem(position);
        ImageView iconView = (ImageView)
                convertView.findViewById(com.xlab.vbrowser.R.id.searchEngineIcon);
        iconView.setImageBitmap(currentItem.getIcon());
        // returns the view for the current row
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup viewGroup) {
        // inflate the layout for each list row
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(com.xlab.vbrowser.R.layout.layout_dropdownview_search_engine_item, viewGroup, false);
        }

        // get current item to be displayed
        SearchEngine currentItem = (SearchEngine) getItem(position);

        if (selectedEngine == currentItem) {
            convertView.setBackgroundColor(context.getResources().getColor(com.xlab.vbrowser.R.color.colorAccent));
        }
        else {
            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        ImageView iconView = (ImageView)
                convertView.findViewById(com.xlab.vbrowser.R.id.searchEngineIcon);
        TextView titleView = (TextView)
                convertView.findViewById(com.xlab.vbrowser.R.id.searchEngineTitle);

        //sets the text for item name and item description from the current item object
        iconView.setImageBitmap(currentItem.getIcon());
        titleView.setText(currentItem.getName());

        // returns the view for the current row
        return convertView;
    }

    public void setSelectedEngine(SearchEngine selectedEngine) {
        this.selectedEngine = selectedEngine;
    }
}
