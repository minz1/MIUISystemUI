package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.policy.DeadZone;

public class NavigationBarFrame extends FrameLayout {
    private DeadZone mDeadZone = null;

    public NavigationBarFrame(Context context) {
        super(context);
    }

    public NavigationBarFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() != 4 || this.mDeadZone == null) {
            return super.dispatchTouchEvent(event);
        }
        return this.mDeadZone.onTouchEvent(event);
    }
}
