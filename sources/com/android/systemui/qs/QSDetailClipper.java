package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewAnimationUtils;

public class QSDetailClipper {
    /* access modifiers changed from: private */
    public Animator mAnimator;
    /* access modifiers changed from: private */
    public final View mDetail;
    private final AnimatorListenerAdapter mGoneOnEnd = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(8);
            Animator unused = QSDetailClipper.this.mAnimator = null;
        }
    };
    private final AnimatorListenerAdapter mVisibleOnStart = new AnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(0);
        }

        public void onAnimationEnd(Animator animation) {
            Animator unused = QSDetailClipper.this.mAnimator = null;
        }
    };

    public QSDetailClipper(View detail) {
        this.mDetail = detail;
    }

    public void animateCircularClip(int x, int y, boolean in, Animator.AnimatorListener listener) {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        int w = this.mDetail.getWidth() - x;
        int h = this.mDetail.getHeight() - y;
        int innerR = 0;
        if (x < 0 || w < 0 || y < 0 || h < 0) {
            innerR = Math.min(Math.min(Math.min(Math.abs(x), Math.abs(y)), Math.abs(w)), Math.abs(h));
        }
        int r = (int) Math.max((double) ((int) Math.max((double) ((int) Math.max((double) ((int) Math.ceil(Math.sqrt((double) ((x * x) + (y * y))))), Math.ceil(Math.sqrt((double) ((w * w) + (y * y)))))), Math.ceil(Math.sqrt((double) ((w * w) + (h * h)))))), Math.ceil(Math.sqrt((double) ((x * x) + (h * h)))));
        if (in) {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) innerR, (float) r);
        } else {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, (float) r, (float) innerR);
        }
        this.mAnimator.setDuration(400);
        this.mAnimator.setInterpolator(QSAnimation.INTERPOLATOR);
        if (listener != null) {
            this.mAnimator.addListener(listener);
        }
        if (in) {
            this.mAnimator.addListener(this.mVisibleOnStart);
        } else {
            this.mAnimator.addListener(this.mGoneOnEnd);
        }
        this.mAnimator.start();
    }
}
