package com.android.systemui.miui.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

public class DarkReceiverLinearLayout extends LinearLayout implements DarkIconDispatcher.DarkReceiver {
    public DarkReceiverLinearLayout(Context context) {
        this(context, null);
    }

    public DarkReceiverLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof DarkIconDispatcher.DarkReceiver) {
                ((DarkIconDispatcher.DarkReceiver) view).onDarkChanged(area, darkIntensity, tint);
            }
        }
    }
}
