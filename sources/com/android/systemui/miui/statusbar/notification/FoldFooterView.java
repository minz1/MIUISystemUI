package com.android.systemui.miui.statusbar.notification;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;

public class FoldFooterView extends ExpandableView {
    private View mDividerView;
    private int mOrientation = getResources().getConfiguration().orientation;

    public FoldFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInShelf(false);
        setClipChildren(false);
        setClipToPadding(false);
        update();
    }

    public int getActualHeight() {
        return getMeasuredHeight();
    }

    public boolean isInShelf() {
        return false;
    }

    public void performRemoveAnimation(long duration, float translationDirection, AnimatorListenerAdapter globalListener, Runnable onFinishedRunnable) {
    }

    public void performAddAnimation(long delay, long duration, AnimatorListenerAdapter globalListener) {
    }

    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        setFocusable(true);
        setClickable(true);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDividerView = findViewById(R.id.divider);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mOrientation != newConfig.orientation) {
            update();
            this.mOrientation = newConfig.orientation;
        }
    }

    private void update() {
        post(new Runnable() {
            public void run() {
                FoldFooterView.this.setActualHeight(FoldFooterView.this.getMeasuredHeight());
                FoldFooterView.this.setDividerWidth();
            }
        });
    }

    /* access modifiers changed from: private */
    public void setDividerWidth() {
        ViewGroup.LayoutParams params = this.mDividerView.getLayoutParams();
        params.width = getMeasuredWidth() - (getResources().getDimensionPixelSize(R.dimen.fold_dividing_line_margin) * 2);
        this.mDividerView.setLayoutParams(params);
    }
}
