package com.cala.scanner.classes.layout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.cala.scanner.R;
import com.cala.scanner.ViewActivity;
import com.cala.scanner.classes.ClipboardManager;
import com.cala.scanner.classes.Info;
import com.cala.scanner.classes.URIChecker;

import java.util.List;

public class InfoAdapter extends RecyclerView.Adapter {
    private List<Info> items;
    private CustomTabsIntent customTabsIntent;

    public InfoAdapter(List<Info> items, CustomTabsIntent customTabsIntent) {
        this.items = items;
        this.customTabsIntent = customTabsIntent;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new InfoViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        InfoViewHolder viewHolder = (InfoViewHolder) holder;
        final Info item = items.get(position);

        viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        viewHolder.titleTextView.setVisibility(View.VISIBLE);
        viewHolder.titleTextView.setText(item.getText());
        viewHolder.dateTextView.setText(item.date.toString());

        viewHolder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView(item, v.getContext());
            }
        });

        viewHolder.openLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOpenLink(item, v);
            }
        });

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView(item, v.getContext());
            }
        });

        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager.copyToClipboard(item.getText(), v.getContext());
                //return false = after long click a normal click is performed
                return true;
            }
        });

        if (!URIChecker.isURI(item.getText()))
            viewHolder.openLinkBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void remove(int position) {
        Info item = items.get(position);
        if (items.contains(item)) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void actionView(Info item, Context context) {
        Intent openViewActivity = new Intent(context,
                ViewActivity.class);
        openViewActivity.putExtra("scan_result", item);
        context.startActivity(openViewActivity);
    }

    private void actionOpenLink(Info item, View v) {
        String uri = item.getText();
        if (URIChecker.isURI(uri)) {
            uri = URIChecker.toLink(uri);
            //open link
            Uri url = Uri.parse(uri);
            customTabsIntent.launchUrl(v.getContext(), url);
        } else
            Snackbar.make(v,
                    v.getContext().getString(R.string.generic_error),
                    BaseTransientBottomBar.LENGTH_SHORT)
                    .show();
    }
}