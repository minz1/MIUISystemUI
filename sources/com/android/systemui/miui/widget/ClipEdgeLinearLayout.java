package com.android.systemui.miui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ClipEdgeLinearLayout extends LinearLayout {
    private boolean mClipEdge = false;
    private boolean mClipEnd = false;
    private Rect mClipRect = new Rect();

    public ClipEdgeLinearLayout(Context context) {
        super(context);
    }

    public ClipEdgeLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ClipEdgeLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ClipEdgeLinearLayout);
            this.mClipEdge = a.getBoolean(R.styleable.ClipEdgeLinearLayout_clipEdge, false);
            this.mClipEnd = a.getBoolean(R.styleable.ClipEdgeLinearLayout_clipEnd, false);
            a.recycle();
        }
    }

    /* access modifiers changed from: protected */
    public void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        super.measureChildWithMargins(child, parentWidthMeasureSpec, this.mClipEdge ? 0 : widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mClipEdge) {
            getClipRect();
        }
    }

    private void getClipRect() {
        int i = 0;
        this.mClipRect.left = 0;
        this.mClipRect.top = 0;
        this.mClipRect.bottom = getHeight();
        this.mClipRect.right = 0;
        if (clipEnd()) {
            i = getWidth();
        }
        setClipRectRight(i, this);
    }

    private void setClipRectRight(int pos, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child.getLeft() < pos && child.getRight() > pos && child.getVisibility() != 8) {
                if (child instanceof ViewGroup) {
                    setClipRectRight(pos - child.getLeft(), (ViewGroup) child);
                } else if (clipEnd()) {
                    this.mClipRect.right = getWidth() - (pos - child.getLeft());
                } else {
                    this.mClipRect.left = child.getRight() - pos;
                    this.mClipRect.right = getWidth();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean clipEnd() {
        if (isLayoutRtl()) {
            return !this.mClipEnd;
        }
        return this.mClipEnd;
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        boolean clip = this.mClipEdge && this.mClipRect.right - this.mClipRect.left > 0;
        if (clip) {
            canvas.save();
            canvas.clipRect(this.mClipRect);
        }
        super.dispatchDraw(canvas);
        if (clip) {
            canvas.restore();
        }
    }

    public void setClipEdge(boolean clipEdge) {
        this.mClipEdge = clipEdge;
    }
}
