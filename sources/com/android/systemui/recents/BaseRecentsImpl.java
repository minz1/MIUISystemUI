package com.android.systemui.recents;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import com.android.internal.os.BackgroundThread;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Application;
import com.android.systemui.Dependency;
import com.android.systemui.Util;
import com.android.systemui.fsgesture.GestureStubView;
import com.android.systemui.miui.ActivityObserver;
import com.android.systemui.miui.statusbar.CloudDataHelper;
import com.android.systemui.plugins.R;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ActivitySetDummyTranslucentEvent;
import com.android.systemui.recents.events.activity.AnimFirstTaskViewAlphaEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsCompleteEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsZoomEvent;
import com.android.systemui.recents.events.activity.FsGestureLaunchTargetTaskViewRectEvent;
import com.android.systemui.recents.events.activity.FsGesturePreloadRecentsEvent;
import com.android.systemui.recents.events.activity.FsGestureShowFirstCardEvent;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideInEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideOutEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.RotationChangedEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.HideNavStubForBackWindow;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.CleanInRecentsEvents;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEndEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.ForegroundThread;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.MutableBoolean;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.statusbar.phone.NavStubView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.miui.internal.transition.IMiuiGestureControlHelper;
import com.miui.internal.transition.MiuiAppTransitionAnimationSpec;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.securityspace.CrossUserUtils;
import miui.util.HardwareInfo;
import miui.widget.CircleProgressBar;

public abstract class BaseRecentsImpl {
    public static int mTaskBarHeight;
    protected static RecentsTaskLoadPlan sInstanceLoadPlan;
    public static boolean sOneKeyCleaning = false;
    private boolean isShowing = false;
    private ActivityObserver.ActivityObserverCallback mActivityStateObserver = new ActivityObserver.ActivityObserverCallback() {
        public void activityResumed(Intent intent) {
            if (intent != null && intent.getComponent() != null) {
                BaseRecentsImpl.this.onResumed(intent.getComponent().getClassName());
            }
        }
    };
    private ContentObserver mAppSwitchAnimChangeListener = new ContentObserver(new Handler(Looper.getMainLooper())) {
        public void onChange(boolean selfChange) {
            boolean z = false;
            if (Settings.Global.getInt(BaseRecentsImpl.this.mContext.getContentResolver(), "show_gesture_appswitch_feature", 0) == 0) {
                z = true;
            }
            boolean isAnimDisabled = z;
            if (BaseRecentsImpl.this.mGestureStubLeft != null) {
                BaseRecentsImpl.this.mGestureStubLeft.disableQuickSwitch(isAnimDisabled);
            }
            if (BaseRecentsImpl.this.mGestureStubRight != null) {
                BaseRecentsImpl.this.mGestureStubRight.disableQuickSwitch(isAnimDisabled);
            }
        }
    };
    private ContentObserver mCloudDataObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            BaseRecentsImpl.this.readCloudDataForFsg();
        }
    };
    protected Context mContext;
    /* access modifiers changed from: private */
    public boolean mDisabledByDriveMode;
    boolean mDraggingInRecents;
    private ContentObserver mDriveModeObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
        public void onChange(boolean selfChange) {
            BaseRecentsImpl baseRecentsImpl = BaseRecentsImpl.this;
            boolean z = true;
            if (Settings.System.getInt(BaseRecentsImpl.this.mContext.getContentResolver(), "drive_mode_drive_mode", 0) != 1) {
                z = false;
            }
            boolean unused = baseRecentsImpl.mDisabledByDriveMode = z;
            BaseRecentsImpl.this.updateFsgWindowState();
        }
    };
    protected TaskStackView mDummyStackView;
    DozeTrigger mFastAltTabTrigger = new DozeTrigger(225, new Runnable() {
        public void run() {
            BaseRecentsImpl.this.showRecents(BaseRecentsImpl.this.mTriggeredFromAltTab, false, true, false, false, -1, false);
        }
    });
    private ContentObserver mForceImmersiveNavBarListener = new ContentObserver(new Handler(Looper.getMainLooper())) {
        public void onChange(boolean selfChange) {
            if (!BaseRecentsImpl.this.mIsInAnotherPro) {
                BaseRecentsImpl.this.updateFsgWindowState();
            }
        }
    };
    /* access modifiers changed from: private */
    public int mFsgBackState = 0;
    private BroadcastReceiver mFsgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.fsgesture".equals(intent.getAction())) {
                BaseRecentsImpl.this.updateFsgWindowVisibilityState(intent.getBooleanExtra("isEnter", false), intent.getStringExtra("typeFrom"));
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                Log.d("RecentsImpl", "registerMiuiGestureControlHelper: user switched.");
                BaseRecentsImpl.this.registerMiuiGestureControlHelper();
            }
        }
    };
    /* access modifiers changed from: private */
    public long mGestureAnimationStartTime;
    private IMiuiGestureControlHelper mGestureControlHelper;
    /* access modifiers changed from: private */
    public GestureStubView mGestureStubLeft;
    /* access modifiers changed from: private */
    public GestureStubView mGestureStubRight;
    protected Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHasNavigationBar;
    private boolean mHasRegistedInput = false;
    TaskViewHeader mHeaderBar;
    final Object mHeaderBarLock = new Object();
    /* access modifiers changed from: private */
    public String mHotZoneChangeActListStr = "";
    /* access modifiers changed from: private */
    public final IWindowManager mIWindowManager;
    /* access modifiers changed from: private */
    public int mInputMethodHeight;
    private BroadcastReceiver mInputMethodVisibleHeightChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("miui.intent.action.INPUT_METHOD_VISIBLE_HEIGHT_CHANGED".equals(intent.getAction()) && BaseRecentsImpl.this.mGestureStubLeft != null && BaseRecentsImpl.this.mGestureStubRight != null && !"lithium".equals(Build.DEVICE)) {
                int inputHeight = intent.getIntExtra("miui.intent.extra.input_method_visible_height", -1);
                if (BaseRecentsImpl.this.mInputMethodHeight <= 0 || inputHeight <= 0) {
                    int unused = BaseRecentsImpl.this.mInputMethodHeight = inputHeight;
                    if (inputHeight != -1 && BaseRecentsImpl.this.mGestureStubLeft.getVisibility() == 0) {
                        if (BaseRecentsImpl.this.mInputMethodHeight > 0) {
                            BaseRecentsImpl.this.sendChangeBackGestureSizeIsNeeded();
                        } else if (BaseRecentsImpl.this.mInputMethodHeight == 0 && BaseRecentsImpl.this.mFsgBackState != 2) {
                            BaseRecentsImpl.this.sendResetBackGestureSizeIsNeeded();
                        }
                    }
                }
            }
        }
    };
    private boolean mIsHideRecentsViewByFsGesture = false;
    /* access modifiers changed from: private */
    public boolean mIsInAnotherPro = false;
    private boolean mIsSizeReset;
    /* access modifiers changed from: private */
    public boolean mIsStartRecent = false;
    protected KeyguardManager mKM;
    protected long mLastToggleTime;
    boolean mLaunchedWhileDocking;
    private String[] mLocalCtrlActs = {"com.android.systemui.fsgesture.HomeDemoAct", "com.android.systemui.fsgesture.DemoFinishAct", "com.android.systemui.fsgesture.DrawerDemoAct", "com.android.systemui.fsgesture.FsGestureBackDemoActivity", "com.android.provision.activities.CongratulationActivity"};
    int mNavBarHeight;
    int mNavBarWidth;
    /* access modifiers changed from: private */
    public NavStubView mNavStubView;
    /* access modifiers changed from: private */
    public String mNoBackActListStr = "";
    /* access modifiers changed from: private */
    public String mNoBackAndHomeActListStr = "";
    /* access modifiers changed from: private */
    public String mNoHomeActListStr = "";
    private Runnable mReadCloudRunnable = new Runnable() {
        public void run() {
            String noBackActListStr = MiuiSettings.SettingsCloudData.getCloudDataString(BaseRecentsImpl.this.mContext.getContentResolver(), "ykrq", "no_back_gesture_only", "");
            String noHomeActListStr = MiuiSettings.SettingsCloudData.getCloudDataString(BaseRecentsImpl.this.mContext.getContentResolver(), "ykrq", "no_home_gesture_only", "");
            String noBackAndHomeActListStr = MiuiSettings.SettingsCloudData.getCloudDataString(BaseRecentsImpl.this.mContext.getContentResolver(), "ykrq", "no_back_and_home", "");
            String hotZoneChangeActListStr = MiuiSettings.SettingsCloudData.getCloudDataString(BaseRecentsImpl.this.mContext.getContentResolver(), "ykrq", "hot_zone_change", "");
            Handler handler = BaseRecentsImpl.this.mHandler;
            final String str = noBackActListStr;
            final String str2 = noHomeActListStr;
            final String str3 = noBackAndHomeActListStr;
            final String str4 = hotZoneChangeActListStr;
            AnonymousClass1 r4 = new Runnable() {
                public void run() {
                    String unused = BaseRecentsImpl.this.mNoBackActListStr = str;
                    if (TextUtils.isEmpty(BaseRecentsImpl.this.mNoBackActListStr)) {
                        String unused2 = BaseRecentsImpl.this.mNoBackActListStr = "com.miui.home.launcher.Launcher:com.miui.personalassistant.fake.FakeStartActivity:com.miui.personalassistant.fake.FakeEndActivity";
                    }
                    String unused3 = BaseRecentsImpl.this.mNoHomeActListStr = str2;
                    String unused4 = BaseRecentsImpl.this.mNoBackAndHomeActListStr = str3;
                    String unused5 = BaseRecentsImpl.this.mHotZoneChangeActListStr = str4;
                    BaseRecentsImpl.access$884(BaseRecentsImpl.this, "com.android.systemui.fsgesture.DemoIntroduceAct:com.android.systemui.sliderpanel.SliderPanelActivity");
                }
            };
            handler.post(r4);
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (BaseRecentsImpl.this.mHasNavigationBar) {
                    if (UserHandle.getUserId(Process.myUid()) == intent.getIntExtra("android.intent.extra.user_handle", -1)) {
                        boolean unused = BaseRecentsImpl.this.mIsInAnotherPro = false;
                        if (MiuiSettings.Global.getBoolean(BaseRecentsImpl.this.mContext.getContentResolver(), "force_fsg_nav_bar") && !BaseRecentsImpl.this.mDisabledByDriveMode) {
                            try {
                                if (BaseRecentsImpl.this.mNavStubView == null) {
                                    Log.d("RecentsImpl", "navstubview will be added: mReceiver Intent.ACTION_USER_SWITCHED userid: " + UserHandle.getUserId(Process.myUid()));
                                    NavStubView unused2 = BaseRecentsImpl.this.mNavStubView = new NavStubView(BaseRecentsImpl.this.mContext);
                                    BaseRecentsImpl.this.mWindowManager.addView(BaseRecentsImpl.this.mNavStubView, BaseRecentsImpl.this.mNavStubView.getWindowParam(BaseRecentsImpl.this.mStubSize));
                                }
                                BaseRecentsImpl.this.addBackStubWindow();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        boolean unused3 = BaseRecentsImpl.this.mIsInAnotherPro = true;
                        try {
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                Log.d("RecentsImpl", "navstubview will be removed: mReceiver Intent.ACTION_USER_SWITCHED userid: " + UserHandle.getUserId(Process.myUid()));
                                BaseRecentsImpl.this.mWindowManager.removeView(BaseRecentsImpl.this.mNavStubView);
                                NavStubView unused4 = BaseRecentsImpl.this.mNavStubView = null;
                            }
                            BaseRecentsImpl.this.clearBackStubWindow();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            } else if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                BaseRecentsImpl.this.adaptToTopActivity();
            }
        }
    };
    RecentsReceiver mRecentsReceiver = new RecentsReceiver();
    /* access modifiers changed from: private */
    public boolean mRecentsVisible;
    /* access modifiers changed from: private */
    public int mScreenWidth;
    int mStatusBarHeight;
    /* access modifiers changed from: private */
    public int mStubSize;
    Rect mTaskStackBounds = new Rect();
    SystemServicesProxy.TaskStackListener mTaskStackListener;
    protected Bitmap mThumbTransitionBitmapCache;
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    protected boolean mTriggeredFromAltTab;
    /* access modifiers changed from: private */
    public final WindowManager mWindowManager;

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 2577) {
                BaseRecentsImpl.this.showBackStubWindow();
            } else if (i == 2677) {
                BaseRecentsImpl.this.hideBackStubWindow();
            } else if (i != 2777) {
                if (i != 2877) {
                    switch (i) {
                        case R.styleable.AppCompatTheme_textAppearancePopupMenuHeader:
                            RecentsEventBus.getDefault().send(new FsGestureEnterRecentsEvent());
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                BaseRecentsImpl.this.mNavStubView.startAppEnterRecentsAnim();
                                break;
                            }
                            break;
                        case R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle:
                            Log.d("RecentsImpl", "handleMessage: MSG_START_RECENTS_ANIAMTION mRecentsVisible = " + BaseRecentsImpl.this.mRecentsVisible);
                            if (!BaseRecentsImpl.this.mRecentsVisible) {
                                removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                                sendMessageDelayed(obtainMessage(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle), 20);
                                break;
                            } else {
                                RecentsEventBus.getDefault().send(new FsGestureSlideInEvent());
                                boolean unused = BaseRecentsImpl.this.mIsStartRecent = true;
                                if (BaseRecentsImpl.this.mNavStubView != null) {
                                    BaseRecentsImpl.this.mNavStubView.postDelayed(new Runnable() {
                                        public void run() {
                                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                                BaseRecentsImpl.this.mNavStubView.performHapticFeedback(1);
                                            }
                                        }
                                    }, 100);
                                    break;
                                }
                            }
                            break;
                        case R.styleable.AppCompatTheme_textAppearanceSearchResultTitle:
                            RecentsEventBus.getDefault().send(new FsGestureEnterRecentsCompleteEvent(true));
                            RecentsEventBus.getDefault().send(new ActivitySetDummyTranslucentEvent(false));
                            boolean unused2 = BaseRecentsImpl.this.mIsStartRecent = false;
                            break;
                        case R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu:
                            RecentsEventBus.getDefault().send(new FsGestureSlideOutEvent());
                            BaseRecentsImpl.this.showBackStubWindow();
                            boolean unused3 = BaseRecentsImpl.this.mIsStartRecent = false;
                            break;
                        case R.styleable.AppCompatTheme_textColorAlertDialogListItem:
                            RecentsEventBus.getDefault().send(new FsGestureShowFirstCardEvent());
                            RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
                            break;
                        case R.styleable.AppCompatTheme_textColorSearchUrl:
                            Log.d("RecentsImpl", "handleMessage: MSG_ZOOM_RECENT_VIEW mRecentsVisible = " + BaseRecentsImpl.this.mRecentsVisible + " mIsStartRecent = " + BaseRecentsImpl.this.mIsStartRecent);
                            if (BaseRecentsImpl.this.mRecentsVisible && BaseRecentsImpl.this.mIsStartRecent) {
                                RecentsEventBus.getDefault().send(new FsGestureEnterRecentsZoomEvent(System.currentTimeMillis() - BaseRecentsImpl.this.mGestureAnimationStartTime));
                                break;
                            } else {
                                removeMessages(R.styleable.AppCompatTheme_textColorSearchUrl);
                                sendMessageDelayed(obtainMessage(R.styleable.AppCompatTheme_textColorSearchUrl), 20);
                                break;
                            }
                            break;
                        case R.styleable.AppCompatTheme_toolbarNavigationButtonStyle:
                            removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                            break;
                        case R.styleable.AppCompatTheme_toolbarStyle:
                            removeMessages(R.styleable.AppCompatTheme_textColorSearchUrl);
                            break;
                    }
                } else if (BaseRecentsImpl.this.mGestureStubLeft != null && BaseRecentsImpl.this.mGestureStubRight != null) {
                    int size = BaseRecentsImpl.this.mScreenWidth != 720 ? 54 : 40;
                    BaseRecentsImpl.this.mGestureStubLeft.setSize(size);
                    BaseRecentsImpl.this.mGestureStubRight.setSize(size);
                }
            } else if (BaseRecentsImpl.this.mGestureStubLeft != null && BaseRecentsImpl.this.mGestureStubRight != null) {
                BaseRecentsImpl.this.mGestureStubLeft.setSize(30);
                BaseRecentsImpl.this.mGestureStubRight.setSize(30);
            }
        }
    }

    public class RecentsReceiver extends BroadcastReceiver {
        private final List<String> pkgsAllowCallClear = Arrays.asList(new String[]{"com.miui.home", "com.miui.securitycenter", "com.miui.touchassistant", "com.android.snapshot", "com.android.keyguard", "com.android.systemui", "com.mi.android.globallauncher", "com.xiaomi.mihomemanager", "com.miui.voiceassist"});

        public RecentsReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.taskmanager.Clear".equals(intent.getAction())) {
                String sender = intent.getSender();
                Slog.d("RecentsReceiver", "onReceive: senderName=" + sender);
                if (this.pkgsAllowCallClear.contains(sender)) {
                    boolean showToast = intent.getBooleanExtra("show_toast", false);
                    ArrayList<String> protectedPkgNames = intent.getStringArrayListExtra("protected_pkgnames");
                    int cleanType = intent.getIntExtra("clean_type", -1);
                    if (Recents.getSystemServices().isRecentsActivityVisible()) {
                        RecentsEventBus.getDefault().post(new CleanInRecentsEvents());
                    } else {
                        removeAllTask(showToast, protectedPkgNames, cleanType);
                    }
                } else {
                    Slog.d("RecentsReceiver", sender + " is not allow to call clear");
                }
            }
        }

        public void removeAllTask(boolean showToast, List<String> pPkgs, int cleanType) {
            long freeAtFirst = 0;
            if (showToast) {
                freeAtFirst = HardwareInfo.getFreeMemory() / 1024;
                Log.d("RecentsReceiver", "freeMemoryAtFirst:" + HardwareInfo.getFreeMemory());
            }
            List<ActivityManager.RecentTaskInfo> taskinfos = SystemServicesProxy.getInstance(BaseRecentsImpl.this.mContext).getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), -2, false, new ArraySet());
            ArrayList<String> protectedPkgNames = new ArrayList<>();
            if (pPkgs != null) {
                protectedPkgNames.addAll(pPkgs);
            }
            try {
                if (Recents.getSystemServices().hasDockedTask()) {
                    ActivityManager.StackInfo dockedStackInfo = ActivityManagerCompat.getStackInfo(3, 3, 0);
                    ComponentName dockedTopActivity = dockedStackInfo.topActivity;
                    if (dockedTopActivity != null && dockedStackInfo.visible) {
                        protectedPkgNames.add(dockedTopActivity.getPackageName());
                    }
                    ActivityManager.StackInfo fullScreenStackInfo = ActivityManagerCompat.getStackInfo(1, 1, 0);
                    ComponentName fullScreenTopActivity = fullScreenStackInfo.topActivity;
                    if (fullScreenTopActivity != null && fullScreenStackInfo.visible) {
                        protectedPkgNames.add(fullScreenTopActivity.getPackageName());
                    }
                }
            } catch (Exception e) {
                Log.e("RecentsReceiver", "getProtectedTaskPkg", e);
            }
            doClear(protectedPkgNames, cleanType, taskinfos);
            if (showToast) {
                showCleanEndMsg(freeAtFirst);
            }
        }

        private void doClear(final List<String> packages, final int cleanType, final List<ActivityManager.RecentTaskInfo> taskinfos) {
            BackgroundThread.getHandler().post(new Runnable() {
                public void run() {
                    ProcessConfig processConfig;
                    if (cleanType == 0) {
                        processConfig = new ProcessConfig(4);
                        processConfig.setWhiteList(packages);
                    } else {
                        processConfig = new ProcessConfig(1);
                    }
                    ArrayList<Integer> removingTaskIds = new ArrayList<>();
                    for (ActivityManager.RecentTaskInfo task : taskinfos) {
                        removingTaskIds.add(Integer.valueOf(task.persistentId));
                    }
                    processConfig.setRemoveTaskNeeded(true);
                    processConfig.setRemovingTaskIdList(removingTaskIds);
                    ProcessManager.kill(processConfig);
                }
            });
        }

        private void showCleanEndMsg(long freeAtFirst) {
            CircleProgressBar clearButton = new CircleProgressBar(BaseRecentsImpl.this.mContext);
            clearButton.setDrawablesForLevels(new int[]{com.android.systemui.R.drawable.clean_tip_bg}, new int[]{com.android.systemui.R.drawable.clean_tip_fg}, null);
            clearButton.setMax((int) (HardwareInfo.getTotalPhysicalMemory() / 1024));
            clearButton.setProgress((int) ((HardwareInfo.getTotalPhysicalMemory() / 1024) - freeAtFirst));
            Log.d("RecentsReceiver", "totalPhysicalMemory:" + HardwareInfo.getTotalPhysicalMemory());
            WindowManager wm = (WindowManager) BaseRecentsImpl.this.mContext.getSystemService("window");
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2006, 0, 1);
            layoutParams.gravity = 81;
            layoutParams.y = BaseRecentsImpl.this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.clean_toast_bottom_margin);
            layoutParams.windowAnimations = R.style.Animation_CleanTip;
            layoutParams.privateFlags = 16;
            wm.addView(clearButton, layoutParams);
            final CircleProgressBar circleProgressBar = clearButton;
            final WindowManager windowManager = wm;
            final long j = freeAtFirst;
            AnonymousClass2 r2 = new Runnable() {
                public void run() {
                    circleProgressBar.setProgressByAnimator(0, new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            final long freeAtLast = (long) ((int) (HardwareInfo.getFreeMemory() / 1024));
                            Log.d("RecentsReceiver", "freeMemoryAtLast:" + HardwareInfo.getFreeMemory());
                            circleProgressBar.setProgressByAnimator((int) ((HardwareInfo.getTotalPhysicalMemory() / 1024) - freeAtLast), new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Animator animation) {
                                    windowManager.removeView(circleProgressBar);
                                    Toast toast = Toast.makeText(BaseRecentsImpl.this.mContext, RecentsActivity.getToastMsg(BaseRecentsImpl.this.mContext, j, freeAtLast), 1);
                                    toast.setType(2006);
                                    toast.getWindowParams().privateFlags |= 16;
                                    toast.show();
                                }
                            });
                        }
                    });
                }
            };
            clearButton.postDelayed(r2, 250);
        }
    }

    static /* synthetic */ String access$884(BaseRecentsImpl x0, Object x1) {
        String str = x0.mNoBackAndHomeActListStr + x1;
        x0.mNoBackAndHomeActListStr = str;
        return str;
    }

    public BaseRecentsImpl(Context context) {
        this.mContext = context;
        this.mHandler = new H();
        this.mKM = (KeyguardManager) context.getSystemService("keyguard");
        ForegroundThread.get();
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTaskStackListener = ssp.getTaskStackListener();
        ssp.registerTaskStackListener(this.mTaskStackListener);
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        this.mDummyStackView = new TaskStackView(this.mContext);
        this.mHeaderBar = (TaskViewHeader) inflater.inflate(com.android.systemui.R.layout.recents_task_view_header, null, false);
        reloadResources();
        ssp.registerMiuiTaskResizeList(this.mContext);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        addFsgGestureWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.systemui.taskmanager.Clear");
        this.mContext.registerReceiverAsUser(this.mRecentsReceiver, UserHandle.CURRENT, filter, null, null);
        registerMiuiGestureControlHelper();
    }

    private void registerInputMethodVisibleHeightReceiver() {
        if (!this.mHasRegistedInput) {
            this.mHasRegistedInput = true;
            IntentFilter inputFilter = new IntentFilter();
            inputFilter.addAction("miui.intent.action.INPUT_METHOD_VISIBLE_HEIGHT_CHANGED");
            this.mContext.registerReceiverAsUser(this.mInputMethodVisibleHeightChangeReceiver, UserHandle.ALL, inputFilter, "miui.permission.USE_INTERNAL_GENERAL_API", null);
        }
    }

    private void unRegisterInputMethodVisibleHeightReceiver() {
        if (this.mHasRegistedInput) {
            this.mHasRegistedInput = false;
            this.mContext.unregisterReceiver(this.mInputMethodVisibleHeightChangeReceiver);
        }
    }

    public void registerMiuiGestureControlHelper() {
        if (miui.os.UserHandle.myUserId() != CrossUserUtils.getCurrentUserId()) {
            Log.w("RecentsImpl", "registerMiuiGestureControlHelper failed: userId is wrong.");
            return;
        }
        if (this.mGestureControlHelper == null) {
            this.mGestureControlHelper = new IMiuiGestureControlHelper.Stub() {
                public MiuiAppTransitionAnimationSpec getSpec(String packageName, int userId) throws RemoteException {
                    return new MiuiAppTransitionAnimationSpec(null, new Rect());
                }

                public void notifyGestureStartRecents() {
                    Log.d("RecentsImpl", "notifyGestureStartRecents");
                    BaseRecentsImpl.this.mHandler.removeMessages(100);
                    BaseRecentsImpl.this.mHandler.sendMessage(BaseRecentsImpl.this.mHandler.obtainMessage(100));
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                    BaseRecentsImpl.this.mHandler.sendMessage(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle));
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_toolbarNavigationButtonStyle);
                    BaseRecentsImpl.this.mHandler.sendMessageDelayed(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_toolbarNavigationButtonStyle), 500);
                }

                public void notifyGestureAnimationStart() {
                    Log.d("RecentsImpl", "notifyGestureAnimationStart");
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_textColorAlertDialogListItem);
                    Message delayMsg = BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_textColorAlertDialogListItem);
                    long delay = 287;
                    try {
                        delay = (long) ((BaseRecentsImpl.this.mIWindowManager.getAnimationScale(2) * 300.0f) - 17.0f);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    BaseRecentsImpl.this.mHandler.sendMessageDelayed(delayMsg, delay);
                    long unused = BaseRecentsImpl.this.mGestureAnimationStartTime = System.currentTimeMillis();
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_textColorSearchUrl);
                    BaseRecentsImpl.this.mHandler.sendMessage(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_textColorSearchUrl));
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_toolbarStyle);
                    BaseRecentsImpl.this.mHandler.sendMessageDelayed(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_toolbarStyle), 500);
                }

                public void notifyGestureAnimationCancel() {
                    Log.d("RecentsImpl", "notifyGestureAnimationCancel");
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu);
                    BaseRecentsImpl.this.mHandler.sendMessage(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu));
                }

                public void notifyGestureAnimationEnd() {
                    Log.d("RecentsImpl", "notifyGestureAnimationEnd");
                    BaseRecentsImpl.this.mHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultTitle);
                    BaseRecentsImpl.this.mHandler.sendMessageDelayed(BaseRecentsImpl.this.mHandler.obtainMessage(R.styleable.AppCompatTheme_textAppearanceSearchResultTitle), 200);
                }
            };
        }
        try {
            Method registerMiuiGestureControlHelperMethod = WindowManagerGlobal.getWindowManagerService().getClass().getMethod("registerMiuiGestureControlHelper", new Class[]{IMiuiGestureControlHelper.class});
            Method unregisterMiuiGestureControlHelperMethod = WindowManagerGlobal.getWindowManagerService().getClass().getMethod("unregisterMiuiGestureControlHelper", new Class[0]);
            if (!(registerMiuiGestureControlHelperMethod == null || unregisterMiuiGestureControlHelperMethod == null)) {
                unregisterMiuiGestureControlHelperMethod.invoke(WindowManagerGlobal.getWindowManagerService(), new Object[0]);
                registerMiuiGestureControlHelperMethod.invoke(WindowManagerGlobal.getWindowManagerService(), new Object[]{this.mGestureControlHelper});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFsgGestureWindow() {
        boolean z = true;
        this.mHasNavigationBar = true;
        try {
            this.mHasNavigationBar = this.mIWindowManager.hasNavigationBar();
        } catch (RemoteException e) {
        }
        if (this.mHasNavigationBar) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "systemui_fsg_version", 10);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, this.mForceImmersiveNavBarListener, -1);
            this.mContext.getContentResolver().registerContentObserver(CloudDataHelper.URI_CLOUD_ALL_DATA_NOTIFY, false, this.mCloudDataObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("drive_mode_drive_mode"), false, this.mDriveModeObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("show_gesture_appswitch_feature"), false, this.mAppSwitchAnimChangeListener, -1);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.USER_PRESENT");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, null);
            IntentFilter fsgFilter = new IntentFilter();
            fsgFilter.addAction("com.android.systemui.fsgesture");
            fsgFilter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiverAsUser(this.mFsgReceiver, UserHandle.ALL, fsgFilter, "miui.permission.USE_INTERNAL_GENERAL_API", null);
            ((ActivityObserver) Dependency.get(ActivityObserver.class)).addCallback(this.mActivityStateObserver);
            readCloudDataForFsg();
            int size = 13;
            if ("lithium".equals(Build.DEVICE)) {
                size = 14;
            }
            this.mStubSize = (int) (((float) size) * this.mContext.getResources().getDisplayMetrics().density);
            boolean isOpen = MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar");
            if (Settings.System.getInt(this.mContext.getContentResolver(), "drive_mode_drive_mode", 0) != 1) {
                z = false;
            }
            this.mDisabledByDriveMode = z;
            if (isOpen && !this.mDisabledByDriveMode) {
                Log.d("RecentsImpl", "navstubview will be added: addFsgGestureWindow");
                this.mNavStubView = new NavStubView(this.mContext);
                this.mWindowManager.addView(this.mNavStubView, this.mNavStubView.getWindowParam(this.mStubSize));
            }
        }
    }

    /* access modifiers changed from: private */
    public void readCloudDataForFsg() {
        BackgroundThread.getHandler().removeCallbacks(this.mReadCloudRunnable);
        BackgroundThread.getHandler().post(this.mReadCloudRunnable);
    }

    public void onResumed(final String className) {
        if (this.mNavStubView != null && !this.mKM.isKeyguardLocked() && miui.os.UserHandle.myUserId() == CrossUserUtils.getCurrentUserId()) {
            String[] strArr = this.mLocalCtrlActs;
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                if (!TextUtils.equals(strArr[i], className)) {
                    i++;
                } else {
                    return;
                }
            }
            if (!"com.miui.home.launcher.Launcher:com.miui.personalassistant.fake.FakeStartActivity:com.miui.personalassistant.fake.FakeEndActivity".contains(className)) {
                if (this.mNoBackActListStr.contains(className)) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            BaseRecentsImpl.this.hideBackStubWindow();
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                BaseRecentsImpl.this.mNavStubView.setVisibility(0);
                            }
                        }
                    });
                } else if (this.mNoHomeActListStr.contains(className)) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                BaseRecentsImpl.this.mNavStubView.setVisibility(8);
                                Log.d("RecentsImpl", "resume nohome nstub gone : " + className);
                            }
                            BaseRecentsImpl.this.showBackStubWindow();
                        }
                    });
                } else if (this.mNoBackAndHomeActListStr.contains(className)) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                BaseRecentsImpl.this.mNavStubView.setVisibility(8);
                                Log.d("RecentsImpl", "resume nobackhome nstub gone : " + className);
                            }
                            BaseRecentsImpl.this.hideBackStubWindow();
                        }
                    });
                } else {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            if (BaseRecentsImpl.this.mNavStubView != null) {
                                BaseRecentsImpl.this.mNavStubView.setVisibility(0);
                            }
                            BaseRecentsImpl.this.showBackStubWindow();
                        }
                    });
                }
                if (!"lithium".equals(Build.DEVICE)) {
                    if (this.mHotZoneChangeActListStr.contains(className)) {
                        this.mFsgBackState = 2;
                        sendChangeBackGestureSizeIsNeeded();
                    } else {
                        this.mFsgBackState = 1;
                        sendResetBackGestureSizeIsNeeded();
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void showBackStubWindow() {
        showBackStubWindow(-1);
    }

    private void showBackStubWindow(int stubSize) {
        if (this.mHasNavigationBar && !this.mDisabledByDriveMode) {
            boolean isOpen = MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar");
            if (this.mGestureStubLeft == null && isOpen) {
                initGestureStub(stubSize);
            }
            if (isOpen) {
                this.mGestureStubLeft.showGestureStub();
                this.mGestureStubRight.showGestureStub();
                this.isShowing = true;
                registerInputMethodVisibleHeightReceiver();
                return;
            }
            hideBackStubWindow();
        }
    }

    private void initGestureStub(int stubSize) {
        boolean z = true;
        boolean isAppSwitchEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "show_gesture_appswitch_feature", 0) != 0;
        this.mGestureStubLeft = new GestureStubView(this.mContext);
        this.mGestureStubLeft.setGestureStubPosition(0);
        this.mGestureStubLeft.enableGestureBackAnimation(true);
        this.mGestureStubLeft.disableQuickSwitch(!isAppSwitchEnabled);
        this.mGestureStubRight = new GestureStubView(this.mContext);
        this.mGestureStubRight.setGestureStubPosition(1);
        this.mGestureStubRight.enableGestureBackAnimation(true);
        GestureStubView gestureStubView = this.mGestureStubRight;
        if (isAppSwitchEnabled) {
            z = false;
        }
        gestureStubView.disableQuickSwitch(z);
        adaptToTopActivity();
    }

    /* access modifiers changed from: private */
    public void sendChangeBackGestureSizeIsNeeded() {
        if (!this.mIsSizeReset) {
            this.mIsSizeReset = true;
            this.mHandler.removeMessages(2777);
            this.mHandler.removeMessages(2877);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2777));
        }
    }

    /* access modifiers changed from: private */
    public void sendResetBackGestureSizeIsNeeded() {
        if (this.mIsSizeReset) {
            this.mIsSizeReset = false;
            this.mHandler.removeMessages(2777);
            this.mHandler.removeMessages(2877);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2877), 300);
        }
    }

    /* access modifiers changed from: private */
    public void adaptToTopActivity() {
        ComponentName cn = Util.getTopActivity(this.mContext);
        if (cn != null) {
            onResumed(cn.getClassName());
        }
    }

    /* access modifiers changed from: private */
    public void hideBackStubWindow() {
        if (this.mGestureStubLeft != null) {
            this.mGestureStubLeft.hideGestureStubDelay();
        }
        if (this.mGestureStubRight != null) {
            this.mGestureStubRight.hideGestureStubDelay();
        }
        this.isShowing = false;
        unRegisterInputMethodVisibleHeightReceiver();
    }

    /* access modifiers changed from: private */
    public void clearBackStubWindow() {
        try {
            if (this.mGestureStubLeft != null) {
                this.mGestureStubLeft.clearGestureStub();
                this.mGestureStubLeft = null;
            }
            if (this.mGestureStubRight != null) {
                this.mGestureStubRight.clearGestureStub();
                this.mGestureStubRight = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.isShowing = false;
        unRegisterInputMethodVisibleHeightReceiver();
    }

    /* access modifiers changed from: private */
    public void updateFsgWindowState() {
        if (this.mHasNavigationBar) {
            if (!MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar") || this.mDisabledByDriveMode) {
                try {
                    if (this.mNavStubView != null) {
                        Log.d("RecentsImpl", "navstubview will be removed: updateFsgWindowState");
                        this.mWindowManager.removeView(this.mNavStubView);
                        this.mNavStubView = null;
                    }
                    clearBackStubWindow();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (this.mNavStubView == null) {
                        Log.d("RecentsImpl", "navstubview will be added: updateFsgWindowState");
                        this.mNavStubView = new NavStubView(this.mContext);
                        this.mWindowManager.addView(this.mNavStubView, this.mNavStubView.getWindowParam(this.mStubSize));
                    }
                    addBackStubWindow();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void addBackStubWindow() {
        if (this.mGestureStubLeft == null) {
            initGestureStub(-1);
        }
        this.mGestureStubLeft.showGestureStub();
        this.mGestureStubRight.showGestureStub();
        this.isShowing = true;
    }

    public void onBootCompleted() {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, -1, false);
        RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
        launchOpts.numVisibleTasks = loader.getIconCacheSize();
        launchOpts.numVisibleTaskThumbnails = loader.getThumbnailCacheSize();
        launchOpts.onlyLoadForCache = true;
        loader.loadTasks(this.mContext, plan, launchOpts);
    }

    public void onConfigurationChanged() {
        reloadResources();
        this.mDummyStackView.reloadOnConfigurationChange();
        this.mHeaderBar.onConfigurationChanged();
    }

    public void onVisibilityChanged(Context context, boolean visible) {
        Recents.getSystemServices().setRecentsVisibility(context, visible);
    }

    public void onStartScreenPinning(Context context, int taskId) {
        StatusBar statusBar = (StatusBar) ((Application) context.getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
        if (statusBar != null) {
            statusBar.showScreenPinningRequest(taskId, false);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x004c A[Catch:{ ActivityNotFoundException -> 0x0073 }] */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A[ADDED_TO_REGION, Catch:{ ActivityNotFoundException -> 0x0073 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void showRecents(boolean r15, boolean r16, boolean r17, boolean r18, boolean r19, int r20, boolean r21) {
        /*
            r14 = this;
            r7 = r14
            r8 = r15
            r9 = r16
            r10 = r18
            r7.mTriggeredFromAltTab = r8
            r7.mDraggingInRecents = r9
            r7.mLaunchedWhileDocking = r10
            com.android.systemui.recents.misc.DozeTrigger r0 = r7.mFastAltTabTrigger
            boolean r0 = r0.isAsleep()
            if (r0 == 0) goto L_0x001a
            com.android.systemui.recents.misc.DozeTrigger r0 = r7.mFastAltTabTrigger
            r0.stopDozing()
            goto L_0x0033
        L_0x001a:
            com.android.systemui.recents.misc.DozeTrigger r0 = r7.mFastAltTabTrigger
            boolean r0 = r0.isDozing()
            if (r0 == 0) goto L_0x002b
            if (r8 != 0) goto L_0x0025
            return
        L_0x0025:
            com.android.systemui.recents.misc.DozeTrigger r0 = r7.mFastAltTabTrigger
            r0.stopDozing()
            goto L_0x0033
        L_0x002b:
            if (r8 == 0) goto L_0x0033
            com.android.systemui.recents.misc.DozeTrigger r0 = r7.mFastAltTabTrigger
            r0.startDozing()
            return
        L_0x0033:
            com.android.systemui.recents.misc.SystemServicesProxy r0 = com.android.systemui.recents.Recents.getSystemServices()     // Catch:{ ActivityNotFoundException -> 0x0073 }
            r1 = 1
            r3 = 0
            if (r10 != 0) goto L_0x0040
            if (r9 == 0) goto L_0x003e
            goto L_0x0040
        L_0x003e:
            r2 = r3
            goto L_0x0041
        L_0x0040:
            r2 = r1
        L_0x0041:
            r11 = r2
            com.android.systemui.recents.model.MutableBoolean r2 = new com.android.systemui.recents.model.MutableBoolean     // Catch:{ ActivityNotFoundException -> 0x0073 }
            r2.<init>(r11)     // Catch:{ ActivityNotFoundException -> 0x0073 }
            r12 = r2
            boolean r2 = sOneKeyCleaning     // Catch:{ ActivityNotFoundException -> 0x0073 }
            if (r2 == 0) goto L_0x0053
            r1 = 2131821978(0x7f11059a, float:1.9276714E38)
            r7.showToast(r1)     // Catch:{ ActivityNotFoundException -> 0x0073 }
            return
        L_0x0053:
            if (r11 != 0) goto L_0x005b
            boolean r2 = r0.isRecentsActivityVisible(r12)     // Catch:{ ActivityNotFoundException -> 0x0073 }
            if (r2 != 0) goto L_0x0072
        L_0x005b:
            android.app.ActivityManager$RunningTaskInfo r2 = r0.getRunningTask()     // Catch:{ ActivityNotFoundException -> 0x0073 }
            boolean r4 = r12.value     // Catch:{ ActivityNotFoundException -> 0x0073 }
            if (r4 != 0) goto L_0x0067
            if (r19 == 0) goto L_0x0066
            goto L_0x0067
        L_0x0066:
            goto L_0x0068
        L_0x0067:
            r3 = r1
        L_0x0068:
            r1 = r7
            r4 = r17
            r5 = r20
            r6 = r21
            r1.startRecentsActivity(r2, r3, r4, r5, r6)     // Catch:{ ActivityNotFoundException -> 0x0073 }
        L_0x0072:
            goto L_0x007b
        L_0x0073:
            r0 = move-exception
            java.lang.String r1 = "RecentsImpl"
            java.lang.String r2 = "Failed to launch RecentsActivity"
            android.util.Log.e(r1, r2, r0)
        L_0x007b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.BaseRecentsImpl.showRecents(boolean, boolean, boolean, boolean, boolean, int, boolean):void");
    }

    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey, boolean triggeredFromFsGesture) {
        if (!triggeredFromAltTab || !this.mFastAltTabTrigger.isDozing()) {
            RecentsEventBus.getDefault().post(new HideRecentsEvent(triggeredFromAltTab, triggeredFromHomeKey, triggeredFromFsGesture));
            return;
        }
        showNextTask();
        this.mFastAltTabTrigger.stopDozing();
    }

    public void toggleRecents(int growTarget) {
        if (!this.mFastAltTabTrigger.isDozing()) {
            this.mDraggingInRecents = false;
            this.mLaunchedWhileDocking = false;
            this.mTriggeredFromAltTab = false;
            try {
                SystemServicesProxy ssp = Recents.getSystemServices();
                MutableBoolean isHomeStackVisible = new MutableBoolean(true);
                long elapsedTime = SystemClock.elapsedRealtime() - this.mLastToggleTime;
                if (ssp.isRecentsActivityVisible(isHomeStackVisible)) {
                    RecentsDebugFlags debugFlags = Recents.getDebugFlags();
                    if (!Recents.getConfiguration().getLaunchState().launchedWithAltTab) {
                        debugFlags.isPagingEnabled();
                        if (((long) ViewConfiguration.getDoubleTapMinTime()) < elapsedTime && elapsedTime < ((long) ViewConfiguration.getDoubleTapTimeout())) {
                            RecentsEventBus.getDefault().post(new LaunchNextTaskRequestEvent());
                            RecentsPushEventHelper.sendSwitchAppEvent("doubleTap", null);
                        } else if (debugFlags.isPagingEnabled()) {
                            RecentsEventBus.getDefault().post(new IterateRecentsEvent());
                        } else {
                            RecentsEventBus.getDefault().post(new ToggleRecentsEvent());
                            RecentsPushEventHelper.sendRecentsEvent("hideRecents", "clickRecentsKey");
                        }
                        this.mLastToggleTime = SystemClock.elapsedRealtime();
                    } else if (elapsedTime >= 350) {
                        RecentsEventBus.getDefault().post(new ToggleRecentsEvent());
                        RecentsPushEventHelper.sendRecentsEvent("hideRecents", "clickAltTabKey");
                        this.mLastToggleTime = SystemClock.elapsedRealtime();
                    }
                } else if (elapsedTime >= 350) {
                    if (sOneKeyCleaning) {
                        showToast(com.android.systemui.R.string.recent_task_cleaning);
                        return;
                    }
                    startRecentsActivity(ssp.getRunningTask(), isHomeStackVisible.value, true, growTarget, false);
                    ssp.sendCloseSystemWindows("recentapps");
                    this.mLastToggleTime = SystemClock.elapsedRealtime();
                }
            } catch (ActivityNotFoundException e) {
                Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
            }
        }
    }

    private void showToast(int resId) {
        Toast.makeText(this.mContext, resId, 1).show();
    }

    public void preloadRecents() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        MutableBoolean isHomeStackVisible = new MutableBoolean(true);
        if (!ssp.isRecentsActivityVisible(isHomeStackVisible)) {
            ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
            int runningTaskId = runningTask != null ? runningTask.id : 0;
            RecentsTaskLoader loader = Recents.getTaskLoader();
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
            sInstanceLoadPlan.preloadRawTasks(!isHomeStackVisible.value);
            loader.preloadTasks(sInstanceLoadPlan, runningTaskId, true ^ isHomeStackVisible.value);
            TaskStack stack = sInstanceLoadPlan.getTaskStack();
            if (stack.getTaskCount() > 0) {
                preloadIcon(runningTaskId);
                updateHeaderBarLayout(stack, null);
            }
        }
    }

    public void cancelPreloadingRecents() {
    }

    public void onDraggingInRecents(float distanceFromTop) {
        RecentsEventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEvent(distanceFromTop));
    }

    public void onDraggingInRecentsEnded(float velocity) {
        RecentsEventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEndedEvent(velocity));
    }

    public void showNextTask() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        int i = 0;
        loader.preloadTasks(plan, -1, false);
        TaskStack focusedStack = plan.getTaskStack();
        if (focusedStack != null && focusedStack.getTaskCount() != 0) {
            ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
            if (runningTask != null) {
                boolean isRunningTaskInHomeStack = SystemServicesProxy.isHomeOrRecentsStack(ActivityManagerCompat.getRunningTaskStackId(runningTask), runningTask);
                ArrayList<Task> tasks = focusedStack.getStackTasks();
                Task toTask = null;
                ActivityOptions launchOpts = null;
                int taskCount = tasks.size();
                while (true) {
                    if (i >= taskCount - 1) {
                        break;
                    }
                    Task task = tasks.get(i);
                    if (isRunningTaskInHomeStack) {
                        toTask = tasks.get(i + 1);
                        launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, com.android.systemui.R.anim.recents_launch_next_affiliated_task_target, com.android.systemui.R.anim.recents_fast_toggle_app_home_exit);
                        break;
                    } else if (task.key.id == runningTask.id) {
                        toTask = tasks.get(i + 1);
                        launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, com.android.systemui.R.anim.recents_launch_prev_affiliated_task_target, com.android.systemui.R.anim.recents_launch_prev_affiliated_task_source);
                        break;
                    } else {
                        i++;
                    }
                }
                if (toTask == null) {
                    ssp.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, com.android.systemui.R.anim.recents_launch_prev_affiliated_task_bounce));
                } else {
                    ssp.startActivityFromRecents(this.mContext, toTask.key, toTask.title, launchOpts);
                }
            }
        }
    }

    public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, Rect initialBounds) {
        int i = dragMode;
        Rect rect = initialBounds;
        boolean z = true;
        if (!Utilities.supportsMultiWindow()) {
            Toast.makeText(this.mContext, com.android.systemui.R.string.recent_cannot_dock, 1).show();
            return;
        }
        if (Recents.getSystemServices().moveTaskToDockedStack(topTaskId, stackCreateMode, rect)) {
            RecentsEventBus.getDefault().send(new DockedTopTaskEvent(i, rect));
            if (i != 0) {
                z = false;
            }
            showRecents(false, z, false, true, false, -1, false);
        }
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan plan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return plan;
    }

    private void reloadResources() {
        Resources res = this.mContext.getResources();
        this.mStatusBarHeight = res.getDimensionPixelSize(17105351);
        this.mNavBarHeight = res.getDimensionPixelSize(17105194);
        this.mNavBarWidth = res.getDimensionPixelSize(17105199);
        mTaskBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(this.mContext, com.android.systemui.R.dimen.recents_task_view_header_height, com.android.systemui.R.dimen.recents_task_view_header_height, com.android.systemui.R.dimen.recents_task_view_header_height, com.android.systemui.R.dimen.recents_task_view_header_height_tablet_land, com.android.systemui.R.dimen.recents_task_view_header_height, com.android.systemui.R.dimen.recents_task_view_header_height_tablet_land);
    }

    private void updateHeaderBarLayout(TaskStack stack, Rect windowRectOverride) {
        Rect rect;
        SystemServicesProxy ssp = Recents.getSystemServices();
        Rect displayRect = ssp.getDisplayRect();
        Rect systemInsets = new Rect();
        ssp.getStableInsets(systemInsets);
        if (windowRectOverride != null) {
            rect = new Rect(windowRectOverride);
        } else {
            rect = ssp.getWindowRect();
        }
        Rect windowRect = rect;
        if (ssp.hasDockedTask()) {
            windowRect.bottom -= systemInsets.bottom;
            systemInsets.bottom = 0;
        }
        calculateWindowStableInsets(systemInsets, windowRect);
        windowRect.offsetTo(0, 0);
        TaskStackLayoutAlgorithm stackLayout = this.mDummyStackView.getStackAlgorithm();
        stackLayout.setSystemInsets(systemInsets);
        if (stack != null) {
            stackLayout.getTaskStackBounds(displayRect, windowRect, systemInsets.top, systemInsets.left, systemInsets.right, this.mTaskStackBounds);
            stackLayout.reset();
            stackLayout.initialize(displayRect, windowRect, this.mTaskStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(stack));
            this.mDummyStackView.setTasks(stack, false);
            Rect taskViewBounds = stackLayout.getUntransformedTaskViewBounds();
            if (!taskViewBounds.isEmpty()) {
                int taskViewWidth = taskViewBounds.width();
                synchronized (this.mHeaderBarLock) {
                    if (!(this.mHeaderBar.getMeasuredWidth() == taskViewWidth && this.mHeaderBar.getMeasuredHeight() == mTaskBarHeight)) {
                        this.mHeaderBar.measure(View.MeasureSpec.makeMeasureSpec(taskViewWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(mTaskBarHeight, 1073741824));
                    }
                    this.mHeaderBar.layout(0, 0, taskViewWidth, mTaskBarHeight);
                }
                if (this.mThumbTransitionBitmapCache == null || this.mThumbTransitionBitmapCache.getWidth() != taskViewWidth || this.mThumbTransitionBitmapCache.getHeight() != mTaskBarHeight) {
                    this.mThumbTransitionBitmapCache = Bitmap.createBitmap(taskViewWidth, mTaskBarHeight, Bitmap.Config.ARGB_8888);
                }
            }
        }
    }

    private void calculateWindowStableInsets(Rect inOutInsets, Rect windowRect) {
        Rect appRect = new Rect(Recents.getSystemServices().getDisplayRect());
        inset(appRect, inOutInsets);
        Rect windowRectWithInsets = new Rect(windowRect);
        windowRectWithInsets.intersect(appRect);
        inOutInsets.left = windowRectWithInsets.left - windowRect.left;
        inOutInsets.top = windowRectWithInsets.top - windowRect.top;
        inOutInsets.right = windowRect.right - windowRectWithInsets.right;
        inOutInsets.bottom = windowRect.bottom - windowRectWithInsets.bottom;
    }

    private void inset(Rect source, Rect insets) {
        source.left += insets.left;
        source.top += insets.top;
        source.right -= insets.right;
        source.bottom -= insets.bottom;
    }

    private void preloadIcon(int runningTaskId) {
        RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
        launchOpts.runningTaskId = runningTaskId;
        launchOpts.loadThumbnails = false;
        launchOpts.onlyLoadForCache = true;
        Recents.getTaskLoader().loadTasks(this.mContext, sInstanceLoadPlan, launchOpts);
    }

    /* access modifiers changed from: protected */
    public ActivityOptions getUnknownTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, com.android.systemui.R.anim.recents_from_unknown_enter, com.android.systemui.R.anim.recents_from_unknown_exit, this.mHandler, null);
    }

    /* access modifiers changed from: protected */
    public ActivityOptions getHomeTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, com.android.systemui.R.anim.recents_from_launcher_enter, com.android.systemui.R.anim.recents_from_launcher_exit, this.mHandler, null);
    }

    /* access modifiers changed from: package-private */
    public ActivityOptions getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo runningTask, TaskStackView stackView, Rect windowOverrideRect) {
        TaskViewTransform toTransform = getThumbnailTransitionTransform(stackView, new Task(), windowOverrideRect);
        if (this.mThumbTransitionBitmapCache == null) {
            return getUnknownTransitionActivityOptions();
        }
        RectF toTaskRect = toTransform.rect;
        toTaskRect.top += (float) mTaskBarHeight;
        return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, this.mThumbTransitionBitmapCache, (int) toTaskRect.left, (int) toTaskRect.top, (int) toTaskRect.width(), (int) toTaskRect.height(), this.mHandler, null);
    }

    /* access modifiers changed from: package-private */
    public TaskViewTransform getThumbnailTransitionTransform(TaskStackView stackView, Task runningTaskOut, Rect windowOverrideRect) {
        TaskStack stack = stackView.getStack();
        Task launchTask = stack.getLaunchTarget();
        if (launchTask != null) {
            runningTaskOut.copyFrom(launchTask);
        } else {
            launchTask = stack.getStackFrontMostTask(true);
            if (launchTask == null) {
                return null;
            }
            runningTaskOut.copyFrom(launchTask);
        }
        stackView.updateLayoutAlgorithm(true);
        stackView.updateToInitialState();
        stackView.getStackAlgorithm().getStackTransformScreenCoordinates(launchTask, stackView.getScroller().getStackScroll(), this.mTmpTransform, null, windowOverrideRect);
        return this.mTmpTransform;
    }

    private RectF getLaunchTargetTaskViewRect(ActivityManager.RunningTaskInfo runningTask, TaskStackView stackView, Rect windowOverrideRect, Task toTask) {
        RectF toTaskRect = getThumbnailTransitionTransform(stackView, toTask, windowOverrideRect).rect;
        toTaskRect.top += (float) mTaskBarHeight;
        return toTaskRect;
    }

    /* access modifiers changed from: protected */
    public void startRecentsActivity(ActivityManager.RunningTaskInfo runningTask, boolean isHomeStackVisible, boolean animate, int growTarget, boolean fsGesture) {
        int i;
        ActivityOptions opts;
        ActivityManager.RunningTaskInfo runningTaskInfo = runningTask;
        boolean z = fsGesture;
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsActivityLaunchState launchState = new RecentsActivityLaunchState();
        StringBuilder sb = new StringBuilder();
        sb.append("startRecentsActivity runningTask: ");
        sb.append(runningTaskInfo != null ? runningTaskInfo.baseActivity.toString() : "null");
        Log.d("RecentsImpl", sb.toString());
        if (this.mLaunchedWhileDocking || runningTaskInfo == null) {
            i = -1;
        } else {
            i = runningTaskInfo.id;
        }
        int runningTaskId = i;
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || sInstanceLoadPlan == null) {
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || !sInstanceLoadPlan.hasTasks()) {
            loader.preloadTasks(sInstanceLoadPlan, runningTaskId, !isHomeStackVisible);
        }
        TaskStack stack = sInstanceLoadPlan.getTaskStack();
        boolean hasRecentTasks = stack.getTaskCount() > 0;
        boolean useThumbnailTransition = runningTaskInfo != null && !isHomeStackVisible && hasRecentTasks;
        launchState.launchedFromHome = !useThumbnailTransition && !this.mLaunchedWhileDocking;
        launchState.launchedFromApp = useThumbnailTransition || this.mLaunchedWhileDocking;
        launchState.launchedViaDockGesture = this.mLaunchedWhileDocking;
        launchState.launchedViaDragGesture = this.mDraggingInRecents;
        launchState.launchedToTaskId = runningTaskId;
        launchState.launchedWithAltTab = this.mTriggeredFromAltTab;
        launchState.launchedViaFsGesture = z;
        preloadIcon(runningTaskId);
        Rect windowOverrideRect = getWindowRectOverride(growTarget);
        updateHeaderBarLayout(stack, windowOverrideRect);
        TaskStackLayoutAlgorithm.VisibilityReport stackVr = this.mDummyStackView.computeStackVisibilityReport();
        launchState.launchedNumVisibleTasks = stackVr.numVisibleTasks;
        launchState.launchedNumVisibleThumbnails = stackVr.numVisibleThumbnails;
        boolean shouldTranslucent = launchState.launchedViaFsGesture && launchState.launchedFromHome;
        RecentsEventBus.getDefault().send(new ActivitySetDummyTranslucentEvent(shouldTranslucent));
        if (!animate) {
        } else if (z) {
            RecentsTaskLoader recentsTaskLoader = loader;
        } else {
            if (useThumbnailTransition) {
                opts = getThumbnailTransitionActivityOptions(runningTaskInfo, this.mDummyStackView, windowOverrideRect);
            } else if (hasRecentTasks) {
                opts = getHomeTransitionActivityOptions();
            } else {
                opts = getUnknownTransitionActivityOptions();
            }
            startRecentsActivity(opts, launchState);
            RecentsTaskLoader recentsTaskLoader2 = loader;
            this.mLastToggleTime = SystemClock.elapsedRealtime();
            return;
        }
        if (isHomeStackVisible) {
            startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), launchState);
            boolean z2 = shouldTranslucent;
        } else {
            Task toTask = new Task();
            TaskViewTransform toTransform = getThumbnailTransitionTransform(this.mDummyStackView, toTask, windowOverrideRect);
            if (toTransform != null) {
                RectF toTaskRect = toTransform.rect;
                Task task = toTask;
                toTaskRect.top += (float) mTaskBarHeight;
                TaskViewTransform taskViewTransform = toTransform;
                boolean z3 = shouldTranslucent;
                try {
                    Method makeTaskLaunch = ActivityOptions.class.getMethod("makeTaskLaunchBehindWithCoordinates", new Class[]{Context.class, View.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE});
                    if (makeTaskLaunch != null) {
                        startRecentsActivity((ActivityOptions) makeTaskLaunch.invoke(ActivityOptions.class, new Object[]{this.mContext, this.mDummyStackView, Integer.valueOf((int) toTaskRect.left), Integer.valueOf((int) toTaskRect.top), Integer.valueOf((int) toTaskRect.width()), Integer.valueOf((int) toTaskRect.height()), -1, -1}), launchState);
                    }
                } catch (Exception e) {
                    Log.e("RecentsImpl", "makeTaskLaunchBehindWithCoordinates method not found");
                    startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), launchState);
                }
            } else {
                TaskViewTransform taskViewTransform2 = toTransform;
                boolean z4 = shouldTranslucent;
                startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), launchState);
            }
        }
        if (hasRecentTasks) {
            Task launchTargetTask = new Task();
            RecentsEventBus.getDefault().send(new FsGestureLaunchTargetTaskViewRectEvent(getLaunchTargetTaskViewRect(runningTaskInfo, this.mDummyStackView, windowOverrideRect, launchTargetTask), launchTargetTask));
        }
    }

    private Rect getWindowRectOverride(int growTarget) {
        if (growTarget == -1) {
            return null;
        }
        Rect result = new Rect();
        Rect displayRect = Recents.getSystemServices().getDisplayRect();
        DockedDividerUtils.calculateBoundsForPosition(growTarget, 4, result, displayRect.width(), displayRect.height(), Recents.getSystemServices().getDockedDividerSize(this.mContext));
        return result;
    }

    private void startRecentsActivity(ActivityOptions opts, RecentsActivityLaunchState launchState) {
        Intent intent = new Intent();
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(277364736);
        intent.putExtra("launchState", launchState);
        if (opts != null) {
            try {
                this.mContext.startActivityAsUser(intent, opts.toBundle(), UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Log.e("RecentsImpl", "startRecentsActivity", e);
                return;
            }
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        RecentsEventBus.getDefault().send(new RecentsActivityStartingEvent());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        sOneKeyCleaning = true;
    }

    public final void onBusEvent(DismissAllTaskViewsEndEvent event) {
        sOneKeyCleaning = false;
    }

    public final void onBusEvent(FsGestureShowStateEvent event) {
        updateFsgWindowVisibilityState(event.isEnter, event.typeFrom);
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        this.mRecentsVisible = event.visible;
    }

    /* access modifiers changed from: private */
    public void updateFsgWindowVisibilityState(boolean isEnter, String typeFrom) {
        if (this.mNavStubView != null && !this.mIsInAnotherPro && MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar")) {
            if (isEnter) {
                char c = 65535;
                switch (typeFrom.hashCode()) {
                    case -1025688671:
                        if (typeFrom.equals("typefrom_keyguard")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -863436742:
                        if (typeFrom.equals("typefrom_provision")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 413913473:
                        if (typeFrom.equals("typefrom_status_bar_expansion")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1076218718:
                        if (typeFrom.equals("typefrom_demo")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1076347482:
                        if (typeFrom.equals("typefrom_home")) {
                            c = 4;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        this.mNavStubView.setVisibility(8);
                        Log.d("RecentsImpl", "resume demo nstub gone");
                        hideBackStubWindow();
                        break;
                    case 2:
                    case 3:
                        Log.d("RecentsImpl", "resume statusbar nstub gone");
                        this.mNavStubView.setVisibility(8);
                        showBackStubWindow();
                        break;
                    default:
                        this.mNavStubView.setVisibility(0);
                        showBackStubWindow();
                        break;
                }
            } else if ("typefrom_keyguard".equals(typeFrom) && this.mKM.isKeyguardLocked()) {
                this.mNavStubView.setVisibility(0);
                showBackStubWindow();
            } else if ("typefrom_home".equals(typeFrom)) {
                ComponentName cn = Util.getTopActivity(this.mContext);
                if (cn != null && "com.miui.home.launcher.Launcher:com.miui.personalassistant.fake.FakeStartActivity:com.miui.personalassistant.fake.FakeEndActivity".contains(cn.getClassName())) {
                    this.mNavStubView.setVisibility(0);
                    hideBackStubWindow();
                }
            } else {
                adaptToTopActivity();
            }
        }
    }

    public final void onBusEvent(FsGestureEnterRecentsEvent event) {
        this.mIsHideRecentsViewByFsGesture = true;
        showRecents(false, false, false, false, false, -1, true);
    }

    public final void onBusEvent(FsGestureShowFirstCardEvent event) {
        this.mIsHideRecentsViewByFsGesture = false;
    }

    public final void onBusEvent(FsGesturePreloadRecentsEvent event) {
        preloadRecents();
    }

    public final void onBusEvent(HideNavStubForBackWindow event) {
        hideBackStubWindow();
    }

    public final void onBusEvent(RotationChangedEvent event) {
        this.mDummyStackView.onBusEvent(event);
    }

    public boolean getIsHideRecentsViewByFsGesture() {
        return this.mIsHideRecentsViewByFsGesture;
    }
}
