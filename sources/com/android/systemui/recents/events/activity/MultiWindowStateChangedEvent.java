package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.model.TaskStack;

public class MultiWindowStateChangedEvent extends RecentsEventBus.AnimatedEvent {
    public final boolean inMultiWindow;
    public final boolean showDeferredAnimation;
    public final TaskStack stack;

    public MultiWindowStateChangedEvent(boolean inMultiWindow2, boolean showDeferredAnimation2, TaskStack stack2) {
        this.inMultiWindow = inMultiWindow2;
        this.showDeferredAnimation = showDeferredAnimation2;
        this.stack = stack2;
    }
}
