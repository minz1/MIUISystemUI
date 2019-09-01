package com.android.systemui.pip;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.os.RemoteException;
import java.util.List;

public class PipUIHelper {
    public static List<ActivityManager.RunningTaskInfo> getTasks(IActivityManager activityManager, int maxNum, int flags) {
        try {
            return activityManager.getTasks(maxNum);
        } catch (RemoteException e) {
            return null;
        }
    }
}
