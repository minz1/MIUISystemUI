package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class FsGestureEnterRecentsZoomEvent extends RecentsEventBus.Event {
    public final long mTimeOffset;

    public FsGestureEnterRecentsZoomEvent(long timeOffset) {
        this.mTimeOffset = timeOffset;
    }
}
