package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.RecentsEventBus;

public class DraggingInRecentsEvent extends RecentsEventBus.Event {
    public final float distanceFromTop;

    public DraggingInRecentsEvent(float distanceFromTop2) {
        this.distanceFromTop = distanceFromTop2;
    }
}
