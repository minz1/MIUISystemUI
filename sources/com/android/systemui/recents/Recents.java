package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.CommandQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Recents extends SystemUI implements RecentsComponent, CommandQueue.Callbacks {
    public static final Set<String> RECENTS_ACTIVITIES = new HashSet();
    private static RecentsConfiguration sConfiguration;
    private static RecentsDebugFlags sDebugFlags;
    /* access modifiers changed from: private */
    public static SystemServicesProxy sSystemServicesProxy;
    private static RecentsTaskLoader sTaskLoader;
    private int mDraggingInRecentsCurrentUser;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public RecentsImpl mImpl;
    private Configuration mLastConfiguration = new Configuration();
    private final ArrayList<Runnable> mOnConnectRunnables = new ArrayList<>();
    private String mOverrideRecentsPackageName;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                int currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                UserInfo userInfo = UserManager.get(context).getUserInfo(currentId);
                if (Recents.this.mSecondaryUser != -10000) {
                    context.stopServiceAsUser(Recents.this.mSecondaryUserServiceIntent, new UserHandle(Recents.this.mSecondaryUser));
                    int unused = Recents.this.mSecondaryUser = -10000;
                }
                if (userInfo != null && currentId != 0) {
                    context.startServiceAsUser(Recents.this.mSecondaryUserServiceIntent, new UserHandle(userInfo.id));
                    int unused2 = Recents.this.mSecondaryUser = userInfo.id;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public int mSecondaryUser = -10000;
    /* access modifiers changed from: private */
    public Intent mSecondaryUserServiceIntent;
    private RecentsSystemUser mSystemToUserCallbacks;
    /* access modifiers changed from: private */
    public IRecentsSystemUserCallbacks mUserToSystemCallbacks;
    /* access modifiers changed from: private */
    public final IBinder.DeathRecipient mUserToSystemCallbacksDeathRcpt = new IBinder.DeathRecipient() {
        public void binderDied() {
            IRecentsSystemUserCallbacks unused = Recents.this.mUserToSystemCallbacks = null;
            EventLog.writeEvent(36060, new Object[]{3, Integer.valueOf(Recents.sSystemServicesProxy.getProcessUser())});
            Recents.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    Recents.this.registerWithSystemUser();
                }
            }, 5000);
        }
    };
    private final ServiceConnection mUserToSystemServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                IRecentsSystemUserCallbacks unused = Recents.this.mUserToSystemCallbacks = IRecentsSystemUserCallbacks.Stub.asInterface(service);
                EventLog.writeEvent(36060, new Object[]{2, Integer.valueOf(Recents.sSystemServicesProxy.getProcessUser())});
                try {
                    service.linkToDeath(Recents.this.mUserToSystemCallbacksDeathRcpt, 0);
                } catch (RemoteException e) {
                    Log.e("Recents", "Lost connection to (System) SystemUI", e);
                }
                Recents.this.runAndFlushOnConnectRunnables();
            }
            Recents.this.mContext.unbindService(this);
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    static {
        RECENTS_ACTIVITIES.add("com.android.systemui.recents.RecentsActivity");
    }

    public IBinder getSystemUserCallbacks() {
        return this.mSystemToUserCallbacks;
    }

    public static RecentsTaskLoader getTaskLoader() {
        return sTaskLoader;
    }

    public static SystemServicesProxy getSystemServices() {
        return sSystemServicesProxy;
    }

    public static RecentsConfiguration getConfiguration() {
        return sConfiguration;
    }

    public static RecentsDebugFlags getDebugFlags() {
        return sDebugFlags;
    }

    public RecentsImpl getRecentsImpl() {
        return this.mImpl;
    }

    public void start() {
        sDebugFlags = new RecentsDebugFlags(this.mContext);
        sSystemServicesProxy = SystemServicesProxy.getInstance(this.mContext);
        sTaskLoader = new RecentsTaskLoader(this.mContext);
        sConfiguration = new RecentsConfiguration(this.mContext);
        this.mHandler = new Handler();
        this.mImpl = new RecentsImpl(this.mContext);
        if ("userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE)) {
            String cnStr = SystemProperties.get("persist.recents_override_pkg");
            if (!cnStr.isEmpty()) {
                this.mOverrideRecentsPackageName = cnStr;
            }
        }
        RecentsEventBus.getDefault().register(this, 1);
        RecentsEventBus.getDefault().register(sSystemServicesProxy, 1);
        RecentsEventBus.getDefault().register(sTaskLoader, 1);
        RecentsEventBus.getDefault().register(this.mImpl, 1);
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mSystemToUserCallbacks = new RecentsSystemUser(this.mContext, this.mImpl);
            this.mSecondaryUserServiceIntent = new Intent(this.mContext, SystemUISecondaryUserService.class);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter, null, null);
            ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
        } else {
            registerWithSystemUser();
        }
        putComponent(Recents.class, this);
    }

    public void onBootCompleted() {
        this.mImpl.onBootCompleted();
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
        if (isUserSetup() && !proxyToOverridePackage("com.android.systemui.recents.ACTION_SHOW")) {
            try {
                ActivityManagerCompat.getService().closeSystemDialogs("recentapps");
            } catch (RemoteException e) {
            }
            int recentsGrowTarget = ((Divider) getComponent(Divider.class)).getView().growsRecents();
            int currentUser = sSystemServicesProxy.getCurrentUser();
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.showRecents(triggeredFromAltTab, false, true, false, fromHome, recentsGrowTarget, false);
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.showRecents(triggeredFromAltTab, false, true, false, fromHome, recentsGrowTarget);
                    } catch (RemoteException e2) {
                        Log.e("Recents", "Callback failed", e2);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
        }
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (isUserSetup() && !proxyToOverridePackage("com.android.systemui.recents.ACTION_HIDE")) {
            int currentUser = sSystemServicesProxy.getCurrentUser();
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.hideRecents(triggeredFromAltTab, triggeredFromHomeKey, false);
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
        }
    }

    public void toggleRecentApps() {
        if (isUserSetup() && !proxyToOverridePackage("com.android.systemui.recents.ACTION_TOGGLE")) {
            int growTarget = ((Divider) getComponent(Divider.class)).getView().growsRecents();
            int currentUser = sSystemServicesProxy.getCurrentUser();
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.toggleRecents(growTarget);
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.toggleRecents(growTarget);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
        }
    }

    public void preloadRecentApps() {
        if (isUserSetup()) {
            int currentUser = sSystemServicesProxy.getCurrentUser();
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.preloadRecents();
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.preloadRecents();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
        }
    }

    public void cancelPreloadRecentApps() {
        if (isUserSetup()) {
            int currentUser = sSystemServicesProxy.getCurrentUser();
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.cancelPreloadingRecents();
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.cancelPreloadingRecents();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
        }
    }

    public boolean dockTopTask(int dragMode, int stackCreateMode, Rect initialBounds, int metricsDockAction) {
        if (!isUserSetup()) {
            return false;
        }
        Point realSize = new Point();
        if (initialBounds == null) {
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(0).getRealSize(realSize);
            initialBounds = new Rect(0, 0, realSize.x, realSize.y);
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        SystemServicesProxy ssp = getSystemServices();
        ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
        boolean screenPinningActive = ssp.isScreenPinningActive();
        boolean isRunningTaskInHomeOrRecentsStack = runningTask != null && SystemServicesProxy.isHomeOrRecentsStack(ActivityManagerCompat.getRunningTaskStackId(runningTask), runningTask);
        if (runningTask == null || isRunningTaskInHomeOrRecentsStack || screenPinningActive) {
            return false;
        }
        logDockAttempt(this.mContext, runningTask.topActivity, ActivityManagerCompat.getRunningTaskResizeMode(runningTask));
        if (ActivityManagerCompat.isRunningTaskDockable(runningTask)) {
            if (metricsDockAction != -1) {
                MetricsLogger.action(this.mContext, metricsDockAction, runningTask.topActivity.flattenToShortString());
            }
            RecentsPushEventHelper.sendMultiWindowEvent("enterMultiWindow", "out of recents");
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.dockTopTask(runningTask.id, dragMode, stackCreateMode, initialBounds);
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (callbacks != null) {
                    try {
                        callbacks.dockTopTask(runningTask.id, dragMode, stackCreateMode, initialBounds);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
            this.mDraggingInRecentsCurrentUser = currentUser;
            return true;
        }
        Toast.makeText(this.mContext, R.string.recents_incompatible_app_message, 0).show();
        return false;
    }

    public static void logDockAttempt(Context ctx, ComponentName activity, int resizeMode) {
        if (resizeMode == 0) {
            MetricsLogger.action(ctx, 391, activity.flattenToShortString());
            RecentsPushEventHelper.sendMultiWindowEvent("tryEnterMultiWindowFailed", activity.flattenToShortString());
        }
        MetricsLogger.count(ctx, getMetricsCounterForResizeMode(resizeMode), 1);
    }

    private static String getMetricsCounterForResizeMode(int resizeMode) {
        switch (resizeMode) {
            case 2:
            case 3:
                return "window_enter_supported";
            case 4:
                return "window_enter_unsupported";
            default:
                return "window_enter_incompatible";
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onConfigurationChanged();
        } else if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (callbacks != null) {
                try {
                    callbacks.onConfigurationChanged();
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            } else {
                Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
            }
        }
        int changes = this.mLastConfiguration.updateFrom(newConfig);
        boolean languageChange = false;
        boolean themeChange = (Integer.MIN_VALUE & changes) != 0;
        if ((changes & 4) != 0) {
            languageChange = true;
        }
        if (themeChange || languageChange) {
            RecentsTaskLoader loader = getTaskLoader();
            if (loader != null) {
                if (themeChange) {
                    loader.onThemeChanged();
                }
                if (languageChange) {
                    loader.onLanguageChange();
                }
            }
            if (languageChange) {
                sSystemServicesProxy.onLanguageChange();
            }
        }
    }

    public final void onBusEvent(final RecentsVisibilityChangedEvent event) {
        SystemServicesProxy ssp = getSystemServices();
        if (ssp.isSystemUser(ssp.getProcessUser())) {
            this.mImpl.onVisibilityChanged(event.applicationContext, event.visible);
        } else {
            postToSystemUser(new Runnable() {
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.updateRecentsVisibility(event.visible);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(final ScreenPinningRequestEvent event) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mImpl.onStartScreenPinning(event.applicationContext, event.taskId);
        } else {
            postToSystemUser(new Runnable() {
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.startScreenPinning(event.taskId);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsDrawnEvent event) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendRecentsDrawnEvent();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(final DockedTopTaskEvent event) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendDockingTopTaskEvent(event.dragMode, event.initialRect);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsActivityStartingEvent event) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendLaunchRecentsEvent();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        this.mImpl.onConfigurationChanged();
    }

    /* access modifiers changed from: private */
    public void registerWithSystemUser() {
        final int processUser = sSystemServicesProxy.getProcessUser();
        postToSystemUser(new Runnable() {
            public void run() {
                try {
                    Recents.this.mUserToSystemCallbacks.registerNonSystemUserCallbacks(new RecentsImplProxy(Recents.this.mImpl), processUser);
                } catch (RemoteException e) {
                    Log.e("Recents", "Failed to register", e);
                }
            }
        });
    }

    private void postToSystemUser(Runnable onConnectRunnable) {
        this.mOnConnectRunnables.add(onConnectRunnable);
        if (this.mUserToSystemCallbacks == null) {
            Intent systemUserServiceIntent = new Intent();
            systemUserServiceIntent.setClass(this.mContext, RecentsSystemUserService.class);
            boolean bound = this.mContext.bindServiceAsUser(systemUserServiceIntent, this.mUserToSystemServiceConnection, 1, UserHandleCompat.SYSTEM);
            EventLog.writeEvent(36060, new Object[]{1, Integer.valueOf(sSystemServicesProxy.getProcessUser())});
            if (!bound) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        Recents.this.registerWithSystemUser();
                    }
                }, 5000);
                return;
            }
            return;
        }
        runAndFlushOnConnectRunnables();
    }

    /* access modifiers changed from: private */
    public void runAndFlushOnConnectRunnables() {
        Iterator<Runnable> it = this.mOnConnectRunnables.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        this.mOnConnectRunnables.clear();
    }

    private boolean isUserSetup() {
        ContentResolver cr = this.mContext.getContentResolver();
        if (Settings.Global.getInt(cr, "device_provisioned", 0) == 0 || Settings.Secure.getIntForUser(cr, "user_setup_complete", 0, -2) == 0) {
            return false;
        }
        return true;
    }

    private boolean proxyToOverridePackage(String action) {
        if (this.mOverrideRecentsPackageName == null) {
            return false;
        }
        Intent intent = new Intent(action);
        intent.setPackage(this.mOverrideRecentsPackageName);
        intent.addFlags(268435456);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
    }

    public void disable(int state1, int state2, boolean animate) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void animateExpandSettingsPanel(String obj) {
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void toggleSplitScreen() {
    }

    public void dismissKeyboardShortcutsMenu() {
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
    }

    public void setWindowState(int window, int state) {
    }

    public void showScreenPinningRequest(int taskId) {
    }

    public void appTransitionPending(boolean forced) {
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
    }

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void showFingerprintDialog(SomeArgs args) {
    }

    public void onFingerprintAuthenticated() {
    }

    public void onFingerprintHelp(String message) {
    }

    public void onFingerprintError(String error) {
    }

    public void hideFingerprintDialog() {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }
}
