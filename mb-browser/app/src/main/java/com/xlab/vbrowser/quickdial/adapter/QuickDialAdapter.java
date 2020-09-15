package com.xlab.vbrowser.quickdial.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.events.IQuickDialgItemClickListener;
import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.quickdial.entity.QuickDialItem;
import com.xlab.vbrowser.quickdial.service.QuickDialService;
import com.xlab.vbrowser.widget.touchhelper.ItemTouchHelperAdapter;
import com.xlab.vbrowser.widget.touchhelper.OnStartDragListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nguyenducthuan on 4/05/18.
 */

public class QuickDialAdapter extends RecyclerView.Adapter<QuickDialAdapter.ViewHolder> implements ItemTouchHelperAdapter {
    private final Context context;
    private ArrayList<QuickDialItem> quickDialItems;
    private IQuickDialgItemClickListener quickDialgItemClickListener;

    private boolean multiSelect = false;
    private ArrayList<QuickDialItem> selectedItems = new ArrayList<QuickDialItem>();
    private final OnStartDragListener dragStartListener;
    private boolean dataSetChanged = false;
    private ActionMode actionMode;

    @Override
    public void onItemDismiss(int position) {
        if (quickDialItems == null) {
            return;
        }

        quickDialItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (quickDialItems == null || fromPosition < 0 || toPosition < 0 ||
                fromPosition > quickDialItems.size() - 1 || toPosition > quickDialItems.size() - 1) {
            return true;
        }

        notifyItemMoved(fromPosition, toPosition);

        dataSetChanged = true;

        return true;
    }

    @Override
    public void onReallyMove(int fromPosition, int toPosition) {
        try {
            if (quickDialItems == null || fromPosition < 0 || toPosition < 0 ||
                    fromPosition > quickDialItems.size() - 1 || toPosition > quickDialItems.size() - 1) {
                return;
            }

            Log.d("onReallyMove", fromPosition + " " + toPosition);

            QuickDialItem fromItem = quickDialItems.get(fromPosition);

            quickDialItems.remove(fromPosition);
            quickDialItems.add(toPosition, fromItem);

            saveNewPos();
        }
        catch(Exception e) {}
    }

    private enum QuickDialViewType {
        ItemData,
        ItemPlus,
    }

    private ActionMode.Callback actionModeCallbacks = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            multiSelect = true;
            mode.getMenuInflater().inflate(R.menu.menu_quick_access, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete) {
                if (quickDialItems == null || selectedItems == null || selectedItems.size() < 1) {
                    return true;
                }

                for (QuickDialItem intItem : selectedItems) {
                    quickDialItems.remove(intItem);
                }

                QuickDialItem [] items = new QuickDialItem[selectedItems.size()];
                selectedItems.toArray(items);
                QuickDialService.remove(context, items);
                multiSelect = false;
                selectedItems.clear();
                mode.finish();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            multiSelect = false;
            selectedItems.clear();
            notifyDataSetChanged();
        }
    };

    public QuickDialAdapter(Context context, List<QuickDialItem> quickDialItems, IQuickDialgItemClickListener quickDialgItemClickListener,
                            OnStartDragListener dragStartListener) {
        this.dragStartListener = dragStartListener;
        this.context = context;
        this.quickDialItems = new ArrayList<>(quickDialItems);
        this.quickDialgItemClickListener = quickDialgItemClickListener;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class ViewHolderData extends ViewHolder{
        // each data item is just a string in this case
        public TextView titleView;
        public TextView iconView ;
        public ImageView faviconView;
        public View faviconViewParent;
        public View iconViewParent;
        public CheckBox cboSelect;

        public ViewHolderData(View view) {
            super(view);
            // set the view's size, margins, paddings and layout parameters
            titleView = (TextView) view.findViewById(com.xlab.vbrowser.R.id.titleView);
            iconView = (TextView) view.findViewById(com.xlab.vbrowser.R.id.iconView);
            faviconView = (ImageView) view.findViewById(com.xlab.vbrowser.R.id.faviconView);
            faviconViewParent = view.findViewById(com.xlab.vbrowser.R.id.faviconViewParent);
            iconViewParent = view.findViewById(com.xlab.vbrowser.R.id.iconViewParent);
            cboSelect = (CheckBox) view.findViewById(R.id.cboSelect);
        }
    }

    public static class ViewHolderPlus extends ViewHolder  {
        // each data item is just a string in this case
        public ViewHolderPlus(View view) {
            super(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (this.quickDialItems == null || position == this.quickDialItems.size()) {
            return QuickDialViewType.ItemPlus.ordinal();
        }
        else {
            return QuickDialViewType.ItemData.ordinal();
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public QuickDialAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        if (viewType == QuickDialViewType.ItemData.ordinal()) {
            // create a new view
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            View v = layoutInflater.inflate(R.layout.quick_dial_item, null);
            QuickDialAdapter.ViewHolderData vh = new QuickDialAdapter.ViewHolderData(v);
            return vh;
        }
        else if (viewType == QuickDialViewType.ItemPlus.ordinal()) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            View v = layoutInflater.inflate(R.layout.quick_dial_plus, null);
            QuickDialAdapter.ViewHolderPlus vh = new QuickDialAdapter.ViewHolderPlus(v);
            return vh;
        }

        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(QuickDialAdapter.ViewHolder holder, final int position) {
        if (holder == null || this.quickDialItems == null) {
            return;
        }

        if (holder instanceof ViewHolderData) {
            final ViewHolderData viewHolderData = (ViewHolderData)holder;

            final QuickDialItem quickDialItem = this.quickDialItems.get(position);
            viewHolderData.titleView.setText(quickDialItem.title != null ? quickDialItem.title : "?");

            String faviconPath = FaviconService.getFavicon(context, quickDialItem.url);

            if (faviconPath == null) {
                viewHolderData.iconViewParent.setVisibility(View.VISIBLE);
                viewHolderData.faviconViewParent.setVisibility(View.GONE);

                if (quickDialItem.url == null || quickDialItem.url.length() < 1) {
                    viewHolderData.iconView.setText("?");
                } else {
                    viewHolderData.iconView.setText(quickDialItem.url.substring(0, 1).toUpperCase());
                }
            } else {
                viewHolderData.iconViewParent.setVisibility(View.GONE);
                viewHolderData.faviconViewParent.setVisibility(View.VISIBLE);
                viewHolderData.faviconView.setImageURI(Uri.fromFile(new File(faviconPath)));
            }

            viewHolderData.cboSelect.setVisibility(multiSelect ? View.VISIBLE : View.GONE);

            viewHolderData.cboSelect.setChecked(selectedItems.contains(quickDialItem));

            viewHolderData.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (multiSelect) {
                        selectItem(quickDialItem);
                        viewHolderData.cboSelect.setChecked(!viewHolderData.cboSelect.isChecked());
                    }
                    else {
                        quickDialgItemClickListener.onItemClickListener(quickDialItem.url);
                    }
                }
            });

            viewHolderData.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!multiSelect) {
                        actionMode = ((AppCompatActivity) view.getContext()).startSupportActionMode(actionModeCallbacks);
                        selectItem(quickDialItem);
                        notifyDataSetChanged();
                    }

                    dragStartListener.onStartDrag(viewHolderData);


                    return true;
                }
            });

            viewHolderData.cboSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectItem(quickDialItem);
                }
            });
        }
        else if (holder instanceof ViewHolderPlus) {
            ((ViewHolderPlus) holder).itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    quickDialgItemClickListener.onAddItemClickListener();
                }
            });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return quickDialItems == null ? 0 : quickDialItems.size() + 1;
    }

    public void closeActionMode() {
        selectedItems.clear();
        multiSelect = false;
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void addItem(final QuickDialItem quickDialItem) {
        if (this.quickDialItems == null) {
            return;
        }

        dataSetChanged = true;
        quickDialItems.add(quickDialItem);
        notifyItemInserted(quickDialItems.size() - 1);
    }

    private void selectItem(QuickDialItem item) {
        if (multiSelect) {
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
        }
    }

    private void saveNewPos() {
        if (quickDialItems == null || quickDialItems.size() < 1 || !dataSetChanged) {
            return;
        }

        QuickDialItem [] items = new QuickDialItem[quickDialItems.size()];
        quickDialItems.toArray(items);
        for (int pos = 0; pos < items.length; pos ++) {
            items[pos].sortOrder = (float)pos;
            Log.d("onDestroy", items[pos].title + "  " + items[pos].sortOrder);
        }
        QuickDialService.update(context, items);
    }
}
