package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R;

public class IconMerger extends LinearLayout {
    private View mClockView;
    private boolean mEnoughSpace;
    private boolean mForceShowingMore;
    private int mIconWidth;
    private View mIcons;
    /* access modifiers changed from: private */
    public View mMoreView;
    private View mStatusBar;
    private View mStatusIcons;
    private int mWidth = 0;

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIconWidth = context.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size) + (2 * context.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_padding));
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        post(new Runnable() {
            public void run() {
                IconMerger.this.requestLayout();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = ((((((this.mStatusBar.getMeasuredWidth() - this.mStatusBar.getPaddingStart()) - this.mStatusBar.getPaddingEnd()) - this.mStatusIcons.getMeasuredWidth()) - this.mClockView.getMeasuredWidth()) - this.mMoreView.getMeasuredWidth()) - this.mIcons.getPaddingStart()) - this.mIcons.getPaddingEnd();
        if (maxWidth > this.mIconWidth * getChildCount()) {
            i = this.mIconWidth * getChildCount();
        } else {
            i = maxWidth - (maxWidth % this.mIconWidth);
        }
        this.mWidth = i;
        int i2 = 0;
        this.mEnoughSpace = maxWidth >= 0;
        if (this.mWidth >= 0) {
            i2 = this.mWidth;
        }
        setMeasuredDimension(i2, getMeasuredHeight());
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow();
    }

    private void checkOverflow() {
        if (this.mMoreView != null) {
            int N = getChildCount();
            boolean z = false;
            int visibleChildren = 0;
            for (int i = 0; i < N; i++) {
                if (getChildAt(i).getVisibility() != 8) {
                    visibleChildren++;
                }
            }
            final boolean moreRequired = (this.mForceShowingMore != 0 || this.mIconWidth * visibleChildren > this.mWidth) && getVisibility() == 0 && this.mEnoughSpace;
            if (this.mMoreView.getVisibility() == 0) {
                z = true;
            }
            if (moreRequired != z) {
                post(new Runnable() {
                    public void run() {
                        IconMerger.this.mMoreView.setVisibility(moreRequired ? 0 : 8);
                    }
                });
            }
        }
    }
}
