package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class ConfigurationChangedEvent extends RecentsEventBus.AnimatedEvent {
    public final boolean fromDeviceOrientationChange;
    public final boolean fromDisplayDensityChange;
    public final boolean fromMultiWindow;
    public final boolean hasStackTasks;

    public ConfigurationChangedEvent(boolean fromMultiWindow2, boolean fromDeviceOrientationChange2, boolean fromDisplayDensityChange2, boolean hasStackTasks2) {
        this.fromMultiWindow = fromMultiWindow2;
        this.fromDeviceOrientationChange = fromDeviceOrientationChange2;
        this.fromDisplayDensityChange = fromDisplayDensityChange2;
        this.hasStackTasks = hasStackTasks2;
    }
}
