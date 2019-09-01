package com.android.systemui.statusbar.phone;

import android.view.animation.Interpolator;

public class BounceInterpolator implements Interpolator {
    public float getInterpolation(float t) {
        float t2 = t * 1.1f;
        if (t2 < 0.36363637f) {
            return 7.5625f * t2 * t2;
        }
        if (t2 < 0.72727275f) {
            float t22 = t2 - 0.54545456f;
            return (7.5625f * t22 * t22) + 0.75f;
        } else if (t2 >= 0.90909094f) {
            return 1.0f;
        } else {
            float t23 = t2 - 0.8181818f;
            return (7.5625f * t23 * t23) + 0.9375f;
        }
    }
}
