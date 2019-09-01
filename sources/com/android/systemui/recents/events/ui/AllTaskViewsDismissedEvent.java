package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.RecentsEventBus;

public class AllTaskViewsDismissedEvent extends RecentsEventBus.Event {
    public final boolean mEmpty;
    public final boolean mFromDockGesture;
    public final int msgResId;

    public AllTaskViewsDismissedEvent(int msgResId2, boolean empty, boolean fromDockGesture) {
        this.msgResId = msgResId2;
        this.mEmpty = empty;
        this.mFromDockGesture = fromDockGesture;
    }

    public AllTaskViewsDismissedEvent(int msgResId2, boolean empty) {
        this.msgResId = msgResId2;
        this.mEmpty = empty;
        this.mFromDockGesture = false;
    }
}
