package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.model.RecentsPackageMonitor;

public class PackagesChangedEvent extends RecentsEventBus.Event {
    public final RecentsPackageMonitor monitor;
    public final String packageName;
    public final int userId;

    public PackagesChangedEvent(RecentsPackageMonitor monitor2, String packageName2, int userId2) {
        this.monitor = monitor2;
        this.packageName = packageName2;
        this.userId = userId2;
    }
}
