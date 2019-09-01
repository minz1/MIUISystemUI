package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;

public interface SilentModeObserverController extends Dumpable, CallbackController<SilentModeListener> {

    public interface SilentModeListener {
        void onSilentModeChanged(boolean z);
    }
}
