package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class LaunchTaskSucceededEvent extends RecentsEventBus.Event {
    public final int taskIndexFromStackFront;

    public LaunchTaskSucceededEvent(int taskIndexFromStackFront2) {
        this.taskIndexFromStackFront = taskIndexFromStackFront2;
    }
}
