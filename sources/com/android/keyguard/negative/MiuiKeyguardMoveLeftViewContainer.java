package com.android.keyguard.negative;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;

public class MiuiKeyguardMoveLeftViewContainer extends FrameLayout {
    MiuiKeyguardMoveLeftBaseView mKeyguardMoveLeftView;
    protected StatusBar mStatusBar;

    public MiuiKeyguardMoveLeftViewContainer(Context context) {
        this(context, null);
    }

    public MiuiKeyguardMoveLeftViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        inflateLeftView();
    }

    public void inflateLeftView() {
        if (this.mKeyguardMoveLeftView != null) {
            removeView(this.mKeyguardMoveLeftView);
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isSupportLockScreenMagazineLeft()) {
            this.mKeyguardMoveLeftView = (MiuiKeyguardMoveLeftLockScreenMagazineView) LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_left_view_lock_screen_magazine_layout, null, false);
        } else {
            this.mKeyguardMoveLeftView = (MiuiKeyguardMoveLeftControlCenterView) LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_left_view_control_center_layout, null, false);
        }
        this.mKeyguardMoveLeftView.setStatusBar(this.mStatusBar);
        addView(this.mKeyguardMoveLeftView);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        if (this.mKeyguardMoveLeftView != null) {
            this.mKeyguardMoveLeftView.setStatusBar(statusBar);
        }
    }

    public void initLeftView() {
        if (this.mKeyguardMoveLeftView != null) {
            this.mKeyguardMoveLeftView.initLeftView();
        }
    }

    public void uploadData() {
        if (this.mKeyguardMoveLeftView != null) {
            this.mKeyguardMoveLeftView.uploadData();
        }
    }

    public boolean isSupportRightMove() {
        if (this.mKeyguardMoveLeftView == null) {
            return false;
        }
        return this.mKeyguardMoveLeftView.isSupportRightMove();
    }

    public void setPreBackgroundDrawable(Drawable background) {
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isSupportLockScreenMagazineLeft()) {
            setBackgroundDrawable(background);
        }
    }
}
