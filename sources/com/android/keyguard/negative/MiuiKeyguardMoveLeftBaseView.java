package com.android.keyguard.negative;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.android.systemui.statusbar.phone.StatusBar;

public abstract class MiuiKeyguardMoveLeftBaseView extends RelativeLayout {
    protected StatusBar mStatusBar;

    public abstract void initLeftView();

    public abstract boolean isSupportRightMove();

    public abstract void uploadData();

    public MiuiKeyguardMoveLeftBaseView(Context context) {
        this(context, null);
    }

    public MiuiKeyguardMoveLeftBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }
}
