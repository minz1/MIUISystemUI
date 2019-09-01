package com.android.systemui.statusbar.phone;

import android.widget.ImageView;
import android.widget.TextView;

public class SignalClusterViewControllerImpl implements SignalClusterViewController {
    public void updateMobileTypeVisible(TextView mobileType, ImageView mobileTypeImage, boolean show4GLTEByImage) {
        mobileType.setVisibility(0);
    }
}
