package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.RecentsEventBus;

public class AnimFirstTaskViewAlphaEvent extends RecentsEventBus.Event {
    public final float mAlpha;
    public final boolean mKeepAlphaWhenRelayout;
    public final boolean mWithAnim;

    public AnimFirstTaskViewAlphaEvent(float alpha, boolean withAnim) {
        this.mAlpha = alpha;
        this.mWithAnim = withAnim;
        this.mKeepAlphaWhenRelayout = false;
    }

    public AnimFirstTaskViewAlphaEvent(float alpha, boolean withAnim, boolean keepAlphaWhenRelayout) {
        this.mAlpha = alpha;
        this.mWithAnim = withAnim;
        this.mKeepAlphaWhenRelayout = keepAlphaWhenRelayout;
    }

    public String description() {
        return "mAlpha=" + this.mAlpha + " mWithAnim=" + this.mWithAnim + " mKeepAlphaWhenRelayout=" + this.mKeepAlphaWhenRelayout;
    }
}
