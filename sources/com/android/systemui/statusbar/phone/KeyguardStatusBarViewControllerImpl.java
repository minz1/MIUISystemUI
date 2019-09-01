package com.android.systemui.statusbar.phone;

import android.graphics.Rect;
import android.util.ArraySet;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;

public class KeyguardStatusBarViewControllerImpl implements KeyguardStatusBarViewController {
    private StatusBarIconController.DarkIconManager mDarkIconManager;
    private KeyguardStatusBarView mStatusBarView;

    public void init(KeyguardStatusBarView statusBarView) {
        this.mStatusBarView = statusBarView;
        if (Constants.IS_NOTCH && !Constants.IS_NARROW_NOTCH) {
            statusBarView.findViewById(R.id.statusIcons).setVisibility(8);
        }
    }

    public void updateNotchVisible() {
    }

    public boolean isPromptCenter() {
        return !Constants.IS_NARROW_NOTCH;
    }

    public void showStatusIcons() {
        if (!Constants.IS_NOTCH || Constants.IS_NARROW_NOTCH) {
            this.mDarkIconManager = new StatusBarIconController.DarkIconManager(this.mStatusBarView.mStatusIcons);
            this.mDarkIconManager.setShieldDarkReceiver(true);
            if (Constants.IS_NARROW_NOTCH) {
                this.mDarkIconManager.mWhiteList = new ArraySet();
                this.mDarkIconManager.mWhiteList.add("volume");
                this.mDarkIconManager.mWhiteList.add("quiet");
                this.mDarkIconManager.mWhiteList.add("alarm_clock");
            }
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mDarkIconManager);
        }
    }

    public void hideStatusIcons() {
        if (this.mDarkIconManager != null) {
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mDarkIconManager);
        }
    }

    public void setDarkMode(Rect area, float darkIntensity, int tint) {
        if (this.mDarkIconManager != null) {
            this.mDarkIconManager.setDarkIntensity(area, darkIntensity, tint);
        }
    }
}
