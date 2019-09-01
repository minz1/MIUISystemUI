package com.android.keyguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

public class KeyguardTextViewDrawable extends TextView {
    private boolean mAliganCenter;
    private int mHeight;
    private int mWidth;

    public KeyguardTextViewDrawable(Context context) {
        this(context, null);
    }

    public KeyguardTextViewDrawable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardTextViewDrawable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mAliganCenter = true;
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        this.mHeight = h;
        Drawable[] drawables = getCompoundDrawables();
        Drawable drawableLeft = drawables[0];
        Drawable drawableTop = drawables[1];
        Drawable drawableRight = drawables[2];
        Drawable drawableBottom = drawables[3];
        if (drawableLeft != null) {
            setDrawable(drawableLeft, 0, 0, 0);
        }
        if (drawableTop != null) {
            setDrawable(drawableTop, 1, 0, 0);
        }
        if (drawableRight != null) {
            setDrawable(drawableRight, 2, 0, 0);
        }
        if (drawableBottom != null) {
            setDrawable(drawableBottom, 3, 0, 0);
        }
        setCompoundDrawables(drawableLeft, drawableTop, drawableRight, drawableBottom);
    }

    private void setDrawable(Drawable drawable, int tag, int drawableWidth, int drawableHeight) {
        int width = drawableWidth == 0 ? drawable.getIntrinsicWidth() : drawableWidth;
        int height = drawableHeight == 0 ? drawable.getIntrinsicHeight() : drawableHeight;
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        int i = 0;
        switch (tag) {
            case 0:
            case 2:
                left = 0;
                if (!this.mAliganCenter) {
                    i = (((-getLineCount()) * getLineHeight()) / 2) + (getLineHeight() / 2);
                }
                top = i;
                right = width;
                bottom = top + height;
                break;
            case 1:
            case 3:
                if (!this.mAliganCenter) {
                    i = ((-this.mWidth) / 2) + (width / 2);
                }
                left = i;
                top = 0;
                right = left + width;
                bottom = 0 + height;
                break;
        }
        drawable.setBounds(left, top, right, bottom);
    }
}
