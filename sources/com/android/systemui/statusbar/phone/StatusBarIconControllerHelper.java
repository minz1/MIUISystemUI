package com.android.systemui.statusbar.phone;

import android.text.TextUtils;
import android.util.ArraySet;

public abstract class StatusBarIconControllerHelper implements StatusBarIconController {
    public static ArraySet<String> getIconBlacklist(String blackListStr) {
        ArraySet<String> ret = new ArraySet<>();
        if (blackListStr == null) {
            blackListStr = "rotate,ime";
        }
        for (String slot : blackListStr.split(",")) {
            if (!TextUtils.isEmpty(slot)) {
                ret.add(slot);
            }
        }
        return ret;
    }
}
