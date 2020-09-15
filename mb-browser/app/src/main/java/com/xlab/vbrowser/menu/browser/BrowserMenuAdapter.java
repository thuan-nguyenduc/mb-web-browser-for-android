/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */

package com.xlab.vbrowser.menu.browser;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.xlab.vbrowser.customtabs.CustomTabConfig;
import com.xlab.vbrowser.R;
import com.xlab.vbrowser.fragment.BrowserFragment;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.Settings;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BrowserMenuAdapter extends RecyclerView.Adapter<BrowserMenuViewHolder> {
    static interface IMenuItemAction {
        void onPerformAction();
    }

    static class MenuItem {
        public final int id;
        public final String label;
        public final @Nullable PendingIntent pendingIntent;
        public IMenuItemAction menuAction;
        public boolean isEnabled = true;
        public Drawable icon;

        public MenuItem(Drawable icon, int id, String label) {
            this.id = id;
            this.label = label;
            this.menuAction = null;
            this.pendingIntent = null;
            this.icon = icon;
        }

        public MenuItem(Drawable icon, int id, String label, IMenuItemAction menuAction) {
            this.id = id;
            this.label = label;
            this.menuAction = menuAction;
            this.pendingIntent = null;
            this.icon = icon;
        }

        public MenuItem(Drawable icon, int id, String label, @Nullable PendingIntent pendingIntent) {
            this.id = id;
            this.label = label;
            this.menuAction = null;
            this.pendingIntent = pendingIntent;
            this.icon = icon;
        }

        public void setEnabled(boolean enabled) {
            this.isEnabled = enabled;
        }
    }

    private final Context context;
    private final BrowserMenu menu;
    private final BrowserFragment fragment;

    private List<MenuItem> items;
    private WeakReference<NavigationItemViewHolder> navigationItemViewHolderReference;
    private WeakReference<BlockingItemViewHolder> blockingItemViewHolderReference;
    private final SharedPreferences prefs;

    public BrowserMenuAdapter(Context context, BrowserMenu menu, BrowserFragment fragment,
                              final @Nullable CustomTabConfig customTabConfig) {
        this.context = context;
        this.menu = menu;
        this.fragment = fragment;

        prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

        initializeMenu(fragment.getUrl(), customTabConfig);
    }

    private void initializeMenu(String url, final @Nullable CustomTabConfig customTabConfig) {
        final Resources resources = context.getResources();
        //final Browsers browsers = new Browsers(context, url);
        Settings settings = Settings.getInstance(context);

        this.items = new ArrayList<>();

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_download_popup_menu)
                ,R.id.download_manager, resources.getString(R.string.download_manager)));

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_history),
                R.id.history, resources.getString(R.string.history)));

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_bookmark_menu_popup),
                R.id.bookmarkActivity, resources.getString(R.string.bookmark)));

        Session currentSession = SessionManager.getInstance().getCurrentSession();
        boolean wasNotAddedToQuickAccess = currentSession != null && !currentSession.wasAddedToQuickAccess();

        if(wasNotAddedToQuickAccess) {
            items.add(new MenuItem(context.getDrawable(R.drawable.ic_add_to_quick_access)
                    , R.id.addToQuickAccess, resources.getString(R.string.add_to_quick_access)));
        }

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_home_menu_popup),
                R.id.add_to_homescreen, resources.getString(R.string.menu_add_to_home_screen)));

        boolean enableSpeedmode = settings.shouldEnterSpeedMode();
        Drawable speedModeDrawbale = enableSpeedmode ? context.getDrawable(R.drawable.ic_speedmode_off)
                                        :  context.getDrawable(R.drawable.ic_speedmode_on);
        items.add(new MenuItem(speedModeDrawbale, R.id.speedMode, enableSpeedmode ? resources.getString(R.string.exit_speed_mode) :
                resources.getString(R.string.speed_mode) ));

        //Night mode
        IMenuItemAction actionNightMode = new IMenuItemAction() {
            @Override
            public void onPerformAction() {
                DialogUtils.showNightmodeConfigDialog(context, true);
            }
        };
        boolean enableNightMode = settings.isEnabledNightMode();
        Drawable nightModeDrawable = enableNightMode ? context.getDrawable(R.drawable.ic_nightmode_off)
                :  context.getDrawable(R.drawable.ic_nightmode_on);
        items.add(new MenuItem(nightModeDrawable, R.id.nightMode, enableNightMode ? resources.getString(R.string.exit_night_mode)
                    : resources.getString(R.string.night_mode) , enableNightMode ? actionNightMode : null));


        if (customTabConfig == null || customTabConfig.showShareMenuItem) {
            items.add(new MenuItem(context.getDrawable(R.drawable.ic_share), R.id.share, resources.getString(R.string.menu_share)));
        }

        //Request Desktop Site
        boolean isRequestingDesktopSite = settings.shouldRequestDesktopSite();
        Drawable requestDesktopDrawable = isRequestingDesktopSite ? context.getDrawable(R.drawable.ic_mobile)
                :  context.getDrawable(R.drawable.ic_desktop);
        items.add(new MenuItem(requestDesktopDrawable, R.id.requestDesktopSite, isRequestingDesktopSite ? resources.getString(R.string.request_mobile_site)
                : resources.getString(R.string.request_desktop_site)));

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_settings),
                R.id.settings, resources.getString(R.string.menu_settings)));

        items.add(new MenuItem(context.getDrawable(R.drawable.ic_rate),
                R.id.rateApp, resources.getString(R.string.menu_rate_app)));
    }

    public void updateTrackers(int trackers) {
        if (blockingItemViewHolderReference == null) {
            return;
        }

        final BlockingItemViewHolder navigationItemViewHolder = blockingItemViewHolderReference.get();
        if (navigationItemViewHolder != null) {
            navigationItemViewHolder.updateTrackers(trackers);
        }
    }

    public void updateLoading(boolean loading) {
        if (navigationItemViewHolderReference == null) {
            return;
        }

        final NavigationItemViewHolder navigationItemViewHolder = navigationItemViewHolderReference.get();
        if (navigationItemViewHolder != null) {
            navigationItemViewHolder.updateLoading(loading);
        }
    }

    @Override
    public BrowserMenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == NavigationItemViewHolder.LAYOUT_ID) {
            NavigationItemViewHolder navigationItemViewHolder = new NavigationItemViewHolder(
                    inflater.inflate(R.layout.menu_navigation, parent, false), fragment);
            navigationItemViewHolderReference = new WeakReference<>(navigationItemViewHolder);
            return navigationItemViewHolder;
        } else if (viewType == MenuItemViewHolder.LAYOUT_ID) {
            return new MenuItemViewHolder(inflater.inflate(R.layout.menu_item, parent, false));
        } else if (viewType == BlockingItemViewHolder.LAYOUT_ID) {
            final BlockingItemViewHolder blockingItemViewHolder = new BlockingItemViewHolder(
                    inflater.inflate(R.layout.menu_blocking_switch, parent, false), fragment);
            blockingItemViewHolderReference = new WeakReference<>(blockingItemViewHolder);
            return blockingItemViewHolder;
        } else if (viewType == CustomTabMenuItemViewHolder.LAYOUT_ID) {
            return new CustomTabMenuItemViewHolder(inflater.inflate(R.layout.custom_tab_menu_item, parent, false));
        }

        throw new IllegalArgumentException("Unknown view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(BrowserMenuViewHolder holder, int position) {
        holder.setMenu(menu);
        holder.setOnClickListener(fragment);

        int actualPosition = translateToMenuPosition(position);

        if (actualPosition >= 0 && position != getBlockingSwitchPosition()) {
            ((MenuItemViewHolder) holder).bind(items.get(actualPosition));
        }
    }

    private int translateToMenuPosition(int position) {
        return shouldShowButtonToolbar() ? position - 2 : position - 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && shouldShowButtonToolbar()) {
            return NavigationItemViewHolder.LAYOUT_ID;
        } else if (position == getBlockingSwitchPosition()) {
            return BlockingItemViewHolder.LAYOUT_ID;
        } else {
            final int actualPosition = translateToMenuPosition(position);
            final MenuItem menuItem = items.get(actualPosition);

            if (menuItem.id == R.id.custom_tab_menu_item) {
                return CustomTabMenuItemViewHolder.LAYOUT_ID;
            } else {
                return MenuItemViewHolder.LAYOUT_ID;
            }
        }
    }

    private int getBlockingSwitchPosition() {
        return shouldShowButtonToolbar() ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        int itemCount = items.size();

        if (shouldShowButtonToolbar()) {
            itemCount++;
        }

        // For the blocking switch
        itemCount++;

        return itemCount;
    }

    private boolean shouldShowButtonToolbar() {
        // On phones we show an extra row with toolbar items (forward/refresh)
        //return !HardwareUtils.isTablet(context);
        return false;
    }
}
