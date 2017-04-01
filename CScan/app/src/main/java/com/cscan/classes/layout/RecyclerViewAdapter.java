package com.cscan.classes.layout;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.cscan.MainActivity;
import com.cscan.EditActivity;
import com.cscan.classes.Info;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter {
    private List<Info> infos;

    public RecyclerViewAdapter(List<Info> infos) {
        this.infos = infos;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        RecyclerViewHolder viewHolder = (RecyclerViewHolder) holder;
        final Info item = infos.get(position);

        viewHolder.titleTextView.setText(item.getText());
        viewHolder.dateTextView.setText(item.date.toString());

        viewHolder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView(item, v.getContext());
            }
        });

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionView(item, v.getContext());
            }
        });
    }

    @Override
    public int getItemCount() {
        return infos.size();
    }

    private void actionView(Info item, Context context) {
        Intent openViewActivity = new Intent(context,
                EditActivity.class);
        openViewActivity.putExtra(MainActivity.INTENT_EXTRA_TITLE, item);
        context.startActivity(openViewActivity);
    }
}