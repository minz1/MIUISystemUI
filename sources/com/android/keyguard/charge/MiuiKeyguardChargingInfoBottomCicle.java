package com.android.keyguard.charge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class MiuiKeyguardChargingInfoBottomCicle extends View {
    private int mHeight;
    private Paint mPaint;
    private float mRadius;
    private int mWidth;
    private float x;
    private float y;

    public MiuiKeyguardChargingInfoBottomCicle(Context context) {
        super(context);
        init();
    }

    public MiuiKeyguardChargingInfoBottomCicle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiuiKeyguardChargingInfoBottomCicle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.mPaint = new Paint();
        this.mPaint.setColor(getResources().getColor(R.color.keyguard_charging_info_cicle_color));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setDither(true);
        this.mPaint.setStrokeWidth(20.0f);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mRadius = 20.0f;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == 1073741824 && heightMode == 1073741824) {
            this.mRadius = (float) (widthSize / 2);
            this.x = (float) (widthSize / 2);
            this.y = (float) (heightSize / 2);
            this.mWidth = widthSize;
            this.mHeight = heightSize;
        }
        if (widthMode == Integer.MIN_VALUE && heightMode == Integer.MIN_VALUE) {
            this.mWidth = (int) (this.mRadius * 2.0f);
            this.mHeight = (int) (this.mRadius * 2.0f);
            this.x = this.mRadius;
            this.y = this.mRadius;
        }
        setMeasuredDimension(this.mWidth, this.mHeight);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(this.x, this.y, this.mRadius, this.mPaint);
    }
}
