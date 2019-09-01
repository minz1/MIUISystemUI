package com.android.systemui.proxy;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

public class ActivityManager {

    public static class TaskThumbnailInfo implements Parcelable {
        public static final Parcelable.Creator<TaskThumbnailInfo> CREATOR = new Parcelable.Creator<TaskThumbnailInfo>() {
            public TaskThumbnailInfo createFromParcel(Parcel source) {
                return new TaskThumbnailInfo(source);
            }

            public TaskThumbnailInfo[] newArray(int size) {
                return new TaskThumbnailInfo[size];
            }
        };
        public Rect insets;
        public float scale;
        public int screenOrientation;
        public int taskHeight;
        public int taskWidth;

        public TaskThumbnailInfo() {
            this.screenOrientation = 0;
            this.insets = new Rect(0, 0, 0, 0);
            this.scale = 1.0f;
        }

        private TaskThumbnailInfo(Parcel source) {
            this.screenOrientation = 0;
            this.insets = new Rect(0, 0, 0, 0);
            this.scale = 1.0f;
            readFromParcel(source);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.taskWidth);
            dest.writeInt(this.taskHeight);
            dest.writeInt(this.screenOrientation);
        }

        public void readFromParcel(Parcel source) {
            this.taskWidth = source.readInt();
            this.taskHeight = source.readInt();
            this.screenOrientation = source.readInt();
        }
    }
}
