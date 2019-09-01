package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;

public class ViewInvertHelper {
    /* access modifiers changed from: private */
    public final Paint mDarkPaint;
    private final long mFadeDuration;
    private final ColorMatrix mGrayscaleMatrix;
    private final ColorMatrix mMatrix;
    /* access modifiers changed from: private */
    public final ArrayList<View> mTargets;

    public ViewInvertHelper(View v, long fadeDuration) {
        this(v.getContext(), fadeDuration);
        addTarget(v);
    }

    public ViewInvertHelper(Context context, long fadeDuration) {
        this.mDarkPaint = new Paint();
        this.mMatrix = new ColorMatrix();
        this.mGrayscaleMatrix = new ColorMatrix();
        this.mTargets = new ArrayList<>();
        this.mFadeDuration = fadeDuration;
    }

    public void clearTargets() {
        this.mTargets.clear();
    }

    public void addTarget(View target) {
        this.mTargets.add(target);
    }

    public void fade(final boolean invert, long delay) {
        float endIntensity = 1.0f;
        float startIntensity = invert ? 0.0f : 1.0f;
        if (!invert) {
            endIntensity = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{startIntensity, endIntensity});
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewInvertHelper.this.updateInvertPaint(((Float) animation.getAnimatedValue()).floatValue());
                for (int i = 0; i < ViewInvertHelper.this.mTargets.size(); i++) {
                    ((View) ViewInvertHelper.this.mTargets.get(i)).setLayerType(2, ViewInvertHelper.this.mDarkPaint);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (!invert) {
                    for (int i = 0; i < ViewInvertHelper.this.mTargets.size(); i++) {
                        ((View) ViewInvertHelper.this.mTargets.get(i)).setLayerType(0, null);
                    }
                }
            }
        });
        animator.setDuration(this.mFadeDuration);
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animator.setStartDelay(delay);
        animator.start();
    }

    public void update(boolean invert) {
        if (invert) {
            updateInvertPaint(1.0f);
            for (int i = 0; i < this.mTargets.size(); i++) {
                this.mTargets.get(i).setLayerType(2, this.mDarkPaint);
            }
            return;
        }
        for (int i2 = 0; i2 < this.mTargets.size(); i2++) {
            this.mTargets.get(i2).setLayerType(0, null);
        }
    }

    /* access modifiers changed from: private */
    public void updateInvertPaint(float intensity) {
        float components = 1.0f - (2.0f * intensity);
        this.mMatrix.set(new float[]{components, 0.0f, 0.0f, 0.0f, 255.0f * intensity, 0.0f, components, 0.0f, 0.0f, 255.0f * intensity, 0.0f, 0.0f, components, 0.0f, 255.0f * intensity, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
        this.mGrayscaleMatrix.setSaturation(1.0f - intensity);
        this.mMatrix.preConcat(this.mGrayscaleMatrix);
        this.mDarkPaint.setColorFilter(new ColorMatrixColorFilter(this.mMatrix));
    }

    public void setInverted(boolean invert, boolean fade, long delay) {
        if (fade) {
            fade(invert, delay);
        } else {
            update(invert);
        }
    }
}
