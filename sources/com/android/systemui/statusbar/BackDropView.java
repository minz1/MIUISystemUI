package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class BackDropView extends FrameLayout {
    private Runnable mOnVisibilityChangedRunnable;

    public BackDropView(Context context) {
        super(context);
    }

    public BackDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackDropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BackDropView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && this.mOnVisibilityChangedRunnable != null) {
            this.mOnVisibilityChangedRunnable.run();
        }
    }

    public void setOnVisibilityChangedRunnable(Runnable runnable) {
        this.mOnVisibilityChangedRunnable = runnable;
    }
}
