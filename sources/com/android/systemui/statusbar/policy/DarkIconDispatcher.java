package com.android.systemui.statusbar.policy;

import android.graphics.Rect;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;

public interface DarkIconDispatcher {
    public static final int[] sTmpInt2 = new int[2];
    public static final Rect sTmpRect = new Rect();

    public interface DarkReceiver {
        void onDarkChanged(Rect rect, float f, int i);
    }

    void addDarkReceiver(DarkReceiver darkReceiver);

    LightBarTransitionsController getTransitionsController();

    void removeDarkReceiver(DarkReceiver darkReceiver);

    void setIconsDarkArea(Rect rect);
}
