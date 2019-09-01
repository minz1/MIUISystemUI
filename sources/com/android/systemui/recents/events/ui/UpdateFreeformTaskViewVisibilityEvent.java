package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.RecentsEventBus;

public class UpdateFreeformTaskViewVisibilityEvent extends RecentsEventBus.Event {
    public final boolean visible;

    public UpdateFreeformTaskViewVisibilityEvent(boolean visible2) {
        this.visible = visible2;
    }
}
