package com.android.keyguard.common;

import android.os.Handler;
import android.os.HandlerThread;

public final class BackgroundThread extends HandlerThread {
    private static Handler sHandler;

    private BackgroundThread() {
        super("Keyguard.Background", 0);
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (BackgroundThread.class) {
            if (sHandler == null) {
                BackgroundThread thread = new BackgroundThread();
                thread.start();
                sHandler = new Handler(thread.getLooper());
            }
            handler = sHandler;
        }
        return handler;
    }

    public static void post(Runnable runnable) {
        getHandler().post(runnable);
    }
}
