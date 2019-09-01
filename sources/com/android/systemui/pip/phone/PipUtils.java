package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

public class PipUtils {
    public static ComponentName getTopPinnedActivity(Context context, IActivityManager activityManager) {
        try {
            String sysUiPackageName = context.getPackageName();
            ActivityManager.StackInfo pinnedStackInfo = ActivityManagerCompat.getStackInfo(4, 2, 0);
            if (!(pinnedStackInfo == null || pinnedStackInfo.taskIds == null || pinnedStackInfo.taskIds.length <= 0)) {
                for (int i = pinnedStackInfo.taskNames.length - 1; i >= 0; i--) {
                    ComponentName cn = ComponentName.unflattenFromString(pinnedStackInfo.taskNames[i]);
                    if (cn != null && !cn.getPackageName().equals(sysUiPackageName)) {
                        return cn;
                    }
                }
            }
        } catch (Exception e) {
            Log.w("PipUtils", "Unable to get pinned stack.");
        }
        return null;
    }
}
