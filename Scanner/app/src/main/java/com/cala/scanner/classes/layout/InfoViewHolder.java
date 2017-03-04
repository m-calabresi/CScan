package com.cala.scanner.classes.layout;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cala.scanner.R;

class InfoViewHolder extends RecyclerView.ViewHolder {

    TextView titleTextView;
    TextView dateTextView;
    Button openLinkBtn;
    Button editBtn;

    InfoViewHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_view, parent, false));
        titleTextView = (TextView) itemView.findViewById(R.id.title_text_view);
        dateTextView = (TextView) itemView.findViewById(R.id.date_text_view);
        openLinkBtn = (Button) itemView.findViewById(R.id.open_btn);
        editBtn = (Button) itemView.findViewById(R.id.edit_btn);
    }

}