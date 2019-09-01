package com.android.settingslib.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.settingslib.R;

public class AppearAnimationUtils implements AppearAnimationCreator<View> {
    protected boolean mAppearing;
    protected final float mDelayScale;
    private final long mDuration;
    private final Interpolator mInterpolator;
    private final AppearAnimationProperties mProperties;
    protected RowTranslationScaler mRowTranslationScaler;
    private final float mStartTranslation;

    public class AppearAnimationProperties {
        public long[][] delays;
        public int maxDelayColIndex;
        public int maxDelayRowIndex;

        public AppearAnimationProperties() {
        }
    }

    public interface RowTranslationScaler {
        float getRowTranslationScale(int i, int i2);
    }

    public AppearAnimationUtils(Context ctx) {
        this(ctx, 220, 1.0f, 1.0f, AnimationUtils.loadInterpolator(ctx, 17563662));
    }

    public AppearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator) {
        this.mProperties = new AppearAnimationProperties();
        this.mInterpolator = interpolator;
        this.mStartTranslation = ((float) ctx.getResources().getDimensionPixelOffset(R.dimen.appear_y_translation_start)) * translationScaleFactor;
        this.mDelayScale = delayScaleFactor;
        this.mDuration = duration;
        this.mAppearing = true;
    }

    public void startAnimation2d(View[][] objects, Runnable finishListener) {
        startAnimation2d(objects, finishListener, this);
    }

    public void startAnimation(View[] objects, Runnable finishListener) {
        startAnimation(objects, finishListener, this);
    }

    public <T> void startAnimation2d(T[][] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        startAnimations(getDelays(objects), objects, finishListener, creator);
    }

    public <T> void startAnimation(T[] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        startAnimations(getDelays(objects), objects, finishListener, creator);
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        float translationScale;
        AppearAnimationProperties appearAnimationProperties = properties;
        if (appearAnimationProperties.maxDelayRowIndex == -1 || appearAnimationProperties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < appearAnimationProperties.delays.length; row++) {
            long delay = appearAnimationProperties.delays[row][0];
            Runnable endRunnable = null;
            if (appearAnimationProperties.maxDelayRowIndex == row && appearAnimationProperties.maxDelayColIndex == 0) {
                endRunnable = finishListener;
            }
            Runnable endRunnable2 = endRunnable;
            if (this.mRowTranslationScaler != null) {
                translationScale = this.mRowTranslationScaler.getRowTranslationScale(row, appearAnimationProperties.delays.length);
            } else {
                translationScale = 1.0f;
            }
            float translation = translationScale * this.mStartTranslation;
            float f = translation;
            creator.createAnimation(objects[row], delay, this.mDuration, this.mAppearing ? translation : -translation, this.mAppearing, this.mInterpolator, endRunnable2);
        }
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[][] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        float translationScale;
        AppearAnimationProperties appearAnimationProperties = properties;
        if (appearAnimationProperties.maxDelayRowIndex == -1 || appearAnimationProperties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < appearAnimationProperties.delays.length; row++) {
            long[] columns = appearAnimationProperties.delays[row];
            if (this.mRowTranslationScaler != null) {
                translationScale = this.mRowTranslationScaler.getRowTranslationScale(row, appearAnimationProperties.delays.length);
            } else {
                translationScale = 1.0f;
            }
            float translation = this.mStartTranslation * translationScale;
            for (int col = 0; col < columns.length; col++) {
                long delay = columns[col];
                Runnable endRunnable = null;
                if (appearAnimationProperties.maxDelayRowIndex == row && appearAnimationProperties.maxDelayColIndex == col) {
                    endRunnable = finishListener;
                }
                creator.createAnimation(objects[row][col], delay, this.mDuration, this.mAppearing ? translation : -translation, this.mAppearing, this.mInterpolator, endRunnable);
            }
        }
    }

    private <T> AppearAnimationProperties getDelays(T[] items) {
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[items.length][];
        long maxDelay = -1;
        for (int row = 0; row < items.length; row++) {
            this.mProperties.delays[row] = new long[1];
            long delay = calculateDelay(row, 0);
            this.mProperties.delays[row][0] = delay;
            if (items[row] != null && delay > maxDelay) {
                maxDelay = delay;
                this.mProperties.maxDelayColIndex = 0;
                this.mProperties.maxDelayRowIndex = row;
            }
        }
        return this.mProperties;
    }

    private <T> AppearAnimationProperties getDelays(T[][] items) {
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[items.length][];
        long maxDelay = -1;
        int row = 0;
        while (row < items.length) {
            T[] columns = items[row];
            this.mProperties.delays[row] = new long[columns.length];
            long maxDelay2 = maxDelay;
            for (int col = 0; col < columns.length; col++) {
                long delay = calculateDelay(row, col);
                this.mProperties.delays[row][col] = delay;
                if (items[row][col] != null && delay > maxDelay2) {
                    maxDelay2 = delay;
                    this.mProperties.maxDelayColIndex = col;
                    this.mProperties.maxDelayRowIndex = row;
                }
            }
            row++;
            maxDelay = maxDelay2;
        }
        return this.mProperties;
    }

    /* access modifiers changed from: protected */
    public long calculateDelay(int row, int col) {
        return (long) ((((double) (row * 40)) + (((double) col) * (Math.pow((double) row, 0.4d) + 0.4d) * 20.0d)) * ((double) this.mDelayScale));
    }

    public Interpolator getInterpolator() {
        return this.mInterpolator;
    }

    public float getStartTranslation() {
        return this.mStartTranslation;
    }

    public void createAnimation(View view, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable endRunnable) {
        Animator alphaAnim;
        final View view2 = view;
        final Runnable runnable = endRunnable;
        if (view2 != null) {
            float f = 1.0f;
            view2.setAlpha(appearing ? 0.0f : 1.0f);
            view2.setTranslationY(appearing ? translationY : 0.0f);
            if (!appearing) {
                f = 0.0f;
            }
            float targetAlpha = f;
            if (view.isHardwareAccelerated()) {
                alphaAnim = new RenderNodeAnimator(11, targetAlpha);
                alphaAnim.setTarget(view2);
            } else {
                alphaAnim = ObjectAnimator.ofFloat(view2, View.ALPHA, new float[]{view.getAlpha(), targetAlpha});
            }
            Animator alphaAnim2 = alphaAnim;
            Interpolator interpolator2 = interpolator;
            alphaAnim2.setInterpolator(interpolator2);
            long j = duration;
            alphaAnim2.setDuration(j);
            long j2 = delay;
            alphaAnim2.setStartDelay(j2);
            if (view.hasOverlappingRendering()) {
                view2.setLayerType(2, null);
                alphaAnim2.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        view2.setLayerType(0, null);
                    }
                });
            }
            if (runnable != null) {
                alphaAnim2.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        runnable.run();
                    }
                });
            }
            alphaAnim2.start();
            startTranslationYAnimation(view2, j2, j, appearing ? 0.0f : translationY, interpolator2);
            return;
        }
        long j3 = duration;
        Interpolator interpolator3 = interpolator;
    }

    public static void startTranslationYAnimation(View view, long delay, long duration, float endTranslationY, Interpolator interpolator) {
        RenderNodeAnimator translationAnimRt;
        if (view.isHardwareAccelerated()) {
            translationAnimRt = new RenderNodeAnimator(1, endTranslationY);
            translationAnimRt.setTarget(view);
        } else {
            translationAnimRt = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, new float[]{view.getTranslationY(), endTranslationY});
        }
        translationAnimRt.setInterpolator(interpolator);
        translationAnimRt.setDuration(duration);
        translationAnimRt.setStartDelay(delay);
        translationAnimRt.start();
    }
}
