package com.android.systemui.miui.volume;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MiuiVerticalVolumeTimerSeekBar extends MiuiVolumeTimerSeekBar {
    public MiuiVerticalVolumeTimerSeekBar(Context context) {
        this(context, null);
    }

    public MiuiVerticalVolumeTimerSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiVerticalVolumeTimerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutDirection(0);
        this.mInjector.setVertical(true);
    }

    /* access modifiers changed from: protected */
    public synchronized void onDraw(Canvas canvas) {
        drawProgress(canvas);
    }

    private void drawProgress(Canvas canvas) {
        Drawable d = getProgressDrawable();
        if (d != null) {
            canvas.save();
            canvas.rotate(-90.0f, (float) (getWidth() / 2), (float) (getHeight() / 2));
            int w = getWidth();
            int h = getHeight();
            d.setBounds(((-(h - w)) / 2) + getPaddingBottom(), ((h - w) / 2) + getPaddingLeft(), ((h + w) / 2) - getPaddingTop(), ((h + w) / 2) - getPaddingRight());
            d.draw(canvas);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public void transformTouchEvent(MotionEvent event) {
        super.transformTouchEvent(event);
        event.setLocation(((((((float) getHeight()) - event.getY()) - ((float) getPaddingBottom())) / ((float) ((getHeight() - getPaddingTop()) - getPaddingBottom()))) * ((float) ((getWidth() - getPaddingLeft()) - getPaddingRight()))) + ((float) getPaddingLeft()), event.getY());
    }
}
