package com.android.settingslib;

import android.content.pm.ApplicationInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;

public class OldmanHelper {
    public static boolean isOldmanMode() {
        return Build.getUserMode() == 1;
    }

    public static List<ApplicationInfo> filterOldmanModeApp(List<ApplicationInfo> applications) {
        if (isOldmanMode()) {
            return applications;
        }
        List<ApplicationInfo> appInfos = new ArrayList<>(applications);
        Iterator<ApplicationInfo> iter = appInfos.iterator();
        while (iter.hasNext()) {
            if (isHideOldModeApp(iter.next().packageName)) {
                iter.remove();
            }
        }
        return appInfos;
    }

    private static boolean isHideOldModeApp(String pkgName) {
        return "com.jeejen.family.miui".equals(pkgName) || "com.jeejen.knowledge".equals(pkgName) || "com.jeejen.store".equals(pkgName);
    }
}
