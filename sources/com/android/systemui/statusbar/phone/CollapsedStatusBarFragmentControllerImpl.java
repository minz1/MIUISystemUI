package com.android.systemui.statusbar.phone;

import android.util.ArraySet;
import android.view.View;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.CollapsedStatusBarFragment;
import java.util.Objects;

public class CollapsedStatusBarFragmentControllerImpl implements CollapsedStatusBarFragmentController {
    private CollapsedStatusBarFragment mFragment;
    private CollapsedStatusBarFragment.LeftEarIconManager mNotchLeftEarIconManager;

    public void init(CollapsedStatusBarFragment fragment) {
        this.mFragment = fragment;
    }

    public void start(View statusBar) {
        if (Constants.IS_NOTCH) {
            ArraySet<String> notchleftearIconsList = this.mFragment.mNotchleftearIconsList;
            notchleftearIconsList.add("bluetooth");
            if (Constants.IS_NARROW_NOTCH) {
                notchleftearIconsList.add("location");
            }
            CollapsedStatusBarFragment collapsedStatusBarFragment = this.mFragment;
            Objects.requireNonNull(collapsedStatusBarFragment);
            this.mNotchLeftEarIconManager = new CollapsedStatusBarFragment.LeftEarIconManager(this.mFragment.mNotchLeftEarIcons);
            this.mNotchLeftEarIconManager.mWhiteList = new ArraySet();
            this.mNotchLeftEarIconManager.mWhiteList.addAll(notchleftearIconsList);
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mNotchLeftEarIconManager);
        }
    }

    public void stop() {
        if (Constants.IS_NOTCH) {
            if (this.mNotchLeftEarIconManager != null) {
                ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mNotchLeftEarIconManager);
            }
            if (this.mFragment.mDarkIconManager != null && Constants.IS_NARROW_NOTCH) {
                ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mFragment.mDarkIconManager);
            }
        }
    }

    public void hideSystemIconArea(boolean animate, boolean isHoldPlace) {
    }

    public void showSystemIconArea(boolean animate) {
    }

    public boolean isStatusIconsVisible() {
        return !Constants.IS_NARROW_NOTCH;
    }

    public void updateLeftPartVisibility(boolean visible, boolean isOnlyClock) {
        if (this.mFragment != null) {
            this.mFragment.clockVisibleAnimate(visible, true);
        }
    }
}
