package com.android.internal.logging;

import android.content.Context;
import android.metrics.LogMaker;

public class MetricsLoggerCompat {
    public static void write(Context context, MetricsLogger metricsLogger, LogMaker logMaker) {
        metricsLogger.write(logMaker);
    }
}
