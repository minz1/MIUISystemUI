package com.android.systemui.miui.statusbar.phone.rank;

public class PackageEntity {
    private int mDailyClick;
    private int mDailyShow;
    private boolean mDataChanged;
    private int mHistoryClick;
    private int mHistoryShow;
    private Object mLock = new Object();
    private String mPackageName;

    public PackageEntity(String packageName) {
        this.mPackageName = packageName;
    }

    public int getDailyClick() {
        return this.mDailyClick;
    }

    public int getDailyShow() {
        return this.mDailyShow;
    }

    public int getTotalClick() {
        return this.mDailyClick + this.mHistoryClick;
    }

    public int getTotalShow() {
        return this.mDailyShow + this.mHistoryShow;
    }

    public void addClickCount() {
        synchronized (this.mLock) {
            this.mDailyClick++;
            this.mDataChanged = true;
        }
    }

    public void addShowCount() {
        synchronized (this.mLock) {
            this.mDailyShow++;
            this.mDataChanged = true;
        }
    }

    public void setDailyData(int click, int show) {
        synchronized (this.mLock) {
            this.mDailyClick += click;
            this.mDailyShow += show;
        }
    }

    public void setHistoryData(int click, int show) {
        synchronized (this.mLock) {
            this.mHistoryClick = click;
            this.mHistoryShow = show;
        }
    }

    public void onDateChanged(int click, int show) {
        synchronized (this.mLock) {
            this.mDailyClick = 0;
            this.mDailyShow = 0;
            this.mHistoryClick = click;
            this.mHistoryShow = show;
            this.mDataChanged = false;
        }
    }

    public void setDataChanged(boolean dataChanged) {
        synchronized (this.mLock) {
            this.mDataChanged = dataChanged;
        }
    }

    public boolean isDataChanged() {
        return this.mDataChanged;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String toString() {
        return "PackageEntity{mPackageName='" + this.mPackageName + '\'' + ", mDailyClick=" + this.mDailyClick + ", mDailyShow=" + this.mDailyShow + ", mHistoryClick=" + this.mHistoryClick + ", mHistoryShow=" + this.mHistoryShow + ", mDataChanged=" + this.mDataChanged + '}';
    }
}
