package com.android.settingslib.drawer;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.widget.RemoteViews;
import java.util.ArrayList;

public class Tile implements Parcelable {
    public static final Parcelable.Creator<Tile> CREATOR = new Parcelable.Creator<Tile>() {
        public Tile createFromParcel(Parcel source) {
            return new Tile(source);
        }

        public Tile[] newArray(int size) {
            return new Tile[size];
        }
    };
    public String category;
    public Bundle extras;
    public Icon icon;
    public Intent intent;
    public boolean isIconTintable;
    public String key;
    public Bundle metaData;
    public int priority;
    public RemoteViews remoteViews;
    public CharSequence summary;
    public CharSequence title;
    public ArrayList<UserHandle> userHandle = new ArrayList<>();

    public Tile() {
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(this.title, dest, flags);
        TextUtils.writeToParcel(this.summary, dest, flags);
        if (this.icon != null) {
            dest.writeByte((byte) 1);
            this.icon.writeToParcel(dest, flags);
        } else {
            dest.writeByte((byte) 0);
        }
        if (this.intent != null) {
            dest.writeByte((byte) 1);
            this.intent.writeToParcel(dest, flags);
        } else {
            dest.writeByte((byte) 0);
        }
        int N = this.userHandle.size();
        dest.writeInt(N);
        for (int i = 0; i < N; i++) {
            this.userHandle.get(i).writeToParcel(dest, flags);
        }
        dest.writeBundle(this.extras);
        dest.writeString(this.category);
        dest.writeInt(this.priority);
        dest.writeBundle(this.metaData);
        dest.writeString(this.key);
        dest.writeParcelable(this.remoteViews, flags);
        dest.writeBoolean(this.isIconTintable);
    }

    public void readFromParcel(Parcel in) {
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        if (in.readByte() != 0) {
            this.icon = (Icon) Icon.CREATOR.createFromParcel(in);
        }
        if (in.readByte() != 0) {
            this.intent = (Intent) Intent.CREATOR.createFromParcel(in);
        }
        int N = in.readInt();
        for (int i = 0; i < N; i++) {
            this.userHandle.add((UserHandle) UserHandle.CREATOR.createFromParcel(in));
        }
        this.extras = in.readBundle();
        this.category = in.readString();
        this.priority = in.readInt();
        this.metaData = in.readBundle();
        this.key = in.readString();
        this.remoteViews = (RemoteViews) in.readParcelable(RemoteViews.class.getClassLoader());
        this.isIconTintable = in.readBoolean();
    }

    Tile(Parcel in) {
        readFromParcel(in);
    }
}
