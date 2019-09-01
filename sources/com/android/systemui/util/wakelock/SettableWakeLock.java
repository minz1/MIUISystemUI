package com.android.systemui.util.wakelock;

import com.android.internal.util.Preconditions;

public class SettableWakeLock {
    private boolean mAcquired;
    private final WakeLock mInner;

    public SettableWakeLock(WakeLock inner) {
        Preconditions.checkNotNull(inner, "inner wakelock required");
        this.mInner = inner;
    }

    public synchronized void setAcquired(boolean acquired) {
        if (this.mAcquired != acquired) {
            if (acquired) {
                this.mInner.acquire();
            } else {
                this.mInner.release();
            }
            this.mAcquired = acquired;
        }
    }
}
