package com.android.systemui.miui.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public class CenterTextDrawable extends Drawable {
    private Paint mPaint = new Paint(1);
    private String mText = "";

    public CenterTextDrawable() {
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mPaint.setFakeBoldText(false);
    }

    public void setText(String text) {
        this.mText = text;
        invalidateSelf();
    }

    public void draw(Canvas canvas) {
        if (!TextUtils.isEmpty(this.mText)) {
            canvas.drawText(this.mText, (float) (getBounds().width() / 2), (float) ((int) (((float) (getBounds().height() / 2)) - ((this.mPaint.descent() + this.mPaint.ascent()) / 2.0f))), this.mPaint);
        }
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public int getAlpha() {
        return this.mPaint.getAlpha();
    }

    public void setTextColor(int color) {
        this.mPaint.setColor(color);
    }

    public void setTextSize(float textSize) {
        this.mPaint.setTextSize(textSize);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    public int getOpacity() {
        return -3;
    }
}
