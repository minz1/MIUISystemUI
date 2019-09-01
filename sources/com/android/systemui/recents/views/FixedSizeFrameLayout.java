package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FixedSizeFrameLayout extends FrameLayout {
    int mHeight;
    private final Rect mLayoutBounds = new Rect();
    int mWidth;

    public FixedSizeFrameLayout(Context context) {
        super(context);
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FixedSizeFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /* access modifiers changed from: protected */
    public final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureContents(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    /* access modifiers changed from: protected */
    public final void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mLayoutBounds.set(left, top, right, bottom);
        this.mWidth = right - left;
        this.mHeight = bottom - top;
        layoutContents(this.mLayoutBounds, changed);
    }

    public void setLeftTopRightBottom(int left, int top, int right, int bottom) {
        this.mWidth = right - left;
        this.mHeight = bottom - top;
        super.setLeftTopRightBottom(left, top, right, bottom);
    }

    public final void requestLayout() {
        if (this.mLayoutBounds == null || this.mLayoutBounds.isEmpty()) {
            super.requestLayout();
            return;
        }
        measureContents(getMeasuredWidth(), getMeasuredHeight());
        layoutContents(this.mLayoutBounds, false);
    }

    /* access modifiers changed from: protected */
    public void measureContents(int width, int height) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE));
    }

    /* access modifiers changed from: protected */
    public void layoutContents(Rect bounds, boolean changed) {
        super.onLayout(changed, bounds.left, bounds.top, bounds.right, bounds.bottom);
        onSizeChanged(this.mWidth, this.mHeight, this.mWidth, this.mHeight);
    }
}
