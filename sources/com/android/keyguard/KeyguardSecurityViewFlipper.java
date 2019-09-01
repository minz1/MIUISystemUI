package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardSecurityViewFlipper extends ViewAnimator implements KeyguardSecurityView {
    private Rect mTempRect;

    public static class LayoutParams extends FrameLayout.LayoutParams {
        @ViewDebug.ExportedProperty(category = "layout")
        public int maxHeight;
        @ViewDebug.ExportedProperty(category = "layout")
        public int maxWidth;

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
        }

        public LayoutParams(LayoutParams other) {
            super(other);
            this.maxWidth = other.maxWidth;
            this.maxHeight = other.maxHeight;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.KeyguardSecurityViewFlipper_Layout, 0, 0);
            this.maxWidth = a.getDimensionPixelSize(1, 0);
            this.maxHeight = a.getDimensionPixelSize(0, 0);
            a.recycle();
        }

        /* access modifiers changed from: protected */
        public void encodeProperties(ViewHierarchyEncoder encoder) {
            super.encodeProperties(encoder);
            encoder.addProperty("layout:maxWidth", this.maxWidth);
            encoder.addProperty("layout:maxHeight", this.maxHeight);
        }
    }

    public KeyguardSecurityViewFlipper(Context context) {
        this(context, null);
    }

    public KeyguardSecurityViewFlipper(Context context, AttributeSet attr) {
        super(context, attr);
        this.mTempRect = new Rect();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        this.mTempRect.set(0, 0, 0, 0);
        boolean result2 = result;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                offsetRectIntoDescendantCoords(child, this.mTempRect);
                ev.offsetLocation((float) this.mTempRect.left, (float) this.mTempRect.top);
                result2 = child.dispatchTouchEvent(ev) || result2;
                ev.offsetLocation((float) (-this.mTempRect.left), (float) (-this.mTempRect.top));
            }
        }
        return result2;
    }

    /* access modifiers changed from: package-private */
    public KeyguardSecurityView getSecurityView() {
        View child = getChildAt(getDisplayedChild());
        if (child instanceof KeyguardSecurityView) {
            return (KeyguardSecurityView) child;
        }
        return null;
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.setKeyguardCallback(callback);
        }
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.setLockPatternUtils(utils);
        }
    }

    public void onPause() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.onPause();
        }
    }

    public void onResume(int reason) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.onResume(reason);
        }
    }

    public boolean needsInput() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            return ksv.needsInput();
        }
        return false;
    }

    public void showPromptReason(int reason) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.showPromptReason(reason);
        }
    }

    public void showMessage(String title, String message, int color) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.showMessage(title, message, color);
        }
    }

    public void applyHintAnimation(long offset) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.applyHintAnimation(offset);
        }
    }

    public void startAppearAnimation() {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            ksv.startAppearAnimation();
        }
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        KeyguardSecurityView ksv = getSecurityView();
        if (ksv != null) {
            return ksv.startDisappearAnimation(finishRunnable);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : new LayoutParams(p);
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthSpec, int heightSpec) {
        int widthMode = View.MeasureSpec.getMode(widthSpec);
        int heightMode = View.MeasureSpec.getMode(heightSpec);
        int widthSize = View.MeasureSpec.getSize(widthSpec);
        int heightSize = View.MeasureSpec.getSize(heightSpec);
        int count = getChildCount();
        int i = 0;
        int maxHeight = heightSize;
        int maxWidth = widthSize;
        for (int i2 = 0; i2 < count; i2++) {
            LayoutParams lp = (LayoutParams) getChildAt(i2).getLayoutParams();
            if (lp.maxWidth > 0 && lp.maxWidth < maxWidth) {
                maxWidth = lp.maxWidth;
            }
            if (lp.maxHeight > 0 && lp.maxHeight < maxHeight) {
                maxHeight = lp.maxHeight;
            }
        }
        int wPadding = getPaddingLeft() + getPaddingRight();
        int hPadding = getPaddingTop() + getPaddingBottom();
        int maxWidth2 = Math.max(0, maxWidth - wPadding);
        int maxHeight2 = Math.max(0, maxHeight - hPadding);
        int width = widthMode == 1073741824 ? widthSize : 0;
        int height = heightMode == 1073741824 ? heightSize : 0;
        while (i < count) {
            View child = getChildAt(i);
            LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
            int childWidthSpec = makeChildMeasureSpec(maxWidth2, lp2.width);
            int widthMode2 = widthMode;
            int widthMode3 = makeChildMeasureSpec(maxHeight2, lp2.height);
            child.measure(childWidthSpec, widthMode3);
            int i3 = widthMode3;
            width = Math.max(width, Math.min(child.getMeasuredWidth(), widthSize - wPadding));
            height = Math.max(height, Math.min(child.getMeasuredHeight(), heightSize - hPadding));
            i++;
            widthMode = widthMode2;
            heightMode = heightMode;
        }
        int i4 = heightMode;
        setMeasuredDimension(width + wPadding, height + hPadding);
    }

    private int makeChildMeasureSpec(int maxSize, int childDimen) {
        int size;
        int mode;
        switch (childDimen) {
            case -2:
                mode = Integer.MIN_VALUE;
                size = maxSize;
                break;
            case -1:
                mode = 1073741824;
                size = maxSize;
                break;
            default:
                mode = 1073741824;
                size = Math.min(maxSize, childDimen);
                break;
        }
        return View.MeasureSpec.makeMeasureSpec(size, mode);
    }
}
