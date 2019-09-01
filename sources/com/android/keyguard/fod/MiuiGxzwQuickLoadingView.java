package com.android.keyguard.fod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import com.android.systemui.R;

public class MiuiGxzwQuickLoadingView extends View {
    private float mCurrentLoadingRadius = this.mLoadingOriginalRadius;
    private boolean mLoading = false;
    private float mLoadingMaxRadius;
    private float mLoadingOriginalRadius;
    private Paint mPaint;

    public MiuiGxzwQuickLoadingView(Context context, float itemRadius) {
        super(context);
        this.mLoadingOriginalRadius = itemRadius;
        initView();
    }

    public void setCurrentLoadingRadius(float radius) {
        this.mCurrentLoadingRadius = radius;
        invalidate();
    }

    public void setLoading(boolean loading) {
        this.mLoading = loading;
        this.mCurrentLoadingRadius = this.mLoadingOriginalRadius;
        invalidate();
    }

    public float getLoadingOriginalRadius() {
        return this.mLoadingOriginalRadius;
    }

    public float getLoadingMaxRadius() {
        return this.mLoadingMaxRadius;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        if (this.mLoading) {
            this.mPaint.setStyle(Paint.Style.FILL);
            this.mPaint.setColor(1306978022);
            canvas.drawCircle((float) centerX, (float) centerY, this.mCurrentLoadingRadius, this.mPaint);
            return;
        }
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(452984831);
        canvas.drawCircle((float) centerX, (float) centerY, this.mCurrentLoadingRadius, this.mPaint);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(1308622847);
        canvas.drawCircle((float) centerX, (float) centerY, this.mCurrentLoadingRadius, this.mPaint);
    }

    private void initView() {
        updatePixelSize();
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setStrokeWidth(1.0f);
    }

    private void updatePixelSize() {
        this.mLoadingMaxRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_loading_max_radius);
    }
}
