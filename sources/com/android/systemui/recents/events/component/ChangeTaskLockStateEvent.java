package com.android.systemui.recents.events.component;

import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.model.Task;

public class ChangeTaskLockStateEvent extends RecentsEventBus.Event {
    public boolean isLocked;
    public final Task task;

    public ChangeTaskLockStateEvent(Task task2, boolean isLocked2) {
        this.task = task2;
        this.isLocked = isLocked2;
    }
}
