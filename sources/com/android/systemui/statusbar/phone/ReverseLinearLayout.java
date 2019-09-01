package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.ArrayList;

public class ReverseLinearLayout extends LinearLayout {
    private boolean mIsAlternativeOrder;
    private boolean mIsLayoutReverse;

    public ReverseLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        updateOrder();
    }

    public void addView(View child) {
        reversParams(child.getLayoutParams());
        if (this.mIsLayoutReverse) {
            super.addView(child, 0);
        } else {
            super.addView(child);
        }
    }

    public void addView(View child, ViewGroup.LayoutParams params) {
        reversParams(params);
        if (this.mIsLayoutReverse) {
            super.addView(child, 0, params);
        } else {
            super.addView(child, params);
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateOrder();
    }

    private void updateOrder() {
        boolean isLayoutRtl = true;
        if (getLayoutDirection() != 1) {
            isLayoutRtl = false;
        }
        boolean isLayoutReverse = this.mIsAlternativeOrder ^ isLayoutRtl;
        if (this.mIsLayoutReverse != isLayoutReverse) {
            int childCount = getChildCount();
            ArrayList<View> childList = new ArrayList<>(childCount);
            for (int i = 0; i < childCount; i++) {
                childList.add(getChildAt(i));
            }
            removeAllViews();
            for (int i2 = childCount - 1; i2 >= 0; i2--) {
                super.addView(childList.get(i2));
            }
            this.mIsLayoutReverse = isLayoutReverse;
        }
    }

    private void reversParams(ViewGroup.LayoutParams params) {
        if (params != null) {
            int width = params.width;
            params.width = params.height;
            params.height = width;
        }
    }
}
