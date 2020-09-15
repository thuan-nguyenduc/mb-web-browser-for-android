/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */
package com.xlab.vbrowser.menu.context;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.xlab.vbrowser.R;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.web.Download;
import com.xlab.vbrowser.web.IWebView;

/**
 * The context menu shown when long pressing a URL or an image inside the WebView.
 */
public class WebContextMenu {

    private static View createTitleView(final @NonNull Context context, final @NonNull String title, final Settings settings) {
        final TextView titleView = (TextView) LayoutInflater.from(context).inflate(com.xlab.vbrowser.R.layout.context_menu_title, null);
        titleView.setText(title);
        return titleView;
    }

    public static void show(final @NonNull Context context, final @NonNull IWebView.Callback callback,
                            final @NonNull IWebView.HitTarget hitTarget, final boolean isBlockingEnabled) {
        if (!(hitTarget.isLink || hitTarget.isImage)) {
            // We don't support any other classes yet:
            throw new IllegalStateException("WebContextMenu can only handle long-press on images and/or links.");
        }

        Settings settings = Settings.getInstance(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogStyle);

        final View titleView;
        if (hitTarget.isLink) {
           titleView = createTitleView(context, hitTarget.linkURL, settings);
        } else if (hitTarget.isImage) {
            titleView = createTitleView(context, hitTarget.imageURL, settings);
        } else {
            throw new IllegalStateException("Unhandled long press target type");
        }
        builder.setCustomTitle(titleView);

        final View view = LayoutInflater.from(context).inflate(com.xlab.vbrowser.R.layout.context_menu, null);
        builder.setView(view);

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // This even is only sent when the back button is pressed, or when a user
                // taps outside of the dialog:
            }
        });

        final Dialog dialog = builder.create();

        NavigationView menu = view.findViewById(R.id.context_menu_light);

        if (settings.isEnabledNightMode()) {
            menu = view.findViewById(R.id.context_menu_dark);
        }

        menu.setVisibility(View.VISIBLE);

        setupMenuForHitTarget(dialog, menu, callback, hitTarget, isBlockingEnabled);

        dialog.show();
    }

    /**
     * Set up the correct menu contents. Note: this method can only be called once the Dialog
     * has already been created - we need the dialog in order to be able to dismiss it in the
     * menu callbacks.
     */
    private static void setupMenuForHitTarget(final @NonNull Dialog dialog,
                                              final @NonNull NavigationView navigationView,
                                              final @NonNull IWebView.Callback callback,
                                              final @NonNull IWebView.HitTarget hitTarget,
                                              final boolean isBlockingEnabled) {
        navigationView.inflateMenu(com.xlab.vbrowser.R.menu.menu_browser_context);

        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_new_tab).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_new_background_tab).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_link_share).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_link_copy).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_image_open_in_new_tab).setVisible(
                hitTarget.isImage && UrlUtils.isHttpOrHttps(hitTarget.imageURL));
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_image_share).setVisible(hitTarget.isImage);
        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_image_copy).setVisible(hitTarget.isImage);

        navigationView.getMenu().findItem(com.xlab.vbrowser.R.id.menu_image_save).setVisible(
                hitTarget.isImage && UrlUtils.isHttpOrHttps(hitTarget.imageURL));

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                dialog.dismiss();

                switch (item.getItemId()) {
                    case com.xlab.vbrowser.R.id.menu_new_tab: {
                        SessionManager.getInstance().createNextSession(Source.MENU, hitTarget.linkURL, isBlockingEnabled, false);
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_new_background_tab: {
                        SessionManager.getInstance().createNextSession(Source.MENU, hitTarget.linkURL, isBlockingEnabled, true);
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_link_share: {
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, hitTarget.linkURL);
                        dialog.getContext().startActivity(Intent.createChooser(shareIntent, dialog.getContext().getString(com.xlab.vbrowser.R.string.share_dialog_title)));
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_image_open_in_new_tab: {
                        SessionManager.getInstance().createNextSession(Source.MENU, hitTarget.imageURL, isBlockingEnabled, false);
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_image_share: {
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, hitTarget.imageURL);
                        dialog.getContext().startActivity(Intent.createChooser(shareIntent, dialog.getContext().getString(com.xlab.vbrowser.R.string.share_dialog_title)));
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_image_save: {
                        final Download download = new Download(hitTarget.imageURL, null, null, null, -1, Environment.DIRECTORY_PICTURES);
                        callback.onDownloadStart(download);
                        return true;
                    }
                    case com.xlab.vbrowser.R.id.menu_link_copy:
                    case com.xlab.vbrowser.R.id.menu_image_copy:
                        final ClipboardManager clipboard = (ClipboardManager)
                                dialog.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard == null) {
                            return true;
                        }
                        final Uri uri;

                        if (item.getItemId() == com.xlab.vbrowser.R.id.menu_link_copy) {
                            uri = Uri.parse(hitTarget.linkURL);
                        } else if (item.getItemId() == com.xlab.vbrowser.R.id.menu_image_copy) {
                            uri = Uri.parse(hitTarget.imageURL);
                        } else {
                            throw new IllegalStateException("Unknown hitTarget type - cannot copy to clipboard");
                        }

                        final ClipData clip = ClipData.newUri(dialog.getContext().getContentResolver(), "URI", uri);
                        clipboard.setPrimaryClip(clip);
                        return true;
                    default:
                        throw new IllegalArgumentException("Unhandled menu item id=" + item.getItemId());
                }
            }
        });
    }
}
