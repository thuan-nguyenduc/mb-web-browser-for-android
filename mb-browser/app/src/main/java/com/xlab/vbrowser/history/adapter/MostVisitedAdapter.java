package com.xlab.vbrowser.history.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.events.IItemClickListener;
import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.history.entity.MostVisited;
import com.xlab.vbrowser.menu.context.MostVisitedMenu;

import java.io.File;

/**
 * Created by nguyenducthuan on 2/27/18.
 */

public class MostVisitedAdapter extends RecyclerView.Adapter<MostVisitedAdapter.ViewHolder> {
    private final Context context;
    private final MostVisited[] mostVisiteds;
    private IItemClickListener itemClickListener;

    public MostVisitedAdapter(Context context, MostVisited [] mostVisiteds, IItemClickListener itemClickListener) {
        this.context = context;
        this.mostVisiteds = mostVisiteds;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder  {
        // each data item is just a string in this case
        public TextView titleView;
        public TextView iconView ;
        public ImageView faviconView;
        public View faviconViewParent;
        public View iconViewParent;
        public View entireView;

        public ViewHolder(View view) {
            super(view);
            entireView = view;
            // set the view's size, margins, paddings and layout parameters
            titleView = (TextView) view.findViewById(com.xlab.vbrowser.R.id.titleView);
            iconView = (TextView) view.findViewById(com.xlab.vbrowser.R.id.iconView);
            faviconView = (ImageView) view.findViewById(com.xlab.vbrowser.R.id.faviconView);
            faviconViewParent = view.findViewById(com.xlab.vbrowser.R.id.faviconViewParent);
            iconViewParent = view.findViewById(com.xlab.vbrowser.R.id.iconViewParent);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MostVisitedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        View v = layoutInflater.inflate(com.xlab.vbrowser.R.layout.most_visited_item, null);
        MostVisitedAdapter.ViewHolder vh = new MostVisitedAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MostVisitedAdapter.ViewHolder holder, int position) {
        final MostVisited mostVisited = this.mostVisiteds[position];
        holder.titleView.setText(mostVisited.url != null ? mostVisited.url : "?");

        String faviconPath = FaviconService.getFavicon(context, mostVisited.url);

        if (faviconPath == null) {
            holder.iconViewParent.setVisibility(View.VISIBLE);
            holder.faviconViewParent.setVisibility(View.GONE);

            if (mostVisited.url == null || mostVisited.url.length() < 1) {
                holder.iconView.setText("?");
            }
            else {
                holder.iconView.setText(mostVisited.url.substring(0, 1).toUpperCase());
            }
        }
        else {
            holder.iconViewParent.setVisibility(View.GONE);
            holder.faviconViewParent.setVisibility(View.VISIBLE);

            holder.faviconView.setImageURI(Uri.fromFile(new File(faviconPath)));
        }

        holder.entireView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
               itemClickListener.onItemClickListener(mostVisited.url);
            }
        });

        holder.entireView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mostVisited.isRemoved = 1;
                MostVisitedMenu.show(context, mostVisited);
                return true;
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mostVisiteds.length;
    }
}
