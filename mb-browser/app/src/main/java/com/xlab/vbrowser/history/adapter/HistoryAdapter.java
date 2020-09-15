package com.xlab.vbrowser.history.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.favicon.FaviconService;
import com.xlab.vbrowser.history.HistoryActionListener;
import com.xlab.vbrowser.history.entity.History;
import com.xlab.vbrowser.menu.context.HistoryContextMenu;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.UrlUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryData> mHistories = new ArrayList<>();
    private String mLastAccessTime = "";
    private HistoryActionListener mHistoryActionListener;


    public HistoryAdapter(HistoryActionListener historyActionListener) {
        this.mHistoryActionListener = historyActionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        try {
            updateView(holder, position);
        }
        catch (Exception e){}
    }

    private void updateView(final ViewHolder holder, final  int position) {
        final Context context = holder.itemView.getContext();

        final HistoryData historyData = mHistories.get(position);

        //Set time header if needed
        if (historyData.isHeader) {
            holder.headerView.setVisibility(View.VISIBLE);
            holder.dataView.setVisibility(View.GONE);
            holder.headerView.setText(historyData.accessTime);
            return;
        }

        holder.headerView.setVisibility(View.GONE);
        holder.dataView.setVisibility(View.VISIBLE);

        holder.titleTextView.setText(TextUtils.isEmpty(historyData.history.title) ? "Default Title" : historyData.history.title);
        holder.urlTextView.setText(historyData.history.url);

        String faviconPath = FaviconService.getFavicon(context, historyData.history.url);

        if (faviconPath == null) {
            holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_default_image));
        }
        else {
            holder.iconImageView.setImageURI(Uri.fromFile(new File(faviconPath)));
        }


        //Set view action
        holder.dataView.setOnClickListener(new View.OnClickListener() {
            @Override
            public  void onClick(View v) {
                mHistoryActionListener.onOpenHistory(historyData.history.url);
                GaReport.sendReportEvent(context, "ON_OPEN_HISTORY", HistoryAdapter.class.getName());
            }
        });

        //Set long press action
        holder.dataView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HistoryContextMenu.show(context, historyData.history.url,  new HistoryContextMenu.IActionMenu() {
                    @Override
                    public void onOpen() {
                        holder.dataView.performClick();
                    }

                    @Override
                    public void onOpenInNewTab() {
                        mHistoryActionListener.onOpenHistoryInNewTab(historyData.history.url);
                        GaReport.sendReportEvent(context, "ON_OPEN_HISTORY_IN_NEW_TAB", HistoryAdapter.class.getName());
                    }

                    @Override
                    public void onDelete() {
                        new android.os.Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                for (int index = 0; index < mHistories.size(); index++) {
                                    final HistoryData data = mHistories.get(index);

                                    if (data.id == historyData.id) {
                                        mHistoryActionListener.onRemoveHistory(data.history);
                                        mHistories.remove(index);
                                        notifyItemRemoved(index);
                                        deleteHeaderIfNeeded(data);
                                        GaReport.sendReportEvent(context,"ON_DELETE_HISTORY", HistoryAdapter.class.getName());
                                    }
                                }
                            }
                        });
                    }


                    @Override
                    public void onShareLink() {
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, historyData.history.url);
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(com.xlab.vbrowser.R.string.share_dialog_title)));
                    }

                    @Override
                    public void onCopyLink() {
                        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

                        if (clipboard == null) {
                            return;
                        }


                        final Uri uri = UrlUtils.parse(historyData.history.url);

                        if (uri == null) {
                            return;
                        }

                        final ClipData clip = ClipData.newUri(context.getContentResolver(), "URI", uri);
                        clipboard.setPrimaryClip(clip);
                    }
                });
                return true;
            }
        });
    }

    public void addHistory(final History history) {
        if (history == null) {
            return;
        }

        Date accessTime = new Date();
        accessTime.setTime(history.accessTime);
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        String strAccessTime = format.format(accessTime);

        //Add time header if needed
        if (!strAccessTime.equals(mLastAccessTime)) {
            final HistoryData headerData = new HistoryData();
            headerData.accessTime = strAccessTime;
            headerData.isHeader = true;
            mHistories.add(headerData);
            notifyItemInserted(mHistories.size() - 1);
            mLastAccessTime = strAccessTime;
        }

        final HistoryData historyData = new HistoryData();
        historyData.isHeader = false;
        historyData.accessTime = strAccessTime;
        historyData.id = history.id;
        historyData.history = history;
        mHistories.add(historyData);
        notifyItemInserted(mHistories.size() - 1);
    }

    @Override
    public int getItemCount() {
        return mHistories.size();
    }

    public void deleteHeaderIfNeeded(final HistoryData historyData) {
        boolean hasItem = false;
        int posOfHeader = -1;

        for (int position = 0; position < mHistories.size(); position++) {
            HistoryData data = mHistories.get(position);

            if (data.accessTime.equals(historyData.accessTime)) {
                if (!data.isHeader) {
                    hasItem = true;
                }
                else {
                    posOfHeader = position;
                }
            }
        }

        if (!hasItem && posOfHeader >= 0) {
            mHistories.remove(posOfHeader);
            notifyItemRemoved(posOfHeader);
        }
    }

    public void clearAll() {
        mLastAccessTime = "";
        mHistories.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView headerView;
        public final View dataView;
        public final TextView titleTextView;
        public final TextView urlTextView;
        public final ImageView iconImageView;

        ViewHolder(View itemView) {
            super(itemView);
            headerView = (TextView)itemView.findViewById(R.id.headerView);
            dataView = itemView.findViewById(R.id.dataView);
            iconImageView = (ImageView) itemView.findViewById(R.id.iconImageView);
            titleTextView = (TextView) itemView.findViewById(R.id.titleTextView);
            urlTextView = (TextView) itemView.findViewById(R.id.urlTextView);
        }

    }

    public static class HistoryData {
        public boolean isHeader;
        public String accessTime;
        public int id;
        public History history;
    }

}
