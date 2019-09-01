package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextSwitcher;

public class UnlockHintSwitcher extends TextSwitcher {
    private CharSequence mHint;
    /* access modifiers changed from: private */
    public boolean mIsShowingTemp;
    private Runnable mRunnable = new Runnable() {
        public void run() {
            boolean unused = UnlockHintSwitcher.this.mIsShowingTemp = false;
        }
    };

    public UnlockHintSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setText(CharSequence text) {
        this.mHint = text;
        if (!this.mIsShowingTemp) {
            super.setText(text);
        }
    }

    public void reset() {
        super.setText(this.mHint);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mRunnable);
    }
}
