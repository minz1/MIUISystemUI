package com.android.systemui.stackdivider;

import android.content.Context;
import android.view.WindowManager;
import com.android.systemui.R;

public class DividerWindowManager {
    private int mDividerInsets;
    private WindowManager.LayoutParams mLp;
    private DividerView mView;
    private final WindowManager mWindowManager;

    public DividerWindowManager(Context ctx) {
        this.mDividerInsets = ctx.getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_insets);
        this.mWindowManager = (WindowManager) ctx.getSystemService(WindowManager.class);
    }

    public void add(DividerView view, int width, int height) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(width, height, 2034, 545521704, -3);
        this.mLp = layoutParams;
        this.mLp.setTitle("DockedStackDivider");
        this.mLp.privateFlags |= 64;
        view.setSystemUiVisibility(1792);
        this.mView = view;
        this.mWindowManager.addView(view, this.mLp);
    }

    public void update(int position) {
        if (this.mView.getResources().getConfiguration().orientation == 1) {
            this.mLp.gravity = 48;
            this.mLp.y = position - this.mDividerInsets;
        } else {
            this.mLp.gravity = 3;
            this.mLp.x = position - this.mDividerInsets;
        }
        this.mWindowManager.updateViewLayout(this.mView, this.mLp);
    }

    public void remove() {
        if (this.mView != null) {
            this.mWindowManager.removeView(this.mView);
        }
        this.mView = null;
    }

    public void setSlippery(boolean slippery) {
        boolean changed = false;
        if (slippery && (this.mLp.flags & 536870912) == 0) {
            WindowManager.LayoutParams layoutParams = this.mLp;
            layoutParams.flags = 536870912 | layoutParams.flags;
            changed = true;
        } else if (!slippery && (536870912 & this.mLp.flags) != 0) {
            this.mLp.flags &= -536870913;
            changed = true;
        }
        if (changed) {
            this.mWindowManager.updateViewLayout(this.mView, this.mLp);
        }
    }

    public void setTouchable(boolean touchable) {
        boolean changed = false;
        if (!touchable && (this.mLp.flags & 16) == 0) {
            this.mLp.flags |= 16;
            changed = true;
        } else if (touchable && (this.mLp.flags & 16) != 0) {
            this.mLp.flags &= -17;
            changed = true;
        }
        if (changed) {
            this.mWindowManager.updateViewLayout(this.mView, this.mLp);
        }
    }
}
