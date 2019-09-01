package com.android.systemui.statusbar.phone;

import android.graphics.Rect;

public interface KeyguardStatusBarViewController {
    void hideStatusIcons();

    void init(KeyguardStatusBarView keyguardStatusBarView);

    boolean isPromptCenter();

    void setDarkMode(Rect rect, float f, int i);

    void showStatusIcons();

    void updateNotchVisible();
}
