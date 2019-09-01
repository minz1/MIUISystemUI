package com.xiaomi.mistatistic.sdk.data;

import android.os.Parcel;
import android.os.Parcelable;

public class StatEventPojo implements Parcelable {
    public static final Parcelable.Creator<StatEventPojo> CREATOR = new Parcelable.Creator<StatEventPojo>() {
        /* renamed from: a */
        public StatEventPojo createFromParcel(Parcel parcel) {
            StatEventPojo statEventPojo = new StatEventPojo();
            statEventPojo.category = parcel.readString();
            statEventPojo.timeStamp = parcel.readLong();
            statEventPojo.key = parcel.readString();
            statEventPojo.type = parcel.readString();
            statEventPojo.value = parcel.readString();
            statEventPojo.extra = parcel.readString();
            statEventPojo.anonymous = parcel.readInt();
            return statEventPojo;
        }

        /* renamed from: a */
        public StatEventPojo[] newArray(int i) {
            return new StatEventPojo[i];
        }
    };
    public int anonymous;
    public String category;
    public String extra;
    public String key;
    public long timeStamp;
    public String type;
    public String value;

    public String toString() {
        return "Event [" + "category=" + this.category + "," + "key=" + this.key + "," + "value=" + this.value + ",params=" + this.extra + ",anonymous=" + this.anonymous + "]";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.category);
        parcel.writeLong(this.timeStamp);
        parcel.writeString(this.key);
        parcel.writeString(this.type);
        parcel.writeString(this.value);
        parcel.writeString(this.extra);
        parcel.writeInt(this.anonymous);
    }
}
