package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class FsGestureShowStateEvent extends RecentsEventBus.Event {
    public boolean isEnter;
    public String typeFrom;

    public FsGestureShowStateEvent(boolean isEnter2) {
        this.isEnter = isEnter2;
        this.typeFrom = "typefrom_demo";
    }

    public FsGestureShowStateEvent(boolean isEnter2, String typeFrom2) {
        this.isEnter = isEnter2;
        this.typeFrom = typeFrom2;
    }
}
