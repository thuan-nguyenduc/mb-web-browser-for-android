package com.xlab.vbrowser.downloadmanagers.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.downloadmanagers.data.FileTypeItem;

import java.util.List;

/**
 * Created by nguyenducthuan on 3/5/18.
 */

public class FileTypeAdapter extends ArrayAdapter {
    private List<FileTypeItem> fileTypeItems;
    private Context context;

    public FileTypeAdapter(@NonNull Context context, int resourceId, List<FileTypeItem> fileTypeItems) {
        super(context, resourceId);
        this.fileTypeItems = fileTypeItems;
        this.context = context;
    }

    @Override
    public int getCount() {
        return fileTypeItems.size();
    }

    @Override
    public Object getItem(int i) {
        return fileTypeItems.get(i);
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
                    inflate(R.layout.layout_file_type_item, viewGroup, false);
        }

        // get current item to be displayed
        FileTypeItem currentItem = (FileTypeItem) getItem(position);

        ImageView iconView = convertView.findViewById(R.id.fileTypeIcon);
        iconView.setVisibility(View.GONE);
        TextView titleView = convertView.findViewById(R.id.fileTypeTitle);
        titleView.setVisibility(View.GONE);

        TextView currentTitle = convertView.findViewById(R.id.currentTitle);
        currentTitle.setVisibility(View.VISIBLE);

        currentTitle.setText(currentItem.title);

        // returns the view for the current row
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup viewGroup) {
        // inflate the layout for each list row
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_file_type_item, viewGroup, false);
        }

        // get current item to be displayed
        FileTypeItem currentItem = (FileTypeItem) getItem(position);

        ImageView iconView = convertView.findViewById(R.id.fileTypeIcon);
        TextView titleView = convertView.findViewById(R.id.fileTypeTitle);

        //sets the text for item name and item description from the current item object
        iconView.setImageDrawable(currentItem.icon);
        titleView.setText(currentItem.title);

        // returns the view for the current row
        return convertView;
    }
}
