package com.android.keyguard.fod;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.android.systemui.R;

public class MiuiGxzwQuickTeachView extends View {
    private float mCicleRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_circle_radius);
    private float mItemRadius;
    private Paint mPaint = new Paint();
    private ValueAnimator mValueAnimator;

    public MiuiGxzwQuickTeachView(Context context, float itemRadius) {
        super(context);
        this.mItemRadius = itemRadius;
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mPaint.setStrokeWidth(this.mItemRadius * 2.0f);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mValueAnimator != null && this.mValueAnimator.isRunning()) {
            float value = ((Float) this.mValueAnimator.getAnimatedValue()).floatValue();
            if (value <= this.mCicleRadius * 2.0f) {
                if (value > this.mCicleRadius) {
                    value = this.mCicleRadius;
                } else if (value < 0.0f) {
                    value = 0.0f;
                }
                this.mPaint.setStyle(Paint.Style.STROKE);
                RectF rect = new RectF(0.0f, this.mItemRadius + value, this.mItemRadius * 2.0f, this.mCicleRadius + this.mItemRadius);
                LinearGradient lg = new LinearGradient(rect.centerX(), rect.top, rect.centerX(), this.mItemRadius + rect.bottom, -13264897, 3512319, Shader.TileMode.CLAMP);
                this.mPaint.setShader(lg);
                canvas.drawLine(rect.centerX(), rect.top, rect.centerX(), rect.bottom, this.mPaint);
                this.mPaint.setShader(null);
                this.mPaint.setStyle(Paint.Style.FILL);
                this.mPaint.setColor(-13264897);
                canvas.drawCircle(this.mItemRadius, this.mItemRadius + value, this.mItemRadius, this.mPaint);
            }
        }
    }

    public void startTeachAnim() {
        if (this.mValueAnimator != null) {
            this.mValueAnimator.cancel();
        }
        this.mValueAnimator = ValueAnimator.ofFloat(new float[]{this.mCicleRadius * 3.0f, -this.mCicleRadius});
        this.mValueAnimator.setDuration(2000);
        this.mValueAnimator.setInterpolator(new LinearInterpolator());
        this.mValueAnimator.setRepeatMode(1);
        this.mValueAnimator.setRepeatCount(-1);
        this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiGxzwQuickTeachView.this.invalidate();
            }
        });
        this.mValueAnimator.start();
        invalidate();
    }

    public void stopTeachAnim() {
        if (this.mValueAnimator != null) {
            this.mValueAnimator.cancel();
        }
        this.mValueAnimator = null;
        invalidate();
    }
}
