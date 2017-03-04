package com.cala.scanner.classes;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.StringTokenizer;

public class Date implements Parcelable {
    private static final int NULL_DAY = 0;
    private static final int NULL_MONTH = 0;
    private static final int NULL_YEAR = 0;

    private static final String DATE_SEPARATOR = "/";

    private int day;
    private int month;
    private int year;

    Date() {
        this.day = NULL_DAY;
        this.month = NULL_MONTH;
        this.year = NULL_YEAR;
    }

    /*Date(Date date) {
        this.day = date.getDay();
        this.month = date.getMonth();
        this.year = date.getYear();
    }*/

    Date(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    private Date(Parcel source) {
        this.day = source.readInt();
        this.month = source.readInt();
        this.year = source.readInt();
    }

    /*
    public Date(Date date){
        this.day = date.day;
        this.month = date.month;
        this.year = date.year;
    }

    int getDay() {
        return this.day;
    }

    int getMonth() {
        return this.month;
    }

    int getYear() {
        return this.year;
    }
    */

    public String toString() {
        return Integer.toString(this.day) + DATE_SEPARATOR
                + Integer.toString(this.month) + DATE_SEPARATOR
                + Integer.toString(this.year);
    }

    static Date getCurrentDate() {
        Calendar calendar = Calendar.getInstance();

        int day = calendar.get(Calendar.DATE);
        int month = 1 + calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        return new Date(day, month, year);
    }

    static Date toDate(String strDate) {
        StringTokenizer stringTokenizer = new StringTokenizer(strDate, DATE_SEPARATOR);
        int day = Integer.parseInt(stringTokenizer.nextToken());
        int month = Integer.parseInt(stringTokenizer.nextToken());
        int year = Integer.parseInt(stringTokenizer.nextToken());

        return new Date(day, month, year);
    }

    boolean isNull() {
        return this.day == NULL_DAY &&
                this.month == NULL_MONTH &&
                this.year == NULL_YEAR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.day);
        dest.writeInt(this.month);
        dest.writeInt(this.year);
    }

    public static final Creator<Date> CREATOR = new Creator<Date>() {
        @Override
        public Date[] newArray(int size) {
            return new Date[size];
        }

        @Override
        public Date createFromParcel(Parcel source) {
            return new Date(source);
        }
    };
}

