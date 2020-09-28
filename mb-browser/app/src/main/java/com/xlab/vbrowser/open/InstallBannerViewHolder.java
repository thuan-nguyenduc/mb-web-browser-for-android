/*Copyright by MonnyLab*/

package com.xlab.vbrowser.open;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.xlab.vbrowser.R;

public class InstallBannerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public static final int LAYOUT_ID = R.layout.item_install_banner;

    private final ImageView iconView;

    public InstallBannerViewHolder(View itemView) {
        super(itemView);

        iconView = itemView.findViewById(R.id.icon);

        itemView.setOnClickListener(this);
    }

    public void bind(AppAdapter.App store) {
        iconView.setImageDrawable(store.loadIcon());
    }

    @Override
    public void onClick(View view) {
    }
}
