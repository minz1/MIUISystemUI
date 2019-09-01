package com.android.systemui.recents.events.component;

import android.content.Context;
import com.android.systemui.recents.events.RecentsEventBus;

public class ScreenPinningRequestEvent extends RecentsEventBus.Event {
    public final Context applicationContext;
    public final int taskId;

    public ScreenPinningRequestEvent(Context context, int taskId2) {
        this.applicationContext = context.getApplicationContext();
        this.taskId = taskId2;
    }
}
