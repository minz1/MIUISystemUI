package com.android.systemui.statusbar;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.ExpandHelper;
import com.android.systemui.statusbar.DragDownHelper;

public class DisableDragDownHelper extends DragDownHelper {
    public DisableDragDownHelper(Context context, View host, ExpandHelper.Callback callback, DragDownHelper.DragDownCallback dragDownCallback) {
        super(context, host, callback, dragDownCallback);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
