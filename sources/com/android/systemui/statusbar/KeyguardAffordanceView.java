package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DisplayListCanvas;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class KeyguardAffordanceView extends ImageView {
    /* access modifiers changed from: private */
    public ValueAnimator mAlphaAnimator;
    private AnimatorListenerAdapter mAlphaEndListener;
    private int mCenterX;
    private int mCenterY;
    /* access modifiers changed from: private */
    public ValueAnimator mCircleAnimator;
    private int mCircleColor;
    private AnimatorListenerAdapter mCircleEndListener;
    private final Paint mCirclePaint;
    private float mCircleRadius;
    private float mCircleStartRadius;
    private AnimatorListenerAdapter mClipEndListener;
    private final ArgbEvaluator mColorInterpolator;
    private boolean mFinishing;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private CanvasProperty<Float> mHwCenterX;
    private CanvasProperty<Float> mHwCenterY;
    private CanvasProperty<Paint> mHwCirclePaint;
    private CanvasProperty<Float> mHwCircleRadius;
    /* access modifiers changed from: private */
    public float mImageScale;
    private final int mInverseColor;
    private boolean mLaunchingAffordance;
    private float mMaxCircleSize;
    private final int mMinBackgroundRadius;
    private final int mNormalColor;
    /* access modifiers changed from: private */
    public Animator mPreviewClipper;
    private View mPreviewView;
    private float mRestingAlpha;
    /* access modifiers changed from: private */
    public ValueAnimator mScaleAnimator;
    private AnimatorListenerAdapter mScaleEndListener;
    private boolean mShouldTint;
    private boolean mSupportHardware;
    private int[] mTempPoint;

    public KeyguardAffordanceView(Context context) {
        this(context, null);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTempPoint = new int[2];
        this.mImageScale = 1.0f;
        this.mRestingAlpha = 1.0f;
        this.mShouldTint = true;
        this.mClipEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Animator unused = KeyguardAffordanceView.this.mPreviewClipper = null;
            }
        };
        this.mCircleEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = KeyguardAffordanceView.this.mCircleAnimator = null;
            }
        };
        this.mScaleEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = KeyguardAffordanceView.this.mScaleAnimator = null;
            }
        };
        this.mAlphaEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = KeyguardAffordanceView.this.mAlphaAnimator = null;
            }
        };
        this.mCirclePaint = new Paint();
        this.mCirclePaint.setAntiAlias(true);
        this.mCircleColor = -1;
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mNormalColor = -1;
        this.mInverseColor = -16777216;
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mColorInterpolator = new ArgbEvaluator();
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.3f);
    }

    public void setImageDrawable(Drawable drawable, boolean tint) {
        super.setImageDrawable(drawable);
        this.mShouldTint = tint;
        updateIconColor();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mMaxCircleSize = getMaxCircleSize();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        this.mSupportHardware = canvas.isHardwareAccelerated();
        drawBackgroundCircle(canvas);
        canvas.save();
        canvas.scale(this.mImageScale, this.mImageScale, (float) (getWidth() / 2), (float) (getHeight() / 2));
        super.onDraw(canvas);
        canvas.restore();
    }

    private void updateIconColor() {
        if (this.mShouldTint) {
            getDrawable().mutate().setColorFilter(((Integer) this.mColorInterpolator.evaluate(Math.min(1.0f, this.mCircleRadius / ((float) this.mMinBackgroundRadius)), Integer.valueOf(this.mNormalColor), Integer.valueOf(this.mInverseColor))).intValue(), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void drawBackgroundCircle(Canvas canvas) {
        if (this.mCircleRadius <= 0.0f && !this.mFinishing) {
            return;
        }
        if (!this.mFinishing || !this.mSupportHardware || this.mHwCenterX == null) {
            updateCircleColor();
            canvas.drawCircle((float) this.mCenterX, (float) this.mCenterY, this.mCircleRadius, this.mCirclePaint);
            return;
        }
        ((DisplayListCanvas) canvas).drawCircle(this.mHwCenterX, this.mHwCenterY, this.mHwCircleRadius, this.mHwCirclePaint);
    }

    private void updateCircleColor() {
        float fraction = 0.5f + (Math.max(0.0f, Math.min(1.0f, (this.mCircleRadius - ((float) this.mMinBackgroundRadius)) / (((float) this.mMinBackgroundRadius) * 0.5f))) * 0.5f);
        if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
            fraction *= 1.0f - (Math.max(0.0f, this.mCircleRadius - this.mCircleStartRadius) / (this.mMaxCircleSize - this.mCircleStartRadius));
        }
        this.mCirclePaint.setColor(Color.argb((int) (((float) Color.alpha(this.mCircleColor)) * fraction), Color.red(this.mCircleColor), Color.green(this.mCircleColor), Color.blue(this.mCircleColor)));
    }

    public void instantFinishAnimation() {
        cancelAnimator(this.mPreviewClipper);
        if (this.mPreviewView != null) {
            this.mPreviewView.setClipBounds(null);
            this.mPreviewView.setVisibility(0);
        }
        this.mCircleRadius = getMaxCircleSize();
        setImageAlpha(0.0f, false);
        invalidate();
    }

    private float getMaxCircleSize() {
        getLocationInWindow(this.mTempPoint);
        float width = (float) (this.mTempPoint[0] + this.mCenterX);
        return (float) Math.hypot((double) Math.max(((float) getRootView().getWidth()) - width, width), (double) ((float) (this.mTempPoint[1] + this.mCenterY)));
    }

    private void cancelAnimator(Animator animator) {
        if (animator != null) {
            animator.cancel();
        }
    }

    public void setImageScale(float imageScale, boolean animate) {
        setImageScale(imageScale, animate, -1, null);
    }

    public void setImageScale(float imageScale, boolean animate, long duration, Interpolator interpolator) {
        Interpolator interpolator2;
        cancelAnimator(this.mScaleAnimator);
        if (!animate) {
            this.mImageScale = imageScale;
            invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mImageScale, imageScale});
        this.mScaleAnimator = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = KeyguardAffordanceView.this.mImageScale = ((Float) animation.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animator.addListener(this.mScaleEndListener);
        if (interpolator == null) {
            if (imageScale == 0.0f) {
                interpolator2 = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator2 = Interpolators.LINEAR_OUT_SLOW_IN;
            }
            interpolator = interpolator2;
        }
        animator.setInterpolator(interpolator);
        if (duration == -1) {
            duration = (long) (200.0f * Math.min(1.0f, Math.abs(this.mImageScale - imageScale) / 0.19999999f));
        }
        animator.setDuration(duration);
        animator.start();
    }

    public float getRestingAlpha() {
        return this.mRestingAlpha;
    }

    public void setImageAlpha(float alpha, boolean animate) {
        setImageAlpha(alpha, animate, -1, null, null);
    }

    public void setImageAlpha(float alpha, boolean animate, long duration, Interpolator interpolator, Runnable runnable) {
        Interpolator interpolator2;
        cancelAnimator(this.mAlphaAnimator);
        float alpha2 = this.mLaunchingAffordance ? 0.0f : alpha;
        int endAlpha = (int) (alpha2 * 255.0f);
        final Drawable background = getBackground();
        if (!animate) {
            if (background != null) {
                background.mutate().setAlpha(endAlpha);
            }
            setImageAlpha(endAlpha);
            return;
        }
        int currentAlpha = getImageAlpha();
        ValueAnimator animator = ValueAnimator.ofInt(new int[]{currentAlpha, endAlpha});
        this.mAlphaAnimator = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int alpha = ((Integer) animation.getAnimatedValue()).intValue();
                if (background != null) {
                    background.mutate().setAlpha(alpha);
                }
                KeyguardAffordanceView.this.setImageAlpha(alpha);
            }
        });
        animator.addListener(this.mAlphaEndListener);
        if (interpolator == null) {
            if (alpha2 == 0.0f) {
                interpolator2 = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator2 = Interpolators.LINEAR_OUT_SLOW_IN;
            }
            interpolator = interpolator2;
        }
        animator.setInterpolator(interpolator);
        if (duration == -1) {
            duration = (long) (200.0f * Math.min(1.0f, ((float) Math.abs(currentAlpha - endAlpha)) / 255.0f));
        }
        animator.setDuration(duration);
        if (runnable != null) {
            animator.addListener(getEndListener(runnable));
        }
        animator.start();
    }

    private Animator.AnimatorListener getEndListener(final Runnable runnable) {
        return new AnimatorListenerAdapter() {
            boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    runnable.run();
                }
            }
        };
    }

    public boolean performClick() {
        if (isClickable()) {
            return super.performClick();
        }
        return false;
    }

    public void setLaunchingAffordance(boolean launchingAffordance) {
        this.mLaunchingAffordance = launchingAffordance;
    }
}
