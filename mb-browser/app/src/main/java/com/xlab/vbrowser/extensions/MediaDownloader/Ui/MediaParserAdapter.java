package com.xlab.vbrowser.extensions.MediaDownloader.Ui;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.extensions.MediaDownloader.MediaItem;

import java.util.ArrayList;

/**
 * Created by nguyenducthuan on 1/31/18.
 */

public class MediaParserAdapter extends RecyclerView.Adapter<MediaParserAdapter.ViewHolder> {
    private ArrayList<MediaItem> mDataset;
    private ArrayList<MediaItem> mDownloadSelected;
    private IMediaSelectEvent mMediaSelectEvent;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder  {
        // each data item is just a string in this case
        public CheckBox cboTitle;

        public ViewHolder(View view) {
            super(view);
            // set the view's size, margins, paddings and layout parameters
            this.cboTitle = (CheckBox)view.findViewById(R.id.txtTitle);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MediaParserAdapter(ArrayList<MediaItem> myDataset, IMediaSelectEvent mediaSelectEvent) {
        mDataset = myDataset;
        mDownloadSelected = new ArrayList<>();
        mMediaSelectEvent = mediaSelectEvent;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_mediaparser_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final MediaItem currentItem = mDataset.get(position);
        holder.cboTitle.setText(currentItem.namefile);
        holder.cboTitle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton var1, boolean var2) {
                if (var1.isChecked()) {
                    if (!mDownloadSelected.contains(currentItem)) {
                        mDownloadSelected.add(currentItem);

                        if (mDownloadSelected.size() == mDataset.size()) {
                            mMediaSelectEvent.onSelectAll();
                        }
                    }
                }
                else {
                    mDownloadSelected.remove(currentItem);
                    mMediaSelectEvent.onRemoveItem();
                }

                mMediaSelectEvent.onClickItem();
            }
        } );

        holder.cboTitle.setChecked(mDownloadSelected.contains(currentItem));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public ArrayList<MediaItem> getDownloadSelected() {
        Log.d("DownloadSelected:", mDownloadSelected.size() + "");
        return mDownloadSelected;
    }

    public void clearSelected() {
       mDownloadSelected.clear();
       notifyDataSetChanged();
    }

    public void selectAll() {
        mDownloadSelected.clear();
        for(MediaItem mediaItem: mDataset) {
            mDownloadSelected.add(mediaItem);
        }
        notifyDataSetChanged();
    }
}
