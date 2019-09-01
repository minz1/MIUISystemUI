package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;

public interface PaperModeController extends Dumpable, CallbackController<PaperModeListener> {

    public interface PaperModeListener {
        void onPaperModeAvailabilityChanged(boolean z);

        void onPaperModeChanged(boolean z);
    }

    boolean isAvailable();

    boolean isEnabled();

    void setEnabled(boolean z);
}
