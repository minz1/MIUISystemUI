package com.android.systemui.stackdivider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class DividerHandleView extends View {
    private static final Property<DividerHandleView, Integer> HEIGHT_PROPERTY = new Property<DividerHandleView, Integer>(Integer.class, "height") {
        public Integer get(DividerHandleView object) {
            return Integer.valueOf(object.mCurrentHeight);
        }

        public void set(DividerHandleView object, Integer value) {
            int unused = object.mCurrentHeight = value.intValue();
            object.invalidate();
        }
    };
    private static final Property<DividerHandleView, Integer> WIDTH_PROPERTY = new Property<DividerHandleView, Integer>(Integer.class, "width") {
        public Integer get(DividerHandleView object) {
            return Integer.valueOf(object.mCurrentWidth);
        }

        public void set(DividerHandleView object, Integer value) {
            int unused = object.mCurrentWidth = value.intValue();
            object.invalidate();
        }
    };
    /* access modifiers changed from: private */
    public AnimatorSet mAnimator;
    private final int mCircleDiameter;
    /* access modifiers changed from: private */
    public int mCurrentHeight;
    /* access modifiers changed from: private */
    public int mCurrentWidth;
    private final int mHeight;
    private final Paint mPaint = new Paint();
    private boolean mTouching;
    private final int mWidth;

    public DividerHandleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint.setColor(getResources().getColor(R.color.docked_divider_handle, null));
        this.mPaint.setAntiAlias(true);
        this.mWidth = getResources().getDimensionPixelSize(R.dimen.docked_divider_handle_width);
        this.mHeight = getResources().getDimensionPixelSize(R.dimen.docked_divider_handle_height);
        this.mCurrentWidth = this.mWidth;
        this.mCurrentHeight = this.mHeight;
        this.mCircleDiameter = (this.mWidth + this.mHeight) / 3;
    }

    public void setTouching(boolean touching, boolean animate) {
        if (touching != this.mTouching) {
            if (this.mAnimator != null) {
                this.mAnimator.cancel();
                this.mAnimator = null;
            }
            if (!animate) {
                if (touching) {
                    this.mCurrentWidth = this.mCircleDiameter;
                    this.mCurrentHeight = this.mCircleDiameter;
                } else {
                    this.mCurrentWidth = this.mWidth;
                    this.mCurrentHeight = this.mHeight;
                }
                invalidate();
            } else {
                animateToTarget(touching ? this.mCircleDiameter : this.mWidth, touching ? this.mCircleDiameter : this.mHeight, touching);
            }
            this.mTouching = touching;
        }
    }

    private void animateToTarget(int targetWidth, int targetHeight, boolean touching) {
        long j;
        Interpolator interpolator;
        ObjectAnimator widthAnimator = ObjectAnimator.ofInt(this, WIDTH_PROPERTY, new int[]{this.mCurrentWidth, targetWidth});
        ObjectAnimator heightAnimator = ObjectAnimator.ofInt(this, HEIGHT_PROPERTY, new int[]{this.mCurrentHeight, targetHeight});
        this.mAnimator = new AnimatorSet();
        this.mAnimator.playTogether(new Animator[]{widthAnimator, heightAnimator});
        AnimatorSet animatorSet = this.mAnimator;
        if (touching) {
            j = 150;
        } else {
            j = 200;
        }
        animatorSet.setDuration(j);
        AnimatorSet animatorSet2 = this.mAnimator;
        if (touching) {
            interpolator = Interpolators.TOUCH_RESPONSE;
        } else {
            interpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        animatorSet2.setInterpolator(interpolator);
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                AnimatorSet unused = DividerHandleView.this.mAnimator = null;
            }
        });
        this.mAnimator.start();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int left = (getWidth() / 2) - (this.mCurrentWidth / 2);
        int top = (getHeight() / 2) - (this.mCurrentHeight / 2);
        int radius = Math.min(this.mCurrentWidth, this.mCurrentHeight) / 2;
        canvas.drawRoundRect((float) left, (float) top, (float) (this.mCurrentWidth + left), (float) (this.mCurrentHeight + top), (float) radius, (float) radius, this.mPaint);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
