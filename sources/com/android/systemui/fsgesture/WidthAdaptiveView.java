package com.android.systemui.fsgesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class WidthAdaptiveView extends View {
    public WidthAdaptiveView(Context context) {
        this(context, null);
    }

    public WidthAdaptiveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidthAdaptiveView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WidthAdaptiveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void draw(Canvas canvas) {
        Drawable background = getBackground();
        if (background != null) {
            canvas.save();
            background.setBounds(0, 0, getWidth(), (int) (((float) getWidth()) * ((((float) background.getIntrinsicHeight()) * 1.0f) / ((float) background.getIntrinsicWidth()))));
            background.draw(canvas);
            canvas.restore();
        }
    }
}
