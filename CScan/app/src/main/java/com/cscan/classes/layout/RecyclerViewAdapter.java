package com.cscan.classes.layout;

import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.cscan.MainActivity;
import com.cscan.R;
import com.cscan.classes.Info;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class RecyclerViewAdapter extends RecyclerView.Adapter {
    private List<Info> infos;
    private int browserType;

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
                ((MainActivity) v.getContext()).openViewActivity(item);
            }
        });

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = v.getContext().getSharedPreferences(
                        v.getContext().getString(
                                R.string.cscan_shared_preference_name), MODE_PRIVATE);
                browserType = sharedPreferences.getInt(v.getContext().
                        getString(R.string.pref_key_browser_type), 0);
                if(((MainActivity) v.getContext()).browser.isURI(item.getText())) //if uri
                    ((MainActivity) v.getContext()).browser.openLink(item, browserType);
                else //not a uri
                    ((MainActivity) v.getContext()).openViewActivity(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return infos.size();
    }
}