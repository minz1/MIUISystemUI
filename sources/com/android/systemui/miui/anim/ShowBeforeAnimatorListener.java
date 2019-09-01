package com.android.systemui.miui.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowBeforeAnimatorListener extends AnimatorListenerAdapter {
    private int mAlphaDuration = 300;
    private Interpolator mAlphaInterpolator;
    private boolean mAnimateAlpha;
    protected final List<View> mViews = new ArrayList();

    public ShowBeforeAnimatorListener(View... views) {
        Collections.addAll(this.mViews, views);
    }

    public void onAnimationStart(Animator animation) {
        super.onAnimationStart(animation);
        for (View v : this.mViews) {
            apply(v);
        }
    }

    public ShowBeforeAnimatorListener animateAlpha(boolean animate) {
        this.mAnimateAlpha = animate;
        return this;
    }

    public ShowBeforeAnimatorListener setAlphaDuration(int duration) {
        this.mAlphaDuration = duration;
        return this;
    }

    public ShowBeforeAnimatorListener setAlphaInterpolator(Interpolator interpolator) {
        this.mAlphaInterpolator = interpolator;
        return this;
    }

    private void apply(View view) {
        if (this.mAnimateAlpha) {
            view.animate().cancel();
            view.animate().alpha(1.0f).setDuration((long) this.mAlphaDuration).setInterpolator(this.mAlphaInterpolator).withLayer().start();
            return;
        }
        view.setVisibility(0);
    }
}
