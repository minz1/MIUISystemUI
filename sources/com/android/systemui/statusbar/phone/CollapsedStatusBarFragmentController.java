package com.android.systemui.statusbar.phone;

import android.view.View;

public interface CollapsedStatusBarFragmentController {
    void hideSystemIconArea(boolean z, boolean z2);

    void init(CollapsedStatusBarFragment collapsedStatusBarFragment);

    boolean isStatusIconsVisible();

    void showSystemIconArea(boolean z);

    void start(View view);

    void stop();

    void updateLeftPartVisibility(boolean z, boolean z2);
}
