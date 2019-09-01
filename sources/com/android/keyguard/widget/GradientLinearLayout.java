package com.android.keyguard.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class GradientLinearLayout extends LinearLayout {
    private Drawable mGradientOverlayDrawable;

    public GradientLinearLayout(Context context) {
        super(context);
    }

    public GradientLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GradientLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        int count = canvas.saveLayer(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), null);
        super.dispatchDraw(canvas);
        if (this.mGradientOverlayDrawable != null) {
            this.mGradientOverlayDrawable.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            this.mGradientOverlayDrawable.setBounds(getLeft(), getTop(), getRight(), getBottom());
            this.mGradientOverlayDrawable.draw(canvas);
        }
        canvas.restoreToCount(count);
    }

    public void setGradientOverlayDrawable(Drawable gradientOverlayDrawable) {
        this.mGradientOverlayDrawable = gradientOverlayDrawable;
    }
}
