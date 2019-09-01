package com.android.systemui.statusbar;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;

public abstract class StackScrollerDecorView extends ExpandableView {
    /* access modifiers changed from: private */
    public boolean mAnimating;
    protected View mContent;
    private boolean mIsVisible;

    /* access modifiers changed from: protected */
    public abstract View findContentView();

    public StackScrollerDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findContentView();
        setInvisible();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setOutlineProvider(null);
    }

    public boolean isTransparent() {
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible) {
        animateText(nowVisible, null);
    }

    public void performVisibilityAnimation(boolean nowVisible, Runnable onFinishedRunnable) {
        animateText(nowVisible, onFinishedRunnable);
    }

    private void animateText(boolean nowVisible, final Runnable onFinishedRunnable) {
        Interpolator interpolator;
        if (nowVisible != this.mIsVisible) {
            float endValue = nowVisible ? 1.0f : 0.0f;
            if (nowVisible) {
                interpolator = Interpolators.ALPHA_IN;
            } else {
                interpolator = Interpolators.ALPHA_OUT;
            }
            this.mAnimating = true;
            this.mContent.animate().alpha(endValue).setInterpolator(interpolator).setDuration(260).withEndAction(new Runnable() {
                public void run() {
                    boolean unused = StackScrollerDecorView.this.mAnimating = false;
                    if (onFinishedRunnable != null) {
                        onFinishedRunnable.run();
                    }
                }
            });
            this.mIsVisible = nowVisible;
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    public void setInvisible() {
        this.mContent.setAlpha(0.0f);
        this.mIsVisible = false;
    }

    public void performRemoveAnimation(long duration, float translationDirection, AnimatorListenerAdapter globalListener, Runnable onFinishedRunnable) {
        performVisibilityAnimation(false);
    }

    public void performAddAnimation(long delay, long duration, AnimatorListenerAdapter globalListener) {
        performVisibilityAnimation(true);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void cancelAnimation() {
        this.mContent.animate().cancel();
    }
}
