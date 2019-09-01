package com.android.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class NonInterceptingScrollView extends ScrollView {
    public NonInterceptingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0 && canScrollVertically(1)) {
            requestDisallowInterceptTouchEvent(true);
        }
        return super.onTouchEvent(ev);
    }
}
