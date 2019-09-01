package com.android.keyguard.util.wakelock;

import android.os.Handler;
import java.util.Objects;

public class DelayedWakeLock implements WakeLock {
    private final Handler mHandler;
    private final WakeLock mInner;
    private final Runnable mRelease;

    public DelayedWakeLock(Handler h, WakeLock inner) {
        this.mHandler = h;
        this.mInner = inner;
        WakeLock wakeLock = this.mInner;
        Objects.requireNonNull(wakeLock);
        this.mRelease = new Runnable() {
            public final void run() {
                WakeLock.this.release();
            }
        };
    }

    public void acquire() {
        this.mInner.acquire();
    }

    public void release() {
        this.mHandler.postDelayed(this.mRelease, 140);
    }

    public Runnable wrap(Runnable r) {
        return WakeLock.wrapImpl(this, r);
    }
}
