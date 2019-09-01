package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.util.function.Consumer;

public class NotificationDozeHelper {
    private final ColorMatrix mGrayscaleColorMatrix = new ColorMatrix();

    public void fadeGrayscale(final ImageView target, final boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationDozeHelper.this.updateGrayscale(target, ((Float) animation.getAnimatedValue()).floatValue());
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    target.setColorFilter(null);
                }
            }
        });
    }

    public void updateGrayscale(ImageView target, boolean dark) {
        updateGrayscale(target, dark ? 1.0f : 0.0f);
    }

    public void updateGrayscale(ImageView target, float darkAmount) {
        if (darkAmount > 0.0f) {
            updateGrayscaleMatrix(darkAmount);
            target.setColorFilter(new ColorMatrixColorFilter(this.mGrayscaleColorMatrix));
            return;
        }
        target.setColorFilter(null);
    }

    public void startIntensityAnimation(ValueAnimator.AnimatorUpdateListener updateListener, boolean dark, long delay, Animator.AnimatorListener listener) {
        float endIntensity = 1.0f;
        float startIntensity = dark ? 0.0f : 1.0f;
        if (!dark) {
            endIntensity = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{startIntensity, endIntensity});
        animator.addUpdateListener(updateListener);
        animator.setDuration(700);
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animator.setStartDelay(delay);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    public void setIntensityDark(final Consumer<Float> listener, boolean dark, boolean animate, long delay) {
        if (animate) {
            startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    listener.accept((Float) a.getAnimatedValue());
                }
            }, dark, delay, null);
        } else {
            listener.accept(Float.valueOf(dark ? 1.0f : 0.0f));
        }
    }

    public void updateGrayscaleMatrix(float intensity) {
        this.mGrayscaleColorMatrix.setSaturation(1.0f - intensity);
    }

    public ColorMatrix getGrayscaleColorMatrix() {
        return this.mGrayscaleColorMatrix;
    }
}
