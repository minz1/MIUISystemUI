package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class HideRecentsEvent extends RecentsEventBus.Event {
    public final boolean triggeredFromAltTab;
    public final boolean triggeredFromFsGesture;
    public final boolean triggeredFromHomeKey;
    public final boolean triggeredFromScroll;

    public HideRecentsEvent(boolean triggeredFromAltTab2, boolean triggeredFromHomeKey2, boolean triggeredFromFsGesture2) {
        this.triggeredFromAltTab = triggeredFromAltTab2;
        this.triggeredFromHomeKey = triggeredFromHomeKey2;
        this.triggeredFromFsGesture = triggeredFromFsGesture2;
        this.triggeredFromScroll = false;
    }

    public HideRecentsEvent(boolean triggeredFromAltTab2, boolean triggeredFromHomeKey2, boolean triggeredFromFsGesture2, boolean triggeredFromScroll2) {
        this.triggeredFromAltTab = triggeredFromAltTab2;
        this.triggeredFromHomeKey = triggeredFromHomeKey2;
        this.triggeredFromFsGesture = triggeredFromFsGesture2;
        this.triggeredFromScroll = triggeredFromScroll2;
    }
}
