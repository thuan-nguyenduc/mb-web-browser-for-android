/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.UpApplication;
import com.xlab.vbrowser.activity.MainActivity;
import com.xlab.vbrowser.downloadmanagers.DownloadManagerActionService;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.DownloadUtils;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.web.WebViewProvider;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

/**
 * As long as a session is active this service will keep the notification (and our process) alive.
 */
public class SessionNotificationService extends Service {
    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;
    private static final int NOTIFICATION_ID = 83;

    private static final String NOTIFICATION_CHANNEL_ID = "browsing-session";

    private static final String ACTION_START = "start";
    private static final String ACTION_ERASE = "erase";
    private Fetch fetch;
    private static boolean isStarted;

    public synchronized static void start(Context context) {
        if (isStarted) {
            return;
        }

        isStarted = true;
        final Intent intent = new Intent(context, SessionNotificationService.class);
        intent.setAction(ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static void stop(Context context) {
        if (!isStarted) {
            return;
        }

        isStarted = false;
        final Intent intent = new Intent(context, SessionNotificationService.class);
        context.stopService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isStarted = true;
        //Get fetch
        fetch = ((UpApplication) getApplication()).getFetch();

        if (fetch == null) {
            return START_NOT_STICKY;
        }

        fetch.addListener(fetchListener);
        final String action = intent.getAction();

        if (action == null) {
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_START:
                createNotificationChannelIfNeeded();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(NOTIFICATION_ID, buildNotification());
                }


                break;

            /*case ACTION_ERASE:
                SessionManager.getInstance().removeAllSessions();
                WebViewProvider.performNewBrowserSessionCleanup(getApplicationContext());

                //VisibilityLifeCycleCallback.finishAndRemoveTaskIfInBackground(this);
                break;*/

            default:
                throw new IllegalStateException("Unknown intent: " + intent);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        isStarted = false;

        //Signout of all websites if needed
        new BackgroundTask(new IBackgroundTask() {
            @Override
            public void run() {
                WebViewProvider.signoutOfWebsitesIfNeeded(getBaseContext());
            }

            @Override
            public void onComplete() {

            }
        }).execute();

        stopForeground(true);
        stopSelf();
        cancelAllDownloadManagerNotification();

        try {
            if (!fetch.isClosed()) {
                fetch.removeListener(fetchListener);
            }
        }catch (Exception e) {}

        //Close Fetch
        ((UpApplication) getApplication()).closeFetch();
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(com.xlab.vbrowser.R.string.up_teaser))
                .setContentIntent(createOpenActionIntent())
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setColor(ContextCompat.getColor(this, com.xlab.vbrowser.R.color.colorErase))
                .addAction(new NotificationCompat.Action(
                        R.mipmap.ic_launcher,
                        getString(com.xlab.vbrowser.R.string.notification_action_open),
                        createOpenActionIntent()))
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_signout,
                        getString(R.string.tabs_tray_action_signout),
                        createOpenAndEraseActionIntent()))
                .build();
    }

    private PendingIntent createOpenActionIntent() {
        final Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_OPEN);

        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createOpenAndEraseActionIntent() {
        final Intent intent = new Intent(this, MainActivity.class);

        intent.setAction(MainActivity.ACTION_SIGNOUT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Notification channels are only available on Android O or higher.
            return;
        }

        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        final String notificationChannelName = getString(com.xlab.vbrowser.R.string.notification_browsing_session_channel_name);
        final String notificationChannelDescription = getString(
                com.xlab.vbrowser.R.string.notification_browsing_session_channel_description,
                getString(com.xlab.vbrowser.R.string.app_name));

        final NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, notificationChannelName, NotificationManager.IMPORTANCE_MIN);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(notificationChannelDescription);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(true);

        notificationManager.createNotificationChannel(channel);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void cancelAllDownloadManagerNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
    }

    private PendingIntent createDownloadActionIntent(Download download) {
        //fetch.pause(download.getId());
        Intent action = new Intent(getBaseContext(), DownloadManagerActionService.class);
        action.setData(Uri.parse(download.getId()+""));

        final Status status = download.getStatus();

        switch (status) {
            case DOWNLOADING:
            case QUEUED:
                action.setAction(DownloadManagerActionService.PAUSE_ACTION);
                break;

            case FAILED:
                action.setAction(DownloadManagerActionService.RETRY_ACTION);
                break;

            case PAUSED:
                action.setAction(DownloadManagerActionService.RESUME_ACTION);

                default:
                    break;
        }

        return PendingIntent.getService(getBaseContext(), 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createDeleteActionIntent(Download download) {
        //fetch.pause(download.getId());
        Intent action = new Intent(getBaseContext(), DownloadManagerActionService.class);
        action.setData(Uri.parse(download.getId()+""));
        action.setAction(DownloadManagerActionService.DELETE_ACTION);

        return PendingIntent.getService(getBaseContext(), 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createDownloadManagerIntent(Download download) {
        if (download.getStatus() == Status.COMPLETED) {
            Intent action = new Intent(this, MainActivity.class);
            action.setData(Uri.parse(download.getFile()));
            action.setAction(MainActivity.ACTION_FILE_OPEN);

            return PendingIntent.getActivity(this, 3, action, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else {
            final Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(MainActivity.ACTION_OPEN_DOWNLOAD_MANAGER);

            return PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private void createDownloadNotification(Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        String eta = "";
        final int PROGRESS_MAX = 100;

        if (etaInMilliseconds!= -1) {
            eta = DownloadUtils.getETAString(getBaseContext(), etaInMilliseconds);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        mBuilder.setContentTitle(download.getFileName())
                .setContentText(eta)
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setContentIntent(createDownloadManagerIntent(download));

        final Status status = download.getStatus();
        mBuilder.setOngoing(status == Status.DOWNLOADING || status == Status.QUEUED);

        if (status == Status.COMPLETED) {
            mBuilder.setSmallIcon(R.drawable.ic_done);
            mBuilder.setContentText(DownloadUtils.getDownloadLongString(getBaseContext(), download.getDownloaded())
                    + " - " +getString(R.string.download_status_done))
                    .setProgress(0, 0, false);
            mBuilder.setAutoCancel(true);
            notificationManager.notify(download.getId(), mBuilder.build());
        }
        else {
            String actionString = "";
            int actionIcon = R.drawable.ic_dm_pause;

            switch (status) {
                case DOWNLOADING:
                    mBuilder.setProgress(PROGRESS_MAX, download.getProgress(), false);
                    actionString = getString(R.string.pause);
                    actionIcon = R.drawable.ic_dm_pause;
                    break;

                case QUEUED:
                    mBuilder.setContentText(getString(R.string.download_status_waiting))
                            .setProgress(0, 0, false);
                    actionString = getString(R.string.pause);
                    actionIcon = R.drawable.ic_dm_pause;
                    break;

                case FAILED:
                    mBuilder.setContentText(getString(R.string.download_status_error))
                            .setProgress(0, 0, false);
                    actionString = getString(R.string.retry);
                    actionIcon = R.drawable.ic_dm_retry;
                    break;

                case PAUSED:
                    mBuilder.setContentText(getString(R.string.download_status_paused))
                            .setProgress(0, 0, false);
                    actionString = getString(R.string.resume);
                    actionIcon = R.drawable.ic_dm_resume;

                default:
                    break;
            }

            mBuilder.addAction(new NotificationCompat.Action(
                    actionIcon,
                    actionString,
                    createDownloadActionIntent(download)));

            mBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_delete,
                    getString(R.string.delete),
                    createDeleteActionIntent(download)));

            notificationManager.notify(download.getId(), mBuilder.build());
        }
    }

    private void removeDownloadNotification(Download download) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(download.getId());
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onQueued(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND );
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND );
        }

        @Override
        public void onError(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            createDownloadNotification(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            createDownloadNotification(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            removeDownloadNotification(download);
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            removeDownloadNotification(download);
        }
    };
}
