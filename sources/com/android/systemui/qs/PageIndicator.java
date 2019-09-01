package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.R;

public class PageIndicator extends ViewGroup {
    private final int mPageDotSize;
    private final int mPageDotSpace;
    private int mPosition = -1;

    public PageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = this.mContext.getResources();
        this.mPageDotSize = (int) res.getDimension(R.dimen.qs_page_indicator_dot_size);
        this.mPageDotSpace = (int) res.getDimension(R.dimen.qs_page_indicator_dot_space);
    }

    public void setNumPages(int numPages) {
        setVisibility(numPages > 1 ? 0 : 4);
        while (numPages < getChildCount()) {
            removeViewAt(getChildCount() - 1);
        }
        while (numPages > getChildCount()) {
            ImageView v = new ImageView(this.mContext);
            v.setImageResource(R.drawable.qs_page_indicator_dot);
            addView(v);
        }
        setIndex(this.mPosition >> 1);
    }

    public void setLocation(float location) {
        int position = (int) location;
        setContentDescription(getContext().getString(R.string.accessibility_quick_settings_page, new Object[]{Integer.valueOf(position + 1), Integer.valueOf(getChildCount())}));
        int lastPosition = this.mPosition;
        if (position != lastPosition && (lastPosition <= position || location - ((float) position) <= 0.0f)) {
            setPosition(position);
        }
    }

    private void setPosition(int position) {
        if (!isVisibleToUser() || Math.abs(this.mPosition - position) != 1) {
            setIndex(position);
        } else {
            animate(this.mPosition, position);
        }
        this.mPosition = position;
    }

    private void setIndex(int index) {
        int N = getChildCount();
        int i = 0;
        while (i < N) {
            ImageView v = (ImageView) getChildAt(i);
            v.setImageResource(R.drawable.qs_page_indicator_dot);
            v.setAlpha(getAlpha(i == index));
            i++;
        }
    }

    private void animate(int from, int to) {
        setIndex(to);
    }

    private float getAlpha(boolean isMajor) {
        return isMajor ? 0.7f : 0.2f;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int N = getChildCount();
        if (N == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int widthChildSpec = View.MeasureSpec.makeMeasureSpec(this.mPageDotSize, 1073741824);
        int heightChildSpec = View.MeasureSpec.makeMeasureSpec(this.mPageDotSize, 1073741824);
        for (int i = 0; i < N; i++) {
            getChildAt(i).measure(widthChildSpec, heightChildSpec);
        }
        setMeasuredDimension(((this.mPageDotSize + this.mPageDotSpace) * N) - this.mPageDotSpace, this.mPageDotSize);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int N = getChildCount();
        if (N != 0) {
            for (int i = 0; i < N; i++) {
                int left = (this.mPageDotSize + this.mPageDotSpace) * i;
                getChildAt(i).layout(left, 0, this.mPageDotSize + left, this.mPageDotSize);
            }
        }
    }
}
