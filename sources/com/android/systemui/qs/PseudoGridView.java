package com.android.systemui.qs;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.systemui.R;
import java.lang.ref.WeakReference;

public class PseudoGridView extends ViewGroup {
    private int mHorizontalSpacing;
    private int mNumColumns = 3;
    private int mVerticalSpacing;

    public static class ViewGroupAdapterBridge extends DataSetObserver {
        private final BaseAdapter mAdapter;
        private boolean mReleased = false;
        private final WeakReference<ViewGroup> mViewGroup;

        public static void link(ViewGroup viewGroup, BaseAdapter adapter) {
            new ViewGroupAdapterBridge(viewGroup, adapter);
        }

        private ViewGroupAdapterBridge(ViewGroup viewGroup, BaseAdapter adapter) {
            this.mViewGroup = new WeakReference<>(viewGroup);
            this.mAdapter = adapter;
            this.mAdapter.registerDataSetObserver(this);
            refresh();
        }

        private void refresh() {
            if (!this.mReleased) {
                ViewGroup viewGroup = (ViewGroup) this.mViewGroup.get();
                if (viewGroup == null) {
                    release();
                    return;
                }
                int childCount = viewGroup.getChildCount();
                int adapterCount = this.mAdapter.getCount();
                int N = Math.max(childCount, adapterCount);
                for (int i = 0; i < N; i++) {
                    if (i < adapterCount) {
                        View oldView = null;
                        if (i < childCount) {
                            oldView = viewGroup.getChildAt(i);
                        }
                        View newView = this.mAdapter.getView(i, oldView, viewGroup);
                        if (oldView == null) {
                            viewGroup.addView(newView);
                        } else if (oldView != newView) {
                            viewGroup.removeViewAt(i);
                            viewGroup.addView(newView, i);
                        }
                    } else {
                        viewGroup.removeViewAt(viewGroup.getChildCount() - 1);
                    }
                }
            }
        }

        public void onChanged() {
            refresh();
        }

        public void onInvalidated() {
            release();
        }

        private void release() {
            if (!this.mReleased) {
                this.mReleased = true;
                this.mAdapter.unregisterDataSetObserver(this);
            }
        }
    }

    public PseudoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PseudoGridView);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    this.mHorizontalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
                case 1:
                    this.mNumColumns = a.getInt(attr, 3);
                    break;
                case 2:
                    this.mVerticalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
            }
        }
        a.recycle();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int children;
        if (View.MeasureSpec.getMode(widthMeasureSpec) != 0) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            int i = 1073741824;
            int childWidthSpec = View.MeasureSpec.makeMeasureSpec((width - ((this.mNumColumns - 1) * this.mHorizontalSpacing)) / this.mNumColumns, 1073741824);
            int rows = ((this.mNumColumns + getChildCount()) - 1) / this.mNumColumns;
            int totalHeight = 0;
            int row = 0;
            while (row < rows) {
                int startOfRow = this.mNumColumns * row;
                int endOfRow = Math.min(this.mNumColumns + startOfRow, children);
                int maxHeight = 0;
                for (int i2 = startOfRow; i2 < endOfRow; i2++) {
                    View child = getChildAt(i2);
                    child.measure(childWidthSpec, 0);
                    maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                }
                int maxHeightSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, i);
                for (int i3 = startOfRow; i3 < endOfRow; i3++) {
                    View child2 = getChildAt(i3);
                    if (child2.getMeasuredHeight() != maxHeight) {
                        child2.measure(childWidthSpec, maxHeightSpec);
                    }
                }
                totalHeight += maxHeight;
                if (row > 0) {
                    totalHeight += this.mVerticalSpacing;
                }
                row++;
                i = 1073741824;
            }
            setMeasuredDimension(width, resolveSizeAndState(totalHeight, heightMeasureSpec, 0));
            return;
        }
        int i4 = heightMeasureSpec;
        throw new UnsupportedOperationException("Needs a maximum width");
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int children;
        boolean isRtl = isLayoutRtl();
        int rows = ((this.mNumColumns + getChildCount()) - 1) / this.mNumColumns;
        int y = 0;
        for (int row = 0; row < rows; row++) {
            int x = isRtl ? getWidth() : 0;
            int maxHeight = 0;
            int startOfRow = this.mNumColumns * row;
            int endOfRow = Math.min(this.mNumColumns + startOfRow, children);
            int x2 = x;
            for (int i = startOfRow; i < endOfRow; i++) {
                View child = getChildAt(i);
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                if (isRtl) {
                    x2 -= width;
                }
                child.layout(x2, y, x2 + width, y + height);
                maxHeight = Math.max(maxHeight, height);
                if (isRtl) {
                    x2 -= this.mHorizontalSpacing;
                } else {
                    x2 += this.mHorizontalSpacing + width;
                }
            }
            y += maxHeight;
            if (row > 0) {
                y += this.mVerticalSpacing;
            }
        }
    }
}
