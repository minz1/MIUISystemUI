package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;

public class PreviewInflater {
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    public PreviewInflater(Context context, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public static boolean wouldLaunchResolverActivity(Context ctx, Intent intent, int currentUserId) {
        return getTargetActivityInfo(ctx, intent, currentUserId, false) == null;
    }

    public static ActivityInfo getTargetActivityInfo(Context ctx, Intent intent, int currentUserId, boolean onlyDirectBootAware) {
        PackageManager packageManager = ctx.getPackageManager();
        int flags = 65536;
        if (!onlyDirectBootAware) {
            flags = 65536 | 786432;
        }
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, flags, currentUserId);
        if (appList.size() == 0) {
            return null;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, flags | 128, currentUserId);
        if (resolved == null || wouldLaunchResolverActivity(resolved, appList)) {
            return null;
        }
        return resolved.activityInfo;
    }

    private static boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }
}
