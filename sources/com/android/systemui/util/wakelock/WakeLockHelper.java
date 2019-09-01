package com.android.systemui.util.wakelock;

import android.content.Context;
import android.os.PowerManager;

public abstract class WakeLockHelper implements WakeLock {
    public static WakeLock createPartial(Context context, String tag) {
        return wrap(createPartialInner(context, tag));
    }

    static PowerManager.WakeLock createPartialInner(Context context, String tag) {
        return ((PowerManager) context.getSystemService(PowerManager.class)).newWakeLock(1, tag);
    }

    static WakeLock wrap(final PowerManager.WakeLock inner) {
        return new WakeLock() {
            public void acquire() {
                inner.acquire();
            }

            public void release() {
                inner.release();
            }
        };
    }
}
