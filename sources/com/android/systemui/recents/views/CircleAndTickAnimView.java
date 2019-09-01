package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import com.android.systemui.R;
import miui.view.animation.CubicEaseOutInterpolator;

public class CircleAndTickAnimView extends View {
    private boolean isNormalDrawableShow;
    private AnimatorSet mAnimatorSet;
    private Drawable mBackDrawable;
    private ValueAnimator mCircleAnimator;
    /* access modifiers changed from: private */
    public float mCircleRotateDegrees;
    private int mDiameter;
    private Drawable mNormalDrawable;
    private Path mTickDstPath;
    /* access modifiers changed from: private */
    public float mTickEndPoint;
    private ValueAnimator mTickEndPointAnimator;
    private Paint mTickPaint;
    private float mTickPathLength;
    private PathMeasure mTickPathMeasure;
    /* access modifiers changed from: private */
    public float mTickStartPoint;
    private ValueAnimator mTickStartPointAnimator;
    private Rect mViewRect;

    public CircleAndTickAnimView(Context context) {
        this(context, null);
    }

    public CircleAndTickAnimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleAndTickAnimView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimatorSet = new AnimatorSet();
        this.isNormalDrawableShow = true;
        this.mViewRect = new Rect();
        this.mTickPathMeasure = new PathMeasure();
        this.mTickDstPath = new Path();
        this.mTickPaint = new Paint();
        initAnimator();
        initTickPaint(context);
        stopAnimator();
    }

    private void initAnimator() {
        this.mCircleAnimator = ValueAnimator.ofFloat(new float[]{0.0f, -90.0f});
        this.mCircleAnimator.setDuration(200);
        this.mCircleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = CircleAndTickAnimView.this.mCircleRotateDegrees = ((Float) animation.getAnimatedValue()).floatValue();
                CircleAndTickAnimView.this.invalidate();
            }
        });
        this.mTickStartPointAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 0.31f});
        this.mTickStartPointAnimator.setInterpolator(new CubicEaseOutInterpolator());
        this.mTickStartPointAnimator.setStartDelay(50);
        this.mTickStartPointAnimator.setDuration(250);
        this.mTickStartPointAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = CircleAndTickAnimView.this.mTickStartPoint = ((Float) animation.getAnimatedValue()).floatValue();
                CircleAndTickAnimView.this.invalidate();
            }
        });
        this.mTickEndPointAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mTickEndPointAnimator.setInterpolator(new CubicEaseOutInterpolator());
        this.mTickEndPointAnimator.setDuration(300);
        this.mTickEndPointAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = CircleAndTickAnimView.this.mTickEndPoint = ((Float) animation.getAnimatedValue()).floatValue();
                CircleAndTickAnimView.this.invalidate();
            }
        });
        this.mAnimatorSet.play(this.mTickStartPointAnimator).with(this.mTickEndPointAnimator).after(this.mCircleAnimator);
    }

    private void initTickPaint(Context context) {
        this.mTickPaint.setStyle(Paint.Style.STROKE);
        this.mTickPaint.setStrokeWidth(5.0f);
        this.mTickPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mTickPaint.setStrokeJoin(Paint.Join.ROUND);
        this.mTickPaint.setAntiAlias(true);
        this.mTickPaint.setColor(context.getColor(R.color.recent_tick_anim_color));
    }

    private void initRightMarkPath() {
        Path path = new Path();
        path.moveTo(0.27f * ((float) this.mDiameter), 0.4f * ((float) this.mDiameter));
        path.lineTo(0.46f * ((float) this.mDiameter), 0.58f * ((float) this.mDiameter));
        path.lineTo(0.62f * ((float) this.mDiameter), 0.42f * ((float) this.mDiameter));
        this.mTickPathMeasure.setPath(path, false);
        this.mTickPathLength = this.mTickPathMeasure.getLength();
    }

    public void animatorStart(Animator.AnimatorListener listener) {
        stopAnimator();
        this.isNormalDrawableShow = false;
        this.mAnimatorSet.addListener(listener);
        this.mAnimatorSet.start();
    }

    public void setBackDrawable(int resourceIdBacks) {
        setBackDrawable(getDrawable(resourceIdBacks));
    }

    public void setBackDrawable(Drawable backs) {
        this.mBackDrawable = backs;
    }

    public void setNormalDrawable(int resourceId) {
        setNormalDrawable(getDrawable(resourceId));
    }

    public void setNormalDrawable(Drawable normalDrawable) {
        this.mNormalDrawable = normalDrawable;
    }

    private Drawable getDrawable(int resourceIds) {
        Drawable drawable = getContext().getResources().getDrawable(resourceIds);
        if (drawable == null) {
            return null;
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.mutate();
        return drawable;
    }

    public void setDrawables(int normalDrawable, int backDrawable) {
        setNormalDrawable(normalDrawable);
        setBackDrawable(backDrawable);
        this.mDiameter = Math.min(getIntrinsicWidth(), getIntrinsicHeight());
        this.mViewRect.set(0, 0, this.mDiameter, this.mDiameter);
        initRightMarkPath();
    }

    public void stopAnimator() {
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.cancel();
        }
        this.mAnimatorSet.removeAllListeners();
        this.mCircleRotateDegrees = 0.0f;
        this.mTickStartPoint = 0.0f;
        this.mTickEndPoint = 0.0f;
        this.isNormalDrawableShow = true;
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mBackDrawable != null) {
            this.mBackDrawable.setState(getDrawableState());
        }
        invalidate();
    }

    private int getIntrinsicWidth() {
        int maxWidth = 0;
        if (this.mNormalDrawable != null) {
            maxWidth = Math.max(0, this.mNormalDrawable.getIntrinsicHeight());
        }
        if (this.mBackDrawable != null) {
            return Math.max(maxWidth, this.mBackDrawable.getIntrinsicHeight());
        }
        return maxWidth;
    }

    private int getIntrinsicHeight() {
        int maxHeight = 0;
        if (this.mNormalDrawable != null) {
            maxHeight = Math.max(0, this.mNormalDrawable.getIntrinsicHeight());
        }
        if (this.mBackDrawable != null) {
            return Math.max(maxHeight, this.mBackDrawable.getIntrinsicHeight());
        }
        return maxHeight;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(this.mDiameter, this.mDiameter);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Rect outRect = new Rect();
        if (this.mNormalDrawable != null) {
            Gravity.apply(17, this.mNormalDrawable.getIntrinsicWidth(), this.mNormalDrawable.getIntrinsicHeight(), this.mViewRect, outRect);
            this.mNormalDrawable.setBounds(outRect);
            if (this.isNormalDrawableShow) {
                this.mNormalDrawable.setAlpha((int) (getAlpha() * 255.0f));
                this.mNormalDrawable.draw(canvas);
            } else {
                this.mNormalDrawable.setAlpha((int) (((this.mCircleRotateDegrees / 90.0f) + 1.0f) * 255.0f));
                canvas.save();
                canvas.rotate(this.mCircleRotateDegrees, (float) (this.mDiameter / 2), (float) (this.mDiameter / 2));
                this.mNormalDrawable.draw(canvas);
                canvas.restore();
            }
        }
        canvas.saveLayer(null, null);
        this.mTickDstPath.reset();
        this.mTickPathMeasure.getSegment(this.mTickPathLength * this.mTickStartPoint, this.mTickPathLength * this.mTickEndPoint, this.mTickDstPath, true);
        canvas.drawPath(this.mTickDstPath, this.mTickPaint);
        canvas.restore();
    }
}
