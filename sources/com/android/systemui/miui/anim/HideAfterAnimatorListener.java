package com.android.systemui.miui.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class HideAfterAnimatorListener extends AnimatorListenerAdapter {
    private boolean mCanceled;
    private View mView;

    public HideAfterAnimatorListener(View v) {
        this.mView = v;
    }

    public void onAnimationCancel(Animator animation) {
        this.mCanceled = true;
    }

    public void onAnimationEnd(Animator animation) {
        if (!this.mCanceled) {
            this.mView.setVisibility(8);
        }
    }
}
