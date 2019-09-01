package com.android.settingslib;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

public class AppItem implements Parcelable, Comparable<AppItem> {
    public static final Parcelable.Creator<AppItem> CREATOR = new Parcelable.Creator<AppItem>() {
        public AppItem createFromParcel(Parcel in) {
            return new AppItem(in);
        }

        public AppItem[] newArray(int size) {
            return new AppItem[size];
        }
    };
    public int category;
    public final int key;
    public long total;
    public SparseBooleanArray uids;

    public AppItem() {
        this.uids = new SparseBooleanArray();
        this.key = 0;
    }

    public AppItem(Parcel parcel) {
        this.uids = new SparseBooleanArray();
        this.key = parcel.readInt();
        this.uids = parcel.readSparseBooleanArray();
        this.total = parcel.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.key);
        dest.writeSparseBooleanArray(this.uids);
        dest.writeLong(this.total);
    }

    public int describeContents() {
        return 0;
    }

    public int compareTo(AppItem another) {
        int comparison = Integer.compare(this.category, another.category);
        if (comparison == 0) {
            return Long.compare(another.total, this.total);
        }
        return comparison;
    }
}
