package com.android.systemui.statusbar;

import com.android.internal.statusbar.StatusBarIcon;

public class ExpandedIcon extends StatusBarIcon {
    public ExpandedIcon(StatusBarIcon icon) {
        super(icon.user, icon.pkg, icon.icon, icon.iconLevel, icon.number, icon.contentDescription);
        this.visible = icon.visible;
    }

    public ExpandedIcon clone() {
        return new ExpandedIcon(ExpandedIcon.super.clone());
    }
}
