package com.android.systemui.recents.events.activity;

import android.graphics.Bitmap;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.events.RecentsEventBus;

public class TaskSnapshotChangedEvent extends RecentsEventBus.AnimatedEvent {
    public final Bitmap snapshot;
    public final int taskId;
    public final ActivityManager.TaskThumbnailInfo taskThumbnailInfo;

    public TaskSnapshotChangedEvent(int taskId2, Bitmap snapshot2, ActivityManager.TaskThumbnailInfo taskThumbnailInfo2) {
        this.taskId = taskId2;
        this.snapshot = snapshot2;
        this.taskThumbnailInfo = taskThumbnailInfo2;
    }
}
