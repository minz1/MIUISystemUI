package com.android.systemui.qs;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import com.android.systemui.R;

public class AutoSizingList extends LinearLayout {
    private ListAdapter mAdapter;
    private final Runnable mBindChildren = new Runnable() {
        public void run() {
            AutoSizingList.this.rebindChildren();
        }
    };
    /* access modifiers changed from: private */
    public int mCount;
    private final DataSetObserver mDataObserver = new DataSetObserver() {
        public void onChanged() {
            if (AutoSizingList.this.mCount > AutoSizingList.this.getDesiredCount()) {
                int unused = AutoSizingList.this.mCount = AutoSizingList.this.getDesiredCount();
            }
            AutoSizingList.this.postRebindChildren();
        }

        public void onInvalidated() {
            AutoSizingList.this.postRebindChildren();
        }
    };
    private boolean mEnableAutoSizing;
    private final Runnable mEvaluateCount = new Runnable() {
        public void run() {
            AutoSizingList.this.evaluateCount();
        }
    };
    private final Handler mHandler = new Handler();
    private final int mItemSize;
    private int mLastRequestHeight;

    public AutoSizingList(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoSizingList);
        this.mItemSize = a.getDimensionPixelSize(1, 0);
        this.mEnableAutoSizing = a.getBoolean(0, true);
        a.recycle();
    }

    public void setAdapter(ListAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataObserver);
        }
        this.mAdapter = adapter;
        if (adapter != null) {
            adapter.registerDataSetObserver(this.mDataObserver);
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mLastRequestHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        postEvaluateCount();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int getItemCount(int requestedHeight) {
        int desiredCount = getDesiredCount();
        return this.mEnableAutoSizing ? Math.min(requestedHeight / this.mItemSize, desiredCount) : desiredCount;
    }

    /* access modifiers changed from: private */
    public int getDesiredCount() {
        if (this.mAdapter != null) {
            return this.mAdapter.getCount();
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public void postRebindChildren() {
        this.mHandler.removeCallbacks(this.mBindChildren);
        this.mHandler.post(this.mBindChildren);
    }

    private void postEvaluateCount() {
        this.mHandler.removeCallbacks(this.mEvaluateCount);
        this.mHandler.postDelayed(this.mEvaluateCount, 50);
    }

    /* access modifiers changed from: private */
    public void rebindChildren() {
        if (this.mAdapter != null) {
            int i = 0;
            while (i < this.mCount) {
                View v = i < getChildCount() ? getChildAt(i) : null;
                View newView = this.mAdapter.getView(i, v, this);
                if (newView != v) {
                    if (v != null) {
                        removeView(v);
                    }
                    addView(newView, i);
                }
                i++;
            }
            while (getChildCount() > this.mCount) {
                removeViewAt(getChildCount() - 1);
            }
        }
    }

    /* access modifiers changed from: private */
    public void evaluateCount() {
        if (this.mLastRequestHeight != 0) {
            int count = getItemCount(this.mLastRequestHeight);
            if (this.mCount != count) {
                postRebindChildren();
                this.mCount = count;
            }
        }
    }
}
