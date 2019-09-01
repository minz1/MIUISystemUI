package com.android.internal.widget;

import android.content.Context;
import android.view.Window;

public class FloatingToolbarCompat {
    public static FloatingToolbar newFloatingToolbar(Context context, Window window) {
        return new FloatingToolbar(window);
    }
}
