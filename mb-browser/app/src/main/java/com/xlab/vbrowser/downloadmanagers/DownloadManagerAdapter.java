package com.xlab.vbrowser.downloadmanagers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.menu.context.DownloadManagerContextMenu;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.DownloadUtils;
import com.xlab.vbrowser.utils.FileExtUtils;
import com.xlab.vbrowser.utils.UrlUtils;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.util.FileTypeValue;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadManagerAdapter.ViewHolder> {

    private final List<DownloadData> downloads = new ArrayList<>();
    private final DownloadActionListener actionListener;
    private String lastCreateDate = "";

    public DownloadManagerAdapter(DownloadActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.download_manager_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        try {
            updateView(holder, position);
        }catch(Exception e) {}
    }

    private void updateView(final ViewHolder holder, int position) {
        holder.actionButton.setOnClickListener(null);
        holder.actionButton.setEnabled(true);
        holder.deleteButton.setEnabled(true);

        final DownloadData downloadData = downloads.get(position);

        //Set time header if needed
        if (downloadData.isHeader) {
            holder.headerView.setVisibility(View.VISIBLE);
            holder.dataView.setVisibility(View.GONE);
            holder.headerView.setText(downloadData.createDate);
            return;
        }

        holder.headerView.setVisibility(View.GONE);
        holder.dataView.setVisibility(View.VISIBLE);
        final Uri uri = Uri.parse(downloadData.download.getUrl());
        final Status status = downloadData.download.getStatus();
        final Context context = holder.itemView.getContext();

        String fileName = downloadData.download.getFileName();

        if (TextUtils.isEmpty(fileName)) {
            fileName = uri.getLastPathSegment();
        }

        holder.titleTextView.setText(fileName);

        int progress = downloadData.download.getProgress();
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0;
        }

        holder.progressBar.setProgress(progress);
        String percentProgress = context.getString(R.string.percent_progress, progress);

        if (downloadData.eta != -1) {
            percentProgress += " - "  + DownloadUtils.getETAString(context, downloadData.eta);
        }

        holder.progressTextView.setText(percentProgress);

        if (downloadData.downloadedBytesPerSecond == 0) {
            holder.downloadedBytesPerSecondTextView.setText("");
        } else {
            holder.downloadedBytesPerSecondTextView.setText(DownloadUtils.getDownloadSpeedString(context,
                    downloadData.downloadedBytesPerSecond));
        }

        //Set IconImageView
        FileTypeValue fileTypeValue = downloadData.download.getFileType();
        switch (fileTypeValue) {
            case FILETYPE_ARCHIVE:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_archive));
                break;

            case FILETYPE_DOC:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_document));
                break;

            case FILETYPE_OTHER:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_file));
                break;

            case FILETYPE_IMAGE:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_image));
                break;

            case FILETYPE_VIDEO:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_video));
                break;

            case FILETYPE_MUSIC:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_music));
                break;

            case FILETYPE_PROGRAM:
                holder.iconImageView.setImageDrawable(context.getDrawable(R.drawable.filetype_android));
                break;

            default:
                break;
        }

        final File downloadedFile = new File(downloadData.download.getFile());
        if (!downloadedFile.exists() && status == Status.COMPLETED) {
            holder.progressBar.setVisibility(View.GONE);
            holder.progressTextView.setText(getStatusString(context, Status.REMOVED));
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_dm_retry));
            holder.actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.actionButton.setEnabled(false);
                    actionListener.onRetryDownload(downloadData.download.getId());
                }
            });
            holder.deleteButton.setVisibility(View.VISIBLE);
        }
        else {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);
            switch (status) {
                case COMPLETED: {
                    holder.deleteButton.setVisibility(View.GONE);
                    holder.actionButton.setVisibility(View.GONE);
                    holder.progressBar.setVisibility(View.GONE);
                    String str = DownloadUtils.getDownloadLongString(context,
                            downloadData.download.getDownloaded());
                    str += " - ";
                    str += UrlUtils.getHost(downloadData.download.getRefererUrl());
                    holder.progressTextView.setText(str);
                    break;
                }
                case FAILED: {
                    Log.d("Failed", downloadData.download.getError().toString());
                    holder.progressBar.setVisibility(View.GONE);
                    holder.progressTextView.setText(getStatusString(context, status));
                    holder.actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_dm_retry));
                    holder.actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.actionButton.setEnabled(false);
                            actionListener.onRetryDownload(downloadData.download.getId());
                        }
                    });
                    break;
                }
                case PAUSED: {
                    holder.progressTextView.setText(getStatusString(context, status));
                    holder.actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_dm_resume));
                    holder.actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.actionButton.setEnabled(false);
                            actionListener.onResumeDownload(downloadData.download.getId());
                        }
                    });
                    break;
                }
                case DOWNLOADING: {
                    holder.actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_dm_pause));
                    holder.actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.actionButton.setEnabled(false);
                            actionListener.onPauseDownload(downloadData.download.getId());
                        }
                    });
                    break;
                }

                case QUEUED: {
                    holder.progressTextView.setText(getStatusString(context, status));
                    holder.actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_dm_pause));
                    holder.actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.actionButton.setEnabled(false);
                            actionListener.onPauseDownload(downloadData.download.getId());
                        }
                    });
                    break;
                }
                default: {
                    break;
                }
            }
        }

        //Set delete action
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.deleteButton.setEnabled(false);
                actionListener.onRemoveDownload(downloadData.download.getId());
                GaReport.sendReportEvent(context, "ON_DELETE_DOWNLOAD", DownloadManagerAdapter.class.getName());
            }
        });

        //Set view action
        holder.dataView.setOnClickListener(new View.OnClickListener() {
            @Override
            public  void onClick(View v) {
                if (!downloadedFile.exists() || status != Status.COMPLETED) {
                    return;
                }

                String filePath = downloadData.download.getFile();
                FileExtUtils.openFile(context, filePath);
                GaReport.sendReportEvent(context, "ON_OPEN_DOWNLOADED_FILE", DownloadManagerAdapter.class.getName());
            }
        });

        //Set long press action
        holder.dataView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DownloadManagerContextMenu.show(context, downloadData.download.getFileName(), downloadedFile.exists() && status == Status.COMPLETED, new DownloadManagerContextMenu.IActionMenu() {
                    @Override
                    public void onOpen() {
                        holder.dataView.performClick();
                    }

                    @Override
                    public void onDelete() {
                        holder.deleteButton.performClick();
                    }

                    @Override
                    public void onOpenWebsite() {
                        actionListener.onFinish();
                        SessionManager.getInstance().getOpenUrlEvent().setValue(downloadData.download.getRefererUrl());
                    }

                    @Override
                    public void onShareLink() {
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, downloadData.download.getUrl());
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(com.xlab.vbrowser.R.string.share_dialog_title)));
                    }

                    @Override
                    public void onCopyLink() {
                        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

                        if (clipboard == null) {
                            return;
                        }


                        final Uri uri = UrlUtils.parse(downloadData.download.getUrl());

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

    public void addDownload(final Download download) {
        if (download != null) {
            Date createDate = new Date();
            createDate.setTime(download.getCreated());
            DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
            String strCreateDate = format.format(createDate);

            //Add time header if needed
            if (!strCreateDate.equals(lastCreateDate)) {
                final DownloadData headerData = new DownloadData();
                headerData.createDate = strCreateDate;
                headerData.isHeader = true;
                downloads.add(headerData);
                notifyItemInserted(downloads.size() - 1);
                lastCreateDate = strCreateDate;
            }

            final DownloadData downloadData = new DownloadData();
            downloadData.isHeader = false;
            downloadData.createDate = strCreateDate;
            downloadData.id = download.getId();
            downloadData.download = download;
            downloads.add(downloadData);
            notifyItemInserted(downloads.size() - 1);
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }


    public void update(final Download download, final long eta, final long downloadedBytesPerSecond) {
        final android.os.Handler handler = new android.os.Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (download != null) {
                    for (int position = 0; position < downloads.size(); position++) {
                        final DownloadData downloadData = downloads.get(position);
                        if (downloadData.id == download.getId()) {
                            switch (download.getStatus()) {
                                case REMOVED:
                                case DELETED: {
                                    downloads.remove(position);
                                    notifyItemRemoved(position);
                                    deleteHeaderIfNeeded(downloadData);
                                    actionListener.onRemovedDownload(downloadData.id);
                                    break;
                                }
                                default: {
                                    downloadData.download = download;
                                    downloadData.eta = eta;
                                    downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                                    notifyItemChanged(position);
                                }
                            }
                            return;
                        }
                    }
                }
            }
        };

        handler.post(runnable);
    }

    public void deleteHeaderIfNeeded(final DownloadData downloadData) {
        boolean hasItem = false;
        int posOfHeader = -1;

        for (int position = 0; position < downloads.size(); position++) {
            DownloadData data = downloads.get(position);

            if (data.createDate.equals(downloadData.createDate)) {
                if (!data.isHeader) {
                    hasItem = true;
                }
                else {
                    posOfHeader = position;
                }
            }
        }

        if (!hasItem && posOfHeader >= 0) {
            downloads.remove(posOfHeader);
            notifyItemRemoved(posOfHeader);
        }
    }

    public String getStatusString(Context context, Status status) {
        Resources resources = context.getResources();

        switch (status) {
            case COMPLETED:
                return resources.getString(R.string.download_status_done);
            case DOWNLOADING:
                return resources.getString(R.string.download_status_downloading);
            case FAILED:
                return resources.getString(R.string.download_status_error);
            case PAUSED:
                return resources.getString(R.string.download_status_paused);
            case QUEUED:
                return resources.getString(R.string.download_status_waiting);
            case REMOVED:
                return resources.getString(R.string.download_status_removed);
            case NONE:
                return resources.getString(R.string.download_status_none);
            default:
                return resources.getString(R.string.download_status_unknown);
        }
    }

    public void clearAll() {
        lastCreateDate = "";
        downloads.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView headerView;
        public final View dataView;
        public final TextView titleTextView;
        public final ProgressBar progressBar;
        public final TextView progressTextView;
        public final ImageButton actionButton;
        public final TextView downloadedBytesPerSecondTextView;
        public final ImageButton deleteButton;
        public final ImageView iconImageView;

        ViewHolder(View itemView) {
            super(itemView);
            headerView = (TextView)itemView.findViewById(R.id.headerView);
            dataView = itemView.findViewById(R.id.dataView);
            iconImageView = (ImageView) itemView.findViewById(R.id.iconImageView);
            titleTextView = (TextView) itemView.findViewById(R.id.titleTextView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            actionButton = (ImageButton) itemView.findViewById(R.id.actionButton);
            progressTextView = (TextView) itemView.findViewById(R.id.progressTextView);
            downloadedBytesPerSecondTextView = (TextView) itemView.findViewById(R.id.downloadSpeedTextView);
            deleteButton = (ImageButton) itemView.findViewById(R.id.deleteButton);
        }

    }

    public static class DownloadData {
        public boolean isHeader;
        public String createDate;
        public int id;
        public Download download;
        public long eta = -1;
        public long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof DownloadData) {
                return ((DownloadData) obj).id == id;
            }
            return false;
        }
    }

}
