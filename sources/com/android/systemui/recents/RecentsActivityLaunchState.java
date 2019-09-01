package com.android.systemui.recents;

import android.os.Parcel;
import android.os.Parcelable;

public class RecentsActivityLaunchState implements Parcelable {
    public static final Parcelable.Creator<RecentsActivityLaunchState> CREATOR = new Parcelable.Creator<RecentsActivityLaunchState>() {
        public RecentsActivityLaunchState createFromParcel(Parcel source) {
            return new RecentsActivityLaunchState(source);
        }

        public RecentsActivityLaunchState[] newArray(int size) {
            return new RecentsActivityLaunchState[size];
        }
    };
    public boolean launchedFromApp;
    public boolean launchedFromHome;
    public int launchedNumVisibleTasks;
    public int launchedNumVisibleThumbnails;
    public int launchedToTaskId;
    public boolean launchedViaDockGesture;
    public boolean launchedViaDragGesture;
    public boolean launchedViaFsGesture;
    public boolean launchedWithAltTab;

    public void reset() {
        this.launchedFromHome = false;
        this.launchedFromApp = false;
        this.launchedToTaskId = -1;
        this.launchedWithAltTab = false;
        this.launchedViaDragGesture = false;
        this.launchedViaDockGesture = false;
        this.launchedViaFsGesture = false;
    }

    public int getInitialFocusTaskIndex(int numTasks) {
        RecentsDebugFlags debugFlags = Recents.getDebugFlags();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (this.launchedFromApp) {
            if (launchState.launchedWithAltTab || !debugFlags.isFastToggleRecentsEnabled()) {
                return Math.max(0, numTasks - 2);
            }
            return numTasks - 1;
        } else if (launchState.launchedWithAltTab || !debugFlags.isFastToggleRecentsEnabled()) {
            return numTasks - 1;
        } else {
            return -1;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.launchedWithAltTab ? (byte) 1 : 0);
        dest.writeByte(this.launchedFromApp ? (byte) 1 : 0);
        dest.writeByte(this.launchedFromHome ? (byte) 1 : 0);
        dest.writeByte(this.launchedViaDragGesture ? (byte) 1 : 0);
        dest.writeByte(this.launchedViaDockGesture ? (byte) 1 : 0);
        dest.writeByte(this.launchedViaFsGesture ? (byte) 1 : 0);
        dest.writeInt(this.launchedToTaskId);
        dest.writeInt(this.launchedNumVisibleTasks);
        dest.writeInt(this.launchedNumVisibleThumbnails);
    }

    public RecentsActivityLaunchState() {
    }

    /* access modifiers changed from: protected */
    public void copyFrom(RecentsActivityLaunchState o) {
        if (o != null) {
            this.launchedWithAltTab = o.launchedWithAltTab;
            this.launchedFromApp = o.launchedFromApp;
            this.launchedFromHome = o.launchedFromHome;
            this.launchedViaDragGesture = o.launchedViaDragGesture;
            this.launchedViaDockGesture = o.launchedViaDockGesture;
            this.launchedViaFsGesture = o.launchedViaFsGesture;
            this.launchedToTaskId = o.launchedToTaskId;
            this.launchedNumVisibleTasks = o.launchedNumVisibleTasks;
            this.launchedNumVisibleThumbnails = o.launchedNumVisibleThumbnails;
        }
    }

    protected RecentsActivityLaunchState(Parcel in) {
        boolean z = false;
        this.launchedWithAltTab = in.readByte() != 0;
        this.launchedFromApp = in.readByte() != 0;
        this.launchedFromHome = in.readByte() != 0;
        this.launchedViaDragGesture = in.readByte() != 0;
        this.launchedViaDockGesture = in.readByte() != 0;
        this.launchedViaFsGesture = in.readByte() != 0 ? true : z;
        this.launchedToTaskId = in.readInt();
        this.launchedNumVisibleTasks = in.readInt();
        this.launchedNumVisibleThumbnails = in.readInt();
    }
}
