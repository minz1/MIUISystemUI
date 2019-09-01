package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.systemui.R;

public abstract class ExpandableOutlineView extends ExpandableView {
    /* access modifiers changed from: private */
    public boolean mCustomOutline;
    private int mKeyguardNotificationBgRadius = getResources().getDimensionPixelSize(R.dimen.notification_stack_scroller_bg_radius);
    /* access modifiers changed from: private */
    public float mOutlineAlpha = -1.0f;
    /* access modifiers changed from: private */
    public final Rect mOutlineRect = new Rect();
    ViewOutlineProvider mProvider = new ViewOutlineProvider() {
        public void getOutline(View view, Outline outline) {
            int translation = (int) ExpandableOutlineView.this.getTranslation();
            if (!ExpandableOutlineView.this.mCustomOutline) {
                outline.setRoundRect(translation, ExpandableOutlineView.this.mClipTopAmount, ExpandableOutlineView.this.getWidth() + translation, Math.max(ExpandableOutlineView.this.getActualHeight() - ExpandableOutlineView.this.mClipBottomAmount, ExpandableOutlineView.this.mClipTopAmount), (float) ExpandableOutlineView.this.getOutlineRadius());
            } else {
                outline.setRoundRect(ExpandableOutlineView.this.mOutlineRect, (float) ExpandableOutlineView.this.getOutlineRadius());
            }
            outline.setAlpha(ExpandableOutlineView.this.mOutlineAlpha);
        }
    };

    public ExpandableOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOutlineProvider(this.mProvider);
        setClipToOutline(true);
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        invalidateOutline();
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        invalidateOutline();
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        super.setClipBottomAmount(clipBottomAmount);
        invalidateOutline();
    }

    /* access modifiers changed from: protected */
    public void setOutlineAlpha(float alpha) {
        if (alpha != this.mOutlineAlpha) {
            this.mOutlineAlpha = alpha;
            invalidateOutline();
        }
    }

    public float getOutlineAlpha() {
        return this.mOutlineAlpha;
    }

    /* access modifiers changed from: protected */
    public void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
            return;
        }
        this.mCustomOutline = false;
        invalidateOutline();
    }

    public int getOutlineTranslation() {
        return this.mCustomOutline ? this.mOutlineRect.left : (int) getTranslation();
    }

    public void updateOutline() {
        setOutlineProvider(needsOutline() ? this.mProvider : null);
    }

    /* access modifiers changed from: protected */
    public boolean needsOutline() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void setOutlineRect(float left, float top, float right, float bottom) {
        this.mCustomOutline = true;
        setClipToOutline(true);
        this.mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);
        this.mOutlineRect.bottom = (int) Math.max(top, (float) this.mOutlineRect.bottom);
        this.mOutlineRect.right = (int) Math.max(left, (float) this.mOutlineRect.right);
        invalidateOutline();
    }

    /* access modifiers changed from: protected */
    public int getOutlineRadius() {
        return this.mKeyguardNotificationBgRadius;
    }
}
