package com.android.systemui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UiOffloadThread {
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    public Future<?> submit(Runnable runnable) {
        return this.mExecutorService.submit(runnable);
    }
}
