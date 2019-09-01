package com.android.systemui.pip.tv;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Debug;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.R;
import com.android.systemui.pip.BasePipManager;
import com.android.systemui.pip.PipUIHelper;
import com.android.systemui.pip.tv.PipManager;
import com.android.systemui.recents.misc.SystemServicesProxy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class PipManager implements BasePipManager {
    static final boolean DEBUG = Log.isLoggable("PipManager", 3);
    private static PipManager sPipManager;
    private static List<Pair<String, String>> sSettingsPackageAndClassNamePairList;
    /* access modifiers changed from: private */
    public final MediaSessionManager.OnActiveSessionsChangedListener mActiveMediaSessionListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            PipManager.this.updateMediaController(controllers);
        }
    };
    private IActivityManager mActivityManager;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.MEDIA_RESOURCE_GRANTED".equals(intent.getAction())) {
                String[] packageNames = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                int resourceType = intent.getIntExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", -1);
                if (packageNames != null && packageNames.length > 0 && resourceType == 0) {
                    PipManager.this.handleMediaResourceGranted(packageNames);
                }
            }
        }
    };
    private final Runnable mClosePipRunnable = new Runnable() {
        public void run() {
            PipManager.this.closePip();
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Rect mCurrentPipBounds;
    /* access modifiers changed from: private */
    public ParceledListSlice mCustomActions;
    /* access modifiers changed from: private */
    public Rect mDefaultPipBounds = new Rect();
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private boolean mInitialized;
    private int mLastOrientation = 0;
    private String[] mLastPackagesResourceGranted;
    /* access modifiers changed from: private */
    public List<Listener> mListeners = new ArrayList();
    private List<MediaListener> mMediaListeners = new ArrayList();
    /* access modifiers changed from: private */
    public MediaSessionManager mMediaSessionManager;
    private Rect mMenuModePipBounds;
    private final PinnedStackListener mPinnedStackListener = new PinnedStackListener();
    /* access modifiers changed from: private */
    public Rect mPipBounds;
    /* access modifiers changed from: private */
    public ComponentName mPipComponentName;
    private MediaController mPipMediaController;
    private PipNotification mPipNotification;
    /* access modifiers changed from: private */
    public int mPipTaskId = -1;
    private final Runnable mResizePinnedStackRunnable = new Runnable() {
        public void run() {
            PipManager.this.resizePinnedStack(PipManager.this.mResumeResizePinnedStackRunnableState);
        }
    };
    /* access modifiers changed from: private */
    public int mResumeResizePinnedStackRunnableState = 0;
    /* access modifiers changed from: private */
    public Rect mSettingsPipBounds;
    /* access modifiers changed from: private */
    public int mState = 0;
    private int mSuspendPipResizingReason;
    private SystemServicesProxy.TaskStackListener mTaskStackListener = new SystemServicesProxy.TaskStackListener() {
        public void onTaskStackChanged() {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onTaskStackChanged()");
            }
            if (checkCurrentUserId(PipManager.this.mContext, PipManager.DEBUG)) {
                if (PipManager.this.getState() != 0) {
                    boolean hasPip = false;
                    ActivityManager.StackInfo stackInfo = PipManager.this.getPinnedStackInfo();
                    if (stackInfo == null || stackInfo.taskIds == null) {
                        Log.w("PipManager", "There is nothing in pinned stack");
                        PipManager.this.closePipInternal(false);
                        return;
                    }
                    int i = stackInfo.taskIds.length - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        } else if (stackInfo.taskIds[i] == PipManager.this.mPipTaskId) {
                            hasPip = true;
                            break;
                        } else {
                            i--;
                        }
                    }
                    if (!hasPip) {
                        PipManager.this.closePipInternal(true);
                        return;
                    }
                }
                if (PipManager.this.getState() == 1) {
                    Rect bounds = PipManager.this.isSettingsShown() ? PipManager.this.mSettingsPipBounds : PipManager.this.mDefaultPipBounds;
                    if (PipManager.this.mPipBounds != bounds) {
                        Rect unused = PipManager.this.mPipBounds = bounds;
                        PipManager.this.resizePinnedStack(1);
                    }
                }
            }
        }

        public void onActivityPinned(String packageName, int userId, int taskId) {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onActivityPinned()");
            }
            if (checkCurrentUserId(PipManager.this.mContext, PipManager.DEBUG)) {
                ActivityManager.StackInfo stackInfo = PipManager.this.getPinnedStackInfo();
                if (stackInfo == null) {
                    Log.w("PipManager", "Cannot find pinned stack");
                    return;
                }
                if (PipManager.DEBUG) {
                    Log.d("PipManager", "PINNED_STACK:" + stackInfo);
                }
                int unused = PipManager.this.mPipTaskId = stackInfo.taskIds[stackInfo.taskIds.length - 1];
                ComponentName unused2 = PipManager.this.mPipComponentName = ComponentName.unflattenFromString(stackInfo.taskNames[stackInfo.taskNames.length - 1]);
                int unused3 = PipManager.this.mState = 1;
                Rect unused4 = PipManager.this.mCurrentPipBounds = PipManager.this.mPipBounds;
                PipManager.this.mMediaSessionManager.addOnActiveSessionsChangedListener(PipManager.this.mActiveMediaSessionListener, null);
                PipManager.this.updateMediaController(PipManager.this.mMediaSessionManager.getActiveSessions(null));
                for (int i = PipManager.this.mListeners.size() - 1; i >= 0; i--) {
                    ((Listener) PipManager.this.mListeners.get(i)).onPipEntered();
                }
                PipManager.this.updatePipVisibility(true);
            }
        }

        public void onPinnedActivityRestartAttempt(boolean clearedTask) {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onPinnedActivityRestartAttempt()");
            }
            if (checkCurrentUserId(PipManager.this.mContext, PipManager.DEBUG)) {
                PipManager.this.movePipToFullscreen();
            }
        }

        public void onPinnedStackAnimationEnded() {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onPinnedStackAnimationEnded()");
            }
            if (checkCurrentUserId(PipManager.this.mContext, PipManager.DEBUG) && PipManager.this.getState() == 2) {
                PipManager.this.showPipMenu();
            }
        }
    };
    private IWindowManager mWindowManager;

    public interface Listener {
        void onMoveToFullscreen();

        void onPipActivityClosed();

        void onPipEntered();

        void onPipMenuActionsChanged(ParceledListSlice parceledListSlice);

        void onPipResizeAboutToStart();

        void onShowPipMenu();
    }

    public interface MediaListener {
        void onMediaControllerChanged();
    }

    private class PinnedStackListener extends IPinnedStackListener.Stub {
        private PinnedStackListener() {
        }

        public void onListenerRegistered(IPinnedStackController controller) {
        }

        public void onImeVisibilityChanged(boolean imeVisible, int imeHeight) {
        }

        public void onMinimizedStateChanged(boolean isMinimized) {
        }

        public void onMovementBoundsChanged(Rect insetBounds, Rect normalBounds, Rect animatingBounds, boolean fromImeAdjustment, boolean fromShelfAdjustment, int displayRotation) {
        }

        public void onActionsChanged(ParceledListSlice actions) {
            ParceledListSlice unused = PipManager.this.mCustomActions = actions;
            PipManager.this.mHandler.post(new Runnable() {
                public final void run() {
                    PipManager.PinnedStackListener.lambda$onActionsChanged$1(PipManager.PinnedStackListener.this);
                }
            });
        }

        public static /* synthetic */ void lambda$onActionsChanged$1(PinnedStackListener pinnedStackListener) {
            for (int i = PipManager.this.mListeners.size() - 1; i >= 0; i--) {
                ((Listener) PipManager.this.mListeners.get(i)).onPipMenuActionsChanged(PipManager.this.mCustomActions);
            }
        }

        public void onShelfVisibilityChanged(boolean shelfVisible, int shelfHeight) {
        }
    }

    private PipManager() {
    }

    public void initialize(Context context) {
        if (!this.mInitialized) {
            this.mInitialized = true;
            this.mContext = context;
            this.mActivityManager = ActivityManagerCompat.getService();
            this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
            SystemServicesProxy.getInstance(context).registerTaskStackListener(this.mTaskStackListener);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MEDIA_RESOURCE_GRANTED");
            this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
            if (sSettingsPackageAndClassNamePairList == null) {
                String[] settings = this.mContext.getResources().getStringArray(R.array.tv_pip_settings_class_name);
                sSettingsPackageAndClassNamePairList = new ArrayList();
                if (settings != null) {
                    for (String split : settings) {
                        Pair<String, String> entry = null;
                        String[] packageAndClassName = split.split("/");
                        switch (packageAndClassName.length) {
                            case 1:
                                entry = Pair.create(packageAndClassName[0], null);
                                break;
                            case 2:
                                if (packageAndClassName[1] != null && packageAndClassName[1].startsWith(".")) {
                                    entry = Pair.create(packageAndClassName[0], packageAndClassName[0] + packageAndClassName[1]);
                                    break;
                                }
                        }
                        if (entry != null) {
                            sSettingsPackageAndClassNamePairList.add(entry);
                        } else {
                            Log.w("PipManager", "Ignoring malformed settings name " + settings[i]);
                        }
                    }
                }
            }
            Configuration initialConfig = this.mContext.getResources().getConfiguration();
            this.mLastOrientation = initialConfig.orientation;
            loadConfigurationsAndApply(initialConfig);
            this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
            try {
                this.mWindowManager.registerPinnedStackListener(0, this.mPinnedStackListener);
            } catch (RemoteException e) {
                Log.e("PipManager", "Failed to register pinned stack listener", e);
            }
            this.mPipNotification = new PipNotification(context);
        }
    }

    private void loadConfigurationsAndApply(Configuration newConfig) {
        if (this.mLastOrientation != newConfig.orientation) {
            this.mLastOrientation = newConfig.orientation;
            return;
        }
        Resources res = this.mContext.getResources();
        this.mSettingsPipBounds = Rect.unflattenFromString(res.getString(R.string.pip_settings_bounds));
        this.mMenuModePipBounds = Rect.unflattenFromString(res.getString(R.string.pip_menu_bounds));
        this.mPipBounds = isSettingsShown() ? this.mSettingsPipBounds : this.mDefaultPipBounds;
        resizePinnedStack(getPinnedStackInfo() == null ? 0 : 1);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        loadConfigurationsAndApply(newConfig);
        this.mPipNotification.onConfigurationChanged(this.mContext);
    }

    public void showPictureInPictureMenu() {
        if (getState() == 1) {
            resizePinnedStack(2);
        }
    }

    public void closePip() {
        closePipInternal(true);
    }

    /* access modifiers changed from: private */
    public void closePipInternal(boolean removePipStack) {
        this.mState = 0;
        this.mPipTaskId = -1;
        this.mPipMediaController = null;
        this.mMediaSessionManager.removeOnActiveSessionsChangedListener(this.mActiveMediaSessionListener);
        if (removePipStack) {
            try {
                this.mActivityManager.removeStack(4);
            } catch (RemoteException e) {
                Log.e("PipManager", "removeStack failed", e);
            }
        }
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onPipActivityClosed();
        }
        this.mHandler.removeCallbacks(this.mClosePipRunnable);
        updatePipVisibility(false);
    }

    /* access modifiers changed from: package-private */
    public void movePipToFullscreen() {
        this.mPipTaskId = -1;
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onMoveToFullscreen();
        }
        resizePinnedStack(0);
        updatePipVisibility(false);
    }

    public void suspendPipResizing(int reason) {
        if (DEBUG) {
            Log.d("PipManager", "suspendPipResizing() reason=" + reason + " callers=" + Debug.getCallers(2));
        }
        this.mSuspendPipResizingReason |= reason;
    }

    public void resumePipResizing(int reason) {
        if ((this.mSuspendPipResizingReason & reason) != 0) {
            if (DEBUG) {
                Log.d("PipManager", "resumePipResizing() reason=" + reason + " callers=" + Debug.getCallers(2));
            }
            this.mSuspendPipResizingReason &= ~reason;
            this.mHandler.post(this.mResizePinnedStackRunnable);
        }
    }

    /* access modifiers changed from: package-private */
    public void resizePinnedStack(int state) {
        if (DEBUG) {
            Log.d("PipManager", "resizePinnedStack() state=" + state, new Exception());
        }
        boolean wasStateNoPip = this.mState == 0;
        int i = this.mListeners.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 < 0) {
                break;
            }
            this.mListeners.get(i2).onPipResizeAboutToStart();
            i = i2 - 1;
        }
        if (this.mSuspendPipResizingReason != 0) {
            this.mResumeResizePinnedStackRunnableState = state;
            if (DEBUG) {
                Log.d("PipManager", "resizePinnedStack() deferring mSuspendPipResizingReason=" + this.mSuspendPipResizingReason + " mResumeResizePinnedStackRunnableState=" + this.mResumeResizePinnedStackRunnableState);
            }
            return;
        }
        this.mState = state;
        switch (this.mState) {
            case 0:
                this.mCurrentPipBounds = null;
                if (wasStateNoPip) {
                    return;
                }
                break;
            case 1:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
            case 2:
                this.mCurrentPipBounds = this.mMenuModePipBounds;
                break;
            default:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
        }
        try {
            this.mActivityManager.resizeStack(4, this.mCurrentPipBounds, true, true, true, -1);
        } catch (RemoteException e) {
            Log.e("PipManager", "resizeStack failed", e);
        }
    }

    /* access modifiers changed from: private */
    public int getState() {
        if (this.mSuspendPipResizingReason != 0) {
            return this.mResumeResizePinnedStackRunnableState;
        }
        return this.mState;
    }

    /* access modifiers changed from: private */
    public void showPipMenu() {
        if (DEBUG) {
            Log.d("PipManager", "showPipMenu()");
        }
        this.mState = 2;
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onShowPipMenu();
        }
        Intent intent = new Intent(this.mContext, PipMenuActivity.class);
        intent.setFlags(268435456);
        intent.putExtra("custom_actions", this.mCustomActions);
        this.mContext.startActivity(intent);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public void addMediaListener(MediaListener listener) {
        this.mMediaListeners.add(listener);
    }

    public void removeMediaListener(MediaListener listener) {
        this.mMediaListeners.remove(listener);
    }

    /* access modifiers changed from: private */
    public ActivityManager.StackInfo getPinnedStackInfo() {
        try {
            return ActivityManagerCompat.getStackInfo(4, 2, 0);
        } catch (Exception e) {
            Log.e("PipManager", "getStackInfo failed", e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void handleMediaResourceGranted(String[] packageNames) {
        if (getState() == 0) {
            this.mLastPackagesResourceGranted = packageNames;
            return;
        }
        boolean requestedFromLastPackages = false;
        if (this.mLastPackagesResourceGranted != null) {
            boolean requestedFromLastPackages2 = false;
            for (String packageName : this.mLastPackagesResourceGranted) {
                int length = packageNames.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (TextUtils.equals(packageNames[i], packageName)) {
                        requestedFromLastPackages2 = true;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            requestedFromLastPackages = requestedFromLastPackages2;
        }
        this.mLastPackagesResourceGranted = packageNames;
        if (!requestedFromLastPackages) {
            closePip();
        }
    }

    /* access modifiers changed from: private */
    public void updateMediaController(List<MediaController> controllers) {
        MediaController mediaController = null;
        if (controllers != null && getState() != 0 && this.mPipComponentName != null) {
            int i = controllers.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                MediaController controller = controllers.get(i);
                if (controller.getPackageName().equals(this.mPipComponentName.getPackageName())) {
                    mediaController = controller;
                    break;
                }
                i--;
            }
        }
        if (this.mPipMediaController != mediaController) {
            this.mPipMediaController = mediaController;
            for (int i2 = this.mMediaListeners.size() - 1; i2 >= 0; i2--) {
                this.mMediaListeners.get(i2).onMediaControllerChanged();
            }
            if (this.mPipMediaController == null) {
                this.mHandler.postDelayed(this.mClosePipRunnable, 3000);
            } else {
                this.mHandler.removeCallbacks(this.mClosePipRunnable);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public MediaController getMediaController() {
        return this.mPipMediaController;
    }

    /* access modifiers changed from: package-private */
    public int getPlaybackState() {
        if (this.mPipMediaController == null || this.mPipMediaController.getPlaybackState() == null) {
            return 2;
        }
        int state = this.mPipMediaController.getPlaybackState().getState();
        boolean isPlaying = state == 6 || state == 8 || state == 3 || state == 4 || state == 5 || state == 9 || state == 10;
        long actions = this.mPipMediaController.getPlaybackState().getActions();
        if (!isPlaying && (4 & actions) != 0) {
            return 1;
        }
        if (!isPlaying || (2 & actions) == 0) {
            return 2;
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public boolean isSettingsShown() {
        try {
            List<ActivityManager.RunningTaskInfo> runningTasks = PipUIHelper.getTasks(this.mActivityManager, 1, 0);
            if (runningTasks == null || runningTasks.size() == 0) {
                return false;
            }
            ComponentName topActivity = runningTasks.get(0).topActivity;
            for (Pair<String, String> componentName : sSettingsPackageAndClassNamePairList) {
                if (topActivity.getPackageName().equals((String) componentName.first)) {
                    String className = (String) componentName.second;
                    if (className == null || topActivity.getClassName().equals(className)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.d("PipManager", "Failed to detect top activity", e);
            return false;
        }
    }

    public static PipManager getInstance() {
        if (sPipManager == null) {
            sPipManager = new PipManager();
        }
        return sPipManager;
    }

    /* access modifiers changed from: private */
    public void updatePipVisibility(boolean visible) {
        SystemServicesProxy.getInstance(this.mContext).setPipVisibility(visible);
    }

    public void dump(PrintWriter pw) {
    }
}
