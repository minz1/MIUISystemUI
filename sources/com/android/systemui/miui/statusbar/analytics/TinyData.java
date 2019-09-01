package com.android.systemui.miui.statusbar.analytics;

public class TinyData {
    private String mCategory;
    private String mData;
    private String mName;
    private String mPkg = "com.android.systemui";

    public TinyData(String category, String name, String data) {
        this.mCategory = category;
        this.mName = name;
        this.mData = data;
    }

    public String getPkg() {
        return this.mPkg;
    }

    public String getCategory() {
        return this.mCategory;
    }

    public String getName() {
        return this.mName;
    }

    public String getData() {
        return this.mData;
    }

    public String toString() {
        return "pkg :" + this.mPkg + ", Category:" + getCategory() + ", Name :" + getName() + ", Data:" + getData();
    }
}
