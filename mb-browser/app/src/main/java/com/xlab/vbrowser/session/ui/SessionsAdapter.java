/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session.ui;

import android.arch.lifecycle.Observer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.xlab.vbrowser.session.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter implementation to show a list of active browsing sessions and an "erase" button at the end.
 */
public class SessionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Observer<List<Session>> {
    private final SessionsSheetFragment fragment;
    private List<Session> sessions;

    /* package */ SessionsAdapter(SessionsSheetFragment fragment) {
        this.fragment = fragment;
        this.sessions = Collections.emptyList();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case SessionViewHolder.LAYOUT_ID:
                return new SessionViewHolder(
                        fragment,
                        (RelativeLayout) inflater.inflate(SessionViewHolder.LAYOUT_ID, parent, false));
            default:
                throw new IllegalStateException("Unknown viewType");
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case SessionViewHolder.LAYOUT_ID:
                ((SessionViewHolder) holder).bind(sessions.get(position), position);
                break;
            default:
                throw new IllegalStateException("Unknown viewType");
        }
    }

    @Override
    public int getItemViewType(int position) {
        return SessionViewHolder.LAYOUT_ID;
    }


    @Override
    public int getItemCount() {
        return sessions.size();
    }

    @Override
    public void onChanged(List<Session> sessions) {
        this.sessions = new ArrayList<>(sessions);
        notifyDataSetChanged();
    }
}
