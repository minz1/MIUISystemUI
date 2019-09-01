package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class DismissRecentsToHomeAnimationStarted extends RecentsEventBus.AnimatedEvent {
    public final boolean animated;

    public DismissRecentsToHomeAnimationStarted(boolean animated2) {
        this.animated = animated2;
    }
}
