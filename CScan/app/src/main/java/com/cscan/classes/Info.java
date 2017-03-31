package com.cscan.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Info implements Parcelable {
    private String text;
    public Date date;

    /*public Info() {
        this.text = null;
        this.date = new Date();
    }*/

    Info(String text, Date date) {
        this.date = date;
        this.text = text;
    }

    /*public Info(String text, int day, int month, int year) {
        this.date = new Date(day, month, year);
        this.text = text;
    }*/

    /*public Info(Info info) {
        this.date = info.date;
        this.text = info.getText();
    }*/

    public Info(String text) {
        this.date = Date.getCurrentDate();
        this.text = text;
    }

    private Info(Parcel source) {
        this.date = source.readParcelable(Date.class.getClassLoader());
        this.text = source.readString();
    }

    public String getText() {
        return this.text;
    }

    /*public void setText(String text) {
        this.text = text;
    }*/

    public boolean equals(Info info) {
        return this.text.equals(info.getText());
    }

    public boolean isNull() {
        boolean a;
        a = this.text == null &&
                this.date.isNull();
        return a;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.date, flags);
        dest.writeString(this.text);
    }

    public static final Creator<Info> CREATOR = new Creator<Info>() {
        @Override
        public Info[] newArray(int size) {
            return new Info[size];
        }

        @Override
        public Info createFromParcel(Parcel source) {
            return new Info(source);
        }
    };
}
