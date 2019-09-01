package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.R;

public class ScrimView extends View {
    /* access modifiers changed from: private */
    public ValueAnimator mAlphaAnimator;
    private ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener;
    private Runnable mChangeRunnable;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mDrawAsSrc;
    private Rect mExcludedRect;
    private boolean mHasExcludedArea;
    private boolean mIsEmpty;
    private final Paint mPaint;
    private int mScrimColor;
    /* access modifiers changed from: private */
    public float mViewAlpha;

    public ScrimView(Context context) {
        this(context, null);
    }

    public ScrimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPaint = new Paint();
        this.mIsEmpty = true;
        this.mViewAlpha = 1.0f;
        this.mExcludedRect = new Rect();
        this.mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = ScrimView.this.mViewAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                ScrimView.this.invalidate();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = ScrimView.this.mAlphaAnimator = null;
            }
        };
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScrimView);
        try {
            this.mScrimColor = ta.getColor(0, -16777216);
        } finally {
            ta.recycle();
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (this.mDrawAsSrc || (!this.mIsEmpty && this.mViewAlpha > 0.0f)) {
            PorterDuff.Mode mode = this.mDrawAsSrc ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER;
            int color = getScrimColorWithAlpha();
            if (!this.mHasExcludedArea) {
                canvas.drawColor(color, mode);
                return;
            }
            this.mPaint.setColor(color);
            if (this.mExcludedRect.top > 0) {
                canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) this.mExcludedRect.top, this.mPaint);
            }
            if (this.mExcludedRect.left > 0) {
                canvas.drawRect(0.0f, (float) this.mExcludedRect.top, (float) this.mExcludedRect.left, (float) this.mExcludedRect.bottom, this.mPaint);
            }
            if (this.mExcludedRect.right < getWidth()) {
                canvas.drawRect((float) this.mExcludedRect.right, (float) this.mExcludedRect.top, (float) getWidth(), (float) this.mExcludedRect.bottom, this.mPaint);
            }
            if (this.mExcludedRect.bottom < getHeight()) {
                canvas.drawRect(0.0f, (float) this.mExcludedRect.bottom, (float) getWidth(), (float) getHeight(), this.mPaint);
            }
        }
    }

    public int getScrimColorWithAlpha() {
        if (getId() != R.id.scrim_in_front) {
            return 0;
        }
        int color = this.mScrimColor;
        return Color.argb((int) (((float) Color.alpha(color)) * this.mViewAlpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    public void setDrawAsSrc(boolean asSrc) {
        PorterDuff.Mode mode;
        this.mDrawAsSrc = asSrc;
        Paint paint = this.mPaint;
        if (this.mDrawAsSrc) {
            mode = PorterDuff.Mode.SRC;
        } else {
            mode = PorterDuff.Mode.SRC_OVER;
        }
        paint.setXfermode(new PorterDuffXfermode(mode));
        invalidate();
    }

    public void setScrimColor(int color) {
        if (getId() == R.id.scrim_in_front && color != this.mScrimColor) {
            this.mIsEmpty = Color.alpha(color) == 0;
            this.mScrimColor = color;
            invalidate();
            if (this.mChangeRunnable != null) {
                this.mChangeRunnable.run();
            }
        }
    }

    public int getScrimColor() {
        return this.mScrimColor;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void animateViewAlpha(float alpha, long durationOut, Interpolator interpolator) {
        if (this.mAlphaAnimator != null) {
            this.mAlphaAnimator.cancel();
        }
        this.mAlphaAnimator = ValueAnimator.ofFloat(new float[]{this.mViewAlpha, alpha});
        this.mAlphaAnimator.addUpdateListener(this.mAlphaUpdateListener);
        this.mAlphaAnimator.addListener(this.mClearAnimatorListener);
        this.mAlphaAnimator.setInterpolator(interpolator);
        this.mAlphaAnimator.setDuration(durationOut);
        this.mAlphaAnimator.start();
    }

    public void setExcludedArea(Rect area) {
    }

    public void setChangeRunnable(Runnable changeRunnable) {
        this.mChangeRunnable = changeRunnable;
    }
}
