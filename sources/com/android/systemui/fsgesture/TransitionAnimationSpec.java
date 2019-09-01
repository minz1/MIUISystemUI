package com.android.systemui.fsgesture;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

public class TransitionAnimationSpec implements Parcelable {
    public static final Parcelable.Creator<TransitionAnimationSpec> CREATOR = new Parcelable.Creator<TransitionAnimationSpec>() {
        public TransitionAnimationSpec createFromParcel(Parcel in) {
            return new TransitionAnimationSpec(in);
        }

        public TransitionAnimationSpec[] newArray(int size) {
            return new TransitionAnimationSpec[size];
        }
    };
    public final Bitmap mBitmap;
    public final Rect mRect;

    public TransitionAnimationSpec(Parcel in) {
        this.mBitmap = (Bitmap) in.readParcelable(null);
        this.mRect = (Rect) in.readParcelable(null);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mBitmap, flags);
        dest.writeParcelable(this.mRect, flags);
    }

    public int describeContents() {
        return 0;
    }
}
