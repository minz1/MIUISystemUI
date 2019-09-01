package com.android.settingslib.core.instrumentation;

import android.content.Context;

public interface LogWriter {
    void hidden(Context context, int i);

    void visible(Context context, int i, int i2);
}
