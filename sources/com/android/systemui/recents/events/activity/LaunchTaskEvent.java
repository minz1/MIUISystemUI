package com.android.systemui.recents.events.activity;

import android.graphics.Rect;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.TaskView;

public class LaunchTaskEvent extends RecentsEventBus.Event {
    public final boolean screenPinningRequested;
    public final Rect targetTaskBounds;
    public final int targetTaskStack;
    public final Task task;
    public final TaskView taskView;

    public LaunchTaskEvent(TaskView taskView2, Task task2, Rect targetTaskBounds2, int targetTaskStack2, boolean screenPinningRequested2) {
        this.taskView = taskView2;
        this.task = task2;
        this.targetTaskBounds = targetTaskBounds2;
        this.targetTaskStack = targetTaskStack2;
        this.screenPinningRequested = screenPinningRequested2;
    }
}
