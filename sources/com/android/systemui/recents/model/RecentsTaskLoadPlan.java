package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.UserInfo;
import android.content.pm.UserInfoCompat;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.List;
import miui.process.ProcessManager;

public class RecentsTaskLoadPlan {
    private static int MIN_NUM_TASKS = 5;
    private static int SESSION_BEGIN_TIME = 21600000;
    Context mContext;
    ArraySet<Integer> mCurrentQuietProfiles = new ArraySet<>();
    List<ActivityManager.RecentTaskInfo> mRawTasks;
    TaskStack mStack;

    public static class Options {
        public boolean loadIcons = true;
        public boolean loadThumbnails = true;
        public int numVisibleTaskThumbnails = 0;
        public int numVisibleTasks = 0;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int runningTaskId = -1;
    }

    RecentsTaskLoadPlan(Context context) {
        this.mContext = context;
    }

    private void updateCurrentQuietProfilesCache(int currentUserId) {
        this.mCurrentQuietProfiles.clear();
        if (currentUserId == -2) {
            currentUserId = ActivityManager.getCurrentUser();
        }
        List<UserInfo> profiles = ((UserManager) this.mContext.getSystemService("user")).getProfiles(currentUserId);
        if (profiles != null) {
            for (int i = 0; i < profiles.size(); i++) {
                UserInfo user = profiles.get(i);
                if (user.isManagedProfile() && UserInfoCompat.isQuietModeEnabled(user)) {
                    this.mCurrentQuietProfiles.add(Integer.valueOf(user.id));
                }
            }
        }
    }

    public synchronized void preloadRawTasks(boolean includeFrontMostExcludedTask) {
        updateCurrentQuietProfilesCache(-2);
        this.mRawTasks = Recents.getSystemServices().getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), -2, includeFrontMostExcludedTask, this.mCurrentQuietProfiles);
    }

    public synchronized void preloadPlan(RecentsTaskLoader loader, int runningTaskId, boolean includeFrontMostExcludedTask) {
        Drawable icon;
        boolean isLocked;
        String str;
        RecentsTaskLoader recentsTaskLoader = loader;
        synchronized (this) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            Resources res = this.mContext.getResources();
            ArrayList arrayList = new ArrayList();
            if (this.mRawTasks == null) {
                preloadRawTasks(includeFrontMostExcludedTask);
            } else {
                boolean z = includeFrontMostExcludedTask;
            }
            SparseArray<Task.TaskKey> affiliatedTasks = new SparseArray<>();
            SparseIntArray affiliatedTaskCounts = new SparseIntArray();
            String dismissDescFormat = this.mContext.getString(R.string.accessibility_recents_item_will_be_dismissed);
            String appInfoDescFormat = this.mContext.getString(R.string.accessibility_recents_item_open_app_info);
            int taskCount = this.mRawTasks.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < taskCount) {
                    ActivityManager.RecentTaskInfo t = this.mRawTasks.get(i2);
                    int taskCount2 = taskCount;
                    SparseArray<Task.TaskKey> affiliatedTasks2 = affiliatedTasks;
                    SparseIntArray affiliatedTaskCounts2 = affiliatedTaskCounts;
                    String dismissDescFormat2 = dismissDescFormat;
                    String appInfoDescFormat2 = appInfoDescFormat;
                    Task.TaskKey taskKey = new Task.TaskKey(t.persistentId, t.stackId, ssp.getWindowModeFromRecentTaskInfo(t), t.baseIntent, t.userId, t.firstActiveTime, t.lastActiveTime);
                    Task.TaskKey taskKey2 = taskKey;
                    boolean isFreeformTask = SystemServicesProxy.isFreeformStack(t.stackId);
                    boolean isLaunchTarget = taskKey2.id == runningTaskId;
                    ActivityInfo info = recentsTaskLoader.getAndUpdateActivityInfo(taskKey2);
                    String title = recentsTaskLoader.getAndUpdateActivityTitle(taskKey2, t.taskDescription);
                    String titleDescription = recentsTaskLoader.getAndUpdateContentDescription(taskKey2, res);
                    String dismissDescFormat3 = dismissDescFormat2;
                    String dismissDescription = String.format(dismissDescFormat3, new Object[]{titleDescription});
                    String appInfoDescFormat3 = appInfoDescFormat2;
                    String appInfoDescription = String.format(appInfoDescFormat3, new Object[]{titleDescription});
                    if (1 != 0) {
                        boolean z2 = isFreeformTask;
                        icon = recentsTaskLoader.getAndUpdateActivityIcon(taskKey2, t.taskDescription, res, false);
                    } else {
                        icon = null;
                    }
                    boolean isAccessLocked = ssp.isAccessLocked(taskKey2);
                    SystemServicesProxy ssp2 = ssp;
                    Bitmap thumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey2, false, isAccessLocked);
                    int activityColor = recentsTaskLoader.getActivityPrimaryColor(t.taskDescription);
                    int backgroundColor = recentsTaskLoader.getActivityBackgroundColor(t.taskDescription);
                    boolean isSystemApp = (info == null || (info.applicationInfo.flags & 1) == 0) ? false : true;
                    if (info != null) {
                        try {
                            str = info.packageName;
                        } catch (Exception e) {
                            Log.e("RecentsTaskLoadPlan", "getAppLockStateForUserId", e);
                            isLocked = false;
                        }
                    } else {
                        str = null;
                    }
                    isLocked = ProcessManager.isLockedApplication(str, t.userId);
                    Resources res2 = res;
                    ActivityInfo activityInfo = info;
                    Task task = new Task(taskKey2, t.affiliatedTaskId, t.affiliatedTaskColor, icon, thumbnail, title, titleDescription, dismissDescription, appInfoDescription, activityColor, backgroundColor, isLaunchTarget, true, isSystemApp, ActivityManagerCompat.isRecentTaskDockable(t), ActivityManagerCompat.getRecentTaskBound(t), t.taskDescription, ActivityManagerCompat.getRecentTaskResizeMode(t), t.topActivity, isLocked, isAccessLocked);
                    arrayList.add(task);
                    SparseIntArray affiliatedTaskCounts3 = affiliatedTaskCounts2;
                    affiliatedTaskCounts3.put(taskKey2.id, affiliatedTaskCounts3.get(taskKey2.id, 0) + 1);
                    SparseArray<Task.TaskKey> affiliatedTasks3 = affiliatedTasks2;
                    affiliatedTasks3.put(taskKey2.id, taskKey2);
                    i = i2 + 1;
                    affiliatedTasks = affiliatedTasks3;
                    affiliatedTaskCounts = affiliatedTaskCounts3;
                    appInfoDescFormat = appInfoDescFormat3;
                    dismissDescFormat = dismissDescFormat3;
                    taskCount = taskCount2;
                    ssp = ssp2;
                    res = res2;
                    recentsTaskLoader = loader;
                    boolean z3 = includeFrontMostExcludedTask;
                } else {
                    Resources resources = res;
                    SparseArray<Task.TaskKey> sparseArray = affiliatedTasks;
                    String str2 = dismissDescFormat;
                    String str3 = appInfoDescFormat;
                    int i3 = taskCount;
                    SparseIntArray sparseIntArray = affiliatedTaskCounts;
                    this.mStack = new TaskStack();
                    this.mStack.setTasks(this.mContext, arrayList, false);
                }
            }
        }
    }

    public synchronized void executePlan(Options opts, RecentsTaskLoader loader, TaskResourceLoadQueue loadQueue) {
        Options options = opts;
        RecentsTaskLoader recentsTaskLoader = loader;
        synchronized (this) {
            RecentsConfiguration config = Recents.getConfiguration();
            Resources res = this.mContext.getResources();
            ArrayList<Task> tasks = this.mStack.getStackTasks();
            int taskCount = tasks.size();
            int i = 0;
            while (i < taskCount) {
                Task task = tasks.get(i);
                Task.TaskKey taskKey = task.key;
                boolean isRunningTask = task.key.id == options.runningTaskId;
                boolean isVisibleTask = i <= options.numVisibleTasks;
                boolean isVisibleThumbnail = i <= options.numVisibleTaskThumbnails;
                if (!options.onlyLoadPausedActivities || !isRunningTask) {
                    if (options.loadIcons && ((isRunningTask || isVisibleTask) && task.icon == null)) {
                        task.icon = recentsTaskLoader.getAndUpdateActivityIcon(taskKey, task.taskDescription, res, true);
                    }
                    if (options.loadThumbnails && ((isRunningTask || isVisibleThumbnail) && (task.thumbnail == null || isRunningTask || task.isAccessLocked))) {
                        if (config.svelteLevel <= 1) {
                            task.thumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey, true, task.isAccessLocked);
                        } else if (config.svelteLevel == 2) {
                            loadQueue.addTask(task);
                            i++;
                        }
                    }
                }
                TaskResourceLoadQueue taskResourceLoadQueue = loadQueue;
                i++;
            }
            TaskResourceLoadQueue taskResourceLoadQueue2 = loadQueue;
        }
    }

    public TaskStack getTaskStack() {
        return this.mStack;
    }

    public boolean hasTasks() {
        boolean z = false;
        if (this.mStack == null) {
            return false;
        }
        if (this.mStack.getTaskCount() > 0) {
            z = true;
        }
        return z;
    }
}
