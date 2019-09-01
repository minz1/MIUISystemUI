package com.android.internal.hardware;

import android.content.Context;

public class AmbientDisplayConfigurationCompat {
    private AmbientDisplayConfiguration mConfig;

    public AmbientDisplayConfigurationCompat(Context context) {
        this.mConfig = new AmbientDisplayConfiguration(context);
    }

    public boolean alwaysOnEnabled(int user) {
        return this.mConfig.alwaysOnEnabled(user);
    }
}
