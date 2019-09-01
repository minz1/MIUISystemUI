package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class RotationChangedEvent extends RecentsEventBus.Event {
    public final int rotation;

    public RotationChangedEvent(int rotation2) {
        this.rotation = rotation2;
    }
}
