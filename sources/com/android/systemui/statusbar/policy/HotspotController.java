package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;

public interface HotspotController extends Dumpable, CallbackController<Callback> {

    public interface Callback {
        void onHotspotChanged(boolean z);
    }

    boolean isHotspotEnabled();

    boolean isHotspotReady();

    boolean isHotspotSupported();

    boolean isHotspotTransient();

    void setHotspotEnabled(boolean z);
}
