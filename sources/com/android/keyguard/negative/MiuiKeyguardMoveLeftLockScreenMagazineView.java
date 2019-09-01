package com.android.keyguard.negative;

import android.content.Context;
import android.util.AttributeSet;
import com.android.keyguard.KeyguardUpdateMonitor;

public class MiuiKeyguardMoveLeftLockScreenMagazineView extends MiuiKeyguardMoveLeftBaseView {
    public MiuiKeyguardMoveLeftLockScreenMagazineView(Context context) {
        this(context, null);
    }

    public MiuiKeyguardMoveLeftLockScreenMagazineView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void initLeftView() {
    }

    public void uploadData() {
    }

    public boolean isSupportRightMove() {
        return KeyguardUpdateMonitor.getInstance(this.mContext).isSupportLockScreenMagazineLeft();
    }
}
