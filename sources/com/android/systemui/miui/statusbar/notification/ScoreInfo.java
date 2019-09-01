package com.android.systemui.miui.statusbar.notification;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class ScoreInfo {
    @SerializedName("n")
    private int mCount;
    @SerializedName("extra_info")
    private String mExtraInfo;
    @SerializedName("group_interval")
    private long mGroupInterval;
    @SerializedName("server_score")
    private double mServerScore;
    @SerializedName("sort_delay")
    private long mSortDelay;
    @SerializedName("threshold")
    private double mThreshold;

    public String getExtraInfo() {
        return this.mExtraInfo;
    }

    public double getServerScore() {
        return this.mServerScore;
    }

    public double getThreshold() {
        return this.mThreshold;
    }

    public long getSortDelay() {
        return this.mSortDelay;
    }

    public long getGroupInterval() {
        return this.mGroupInterval;
    }

    public int getCount() {
        return this.mCount;
    }

    public String toJSONObject() {
        return new Gson().toJson(this);
    }
}
