package com.android.systemui.statusbar;

import android.animation.Animator;
import android.content.Context;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.NotificationUtils;

public class FlingAnimationUtils {
    private AnimatorProperties mAnimatorProperties;
    private float mCachedStartGradient;
    private float mCachedVelocityFactor;
    private float mHighVelocityPxPerSecond;
    private PathInterpolator mInterpolator;
    private float mLinearOutSlowInX2;
    private float mMaxLengthSeconds;
    private float mMinVelocityPxPerSecond;
    private final float mSpeedUpFactor;
    private final float mY2;

    private static class AnimatorProperties {
        long duration;
        Interpolator interpolator;

        private AnimatorProperties() {
        }
    }

    private static final class InterpolatorInterpolator implements Interpolator {
        private Interpolator mCrossfader;
        private Interpolator mInterpolator1;
        private Interpolator mInterpolator2;

        InterpolatorInterpolator(Interpolator interpolator1, Interpolator interpolator2, Interpolator crossfader) {
            this.mInterpolator1 = interpolator1;
            this.mInterpolator2 = interpolator2;
            this.mCrossfader = crossfader;
        }

        public float getInterpolation(float input) {
            float t = this.mCrossfader.getInterpolation(input);
            return ((1.0f - t) * this.mInterpolator1.getInterpolation(input)) + (this.mInterpolator2.getInterpolation(input) * t);
        }
    }

    private static final class VelocityInterpolator implements Interpolator {
        private float mDiff;
        private float mDurationSeconds;
        private float mVelocity;

        private VelocityInterpolator(float durationSeconds, float velocity, float diff) {
            this.mDurationSeconds = durationSeconds;
            this.mVelocity = velocity;
            this.mDiff = diff;
        }

        public float getInterpolation(float input) {
            return (this.mVelocity * (this.mDurationSeconds * input)) / this.mDiff;
        }
    }

    public FlingAnimationUtils(Context ctx, float maxLengthSeconds) {
        this(ctx, maxLengthSeconds, 0.0f);
    }

    public FlingAnimationUtils(Context ctx, float maxLengthSeconds, float speedUpFactor) {
        this(ctx, maxLengthSeconds, speedUpFactor, -1.0f, 1.0f);
    }

    public FlingAnimationUtils(Context ctx, float maxLengthSeconds, float speedUpFactor, float x2, float y2) {
        this.mAnimatorProperties = new AnimatorProperties();
        this.mCachedStartGradient = -1.0f;
        this.mCachedVelocityFactor = -1.0f;
        this.mMaxLengthSeconds = maxLengthSeconds;
        this.mSpeedUpFactor = speedUpFactor;
        if (x2 < 0.0f) {
            this.mLinearOutSlowInX2 = NotificationUtils.interpolate(0.35f, 0.68f, this.mSpeedUpFactor);
        } else {
            this.mLinearOutSlowInX2 = x2;
        }
        this.mY2 = y2;
        this.mMinVelocityPxPerSecond = 250.0f * ctx.getResources().getDisplayMetrics().density;
        this.mHighVelocityPxPerSecond = 3000.0f * ctx.getResources().getDisplayMetrics().density;
    }

    public void apply(Animator animator, float currValue, float endValue, float velocity) {
        apply(animator, currValue, endValue, velocity, Math.abs(endValue - currValue));
    }

    public void apply(ViewPropertyAnimator animator, float currValue, float endValue, float velocity) {
        apply(animator, currValue, endValue, velocity, Math.abs(endValue - currValue));
    }

    public void apply(Animator animator, float currValue, float endValue, float velocity, float maxDistance) {
        AnimatorProperties properties = getProperties(currValue, endValue, velocity, maxDistance);
        animator.setDuration(properties.duration);
        animator.setInterpolator(properties.interpolator);
    }

    public void apply(ViewPropertyAnimator animator, float currValue, float endValue, float velocity, float maxDistance) {
        AnimatorProperties properties = getProperties(currValue, endValue, velocity, maxDistance);
        animator.setDuration(properties.duration);
        animator.setInterpolator(properties.interpolator);
    }

    private AnimatorProperties getProperties(float currValue, float endValue, float velocity, float maxDistance) {
        float maxLengthSeconds = (float) (((double) this.mMaxLengthSeconds) * Math.sqrt((double) (Math.abs(endValue - currValue) / maxDistance)));
        float diff = Math.abs(endValue - currValue);
        float velAbs = Math.abs(velocity);
        float f = 1.0f;
        if (this.mSpeedUpFactor != 0.0f) {
            f = Math.min(velAbs / 3000.0f, 1.0f);
        }
        float velocityFactor = f;
        float startGradient = NotificationUtils.interpolate(0.75f, this.mY2 / this.mLinearOutSlowInX2, velocityFactor);
        float durationSeconds = (startGradient * diff) / velAbs;
        Interpolator slowInInterpolator = getInterpolator(startGradient, velocityFactor);
        if (durationSeconds <= maxLengthSeconds) {
            this.mAnimatorProperties.interpolator = slowInInterpolator;
        } else if (velAbs >= this.mMinVelocityPxPerSecond) {
            durationSeconds = maxLengthSeconds;
            this.mAnimatorProperties.interpolator = new InterpolatorInterpolator(new VelocityInterpolator(durationSeconds, velAbs, diff), slowInInterpolator, Interpolators.LINEAR_OUT_SLOW_IN);
        } else {
            durationSeconds = maxLengthSeconds;
            this.mAnimatorProperties.interpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        this.mAnimatorProperties.duration = (long) (1000.0f * durationSeconds);
        return this.mAnimatorProperties;
    }

    private Interpolator getInterpolator(float startGradient, float velocityFactor) {
        if (!(startGradient == this.mCachedStartGradient && velocityFactor == this.mCachedVelocityFactor)) {
            float speedup = this.mSpeedUpFactor * (1.0f - velocityFactor);
            this.mInterpolator = new PathInterpolator(speedup, speedup * startGradient, this.mLinearOutSlowInX2, this.mY2);
            this.mCachedStartGradient = startGradient;
            this.mCachedVelocityFactor = velocityFactor;
        }
        return this.mInterpolator;
    }

    public void applyDismissing(Animator animator, float currValue, float endValue, float velocity, float maxDistance) {
        AnimatorProperties properties = getDismissingProperties(currValue, endValue, velocity, maxDistance);
        animator.setDuration(properties.duration);
        animator.setInterpolator(properties.interpolator);
    }

    private AnimatorProperties getDismissingProperties(float currValue, float endValue, float velocity, float maxDistance) {
        float maxLengthSeconds = (float) (((double) this.mMaxLengthSeconds) * Math.pow((double) (Math.abs(endValue - currValue) / maxDistance), 0.5d));
        float diff = Math.abs(endValue - currValue);
        float velAbs = Math.abs(velocity);
        float y2 = calculateLinearOutFasterInY2(velAbs);
        Interpolator mLinearOutFasterIn = new PathInterpolator(0.0f, 0.0f, 0.5f, y2);
        float durationSeconds = ((y2 / 0.5f) * diff) / velAbs;
        if (durationSeconds <= maxLengthSeconds) {
            this.mAnimatorProperties.interpolator = mLinearOutFasterIn;
        } else if (velAbs >= this.mMinVelocityPxPerSecond) {
            durationSeconds = maxLengthSeconds;
            this.mAnimatorProperties.interpolator = new InterpolatorInterpolator(new VelocityInterpolator(durationSeconds, velAbs, diff), mLinearOutFasterIn, Interpolators.LINEAR_OUT_SLOW_IN);
        } else {
            durationSeconds = maxLengthSeconds;
            this.mAnimatorProperties.interpolator = Interpolators.FAST_OUT_LINEAR_IN;
        }
        this.mAnimatorProperties.duration = (long) (1000.0f * durationSeconds);
        return this.mAnimatorProperties;
    }

    private float calculateLinearOutFasterInY2(float velocity) {
        float t = Math.max(0.0f, Math.min(1.0f, (velocity - this.mMinVelocityPxPerSecond) / (this.mHighVelocityPxPerSecond - this.mMinVelocityPxPerSecond)));
        return ((1.0f - t) * 0.4f) + (0.5f * t);
    }

    public float getMinVelocityPxPerSecond() {
        return this.mMinVelocityPxPerSecond;
    }
}
