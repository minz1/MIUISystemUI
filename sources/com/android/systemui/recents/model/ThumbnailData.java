package com.android.systemui.recents.model;

import android.graphics.Bitmap;
import com.android.systemui.proxy.ActivityManager;

public class ThumbnailData {
    public boolean isAccessLocked;
    public Bitmap thumbnail;
    public ActivityManager.TaskThumbnailInfo thumbnailInfo;
}
