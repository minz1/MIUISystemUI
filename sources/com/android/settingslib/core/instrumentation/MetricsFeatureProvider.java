package com.android.settingslib.core.instrumentation;

import android.content.Context;
import java.util.List;

public class MetricsFeatureProvider {
    private List<LogWriter> mLoggerWriters;

    public void visible(Context context, int source, int category) {
        for (LogWriter writer : this.mLoggerWriters) {
            writer.visible(context, source, category);
        }
    }

    public void hidden(Context context, int category) {
        for (LogWriter writer : this.mLoggerWriters) {
            writer.hidden(context, category);
        }
    }
}
