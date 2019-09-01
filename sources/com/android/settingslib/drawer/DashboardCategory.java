package com.android.settingslib.drawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DashboardCategory implements Parcelable {
    public static final Parcelable.Creator<DashboardCategory> CREATOR = new Parcelable.Creator<DashboardCategory>() {
        public DashboardCategory createFromParcel(Parcel source) {
            return new DashboardCategory(source);
        }

        public DashboardCategory[] newArray(int size) {
            return new DashboardCategory[size];
        }
    };
    private static final boolean DEBUG = Log.isLoggable("DashboardCategory", 3);
    public static final Comparator<Tile> TILE_COMPARATOR = new Comparator<Tile>() {
        public int compare(Tile lhs, Tile rhs) {
            return rhs.priority - lhs.priority;
        }
    };
    public String key;
    private List<Tile> mTiles = new ArrayList();
    public int priority;
    public CharSequence title;

    public DashboardCategory() {
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(this.title, dest, flags);
        dest.writeString(this.key);
        dest.writeInt(this.priority);
        int count = this.mTiles.size();
        dest.writeInt(count);
        for (int n = 0; n < count; n++) {
            this.mTiles.get(n).writeToParcel(dest, flags);
        }
    }

    public void readFromParcel(Parcel in) {
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.key = in.readString();
        this.priority = in.readInt();
        int count = in.readInt();
        for (int n = 0; n < count; n++) {
            this.mTiles.add(Tile.CREATOR.createFromParcel(in));
        }
    }

    DashboardCategory(Parcel in) {
        readFromParcel(in);
    }
}
