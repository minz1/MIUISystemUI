package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import com.android.systemui.R;

public class CaretDrawable extends Drawable {
    private final int mCaretHeight;
    private Paint mCaretPaint = new Paint();
    private float mCaretProgress = 0.0f;
    private final int mCaretWidth;
    private Path mPath = new Path();
    private Paint mShadowPaint = new Paint();

    public CaretDrawable(Context context) {
        Resources res = context.getResources();
        int strokeWidth = res.getDimensionPixelSize(R.dimen.qs_panel_expand_indicator_stroke_width);
        int shadowSpread = res.getDimensionPixelSize(R.dimen.qs_panel_expand_indicator_shadow_spread);
        this.mCaretPaint.setColor(res.getColor(R.color.qs_panel_expand_indicator_color));
        this.mCaretPaint.setAntiAlias(true);
        this.mCaretPaint.setStrokeWidth((float) (strokeWidth + shadowSpread));
        this.mCaretPaint.setStyle(Paint.Style.STROKE);
        this.mCaretPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mCaretPaint.setStrokeJoin(Paint.Join.MITER);
        this.mShadowPaint.setColor(res.getColor(R.color.qs_tile_divider));
        this.mShadowPaint.setAntiAlias(true);
        this.mShadowPaint.setStrokeWidth((float) ((shadowSpread * 2) + strokeWidth));
        this.mShadowPaint.setStyle(Paint.Style.STROKE);
        this.mShadowPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mShadowPaint.setStrokeJoin(Paint.Join.ROUND);
        this.mCaretWidth = res.getDimensionPixelSize(R.dimen.qs_panel_expand_indicator_width);
        this.mCaretHeight = res.getDimensionPixelSize(R.dimen.qs_panel_expand_indicator_height);
    }

    public int getIntrinsicWidth() {
        return this.mCaretWidth;
    }

    public int getIntrinsicHeight() {
        return this.mCaretHeight;
    }

    public void draw(Canvas canvas) {
        if (Float.compare((float) this.mCaretPaint.getAlpha(), 0.0f) != 0) {
            float width = ((float) getBounds().width()) - this.mShadowPaint.getStrokeWidth();
            float height = ((float) getBounds().height()) - this.mShadowPaint.getStrokeWidth();
            float left = ((float) getBounds().left) + (this.mShadowPaint.getStrokeWidth() / 2.0f);
            float top = ((float) getBounds().top) + ((height - (this.mShadowPaint.getStrokeWidth() / 3.0f)) / 2.0f);
            float caretHeight = height / 4.0f;
            this.mPath.reset();
            this.mPath.moveTo(left, ((1.0f - getNormalizedCaretProgress()) * caretHeight) + top);
            this.mPath.lineTo((width / 2.0f) + left, (getNormalizedCaretProgress() * caretHeight) + top);
            this.mPath.lineTo(left + width, ((1.0f - getNormalizedCaretProgress()) * caretHeight) + top);
            canvas.drawPath(this.mPath, this.mCaretPaint);
        }
    }

    public void setCaretProgress(float progress) {
        this.mCaretProgress = progress;
        invalidateSelf();
    }

    public float getNormalizedCaretProgress() {
        return (this.mCaretProgress - -1.0f) / 2.0f;
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
        this.mCaretPaint.setAlpha(alpha);
        this.mShadowPaint.setAlpha(alpha);
        invalidateSelf();
    }

    public void setColorFilter(ColorFilter cf) {
    }
}
