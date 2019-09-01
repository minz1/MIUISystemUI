package com.android.systemui.miui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;

public class FoldHeaderView extends ExpandableView {
    private static float ALPHA_PRESSED = 0.3f;
    /* access modifiers changed from: private */
    public ClickListener mClickListener;
    private View mDividerView;
    private int mOrientation = getResources().getConfiguration().orientation;

    public interface ClickListener {
        void onClickTips();
    }

    public FoldHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInShelf(false);
        setClipChildren(false);
        setClipToPadding(false);
        update();
    }

    public void setClickListener(ClickListener clickListener) {
        this.mClickListener = clickListener;
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
        Animator anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Z, new float[]{-1.0f * ((float) getHeight()), 0.0f});
        anim.setDuration(duration);
        anim.setStartDelay(delay);
        anim.start();
    }

    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        setFocusable(true);
        setClickable(true);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean upOrCancel = true;
        if (!(event.getActionMasked() == 1 || event.getActionMasked() == 3)) {
            upOrCancel = false;
        }
        float alpha = upOrCancel ? 1.0f : ALPHA_PRESSED;
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view.getId() != R.id.divider) {
                view.setAlpha(alpha);
            }
        }
        return super.onTouchEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDividerView = findViewById(R.id.divider);
        View tipsView = findViewById(R.id.tips);
        tipsView.setVisibility(Constants.ENABLE_USER_FOLD ? 0 : 8);
        tipsView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (FoldHeaderView.this.mClickListener != null) {
                    FoldHeaderView.this.mClickListener.onClickTips();
                }
            }
        });
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
                FoldHeaderView.this.setActualHeight(FoldHeaderView.this.getMeasuredHeight());
                FoldHeaderView.this.setDividerWidth();
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
