package com.android.systemui.recents.events.activity;

import android.graphics.Rect;
import com.android.systemui.recents.events.RecentsEventBus;

public class DockedTopTaskEvent extends RecentsEventBus.Event {
    public int dragMode;
    public Rect initialRect;

    public DockedTopTaskEvent(int dragMode2, Rect initialRect2) {
        this.dragMode = dragMode2;
        this.initialRect = initialRect2;
    }
}
