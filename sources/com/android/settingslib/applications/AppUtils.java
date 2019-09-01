package com.android.settingslib.applications;

import android.content.pm.ApplicationInfo;
import android.os.SystemProperties;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

public class AppUtils {
    private static InstantAppDataProvider sInstantAppDataProvider = null;

    public static boolean isInstant(ApplicationInfo info) {
        if (sInstantAppDataProvider != null) {
            if (sInstantAppDataProvider.isInstantApp(info)) {
                return true;
            }
        } else if (info.isInstantApp()) {
            return true;
        }
        String propVal = SystemProperties.get("settingsdebug.instant.packages");
        if (!(propVal == null || propVal.isEmpty() || info.packageName == null)) {
            String[] searchTerms = propVal.split(",");
            if (searchTerms != null) {
                for (String term : searchTerms) {
                    if (info.packageName.contains(term)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
