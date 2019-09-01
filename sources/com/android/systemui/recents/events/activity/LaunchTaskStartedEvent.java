package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.views.TaskView;

public class LaunchTaskStartedEvent extends RecentsEventBus.AnimatedEvent {
    public final boolean screenPinningRequested;
    public final TaskView taskView;

    public LaunchTaskStartedEvent(TaskView taskView2, boolean screenPinningRequested2) {
        this.taskView = taskView2;
        this.screenPinningRequested = screenPinningRequested2;
    }
}
