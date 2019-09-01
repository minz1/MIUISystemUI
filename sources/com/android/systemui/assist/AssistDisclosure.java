package com.android.systemui.assist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class AssistDisclosure {
    private final Context mContext;
    private final Handler mHandler;
    private Runnable mShowRunnable = new Runnable() {
        public void run() {
            AssistDisclosure.this.show();
        }
    };
    private AssistDisclosureView mView;
    private boolean mViewAdded;
    private final WindowManager mWm;

    private class AssistDisclosureView extends View implements ValueAnimator.AnimatorUpdateListener {
        private int mAlpha = 0;
        private final ValueAnimator mAlphaInAnimator = ValueAnimator.ofInt(new int[]{0, 222}).setDuration(400);
        private final ValueAnimator mAlphaOutAnimator;
        private final AnimatorSet mAnimator;
        private final Paint mPaint = new Paint();
        private final Paint mShadowPaint = new Paint();
        private float mShadowThickness;
        private float mThickness;

        public AssistDisclosureView(Context context) {
            super(context);
            this.mAlphaInAnimator.addUpdateListener(this);
            this.mAlphaInAnimator.setInterpolator(Interpolators.CUSTOM_40_40);
            this.mAlphaOutAnimator = ValueAnimator.ofInt(new int[]{222, 0}).setDuration(300);
            this.mAlphaOutAnimator.addUpdateListener(this);
            this.mAlphaOutAnimator.setInterpolator(Interpolators.CUSTOM_40_40);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.play(this.mAlphaInAnimator).before(this.mAlphaOutAnimator);
            this.mAnimator.addListener(new AnimatorListenerAdapter(AssistDisclosure.this) {
                boolean mCancelled;

                public void onAnimationStart(Animator animation) {
                    this.mCancelled = false;
                }

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    if (!this.mCancelled) {
                        AssistDisclosure.this.hide();
                    }
                }
            });
            PorterDuffXfermode srcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
            this.mPaint.setColor(-1);
            this.mPaint.setXfermode(srcMode);
            this.mShadowPaint.setColor(-12303292);
            this.mShadowPaint.setXfermode(srcMode);
            this.mThickness = getResources().getDimension(R.dimen.assist_disclosure_thickness);
            this.mShadowThickness = getResources().getDimension(R.dimen.assist_disclosure_shadow_thickness);
        }

        /* access modifiers changed from: protected */
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            startAnimation();
            sendAccessibilityEvent(16777216);
        }

        /* access modifiers changed from: protected */
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mAnimator.cancel();
            this.mAlpha = 0;
        }

        private void startAnimation() {
            this.mAnimator.cancel();
            this.mAnimator.start();
        }

        /* access modifiers changed from: protected */
        public void onDraw(Canvas canvas) {
            this.mPaint.setAlpha(this.mAlpha);
            this.mShadowPaint.setAlpha(this.mAlpha / 4);
            drawGeometry(canvas, this.mShadowPaint, this.mShadowThickness);
            drawGeometry(canvas, this.mPaint, 0.0f);
        }

        private void drawGeometry(Canvas canvas, Paint paint, float padding) {
            int width = getWidth();
            int height = getHeight();
            float thickness = this.mThickness;
            Canvas canvas2 = canvas;
            Paint paint2 = paint;
            float f = padding;
            drawBeam(canvas2, 0.0f, ((float) height) - thickness, (float) width, (float) height, paint2, f);
            drawBeam(canvas2, 0.0f, 0.0f, thickness, ((float) height) - thickness, paint2, f);
            drawBeam(canvas2, ((float) width) - thickness, 0.0f, (float) width, ((float) height) - thickness, paint2, f);
            drawBeam(canvas2, thickness, 0.0f, ((float) width) - thickness, thickness, paint2, f);
        }

        private void drawBeam(Canvas canvas, float left, float top, float right, float bottom, Paint paint, float padding) {
            canvas.drawRect(left - padding, top - padding, right + padding, bottom + padding, paint);
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            if (animation == this.mAlphaOutAnimator) {
                this.mAlpha = ((Integer) this.mAlphaOutAnimator.getAnimatedValue()).intValue();
            } else if (animation == this.mAlphaInAnimator) {
                this.mAlpha = ((Integer) this.mAlphaInAnimator.getAnimatedValue()).intValue();
            }
            invalidate();
        }
    }

    public AssistDisclosure(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
    }

    public void postShow() {
        this.mHandler.removeCallbacks(this.mShowRunnable);
        this.mHandler.post(this.mShowRunnable);
    }

    /* access modifiers changed from: private */
    public void show() {
        if (this.mView == null) {
            this.mView = new AssistDisclosureView(this.mContext);
        }
        if (!this.mViewAdded) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(2015, 17302792, -3);
            lp.setTitle("AssistDisclosure");
            this.mWm.addView(this.mView, lp);
            this.mViewAdded = true;
        }
    }

    /* access modifiers changed from: private */
    public void hide() {
        if (this.mViewAdded) {
            this.mWm.removeView(this.mView);
            this.mViewAdded = false;
        }
    }
}
