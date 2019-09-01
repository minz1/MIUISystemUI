package com.android.systemui.recents.events.ui.focus;

import com.android.systemui.recents.events.RecentsEventBus;

public class FocusNextTaskViewEvent extends RecentsEventBus.Event {
    public final int timerIndicatorDuration;

    public FocusNextTaskViewEvent(int timerIndicatorDuration2) {
        this.timerIndicatorDuration = timerIndicatorDuration2;
    }
}
