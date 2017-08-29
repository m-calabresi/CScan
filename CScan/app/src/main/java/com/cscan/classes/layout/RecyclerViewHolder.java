package com.cscan.classes.layout;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cscan.R;

class RecyclerViewHolder extends RecyclerView.ViewHolder {

    TextView titleTextView;
    TextView dateTextView;
    Button editBtn;

    RecyclerViewHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item_layout, parent, false));
        titleTextView = itemView.findViewById(R.id.title_text_view);
        dateTextView = itemView.findViewById(R.id.date_text_view);
        editBtn = itemView.findViewById(R.id.edit_btn);
    }

}