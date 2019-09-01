package com.android.systemui;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.model.MutableBoolean;
import com.android.systemui.recents.model.Task;
import java.util.List;
import miui.securityspace.XSpaceUserHandle;

public class SystemUICompat {
    private static INotificationManager sINM = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));

    public static void registerStatusBar(IStatusBarService barService, IStatusBar callbacks, List<String> iconSlots, List<StatusBarIcon> iconList, int[] switches, List<IBinder> binders, Rect fullscreenStackBounds, Rect dockedStackBounds) throws RemoteException {
        barService.registerStatusBar(callbacks, iconSlots, iconList, switches, binders, fullscreenStackBounds, dockedStackBounds);
    }

    public static Object getLocales(Configuration newConfig) {
        return newConfig.getLocales();
    }

    public static void setRecentsVisibility(Context context, boolean visible) {
        try {
            WindowManagerGlobal.getWindowManagerService().setRecentsVisibility(visible);
        } catch (RemoteException e) {
            Log.e("SystemServicesProxy", "Unable to reach window manager", e);
        }
    }

    public static int getNotificationDefaultColor() {
        return 17170681;
    }

    public static void dismissKeyguardOnNextActivity() {
        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard(new IKeyguardDismissCallback.Stub() {
                public void onDismissError() {
                }

                public void onDismissSucceeded() {
                }

                public void onDismissCancelled() {
                }
            }, "");
        } catch (RemoteException e) {
            Log.w("SystemUICompat", "Error dismissing keyguard", e);
        }
    }

    public static boolean isHighPriority(String pkg, int uid) throws RemoteException {
        return false;
    }

    public static boolean isHomeOrRecentsStack(int stackId, ActivityManager.RunningTaskInfo runningTask) {
        boolean z = false;
        if (runningTask == null) {
            return false;
        }
        int activityType = runningTask.configuration.windowConfiguration.getActivityType();
        if (activityType == 2 || activityType == 3) {
            z = true;
        }
        return z;
    }

    public static void cancelTaskWindowTransition(IActivityManager mIam, int taskId) throws RemoteException {
        mIam.cancelTaskWindowTransition(taskId);
    }

    public static void cancelTaskThumbnailTransition(IActivityManager mIam, int taskId) throws RemoteException {
    }

    public static void getStableInsets(Rect outStableInsets) throws RemoteException {
        WindowManagerGlobal.getWindowManagerService().getStableInsets(0, outStableInsets);
    }

    public static Rect getRecentsWindowRect(IActivityManager iam) {
        Rect windowRect = new Rect();
        if (iam == null) {
            return windowRect;
        }
        try {
            ActivityManager.StackInfo stackInfo = iam.getStackInfo(0, 3);
            if (stackInfo == null) {
                stackInfo = iam.getStackInfo(1, 1);
            }
            if (stackInfo != null) {
                windowRect.set(stackInfo.bounds);
            }
            return windowRect;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Throwable th) {
        }
        return windowRect;
    }

    public static boolean isRecentsActivityVisible(MutableBoolean isHomeStackVisible, IActivityManager mIam, PackageManager mPm) {
        boolean z = false;
        if (mIam == null) {
            return false;
        }
        try {
            List<ActivityManager.StackInfo> stackInfos = mIam.getAllStackInfos();
            ActivityManager.StackInfo recentsStackInfo = null;
            ActivityManager.StackInfo fullscreenStackInfo = null;
            ActivityManager.StackInfo homeStackInfo = null;
            for (int i = 0; i < stackInfos.size(); i++) {
                ActivityManager.StackInfo stackInfo = stackInfos.get(i);
                WindowConfiguration winConfig = stackInfo.configuration.windowConfiguration;
                int activityType = winConfig.getActivityType();
                int windowingMode = winConfig.getWindowingMode();
                if (homeStackInfo == null && activityType == 2) {
                    homeStackInfo = stackInfo;
                } else if (fullscreenStackInfo == null && activityType == 1 && (windowingMode == 1 || windowingMode == 4)) {
                    fullscreenStackInfo = stackInfo;
                } else if (recentsStackInfo == null && activityType == 3) {
                    recentsStackInfo = stackInfo;
                }
            }
            boolean homeStackVisibleNotOccluded = isStackNotOccluded(homeStackInfo, fullscreenStackInfo);
            boolean recentsStackVisibleNotOccluded = isStackNotOccluded(recentsStackInfo, fullscreenStackInfo);
            if (isHomeStackVisible != null) {
                isHomeStackVisible.value = homeStackVisibleNotOccluded;
            }
            ComponentName topActivity = recentsStackInfo != null ? recentsStackInfo.topActivity : null;
            if (recentsStackVisibleNotOccluded && topActivity != null && topActivity.getPackageName().equals("com.android.systemui") && Recents.RECENTS_ACTIVITIES.contains(topActivity.getClassName())) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isStackNotOccluded(ActivityManager.StackInfo stackInfo, ActivityManager.StackInfo fullscreenStackInfo) {
        boolean z = true;
        boolean stackVisibleNotOccluded = stackInfo == null || stackInfo.visible;
        if (fullscreenStackInfo == null || stackInfo == null) {
            return stackVisibleNotOccluded;
        }
        if (fullscreenStackInfo.visible && fullscreenStackInfo.position > stackInfo.position) {
            z = false;
        }
        return stackVisibleNotOccluded & z;
    }

    private static ActivityManager.StackInfo getSplitScreenPrimaryStack(IActivityManager iam) {
        try {
            return iam.getStackInfo(3, 0);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static boolean hasDockedTask(IActivityManager mIam) {
        if (mIam == null) {
            return false;
        }
        ActivityManager.StackInfo stackInfo = getSplitScreenPrimaryStack(mIam);
        if (stackInfo == null) {
            return false;
        }
        int userId = ActivityManager.getCurrentUser();
        boolean hasUserTask = false;
        for (int i = stackInfo.taskUserIds.length - 1; i >= 0 && !hasUserTask; i--) {
            hasUserTask = stackInfo.taskUserIds[i] == userId || XSpaceUserHandle.isXSpaceUserId(stackInfo.taskUserIds[i]);
        }
        return hasUserTask;
    }

    public static boolean startTaskInDockedMode(Task task, int createMode, IActivityManager mIam, Context context) {
        if (mIam == null) {
            return false;
        }
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchWindowingMode(3);
            options.setSplitScreenCreateMode(createMode == 0 ? 0 : 1);
            mIam.startActivityFromRecents(task.key.id, options.toBundle());
            Log.i("SystemServicesProxy", "enter splitScreen mode");
            return true;
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "Failed to dock task: " + task + " with createMode: " + createMode, e);
            return false;
        }
    }

    public static String getConnectionSummary(Context context, CachedBluetoothDevice device) {
        return device == null ? "" : device.getConnectionSummary();
    }

    public static boolean setDeviceActive(CachedBluetoothDevice device) {
        if (device == null || device.isActiveDevice(2) || device.isActiveDevice(1) || device.isActiveDevice(21)) {
            return false;
        }
        return device.setActive();
    }

    public static boolean isDeviceActive(CachedBluetoothDevice device) {
        boolean z = false;
        if (device == null) {
            return false;
        }
        if (device.isActiveDevice(2) || device.isActiveDevice(1) || device.isActiveDevice(21)) {
            z = true;
        }
        return z;
    }
}
