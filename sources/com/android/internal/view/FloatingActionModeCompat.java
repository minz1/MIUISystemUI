package com.android.internal.view;

import android.content.Context;
import android.view.ActionMode;
import android.view.View;
import com.android.internal.widget.FloatingToolbar;

public class FloatingActionModeCompat {
    public static FloatingActionMode newFloatingActionMode(Context context, ActionMode.Callback2 callback, View originatingView, FloatingToolbar floatingToolbar) {
        return new FloatingActionMode(context, callback, originatingView, floatingToolbar);
    }
}
