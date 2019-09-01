package android.app;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;
import java.util.List;

public class ActivityManagerCompat {
    public static IActivityManager getService() {
        return ActivityManager.getService();
    }

    public static void registerUserSwitchObserver(IUserSwitchObserver observer, String name) throws RemoteException {
        getService().registerUserSwitchObserver(observer, name);
    }

    public static int getLastResumedActivityUserId(int userIdLegacy) throws RemoteException {
        return getService().getLastResumedActivityUserId();
    }

    public static void startConfirmDeviceCredentialIntent(Intent intent, Bundle options) throws RemoteException {
        getService().startConfirmDeviceCredentialIntent(intent, options);
    }

    public static void setHasTopUi(boolean hasTopUi) throws RemoteException {
        getService().setHasTopUi(hasTopUi);
    }

    public static void keyguardGoingAway(int flags) throws RemoteException {
        getService().keyguardGoingAway(flags);
    }

    public static int getUserId(ActivityManager.StackInfo info) {
        return info.userId;
    }

    public static void setLockScreenShown(boolean showing, boolean occluded) throws RemoteException {
        getService().setLockScreenShown(showing, false, -1);
    }

    public static void stopSystemLockTaskMode() throws RemoteException {
        getService().stopSystemLockTaskMode();
    }

    public static void startSystemLockTaskMode(int taskId) throws RemoteException {
        getService().startSystemLockTaskMode(taskId);
    }

    public static List<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) throws RemoteException {
        return getService().getRecentTasks(maxNum, flags, userId).getList();
    }

    public static void logoutCurrentUser() {
        ActivityManager.logoutCurrentUser();
    }

    public static int getRunningTaskStackId(ActivityManager.RunningTaskInfo runningTask) {
        return runningTask.stackId;
    }

    public static int getRunningTaskResizeMode(ActivityManager.RunningTaskInfo runningTask) {
        return runningTask.resizeMode;
    }

    public static Rect getRecentTaskBound(ActivityManager.RecentTaskInfo t) {
        return t.bounds;
    }

    public static int getRecentTaskResizeMode(ActivityManager.RecentTaskInfo t) {
        return t.resizeMode;
    }

    public static int getTaskDescriptionBackgroundColor(ActivityManager.TaskDescription td) {
        return td.getBackgroundColor();
    }

    public static Bitmap loadTaskDescriptionIcon(String iconFilename, int userId) {
        return ActivityManager.TaskDescription.loadTaskDescriptionIcon(iconFilename, userId);
    }

    public static ActivityManager.StackInfo getStackInfo(int stackId, int windowingMode, int activityType) throws RemoteException {
        return getService().getStackInfo(windowingMode, activityType);
    }

    public static List<ActivityManager.RecentTaskInfo> getRecentTasksForUser(ActivityManager activityManager, int maxNum, int flags, int userId) throws RemoteException {
        return ActivityManager.getService().getRecentTasks(maxNum, flags, userId).getList();
    }

    public static int getFocusedStackId() throws RemoteException {
        ActivityManager.StackInfo stackInfo = getService().getFocusedStackInfo();
        if (stackInfo != null) {
            return stackInfo.stackId;
        }
        return -1;
    }

    public static boolean isRunningTaskDockable(ActivityManager.RunningTaskInfo runningTask) {
        return runningTask.supportsSplitScreenMultiWindow;
    }

    public static boolean isRecentTaskDockable(ActivityManager.RecentTaskInfo recentTask) {
        return recentTask.supportsSplitScreenMultiWindow;
    }

    public static void startInPlaceAnimationOnFrontMostApplication(IActivityManager mIam, ActivityOptions opts) throws RemoteException {
        mIam.startInPlaceAnimationOnFrontMostApplication(opts == null ? null : opts.toBundle());
    }

    public static boolean moveTaskToDockedStack(IActivityManager mIam, int taskId, int createMode, boolean onTop, boolean animate, Rect initialBounds, boolean moveHomeStackFront) throws RemoteException {
        return mIam.setTaskWindowingModeSplitScreenPrimary(taskId, createMode, true, false, initialBounds, true);
    }
}
