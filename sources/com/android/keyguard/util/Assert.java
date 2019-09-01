package com.android.keyguard.util;

import android.os.Looper;

public class Assert {
    public static void isMainThread() {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("should be called from the main thread.");
        }
    }
}
