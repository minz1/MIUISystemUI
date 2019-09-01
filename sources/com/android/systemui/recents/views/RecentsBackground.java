package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import com.android.systemui.statusbar.AlphaOptimizedView;

public class RecentsBackground extends AlphaOptimizedView {
    private Display mDisplay;
    private int mScreenHeight;
    private int mScreenWidth;

    public RecentsBackground(Context context) {
        this(context, null);
    }

    public RecentsBackground(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsBackground(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point screenSizePoint = new Point();
        this.mDisplay.getRealSize(screenSizePoint);
        this.mScreenWidth = Math.min(screenSizePoint.x, screenSizePoint.y);
        this.mScreenHeight = Math.max(screenSizePoint.x, screenSizePoint.y);
    }

    public void draw(Canvas canvas) {
        int top;
        Drawable background = getBackground();
        if (background != null) {
            canvas.save();
            if (background instanceof ColorDrawable) {
                background.setBounds(0, 0, getWidth(), getHeight());
            } else {
                int rotation = this.mDisplay.getRotation();
                int[] loc = getLocationOnScreen();
                if (rotation == 1) {
                    canvas.rotate(-90.0f, (float) (canvas.getHeight() / 2), (float) (canvas.getHeight() / 2));
                    top = -loc[0];
                } else if (rotation == 3) {
                    canvas.rotate(90.0f, (float) (canvas.getWidth() / 2), (float) (canvas.getWidth() / 2));
                    top = 0;
                } else {
                    top = -loc[1];
                }
                background.setBounds(0, top, this.mScreenWidth, this.mScreenHeight + top);
            }
            background.draw(canvas);
            canvas.restore();
        }
    }
}
