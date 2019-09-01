package com.android.systemui.recents.misc;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.Display;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDockedStackListener;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.R;
import com.android.systemui.SystemUICompat;
import com.android.systemui.fsgesture.IFsGestureCallback;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.TaskSnapshotChangedEvent;
import com.android.systemui.recents.model.MutableBoolean;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.ThumbnailData;
import com.miui.browser.webapps.WebAppDAO;
import com.miui.browser.webapps.WebAppInfo;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import miui.maml.util.AppIconsHelper;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.security.SecurityManager;
import miui.securityspace.XSpaceUserHandle;
import org.json.JSONObject;

public class SystemServicesProxy {
    public static boolean DEBUG = false;
    static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    private static final List<String> sMultiWindowForceNotResizePkgList = new ArrayList();
    private static final List<String> sMultiWindowForceResizePkgList = new ArrayList();
    static final List<String> sRecentsBlacklist = new ArrayList();
    private static SystemServicesProxy sSystemServicesProxy;
    private int mAccessControlLockMode;
    private SoftReference<Bitmap> mAccessLockedFakeScreenshotLand;
    private SoftReference<Bitmap> mAccessLockedFakeScreenshotPort;
    AccessibilityManager mAccm;
    ActivityManager mAm;
    ComponentName mAssistComponent;
    AssistUtils mAssistUtils;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;
    Context mContext;
    Display mDisplay;
    /* access modifiers changed from: private */
    public final IDreamManager mDreamManager;
    int mDummyThumbnailHeight;
    int mDummyThumbnailWidth;
    /* access modifiers changed from: private */
    public final Handler mHandler = new H();
    boolean mHasFreeformWorkspaceSupport;
    public final HashMap<String, IFsGestureCallback> mIFsGestureCallbackMap = new HashMap<>();
    IActivityManager mIam;
    IPackageManager mIpm;
    private boolean mIsFsGestureAnimating = false;
    boolean mIsSafeMode;
    IWindowManager mIwm;
    PackageManager mPm;
    String mRecentsPackage;
    int mStatusBarHeight;
    private TaskStackListener mTaskStackListener = new TaskStackListener() {
        private final List<TaskStackListener> mTmpListeners = new ArrayList();

        public void onTaskStackChanged() throws RemoteException {
            synchronized (SystemServicesProxy.this.mTaskStackListeners) {
                this.mTmpListeners.clear();
                this.mTmpListeners.addAll(SystemServicesProxy.this.mTaskStackListeners);
            }
            for (int i = this.mTmpListeners.size() - 1; i >= 0; i--) {
                this.mTmpListeners.get(i).onTaskStackChangedBackground();
            }
            SystemServicesProxy.this.mHandler.removeMessages(1);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(1);
        }

        public void onActivityPinned(String packageName, int userId, int taskId, int stackId) throws RemoteException {
        }

        public void onActivityUnpinned() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(10);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(10);
        }

        public void onPinnedActivityRestartAttempt(boolean clearedTask) throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(4);
            SystemServicesProxy.this.mHandler.obtainMessage(4, clearedTask, 0).sendToTarget();
        }

        public void onPinnedStackAnimationStarted() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(9);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(9);
        }

        public void onPinnedStackAnimationEnded() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(5);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(5);
        }

        public void onActivityForcedResizable(String packageName, int taskId, int reason) throws RemoteException {
            SystemServicesProxy.this.mHandler.obtainMessage(6, taskId, reason, packageName).sendToTarget();
        }

        public void onActivityDismissingDockedStack() throws RemoteException {
            SystemServicesProxy.this.mHandler.sendEmptyMessage(7);
        }

        public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
            SystemServicesProxy.this.mHandler.sendEmptyMessage(11);
        }

        public void onTaskProfileLocked(int taskId, int userId) {
            SystemServicesProxy.this.mHandler.obtainMessage(8, taskId, userId).sendToTarget();
        }

        public void onTaskSnapshotChanged(int taskId, ActivityManager.TaskSnapshot snapshot) throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(2);
            SystemServicesProxy.this.mHandler.obtainMessage(2, taskId, 0, snapshot).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public List<TaskStackListener> mTaskStackListeners = new ArrayList();
    TtsEngines mTtsEngines;
    UserManager mUm;
    WebAppDAO mWebAppDAO;
    public WindowManager mWm;
    WallpaperManager mWpm;

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            synchronized (SystemServicesProxy.this.mTaskStackListeners) {
                switch (msg.what) {
                    case 1:
                        Trace.beginSection("onTaskStackChanged");
                        for (int i = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i)).onTaskStackChanged();
                        }
                        Trace.endSection();
                        break;
                    case 2:
                        Trace.beginSection("onTaskSnapshotChanged");
                        for (int i2 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i2 >= 0; i2--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i2)).onTaskSnapshotChanged(msg.arg1, (ActivityManager.TaskSnapshot) msg.obj);
                        }
                        Trace.endSection();
                        break;
                    case 3:
                        for (int i3 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i3 >= 0; i3--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i3)).onActivityPinned((String) msg.obj, msg.arg1, msg.arg2);
                        }
                        break;
                    case 4:
                        for (int i4 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i4 >= 0; i4--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i4)).onPinnedActivityRestartAttempt(msg.arg1 != 0);
                        }
                        break;
                    case 5:
                        for (int i5 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i5 >= 0; i5--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i5)).onPinnedStackAnimationEnded();
                        }
                        break;
                    case 6:
                        for (int i6 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i6 >= 0; i6--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i6)).onActivityForcedResizable((String) msg.obj, msg.arg1, msg.arg2);
                        }
                        break;
                    case 7:
                        for (int i7 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i7 >= 0; i7--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i7)).onActivityDismissingDockedStack();
                        }
                        break;
                    case 8:
                        for (int i8 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i8 >= 0; i8--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i8)).onTaskProfileLocked(msg.arg1, msg.arg2);
                        }
                        break;
                    case 9:
                        for (int i9 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i9 >= 0; i9--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i9)).onPinnedStackAnimationStarted();
                        }
                        break;
                    case 10:
                        for (int i10 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i10 >= 0; i10--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i10)).onActivityUnpinned();
                        }
                        break;
                    case 11:
                        for (int i11 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i11 >= 0; i11--) {
                            ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i11)).onActivityLaunchOnSecondaryDisplayFailed();
                        }
                        break;
                }
            }
        }
    }

    public static abstract class TaskStackListener {
        public void onTaskStackChangedBackground() {
        }

        public void onTaskStackChanged() {
        }

        public void onTaskSnapshotChanged(int taskId, ActivityManager.TaskSnapshot snapshot) {
        }

        public void onActivityPinned(String packageName, int userId, int taskId) {
        }

        public void onActivityUnpinned() {
        }

        public void onPinnedActivityRestartAttempt(boolean clearedTask) {
        }

        public void onPinnedStackAnimationStarted() {
        }

        public void onPinnedStackAnimationEnded() {
        }

        public void onActivityForcedResizable(String packageName, int taskId, int reason) {
        }

        public void onActivityDismissingDockedStack() {
        }

        public void onActivityLaunchOnSecondaryDisplayFailed() {
        }

        public void onTaskProfileLocked(int taskId, int userId) {
        }

        /* access modifiers changed from: protected */
        public final boolean checkCurrentUserId(Context context, boolean debug) {
            int processUserId = UserHandle.myUserId();
            int currentUserId = SystemServicesProxy.getInstance(context).getCurrentUser();
            if (processUserId == currentUserId) {
                return true;
            }
            if (debug) {
                Log.d("SystemServicesProxy", "UID mismatch. SystemUI is running uid=" + processUserId + " and the current user is uid=" + currentUserId);
            }
            return false;
        }
    }

    static {
        sBitmapOptions.inMutable = true;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sRecentsBlacklist.add("com.android.systemui.tv.pip.PipOnboardingActivity");
        sRecentsBlacklist.add("com.android.systemui.tv.pip.PipMenuActivity");
        sRecentsBlacklist.add("com.android.systemui.recents.RecentsActivity");
    }

    private SystemServicesProxy(Context context) {
        this.mAccm = AccessibilityManager.getInstance(context);
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mIam = ActivityManagerNative.getDefault();
        this.mPm = context.getPackageManager();
        this.mIpm = AppGlobals.getPackageManager();
        this.mAssistUtils = new AssistUtils(context);
        this.mWm = (WindowManager) context.getSystemService("window");
        this.mIwm = WindowManagerGlobal.getWindowManagerService();
        this.mUm = UserManager.get(context);
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mWpm = WallpaperManager.getInstance(context);
        this.mRecentsPackage = context.getPackageName();
        this.mHasFreeformWorkspaceSupport = false;
        this.mIsSafeMode = this.mPm.isSafeMode();
        Resources res = context.getResources();
        this.mDummyThumbnailWidth = res.getDimensionPixelSize(17104898);
        this.mDummyThumbnailHeight = res.getDimensionPixelSize(17104897);
        this.mStatusBarHeight = res.getDimensionPixelSize(17105351);
        this.mBgProtectionPaint = new Paint();
        this.mBgProtectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        this.mBgProtectionPaint.setColor(-1);
        this.mBgProtectionCanvas = new Canvas();
        this.mAssistComponent = this.mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
        this.mWebAppDAO = WebAppDAO.getInstance(context);
        this.mTtsEngines = new TtsEngines(this.mContext);
        this.mContext = context.getApplicationContext();
        this.mAccessControlLockMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_mode", 1, -2);
    }

    public static SystemServicesProxy getInstance(Context context) {
        if (Looper.getMainLooper().isCurrentThread()) {
            if (sSystemServicesProxy == null) {
                sSystemServicesProxy = new SystemServicesProxy(context);
            }
            return sSystemServicesProxy;
        }
        throw new RuntimeException("Must be called on the UI thread");
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int numLatestTasks, int userId, boolean includeFrontMostExcludedTask, ArraySet<Integer> quietProfileIds) {
        List<ActivityManager.RecentTaskInfo> tasks = null;
        if (this.mAm == null) {
            return null;
        }
        int numTasksToQuery = Math.max(10, numLatestTasks);
        int flags = 62;
        if (includeFrontMostExcludedTask) {
            flags = 62 | 1;
        }
        try {
            tasks = ActivityManager.getService().getRecentTasks(numTasksToQuery, flags, userId).getList();
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "Failed to get recent tasks", e);
        }
        if (tasks == null) {
            return new ArrayList();
        }
        boolean isFirstValidTask = true;
        Iterator<ActivityManager.RecentTaskInfo> iter = tasks.iterator();
        while (true) {
            boolean isExcluded = false;
            if (!iter.hasNext()) {
                return tasks.subList(0, Math.min(tasks.size(), numLatestTasks));
            }
            ActivityManager.RecentTaskInfo t = iter.next();
            if (t == null || t.topActivity == null || (!sRecentsBlacklist.contains(t.topActivity.getClassName()) && !sRecentsBlacklist.contains(t.topActivity.getPackageName()))) {
                if ((t.baseIntent.getFlags() & 8388608) == 8388608) {
                    isExcluded = true;
                }
                if ((isExcluded || quietProfileIds.contains(Integer.valueOf(t.userId))) && (!isFirstValidTask || !includeFrontMostExcludedTask)) {
                    iter.remove();
                }
                isFirstValidTask = false;
            } else {
                iter.remove();
            }
        }
    }

    public ActivityManager.RunningTaskInfo getRunningTask() {
        List<ActivityManager.RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0);
    }

    public boolean isRecentsActivityVisible() {
        return isRecentsActivityVisible(null);
    }

    public boolean isRecentsActivityVisible(MutableBoolean isHomeStackVisible) {
        return SystemUICompat.isRecentsActivityVisible(isHomeStackVisible, this.mIam, this.mPm);
    }

    public boolean hasFreeformWorkspaceSupport() {
        return this.mHasFreeformWorkspaceSupport;
    }

    public boolean isInSafeMode() {
        return this.mIsSafeMode;
    }

    public boolean startTaskInDockedMode(Task task, int createMode, Context context) {
        return SystemUICompat.startTaskInDockedMode(task, createMode, this.mIam, context);
    }

    public boolean startTaskInDockedMode(int taskId, int createMode) {
        if (this.mIam == null) {
            return false;
        }
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchWindowingMode(3);
            options.setSplitScreenCreateMode(createMode == 0 ? 0 : 1);
            this.mIam.startActivityFromRecents(taskId, options.toBundle());
            return true;
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "Failed to dock taskId: " + taskId + " with createMode: " + createMode, e);
            return false;
        }
    }

    public boolean moveTaskToDockedStack(int taskId, int createMode, Rect initialBounds) {
        if (this.mIam == null) {
            return false;
        }
        try {
            return ActivityManagerCompat.moveTaskToDockedStack(this.mIam, taskId, createMode, true, false, initialBounds, true);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isHomeOrRecentsStack(int stackId, ActivityManager.RunningTaskInfo runningTask) {
        return SystemUICompat.isHomeOrRecentsStack(stackId, runningTask);
    }

    public static boolean isFreeformStack(int stackId) {
        return stackId == 2;
    }

    public boolean hasDockedTask() {
        return SystemUICompat.hasDockedTask(this.mIam);
    }

    public int getWindowModeFromRecentTaskInfo(ActivityManager.RecentTaskInfo info) {
        return info.configuration.windowConfiguration.getWindowingMode();
    }

    public boolean hasSoftNavigationBar() {
        try {
            return WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cancelWindowTransition(int taskId) {
        if (this.mIam != null) {
            try {
                SystemUICompat.cancelTaskWindowTransition(this.mIam, taskId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelThumbnailTransition(int taskId) {
        if (this.mIam != null) {
            try {
                SystemUICompat.cancelTaskThumbnailTransition(this.mIam, taskId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public ThumbnailData getTaskThumbnail(Task.TaskKey taskKey) {
        if (this.mAm == null) {
            return null;
        }
        ThumbnailData thumbnailData = new ThumbnailData();
        getThumbnail(taskKey, thumbnailData);
        if (thumbnailData.thumbnail != null) {
            thumbnailData.thumbnail.setHasAlpha(false);
        }
        return thumbnailData;
    }

    public void getThumbnail(Task.TaskKey taskKey, ThumbnailData thumbnailDataOut) {
        boolean z = false;
        if (isAccessLocked(taskKey)) {
            int i = 2;
            if (this.mDisplay.getRotation() == 0 || this.mDisplay.getRotation() == 2) {
                z = true;
            }
            boolean mIsPort = z;
            Bitmap accessLockedFakeScreenshot = getAccessLockedFakeScreenshot(mIsPort);
            thumbnailDataOut.thumbnail = accessLockedFakeScreenshot;
            ActivityManager.TaskThumbnailInfo taskThumbnailInfo = new ActivityManager.TaskThumbnailInfo();
            if (mIsPort) {
                i = 1;
            }
            taskThumbnailInfo.screenOrientation = i;
            taskThumbnailInfo.taskWidth = accessLockedFakeScreenshot.getWidth();
            taskThumbnailInfo.taskHeight = accessLockedFakeScreenshot.getHeight();
            thumbnailDataOut.thumbnailInfo = taskThumbnailInfo;
            thumbnailDataOut.isAccessLocked = true;
        } else if (this.mAm != null) {
            ActivityManager.TaskSnapshot snapshot = null;
            try {
                snapshot = android.app.ActivityManager.getService().getTaskSnapshot(taskKey.id, true);
            } catch (RemoteException e) {
                Log.w("SystemServicesProxy", "Failed to retrieve task snapshot", e);
            }
            Bitmap thumbnail = null;
            if (snapshot != null) {
                thumbnail = Bitmap.createHardwareBitmap(snapshot.getSnapshot());
            }
            thumbnailDataOut.thumbnail = thumbnail;
            thumbnailDataOut.thumbnailInfo = getTaskThumbnailInfo(snapshot);
            thumbnailDataOut.isAccessLocked = false;
        }
    }

    private ActivityManager.TaskThumbnailInfo getTaskThumbnailInfo(ActivityManager.TaskSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        ActivityManager.TaskThumbnailInfo taskThumbnailInfo = new ActivityManager.TaskThumbnailInfo();
        taskThumbnailInfo.taskWidth = snapshot.getSnapshot().getWidth();
        taskThumbnailInfo.taskHeight = snapshot.getSnapshot().getHeight();
        taskThumbnailInfo.screenOrientation = snapshot.getOrientation();
        taskThumbnailInfo.insets = snapshot.getContentInsets();
        taskThumbnailInfo.scale = snapshot.getScale();
        return taskThumbnailInfo;
    }

    public void moveTaskToStack(int taskId, int stackId) {
        if (this.mIam != null) {
            try {
                this.mIam.positionTaskInStack(taskId, stackId, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void removeTask(final int taskId, boolean remainProcess) {
        if (this.mAm != null) {
            BackgroundThread.getHandler().post(new Runnable() {
                public void run() {
                    try {
                        android.app.ActivityManager.getService().removeTask(taskId);
                    } catch (RemoteException e) {
                        Log.w("SystemServicesProxy", "Failed to remove task=" + taskId, e);
                    }
                }
            });
        }
    }

    public void killProcess(final Task task) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                ProcessConfig processConfig = new ProcessConfig(7, task.key.getComponent().getPackageName(), task.key.userId, task.key.id);
                processConfig.setRemoveTaskNeeded(true);
                ProcessManager.kill(processConfig);
            }
        });
    }

    public void sendCloseSystemWindows(String reason) {
        try {
            this.mIam.closeSystemDialogs(reason);
        } catch (RemoteException e) {
        }
    }

    public ActivityInfo getActivityInfo(ComponentName cn, int userId) {
        if (this.mIpm == null) {
            return null;
        }
        try {
            return this.mIpm.getActivityInfo(cn, 128, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ActivityInfo getActivityInfo(ComponentName cn) {
        if (this.mPm == null) {
            return null;
        }
        try {
            return this.mPm.getActivityInfo(cn, 128);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getBadgedActivityLabel(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        String title = null;
        WebAppInfo webAppInfo = this.mWebAppDAO.get(info);
        if (webAppInfo != null) {
            title = webAppInfo.mLabel;
        }
        if (title == null) {
            title = info.loadLabel(this.mPm).toString();
        }
        return getBadgedLabel(title, userId);
    }

    public String getBadgedContentDescription(ActivityInfo info, int userId, Resources res) {
        String activityLabel = info.loadLabel(this.mPm).toString();
        String applicationLabel = info.applicationInfo.loadLabel(this.mPm).toString();
        String badgedApplicationLabel = getBadgedLabel(applicationLabel, userId);
        if (applicationLabel.equals(activityLabel)) {
            return badgedApplicationLabel;
        }
        return res.getString(R.string.accessibility_recents_task_header, new Object[]{badgedApplicationLabel, activityLabel});
    }

    public Drawable getBadgedActivityIcon(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        Drawable icon = null;
        WebAppInfo webAppInfo = this.mWebAppDAO.get(info);
        if (webAppInfo != null) {
            icon = webAppInfo.getIcon(this.mContext);
        }
        if (icon == null) {
            icon = AppIconsHelper.getIconDrawable(this.mContext, info, this.mPm, 43200000);
        }
        if (icon == null) {
            icon = info.loadIcon(this.mPm);
        }
        if (XSpaceUserHandle.isXSpaceUserId(userId)) {
            icon = XSpaceUserHandle.getXSpaceIcon(this.mContext, icon);
        }
        return getBadgedIcon(icon, userId);
    }

    public Drawable getBadgedTaskDescriptionIcon(ActivityManager.TaskDescription taskDescription, int userId, Resources res) {
        Bitmap tdIcon = taskDescription.getInMemoryIcon();
        if (tdIcon == null) {
            tdIcon = ActivityManagerCompat.loadTaskDescriptionIcon(taskDescription.getIconFilename(), userId);
        }
        if (tdIcon != null) {
            return getBadgedIcon(new BitmapDrawable(res, tdIcon), userId);
        }
        return null;
    }

    private Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    private String getBadgedLabel(String label, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedLabel(label, new UserHandle(userId)).toString();
        }
        return label;
    }

    public boolean isSystemUser(int userId) {
        return userId == UserHandleCompat.SYSTEM.getIdentifier();
    }

    public int getCurrentUser() {
        if (this.mAm == null) {
            return 0;
        }
        android.app.ActivityManager activityManager = this.mAm;
        return android.app.ActivityManager.getCurrentUser();
    }

    public int getProcessUser() {
        if (this.mUm == null) {
            return 0;
        }
        return this.mUm.getUserHandle();
    }

    public boolean isTouchExplorationEnabled() {
        boolean z = false;
        if (this.mAccm == null) {
            return false;
        }
        if (this.mAccm.isEnabled() && this.mAccm.isTouchExplorationEnabled()) {
            z = true;
        }
        return z;
    }

    public boolean isScreenPinningActive() {
        if (this.mIam == null) {
            return false;
        }
        try {
            return this.mIam.isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public int getSystemSetting(Context context, String setting) {
        return Settings.System.getInt(context.getContentResolver(), setting, 0);
    }

    public int getDeviceSmallestWidth() {
        if (this.mDisplay == null) {
            return 0;
        }
        Point smallestSizeRange = new Point();
        this.mDisplay.getCurrentSizeRange(smallestSizeRange, new Point());
        return smallestSizeRange.x;
    }

    public Rect getDisplayRect() {
        Rect displayRect = new Rect();
        if (this.mDisplay == null) {
            return displayRect;
        }
        Point p = new Point();
        this.mDisplay.getRealSize(p);
        displayRect.set(0, 0, p.x, p.y);
        return displayRect;
    }

    public Rect getWindowRect() {
        return SystemUICompat.getRecentsWindowRect(this.mIam);
    }

    public int getDisplayRotation() {
        if (this.mDisplay == null) {
            return 0;
        }
        return this.mDisplay.getRotation();
    }

    public boolean startActivityFromRecents(Context context, Task.TaskKey taskKey, String taskName, ActivityOptions options) {
        if (this.mIam != null) {
            try {
                if (taskKey.windowingMode == 3) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLaunchWindowingMode(4);
                }
                this.mIam.startActivityFromRecents(taskKey.id, options == null ? null : options.toBundle());
                return true;
            } catch (Exception e) {
                Log.e("SystemServicesProxy", context.getString(R.string.recents_launch_error_message, new Object[]{taskName}), e);
            }
        }
        return false;
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions opts) {
        if (this.mIam != null) {
            try {
                ActivityManagerCompat.startInPlaceAnimationOnFrontMostApplication(this.mIam, opts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerTaskStackListener(TaskStackListener listener) {
        if (this.mIam != null) {
            this.mTaskStackListeners.add(listener);
            if (this.mTaskStackListeners.size() == 1) {
                try {
                    this.mIam.registerTaskStackListener(this.mTaskStackListener);
                } catch (Exception e) {
                    Log.w("SystemServicesProxy", "Failed to call registerTaskStackListener", e);
                }
            }
        }
    }

    public void endProlongedAnimations() {
        if (this.mWm != null) {
            try {
                WindowManagerGlobal.getWindowManagerService().endProlongedAnimations();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerDockedStackListener(IDockedStackListener listener) {
        if (this.mWm != null) {
            try {
                WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getDockedDividerSize(Context context) {
        Resources res = context.getResources();
        return res.getDimensionPixelSize(R.dimen.docked_stack_divider_thickness) - (2 * res.getDimensionPixelSize(R.dimen.docked_stack_divider_insets));
    }

    public void getStableInsets(Rect outStableInsets) {
        if (this.mWm != null) {
            try {
                SystemUICompat.getStableInsets(outStableInsets);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar")) {
                outStableInsets.left = 0;
                outStableInsets.right = 0;
                outStableInsets.bottom = 0;
            }
        }
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture future, IRemoteCallback animStartedListener, boolean scaleUp) {
        try {
            WindowManagerGlobal.getWindowManagerService().overridePendingAppTransitionMultiThumbFuture(future, animStartedListener, scaleUp);
        } catch (RemoteException e) {
            Log.w("SystemServicesProxy", "Failed to override transition: " + e);
        }
    }

    public void setAccessControlLockMode(int accessControlLockMode) {
        this.mAccessControlLockMode = accessControlLockMode;
    }

    public boolean isAccessLocked(Task.TaskKey taskKey) {
        SecurityManager sm = (SecurityManager) this.mContext.getSystemService("security");
        boolean z = false;
        if (!sm.isAccessControlActived(this.mContext) || !sm.getApplicationAccessControlEnabledAsUser(taskKey.getComponent().getPackageName(), taskKey.userId)) {
            return false;
        }
        if (this.mAccessControlLockMode == 0 || !sm.checkAccessControlPassAsUser(taskKey.getComponent().getPackageName(), taskKey.userId)) {
            z = true;
        }
        return z;
    }

    private Bitmap getAccessLockedFakeScreenshot(boolean mIsPort) {
        Bitmap bmp = null;
        if (mIsPort) {
            if (this.mAccessLockedFakeScreenshotPort != null) {
                bmp = this.mAccessLockedFakeScreenshotPort.get();
            }
        } else if (this.mAccessLockedFakeScreenshotLand != null) {
            bmp = this.mAccessLockedFakeScreenshotLand.get();
        }
        if (bmp == null) {
            bmp = createAccessLockedFakeScreenshot();
            if (mIsPort) {
                this.mAccessLockedFakeScreenshotPort = new SoftReference<>(bmp);
            } else {
                this.mAccessLockedFakeScreenshotLand = new SoftReference<>(bmp);
            }
        }
        return bmp;
    }

    private Bitmap createAccessLockedFakeScreenshot() {
        Point size = new Point();
        Rect displayRect = getDisplayRect();
        size.set((int) (((float) displayRect.width()) * 0.6f), (int) (((float) displayRect.height()) * 0.6f));
        Bitmap bmp = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.task_access_locked_app, null);
        view.measure(View.MeasureSpec.makeMeasureSpec(size.x, 1073741824), View.MeasureSpec.makeMeasureSpec(size.y, 1073741824));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bmp;
    }

    public void onLanguageChange() {
        if (this.mAccessLockedFakeScreenshotPort != null) {
            this.mAccessLockedFakeScreenshotPort.clear();
        }
        if (this.mAccessLockedFakeScreenshotLand != null) {
            this.mAccessLockedFakeScreenshotLand.clear();
        }
    }

    public void setRecentsVisibility(Context context, boolean visible) {
        SystemUICompat.setRecentsVisibility(context, visible);
    }

    public void setPipVisibility(boolean visible) {
        try {
            this.mIwm.setPipVisibility(visible);
        } catch (RemoteException e) {
            Log.e("SystemServicesProxy", "Unable to reach window manager", e);
        }
    }

    public void awakenDreamsAsync() {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                try {
                    SystemServicesProxy.this.mDreamManager.awaken();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void registerMiuiTaskResizeList(final Context context) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                List<String> multiWindowForceResizeList = SystemServicesProxy.getMultiWindowForceResizeList(context);
                try {
                    SystemServicesProxy.this.mAm.getClass().getMethod("setResizeWhiteList", new Class[]{List.class}).invoke(SystemServicesProxy.this.mAm, new Object[]{multiWindowForceResizeList});
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e2) {
                    e2.printStackTrace();
                } catch (IllegalAccessException e3) {
                    e3.printStackTrace();
                }
                List<String> multiWindowForceNotResizeList = SystemServicesProxy.getMultiWindowForceNotResizeList(context);
                try {
                    SystemServicesProxy.this.mAm.getClass().getMethod("setResizeBlackList", new Class[]{List.class}).invoke(SystemServicesProxy.this.mAm, new Object[]{multiWindowForceNotResizeList});
                } catch (NoSuchMethodException e4) {
                    e4.printStackTrace();
                } catch (InvocationTargetException e5) {
                    e5.printStackTrace();
                } catch (IllegalAccessException e6) {
                    e6.printStackTrace();
                }
            }
        });
    }

    public static List<String> getMultiWindowForceResizeList(Context context) {
        synchronized (sMultiWindowForceResizePkgList) {
            if (sMultiWindowForceResizePkgList.isEmpty()) {
                List<MiuiSettings.SettingsCloudData.CloudData> multiWindowForceResizePkgsCloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(context.getContentResolver(), Utilities.isAndroidNorNewer() ? "MultiWindowForceResizePkgsForN" : "MultiWindowForceResizePkgsForM");
                if (multiWindowForceResizePkgsCloudDataList != null) {
                    try {
                        for (MiuiSettings.SettingsCloudData.CloudData data : multiWindowForceResizePkgsCloudDataList) {
                            String json = data.toString();
                            Log.d("SystemServicesProxy", "json=" + json);
                            if (!TextUtils.isEmpty(json)) {
                                String pkg = new JSONObject(json).optString("pkg");
                                if (!TextUtils.isEmpty(pkg)) {
                                    sMultiWindowForceResizePkgList.add(pkg);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (sMultiWindowForceResizePkgList.isEmpty()) {
                sMultiWindowForceResizePkgList.addAll(Arrays.asList(context.getResources().getStringArray(R.array.multi_window_force_resize_pkgs)));
            }
        }
        return sMultiWindowForceResizePkgList;
    }

    public static List<String> getMultiWindowForceNotResizeList(Context context) {
        synchronized (sMultiWindowForceNotResizePkgList) {
            if (sMultiWindowForceNotResizePkgList.isEmpty()) {
                List<MiuiSettings.SettingsCloudData.CloudData> multiWindowForceNotResizePkgsCloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(context.getContentResolver(), Utilities.isAndroidNorNewer() ? "MultiWindowForceNotResizePkgsForN" : "MultiWindowForceNotResizePkgsForM");
                if (multiWindowForceNotResizePkgsCloudDataList != null) {
                    try {
                        for (MiuiSettings.SettingsCloudData.CloudData data : multiWindowForceNotResizePkgsCloudDataList) {
                            String json = data.toString();
                            Log.d("SystemServicesProxy", "json=" + json);
                            if (!TextUtils.isEmpty(json)) {
                                String pkg = new JSONObject(json).optString("pkg");
                                if (!TextUtils.isEmpty(pkg)) {
                                    sMultiWindowForceNotResizePkgList.add(pkg);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (sMultiWindowForceNotResizePkgList.isEmpty()) {
                sMultiWindowForceNotResizePkgList.addAll(Arrays.asList(context.getResources().getStringArray(R.array.multi_window_force_not_resize_pkgs)));
            }
        }
        return sMultiWindowForceNotResizePkgList;
    }

    public void registerFsGestureCall(String callbackKey, IFsGestureCallback callback) {
        if (callbackKey != null && callback != null) {
            this.mIFsGestureCallbackMap.put(callbackKey, callback);
        }
    }

    public void unRegisterFsGestureCall(String callbackKey, IFsGestureCallback callback) {
        if (!(callbackKey == null || callback == null || !this.mIFsGestureCallbackMap.containsKey(callbackKey))) {
            this.mIFsGestureCallbackMap.remove(callbackKey);
        }
    }

    public void changeAlphaScaleForFsGesture(String callbackKey, float alpha, float scale) {
        changeAlphaScaleForFsGesture(callbackKey, alpha, scale, 0, 0);
    }

    public void changeAlphaScaleForFsGesture(String callbackKey, float alpha, float scale, int pivotX, int pivotY) {
        changeAlphaScaleForFsGesture(callbackKey, alpha, scale, pivotX, pivotY, 0, 0, false);
    }

    public void changeAlphaScaleForFsGesture(String callbackKey, float alpha, float scale, int pivotX, int pivotY, int iconPivotX, int iconPivotY, boolean visible) {
        String str = callbackKey;
        IFsGestureCallback callback = this.mIFsGestureCallbackMap.get(str);
        if (callback != null) {
            try {
                callback.changeAlphaScale(alpha, scale, pivotX, pivotY, iconPivotX, iconPivotY, visible);
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("changeAlphaScaleForFsGesture callbackKey=");
                    sb.append(str);
                    sb.append(" alpha=");
                    try {
                        sb.append(alpha);
                        sb.append(" scale=");
                        try {
                            sb.append(scale);
                            Log.d("SystemServicesProxy", sb.toString(), new Throwable());
                            return;
                        } catch (RemoteException e) {
                            e = e;
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        float f = scale;
                        e.printStackTrace();
                        return;
                    }
                }
            } catch (RemoteException e3) {
                e = e3;
                float f2 = alpha;
                float f3 = scale;
                e.printStackTrace();
                return;
            }
        }
        float f4 = alpha;
        float f5 = scale;
    }

    public void setIsFsGestureAnimating(boolean isAnimating) {
        this.mIsFsGestureAnimating = isAnimating;
    }

    public boolean isFsGestureAnimating() {
        return this.mIsFsGestureAnimating;
    }

    public TaskStackListener getTaskStackListener() {
        return new TaskStackListener() {
            public void onTaskStackChanged() {
                if (Recents.getConfiguration().svelteLevel == 0) {
                    RecentsTaskLoader loader = Recents.getTaskLoader();
                    ActivityManager.RunningTaskInfo runningTaskInfo = Recents.getSystemServices().getRunningTask();
                    RecentsTaskLoadPlan plan = loader.createLoadPlan(SystemServicesProxy.this.mContext);
                    loader.preloadTasks(plan, -1, false);
                    RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
                    if (runningTaskInfo != null) {
                        launchOpts.runningTaskId = runningTaskInfo.id;
                        if (!runningTaskInfo.topActivity.getPackageName().equals("com.miui.home") && !runningTaskInfo.topActivity.getPackageName().equals("com.android.systemui")) {
                            RecentsActivity.mFreeBeforeClean = 0;
                        }
                    }
                    launchOpts.numVisibleTasks = 2;
                    launchOpts.numVisibleTaskThumbnails = 2;
                    launchOpts.onlyLoadForCache = true;
                    launchOpts.onlyLoadPausedActivities = true;
                    loader.loadTasks(SystemServicesProxy.this.mContext, plan, launchOpts);
                }
                RecentsPushEventHelper.sendTaskStackChangedEvent();
            }

            public void onTaskSnapshotChanged(int taskId, ActivityManager.TaskSnapshot snapshot) {
                if (checkCurrentUserId(SystemServicesProxy.this.mContext, false) && snapshot != null && snapshot.getSnapshot() != null) {
                    ActivityManager.TaskThumbnailInfo taskThumbnailInfo = new ActivityManager.TaskThumbnailInfo();
                    taskThumbnailInfo.taskWidth = snapshot.getSnapshot().getWidth();
                    taskThumbnailInfo.taskHeight = snapshot.getSnapshot().getHeight();
                    taskThumbnailInfo.screenOrientation = snapshot.getOrientation();
                    taskThumbnailInfo.insets = snapshot.getContentInsets();
                    RecentsEventBus.getDefault().send(new TaskSnapshotChangedEvent(taskId, Bitmap.createHardwareBitmap(snapshot.getSnapshot()), taskThumbnailInfo));
                }
            }
        };
    }
}
