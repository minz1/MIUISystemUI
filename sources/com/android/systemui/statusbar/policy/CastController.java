package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;

public interface CastController extends Dumpable, CallbackController<Callback> {

    public interface Callback {
        void onCastDevicesChanged();
    }
}
