package com.xlab.vbrowser.search.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.events.IItemClickListener;
import com.xlab.vbrowser.search.data.SearchSuggestionItem;

/**
 * Created by nguyenducthuan on 3/7/18.
 */

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {
    private final Context context;
    private final SearchSuggestionItem[] searchSuggestionItems;
    private IItemClickListener itemClickListener;

    public SearchSuggestionAdapter(Context context, SearchSuggestionItem[] searchSuggestionItems, IItemClickListener itemClickListener) {
        this.context = context;
        this.searchSuggestionItems = searchSuggestionItems;
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
        public ImageButton selectSearchView;
        public View searchItemView ;
        public TextView text1View;
        public TextView text2View;

        public ViewHolder(View view) {
            super(view);
            searchItemView = view;
            selectSearchView = (ImageButton) view.findViewById(com.xlab.vbrowser.R.id.selectSearchView);
            text1View = (TextView) view.findViewById(com.xlab.vbrowser.R.id.text1View);
            text2View = (TextView) view.findViewById(com.xlab.vbrowser.R.id.text2View);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SearchSuggestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        View v = layoutInflater.inflate(R.layout.search_suggestion_item, parent, false);
        SearchSuggestionAdapter.ViewHolder vh = new SearchSuggestionAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SearchSuggestionAdapter.ViewHolder holder, int position) {
        final SearchSuggestionItem searchSuggestionItem = this.searchSuggestionItems[position];

        if (searchSuggestionItem.isUrl) {
            holder.text2View.setText(searchSuggestionItem.searchTerm);
            holder.text1View.setText(searchSuggestionItem.urlTitle);
            holder.text2View.setVisibility(View.VISIBLE);
            holder.text1View.setVisibility(View.VISIBLE);
        }
        else {
            holder.text2View.setVisibility(View.GONE);
            holder.text1View.setVisibility(View.VISIBLE);
            holder.text1View.setText(searchSuggestionItem.searchTerm);
        }

        holder.searchItemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClickListener(searchSuggestionItem.searchTerm);
            }
        });

        holder.selectSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClickListener(searchSuggestionItem.searchTerm, true);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return searchSuggestionItems.length;
    }
}
