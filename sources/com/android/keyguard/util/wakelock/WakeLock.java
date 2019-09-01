package com.android.keyguard.util.wakelock;

import android.content.Context;
import android.os.PowerManager;

public interface WakeLock {
    void acquire();

    void release();

    Runnable wrap(Runnable runnable);

    static WakeLock createPartial(Context context, String tag) {
        return wrap(createPartialInner(context, tag));
    }

    static PowerManager.WakeLock createPartialInner(Context context, String tag) {
        return ((PowerManager) context.getSystemService(PowerManager.class)).newWakeLock(1, tag);
    }

    static Runnable wrapImpl(WakeLock w, Runnable r) {
        w.acquire();
        return new Runnable(r, w) {
            private final /* synthetic */ Runnable f$0;
            private final /* synthetic */ WakeLock f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run() {
                WakeLock.lambda$wrapImpl$0(this.f$0, this.f$1);
            }
        };
    }

    static /* synthetic */ void lambda$wrapImpl$0(Runnable r, WakeLock w) {
        try {
            r.run();
        } finally {
            w.release();
        }
    }

    static WakeLock wrap(final PowerManager.WakeLock inner) {
        return new WakeLock() {
            public void acquire() {
                inner.acquire();
            }

            public void release() {
                inner.release();
            }

            public Runnable wrap(Runnable runnable) {
                return WakeLock.wrapImpl(this, runnable);
            }
        };
    }
}
