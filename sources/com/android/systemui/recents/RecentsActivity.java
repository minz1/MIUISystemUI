package com.android.systemui.recents;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.keyguard.BoostFrameworkHelper;
import com.android.systemui.Application;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.CloudDataHelper;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ActivitySetDummyTranslucentEvent;
import com.android.systemui.recents.events.activity.AnimFirstTaskViewAlphaEvent;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DebugFlagsChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsCompleteEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsZoomEvent;
import com.android.systemui.recents.events.activity.FsGestureRecentsViewWrapperEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideInEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideOutEvent;
import com.android.systemui.recents.events.activity.HideMemoryAndDockEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.NavStubViewAttachToWindowEvent;
import com.android.systemui.recents.events.activity.RotationChangedEvent;
import com.android.systemui.recents.events.activity.ScrollerFlingFinishEvent;
import com.android.systemui.recents.events.activity.ShowMemoryAndDockEvent;
import com.android.systemui.recents.events.activity.StackScrollChangedEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.events.component.ChangeTaskLockStateEvent;
import com.android.systemui.recents.events.component.ExitMultiModeEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.CleanInRecentsEvents;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEndEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.UpdateFreeformTaskViewVisibilityEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.CircleAndTickAnimView;
import com.android.systemui.recents.views.RecentsRecommendView;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.recents.views.TaskStackView;
import com.miui.daemon.performance.PerfShielderManager;
import com.miui.enterprise.ApplicationHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import miui.os.Build;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.securityspace.CrossUserUtils;
import miui.securityspace.XSpaceUserHandle;
import miui.util.HardwareInfo;
import miui.view.animation.SineEaseInOutInterpolator;
import miui.view.animation.SineEaseOutInterpolator;

public class RecentsActivity extends Activity implements ViewTreeObserver.OnPreDrawListener {
    public static long mFreeBeforeClean;
    /* access modifiers changed from: private */
    public static boolean sForceBlack = false;
    private ContentObserver mAccessControlLockModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            if (ssp != null) {
                ssp.setAccessControlLockMode(Settings.Secure.getIntForUser(RecentsActivity.this.getContentResolver(), "access_control_lock_mode", 1, -2));
            }
        }
    };
    private View mBackGround;
    private CircleAndTickAnimView mClearAnimView;
    public final DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    /* access modifiers changed from: private */
    public ReferenceCountedTrigger mDismissAllTaskViewEventTrigger;
    private DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onDisplayChanged(int displayId) {
            int rotation = ((WindowManager) RecentsActivity.this.getSystemService("window")).getDefaultDisplay().getRotation();
            if (RecentsActivity.this.mRotation != rotation) {
                int unused = RecentsActivity.this.mRotation = rotation;
                RecentsActivity.this.setNotchPadding();
                RecentsEventBus.getDefault().send(new RotationChangedEvent(RecentsActivity.this.mRotation));
            }
        }
    };
    private TextView mDockBtn;
    private Button mExitMultiModeBtn;
    private boolean mFinishedOnStartup;
    private int mFocusTimerDuration;
    private ContentObserver mForceBlackObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            boolean unused = RecentsActivity.sForceBlack = MiuiSettings.Global.getBoolean(RecentsActivity.this.getContentResolver(), "force_black");
        }
    };
    /* access modifiers changed from: private */
    public long mFreeAtFirst;
    /* access modifiers changed from: private */
    public Handler mHandler = new RecentsHandler();
    private Intent mHomeIntent;
    private boolean mIgnoreAltTabRelease;
    private View mIncompatibleAppOverlay;
    private boolean mIsAddExitMultiModeBtn;
    private boolean mIsRecommendVisible;
    /* access modifiers changed from: private */
    public boolean mIsShowRecommend;
    /* access modifiers changed from: private */
    public boolean mIsVisible;
    private DozeTrigger mIterateTrigger;
    private int mLastDeviceOrientation = 0;
    private int mLastDisplayDensity;
    private long mLastTabKeyEventTime;
    private ViewGroup mMemoryAndClearContainer;
    private boolean mNeedMoveRecentsToFrontOfFsGesture = true;
    private boolean mNeedReloadStackView = true;
    private RecentsPackageMonitor mPackageMonitor;
    private boolean mReceivedNewIntent;
    private FrameLayout mRecentsContainer;
    private final ViewTreeObserver.OnPreDrawListener mRecentsDrawnEventListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            RecentsActivity.this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
            RecentsEventBus.getDefault().post(new RecentsDrawnEvent());
            RecentsActivity.this.checkFsGestureOnEnterRecents();
            return true;
        }
    };
    private RecentsRecommendView mRecentsRecommendView;
    /* access modifiers changed from: private */
    public RecentsView mRecentsView;
    /* access modifiers changed from: private */
    public int mRotation = -1;
    private final Runnable mSendEnterWindowAnimationCompleteRunnable = new Runnable() {
        public void run() {
            RecentsEventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
        }
    };
    private View mSeparatorForMemoryInfo;
    private Method mSetDummyTranslucentMethod;
    private ContentObserver mShowRecommendObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            BackgroundThread.getHandler().removeCallbacks(RecentsActivity.this.mShowRecommendRunnable);
            BackgroundThread.getHandler().post(RecentsActivity.this.mShowRecommendRunnable);
        }
    };
    /* access modifiers changed from: private */
    public Runnable mShowRecommendRunnable = new Runnable() {
        public void run() {
            final boolean isShowRecommend = MiuiSettings.System.getBooleanForUser(RecentsActivity.this.getContentResolver(), "miui_recents_show_recommend", MiuiSettings.SettingsCloudData.getCloudDataBoolean(RecentsActivity.this.getContentResolver(), "showRecentsRecommend", "isShow", true), -2);
            RecentsActivity.this.mHandler.post(new Runnable() {
                public void run() {
                    boolean unused = RecentsActivity.this.mIsShowRecommend = isShowRecommend;
                    RecentsActivity.this.updateRecentsRecommendViewVisible();
                }
            });
        }
    };
    private ContentObserver mSlideCoverObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (Settings.System.getIntForUser(RecentsActivity.this.getContentResolver(), "sc_status", -1, -2) == 0 && RecentsActivity.this.mIsVisible) {
                RecentsActivity.this.moveTaskToBack(true);
            }
        }
    };
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                RecentsActivity.this.mHandler.post(new Runnable() {
                    public void run() {
                        RecentsActivity.this.dismissRecentsToHomeIfVisible(false);
                    }
                });
            }
        }
    };
    private TextView mTipView;
    /* access modifiers changed from: private */
    public long mTotalMemory;
    private ViewGroup mTxtMemoryContainer;
    private TextView mTxtMemoryInfo1;
    private TextView mTxtMemoryInfo2;
    private final UserInteractionEvent mUserInteractionEvent = new UserInteractionEvent();

    class LaunchHomeRunnable implements Runnable {
        Intent mLaunchIntent;
        ActivityOptions mOpts;

        public LaunchHomeRunnable(Intent launchIntent, ActivityOptions opts) {
            this.mLaunchIntent = launchIntent;
            this.mOpts = opts;
        }

        public void run() {
            RecentsActivity.this.mHandler.post(new Runnable() {
                public void run() {
                    try {
                        ActivityOptions opts = LaunchHomeRunnable.this.mOpts;
                        if (opts == null) {
                            opts = ActivityOptions.makeCustomAnimation(RecentsActivity.this, R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
                        }
                        RecentsEventBus.getDefault().send(new ActivitySetDummyTranslucentEvent(false));
                        RecentsActivity.this.startActivityAsUser(LaunchHomeRunnable.this.mLaunchIntent, opts.toBundle(), UserHandle.CURRENT);
                        Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, 1.0f);
                    } catch (Exception e) {
                        Log.e("RecentsActivity", RecentsActivity.this.getString(R.string.recents_launch_error_message, new Object[]{"Home"}), e);
                    }
                }
            });
        }
    }

    class RecentsHandler extends Handler {
        RecentsHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1000) {
                RecentsActivity.this.doClearAnim();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToTargetTask(int logCategory) {
        if (!Recents.getSystemServices().isRecentsActivityVisible() || !this.mRecentsView.launchTargetTask(logCategory)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToLaunchTargetTaskOrHome() {
        if (Recents.getSystemServices().isRecentsActivityVisible()) {
            if (this.mRecentsView.launchPreviousTask()) {
                return true;
            }
            dismissRecentsToHome(true);
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToTargetTaskOrHome() {
        if (!Recents.getSystemServices().isRecentsActivityVisible()) {
            return false;
        }
        if (this.mRecentsView.launchTargetTask(0)) {
            return true;
        }
        dismissRecentsToHome(true);
        return true;
    }

    /* access modifiers changed from: package-private */
    public void dismissRecentsToHome(boolean animateTaskViews) {
        dismissRecentsToHome(animateTaskViews, null);
    }

    /* access modifiers changed from: package-private */
    public void dismissRecentsToHome(boolean animateTaskViews, ActivityOptions overrideAnimation) {
        this.mRecentsView.getMenuView().removeMenu(false);
        DismissRecentsToHomeAnimationStarted dismissEvent = new DismissRecentsToHomeAnimationStarted(animateTaskViews);
        this.mHandler.post(new LaunchHomeRunnable(this.mHomeIntent, overrideAnimation));
        if (Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            this.mNeedMoveRecentsToFrontOfFsGesture = false;
            RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
        }
        Recents.getSystemServices().sendCloseSystemWindows("homekey");
        RecentsEventBus.getDefault().send(dismissEvent);
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToHomeIfVisible(boolean animated) {
        if (!Recents.getSystemServices().isRecentsActivityVisible()) {
            return false;
        }
        dismissRecentsToHome(animated);
        return true;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(1);
        boolean shouldTranslucent = false;
        this.mFinishedOnStartup = false;
        if (Recents.getSystemServices() == null) {
            this.mFinishedOnStartup = true;
            finish();
            return;
        }
        Recents.getConfiguration().getLaunchState().copyFrom((RecentsActivityLaunchState) getIntent().getParcelableExtra("launchState"));
        RecentsEventBus.getDefault().register(this, 2);
        this.mPackageMonitor = new RecentsPackageMonitor();
        this.mPackageMonitor.register(this);
        BoostFrameworkHelper.initBoostFramework();
        setContentView(R.layout.recents);
        takeKeyEvents(true);
        this.mRecentsContainer = (FrameLayout) findViewById(R.id.recents_container);
        this.mRecentsView = (RecentsView) findViewById(R.id.recents_view);
        this.mTotalMemory = HardwareInfo.getTotalPhysicalMemory() / 1024;
        this.mMemoryAndClearContainer = (ViewGroup) findViewById(R.id.memoryAndClearContainer);
        this.mTxtMemoryContainer = (ViewGroup) findViewById(R.id.txtMemoryContainer);
        this.mTxtMemoryInfo1 = (TextView) findViewById(R.id.txtMemoryInfo1);
        this.mTxtMemoryInfo2 = (TextView) findViewById(R.id.txtMemoryInfo2);
        this.mSeparatorForMemoryInfo = findViewById(R.id.separatorForMemoryInfo);
        this.mClearAnimView = (CircleAndTickAnimView) findViewById(R.id.clearAnimView);
        this.mDockBtn = (TextView) findViewById(R.id.btnDock);
        this.mTipView = (TextView) findViewById(R.id.tip);
        this.mBackGround = findViewById(R.id.background);
        addRecentsRecommendViewIfNeeded();
        getWindow().getAttributes().privateFlags |= 16384;
        Configuration appConfiguration = Utilities.getAppConfiguration(this);
        this.mLastDeviceOrientation = appConfiguration.orientation;
        this.mLastDisplayDensity = appConfiguration.densityDpi;
        this.mFocusTimerDuration = getResources().getInteger(R.integer.recents_auto_advance_duration);
        this.mIterateTrigger = new DozeTrigger(this.mFocusTimerDuration, new Runnable() {
            public void run() {
                RecentsActivity.this.dismissRecentsToTargetTask(288);
            }
        });
        this.mBackGround.setBackgroundDrawable(this.mRecentsView.getBackgroundScrim());
        this.mHomeIntent = new Intent("android.intent.action.MAIN", null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.mSystemBroadcastReceiver, filter, null, (Handler) Dependency.get(Dependency.SCREEN_OFF_HANDLER));
        getWindow().addPrivateFlags(64);
        this.mClearAnimView.setDrawables(R.drawable.notifications_clear_all, R.drawable.btn_clear_all);
        this.mClearAnimView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RecentsActivity.this.cleanInRecents();
            }
        });
        this.mClearAnimView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setClassName("com.android.settings", "com.android.settings.applications.ManageApplicationsActivity");
                intent.putExtra("com.android.settings.APPLICATION_LIST_TYPE", 2);
                intent.setFlags(268435456);
                TaskStackBuilder.create(RecentsActivity.this.getApplicationContext()).addNextIntentWithParentStack(intent).startActivities(null, UserHandle.CURRENT);
                return true;
            }
        });
        this.mDockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RecentsActivity.this.updateDockRegions(true);
            }
        });
        fitsSystemWindowInsets(this.mDockBtn);
        fitsSystemWindowInsets(this.mMemoryAndClearContainer);
        fitsSystemWindowInsets(this.mTxtMemoryContainer);
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        RecentsEventBus.getDefault().send(new FsGestureRecentsViewWrapperEvent(this.mRecentsView, this.mBackGround, this.mRecentsContainer));
        try {
            this.mSetDummyTranslucentMethod = getClass().getMethod("setDummyTranslucent", new Class[]{Boolean.TYPE});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        if (Recents.getConfiguration().getLaunchState().launchedViaFsGesture && Recents.getConfiguration().getLaunchState().launchedFromHome) {
            shouldTranslucent = true;
        }
        RecentsEventBus.getDefault().send(new ActivitySetDummyTranslucentEvent(shouldTranslucent));
        registerContentObservers();
    }

    public static boolean isForceBlack() {
        return sForceBlack;
    }

    private void registerContentObservers() {
        if (Constants.IS_NOTCH) {
            getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_black"), false, this.mForceBlackObserver, -1);
            this.mForceBlackObserver.onChange(false);
        }
        if (Utilities.isSlideCoverDevice()) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor("sc_status"), false, this.mSlideCoverObserver, -1);
        }
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor("access_control_lock_mode"), false, this.mAccessControlLockModeObserver, -1);
        this.mAccessControlLockModeObserver.onChange(false);
        getContentResolver().registerContentObserver(Settings.System.getUriFor("miui_recents_show_recommend"), false, this.mShowRecommendObserver, -1);
        getContentResolver().registerContentObserver(CloudDataHelper.URI_CLOUD_ALL_DATA_NOTIFY, false, this.mShowRecommendObserver, -1);
        this.mShowRecommendObserver.onChange(false);
    }

    private void unRegisterContentObservers() {
        if (Constants.IS_NOTCH) {
            getContentResolver().unregisterContentObserver(this.mForceBlackObserver);
        }
        if (Utilities.isSlideCoverDevice()) {
            getContentResolver().unregisterContentObserver(this.mSlideCoverObserver);
        }
        getContentResolver().unregisterContentObserver(this.mAccessControlLockModeObserver);
        getContentResolver().unregisterContentObserver(this.mShowRecommendObserver);
    }

    /* access modifiers changed from: private */
    public void cleanInRecents() {
        BoostFrameworkHelper.setBoost(7);
        long freeMemory = getFreeMemory();
        this.mFreeAtFirst = freeMemory;
        mFreeBeforeClean = freeMemory;
        deepClean();
        DismissAllTaskViewsEvent dismissAllTaskViewsEvent = new DismissAllTaskViewsEvent();
        dismissAllTaskViewsEvent.getAnimationTrigger().increment();
        RecentsEventBus.getDefault().send(dismissAllTaskViewsEvent);
        this.mDismissAllTaskViewEventTrigger = dismissAllTaskViewsEvent.getAnimationTrigger();
        this.mHandler.removeMessages(1000);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1000), 300);
    }

    public void updateDockRegions(boolean canEnterMultiWindow) {
        RecentsConfiguration.sCanMultiWindow = canEnterMultiWindow;
        float f = 1.0f;
        if (this.mIsVisible) {
            this.mDockBtn.animate().alpha((canEnterMultiWindow || this.mDockBtn.getTranslationY() != 0.0f) ? 0.0f : 1.0f).setDuration(50).start();
            this.mTxtMemoryContainer.animate().alpha((canEnterMultiWindow || this.mTxtMemoryContainer.getTranslationY() != 0.0f) ? 0.0f : 1.0f).setDuration(50).start();
            if (this.mRecentsRecommendView != null) {
                ViewPropertyAnimator animate = this.mRecentsRecommendView.animate();
                if (canEnterMultiWindow) {
                    f = 0.0f;
                }
                animate.alpha(f).setDuration(50).start();
                this.mRecentsRecommendView.setAllItemClickable(!canEnterMultiWindow);
            }
        } else if (this.mRecentsRecommendView != null) {
            RecentsRecommendView recentsRecommendView = this.mRecentsRecommendView;
            if (canEnterMultiWindow) {
                f = 0.0f;
            }
            recentsRecommendView.setAlpha(f);
            this.mRecentsRecommendView.setAllItemClickable(!canEnterMultiWindow);
        }
        if (canEnterMultiWindow) {
            this.mRecentsView.announceForAccessibility(getString(R.string.accessibility_drag_hint_message));
            this.mRecentsView.showDockRegionsAnim();
            return;
        }
        this.mRecentsView.hideDockRegionsAnim();
    }

    private void fitsSystemWindowInsets(View view) {
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                Rect systemWindowInsets = insets.getSystemWindowInsets();
                ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).setMargins(systemWindowInsets.left, 0, systemWindowInsets.right, systemWindowInsets.bottom);
                return insets;
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        Log.d("RecentsActivity", "onStart");
        reloadStackView();
        RecentsEventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, true));
        MetricsLogger.visible(this, 224);
        if (!Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 0.0f, 1.0f);
        }
        registerDisplayListener();
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Recents.getConfiguration().getLaunchState().copyFrom((RecentsActivityLaunchState) intent.getParcelableExtra("launchState"));
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        reloadStackView();
        this.mReceivedNewIntent = true;
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mNeedReloadStackView = true;
        this.mClearAnimView.stopAnimator();
        refreshMemoryInfo();
        this.mTxtMemoryContainer.setVisibility(isMemInfoShow() ? 0 : 4);
        setupVisible();
        if (this.mRecentsRecommendView != null) {
            RecentsPushEventHelper.sendShowRecommendCardEvent(this.mIsRecommendVisible);
        }
        if (!Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            if (Recents.getConfiguration().getLaunchState().launchedFromApp) {
                this.mRecentsView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(200).setStartDelay(0).setInterpolator(new SineEaseInOutInterpolator()).start();
            } else {
                this.mRecentsView.animate().cancel();
                this.mRecentsView.setAlpha(1.0f);
                this.mRecentsView.setScaleX(1.0f);
                this.mRecentsView.setScaleY(1.0f);
            }
        }
        updateBlurRatioIfNeed();
        if (!Recents.getSystemServices().hasDockedTask()) {
            setSystemUiVisibility();
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateBlurRatioIfNeed();
    }

    private void updateBlurRatioIfNeed() {
        if (!Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            this.mRecentsView.updateBlurRatio(1.0f);
        }
    }

    private void reloadStackView() {
        if (this.mNeedReloadStackView) {
            int launchTaskIndexInStack = 0;
            this.mNeedReloadStackView = false;
            RecentsTaskLoader loader = Recents.getTaskLoader();
            RecentsTaskLoadPlan loadPlan = RecentsImpl.consumeInstanceLoadPlan();
            if (loadPlan == null) {
                loadPlan = loader.createLoadPlan(this);
            }
            RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
            if (!loadPlan.hasTasks()) {
                loader.preloadTasks(loadPlan, launchState.launchedToTaskId, !launchState.launchedFromHome);
            }
            RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
            loadOpts.runningTaskId = launchState.launchedToTaskId;
            loadOpts.numVisibleTasks = launchState.launchedNumVisibleTasks;
            loadOpts.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
            loader.loadTasks(this, loadPlan, loadOpts);
            TaskStack stack = loadPlan.getTaskStack();
            this.mRecentsView.onReload(this.mIsVisible, stack.getTaskCount() == 0);
            this.mRecentsView.updateStack(stack, true);
            if (!launchState.launchedFromHome && !launchState.launchedFromApp) {
                RecentsEventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
            }
            if (launchState.launchedWithAltTab) {
                MetricsLogger.count(this, "overview_trigger_alttab", 1);
            } else {
                MetricsLogger.count(this, "overview_trigger_nav_btn", 1);
            }
            if (launchState.launchedFromApp) {
                Task launchTarget = stack.getLaunchTarget();
                if (launchTarget != null) {
                    launchTaskIndexInStack = stack.indexOfStackTask(launchTarget);
                }
                MetricsLogger.count(this, "overview_source_app", 1);
                MetricsLogger.histogram(this, "overview_source_app_index", launchTaskIndexInStack);
            } else {
                MetricsLogger.count(this, "overview_source_home", 1);
            }
            MetricsLogger.histogram(this, "overview_task_count", this.mRecentsView.getStack().getTaskCount());
            this.mIsVisible = true;
            RecentsPushEventHelper.sendEnterRecentsEvent(stack);
        }
    }

    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        this.mHandler.removeCallbacks(this.mSendEnterWindowAnimationCompleteRunnable);
        if (!this.mReceivedNewIntent) {
            this.mHandler.post(this.mSendEnterWindowAnimationCompleteRunnable);
        } else {
            this.mSendEnterWindowAnimationCompleteRunnable.run();
        }
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        this.mIgnoreAltTabRelease = false;
        this.mIterateTrigger.stopDozing();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mRecentsView == null) {
            Log.e("RecentsActivity", "onConfigurationChanged error, mRecentsView==null.");
            return;
        }
        Configuration newDeviceConfiguration = Utilities.getAppConfiguration(this);
        int numStackTasks = this.mRecentsView.getStack().getStackTaskCount();
        boolean z = true;
        boolean orientationChange = this.mLastDeviceOrientation != newDeviceConfiguration.orientation;
        RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
        boolean z2 = this.mLastDisplayDensity != newDeviceConfiguration.densityDpi;
        if (numStackTasks <= 0) {
            z = false;
        }
        recentsEventBus.send(new ConfigurationChangedEvent(false, orientationChange, z2, z));
        if (orientationChange) {
            this.mMemoryAndClearContainer.setPadding(this.mMemoryAndClearContainer.getPaddingLeft(), this.mMemoryAndClearContainer.getPaddingTop(), this.mMemoryAndClearContainer.getPaddingRight(), getResources().getDimensionPixelSize(R.dimen.recent_task_padding_bottom));
            this.mLastDeviceOrientation = newDeviceConfiguration.orientation;
            updateRecentsRecommendViewVisible();
        }
        this.mLastDisplayDensity = newDeviceConfiguration.densityDpi;
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (this.mIsVisible) {
            RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
            RecentsTaskLoader loader = Recents.getTaskLoader();
            RecentsTaskLoadPlan loadPlan = loader.createLoadPlan(this);
            loader.preloadTasks(loadPlan, -1, false);
            RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
            loadOpts.numVisibleTasks = launchState.launchedNumVisibleTasks;
            loadOpts.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
            loader.loadTasks(this, loadPlan, loadOpts);
            TaskStack stack = loadPlan.getTaskStack();
            int numStackTasks = stack.getStackTaskCount();
            boolean showDeferredAnimation = numStackTasks > 0;
            RecentsEventBus.getDefault().send(new ConfigurationChangedEvent(true, false, false, numStackTasks > 0));
            RecentsEventBus.getDefault().send(new MultiWindowStateChangedEvent(isInMultiWindowMode, showDeferredAnimation, stack));
            setupVisible();
        }
        updateDockRegions(false);
        if (!isInMultiWindowMode) {
            setSystemUiVisibility();
        }
        setNotchPadding();
    }

    private void updateExitMultiModeBtnVisible(boolean showExitMultiModeBtn) {
        if (Utilities.supportsMultiWindow()) {
            if (showExitMultiModeBtn) {
                if (this.mExitMultiModeBtn == null) {
                    this.mExitMultiModeBtn = (Button) LayoutInflater.from(getApplicationContext()).inflate(R.layout.exit_multi_window_btn, null);
                    this.mExitMultiModeBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            RecentsEventBus.getDefault().send(new UndockingTaskEvent());
                            Log.i("RecentsActivity", "exit splitScreen mode ---- click exit button.");
                        }
                    });
                    Recents.getSystemServices().mWm.addView(this.mExitMultiModeBtn, getExitMultiModeBtnParams());
                    this.mIsAddExitMultiModeBtn = true;
                }
                this.mExitMultiModeBtn.setVisibility(0);
            } else if (this.mExitMultiModeBtn != null && this.mIsAddExitMultiModeBtn) {
                this.mExitMultiModeBtn.setVisibility(8);
            }
        }
    }

    private WindowManager.LayoutParams getExitMultiModeBtnParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-2, -2, 2027, 40, -3);
        lp.gravity = 49;
        lp.windowAnimations = com.android.systemui.plugins.R.style.Animation_StatusBarBlur;
        lp.setTitle("ExitMultiModeBtn");
        return lp;
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        Log.d("RecentsActivity", "onStop");
        this.mIsVisible = false;
        this.mReceivedNewIntent = false;
        RecentsEventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, false));
        MetricsLogger.hidden(this, 224);
        RecentsPushEventHelper.sendRecentsEvent("hideRecents", "total");
        Recents.getConfiguration().getLaunchState().reset();
        updateExitMultiModeBtnVisible(false);
        updateDockRegions(false);
        resetHomeAlphaScale();
        TaskStackView.setIsChangingConfigurations(isChangingConfigurations());
        Recents.getSystemServices().endProlongedAnimations();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        if (!this.mFinishedOnStartup) {
            unregisterReceiver(this.mSystemBroadcastReceiver);
            this.mPackageMonitor.unregister();
            RecentsEventBus.getDefault().unregister(this);
            if (this.mExitMultiModeBtn != null && this.mIsAddExitMultiModeBtn) {
                Recents.getSystemServices().mWm.removeView(this.mExitMultiModeBtn);
                this.mIsAddExitMultiModeBtn = false;
            }
            RecentsEventBus.getDefault().send(new FsGestureRecentsViewWrapperEvent(null, null, null));
            unRegisterDisplayListener();
            unRegisterContentObservers();
            if (this.mRecentsView != null) {
                this.mRecentsView.release();
            }
        }
    }

    public void onTrimMemory(int level) {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        if (loader != null) {
            loader.onTrimMemory(level);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 61) {
            if (keyCode != 67 && keyCode != 112) {
                switch (keyCode) {
                    case 19:
                        RecentsEventBus.getDefault().send(new FocusNextTaskViewEvent(0));
                        return true;
                    case 20:
                        RecentsEventBus.getDefault().send(new FocusPreviousTaskViewEvent());
                        return true;
                }
            } else if (event.getRepeatCount() <= 0) {
                RecentsEventBus.getDefault().send(new DismissFocusedTaskViewEvent());
                MetricsLogger.histogram(this, "overview_task_dismissed_source", 0);
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
        boolean hasRepKeyTimeElapsed = SystemClock.elapsedRealtime() - this.mLastTabKeyEventTime > ((long) getResources().getInteger(R.integer.recents_alt_tab_key_delay));
        if (event.getRepeatCount() <= 0 || hasRepKeyTimeElapsed) {
            if (event.isShiftPressed()) {
                RecentsEventBus.getDefault().send(new FocusPreviousTaskViewEvent());
            } else {
                RecentsEventBus.getDefault().send(new FocusNextTaskViewEvent(0));
            }
            this.mLastTabKeyEventTime = SystemClock.elapsedRealtime();
            if (event.isAltPressed()) {
                this.mIgnoreAltTabRelease = false;
            }
        }
        return true;
    }

    public void onUserInteraction() {
        RecentsEventBus.getDefault().send(this.mUserInteractionEvent);
    }

    public void onBackPressed() {
        if (RecentsConfiguration.sCanMultiWindow) {
            updateDockRegions(false);
        } else if (this.mRecentsView.getMenuView().isShowing()) {
            this.mRecentsView.getMenuView().removeMenu(true);
        } else {
            RecentsEventBus.getDefault().send(new ToggleRecentsEvent());
            RecentsPushEventHelper.sendRecentsEvent("hideRecents", "clickBackKey");
        }
    }

    public final void onBusEvent(ToggleRecentsEvent event) {
        if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
            dismissRecentsToHome(true);
        } else {
            dismissRecentsToLaunchTargetTaskOrHome();
        }
    }

    public final void onBusEvent(IterateRecentsEvent event) {
        int timerIndicatorDuration = 0;
        if (Recents.getDebugFlags().isFastToggleRecentsEnabled()) {
            timerIndicatorDuration = getResources().getInteger(R.integer.recents_subsequent_auto_advance_duration);
            this.mIterateTrigger.setDozeDuration(timerIndicatorDuration);
            if (!this.mIterateTrigger.isDozing()) {
                this.mIterateTrigger.startDozing();
            } else {
                this.mIterateTrigger.poke();
            }
        }
        RecentsEventBus.getDefault().send(new FocusNextTaskViewEvent(timerIndicatorDuration));
        MetricsLogger.action(this, 276);
    }

    public final void onBusEvent(UserInteractionEvent event) {
        this.mIterateTrigger.stopDozing();
    }

    public final void onBusEvent(HideRecentsEvent event) {
        if (RecentsConfiguration.sCanMultiWindow) {
            updateDockRegions(false);
            if (!event.triggeredFromScroll) {
                return;
            }
        }
        if (event.triggeredFromAltTab) {
            if (!this.mIgnoreAltTabRelease) {
                dismissRecentsToTargetTaskOrHome();
            }
        } else if (event.triggeredFromHomeKey || event.triggeredFromScroll) {
            dismissRecentsToHome(true);
            RecentsEventBus.getDefault().send(this.mUserInteractionEvent);
        } else if (event.triggeredFromFsGesture) {
            this.mRecentsView.launchPreviousTask();
        }
    }

    public final void onBusEvent(EnterRecentsWindowLastAnimationFrameEvent event) {
        RecentsEventBus.getDefault().send(new UpdateFreeformTaskViewVisibilityEvent(true));
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(ExitRecentsWindowFirstAnimationFrameEvent event) {
        if (this.mRecentsView.isLastTaskLaunchedFreeform()) {
            RecentsEventBus.getDefault().send(new UpdateFreeformTaskViewVisibilityEvent(false));
        }
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
        this.mRecentsContainer.animate().alpha(0.0f).setStartDelay(0).setDuration(80).setInterpolator(new SineEaseOutInterpolator()).start();
        this.mRecentsView.animate().alpha(0.0f).scaleX(0.97f).scaleY(0.97f).setStartDelay(0).setDuration(180).setInterpolator(new SineEaseOutInterpolator()).start();
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent event) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(CancelEnterRecentsWindowAnimationEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        int launchToTaskId = launchState.launchedToTaskId;
        if (launchToTaskId == -1) {
            return;
        }
        if (event.launchTask == null || launchToTaskId != event.launchTask.key.id) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            ssp.cancelWindowTransition(launchState.launchedToTaskId);
            ssp.cancelThumbnailTransition(getTaskId());
        }
    }

    public final void onBusEvent(ShowApplicationInfoEvent event) {
        Intent intent = new Intent();
        String packageName = event.task.key.getComponent().getPackageName();
        if (Build.IS_TABLET) {
            if (!showHybridApplicationInfo(intent, packageName, event.task.key.baseIntent)) {
                intent.setAction(null);
                intent.setClassName("com.android.settings", "com.android.settings.applications.InstalledAppDetailsTop");
            }
            intent.putExtra("package", packageName);
            intent.setFlags(335544320);
            if (XSpaceUserHandle.isXSpaceUserId(event.task.key.userId)) {
                intent.putExtra("is_xspace_app", true);
            } else {
                intent.putExtra("is_xspace_app", false);
            }
        } else {
            if (!showHybridApplicationInfo(intent, packageName, event.task.key.baseIntent)) {
                intent.setAction("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL");
            }
            intent.putExtra("package_name", packageName);
            intent.putExtra("miui.intent.extra.USER_ID", event.task.key.userId);
            intent.setFlags(276824064);
        }
        try {
            TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities(null, UserHandle.CURRENT);
        } catch (Exception e) {
            Log.e("RecentsActivity", "ShowApplicationInfo", e);
        }
        MetricsLogger.count(this, "overview_app_info", 1);
        RecentsPushEventHelper.sendRecentsEvent("showAppInfo", event.task.key.getComponent().getPackageName());
    }

    private boolean showHybridApplicationInfo(Intent intent, String packageName, Intent baseIntent) {
        if (!packageName.equals("com.miui.hybrid") || intent.setAction("com.miui.hybrid.action.APP_DETAIL_MANAGER").resolveActivity(getPackageManager()) == null) {
            return false;
        }
        intent.putExtra("base_intent", baseIntent);
        return true;
    }

    public final void onBusEvent(ShowIncompatibleAppOverlayEvent event) {
        if (this.mIncompatibleAppOverlay == null) {
            this.mIncompatibleAppOverlay = Utilities.findViewStubById((Activity) this, (int) R.id.incompatible_app_overlay_stub).inflate();
            this.mIncompatibleAppOverlay.setWillNotDraw(false);
            this.mIncompatibleAppOverlay.setVisibility(0);
        }
        this.mIncompatibleAppOverlay.animate().alpha(1.0f).setDuration(150).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public final void onBusEvent(HideIncompatibleAppOverlayEvent event) {
        if (this.mIncompatibleAppOverlay != null) {
            this.mIncompatibleAppOverlay.animate().alpha(0.0f).setDuration(150).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DeleteTaskDataEvent event) {
        if (!ApplicationHelper.shouldKeeAlive(this, event.task.key.getComponent().getPackageName(), event.task.key.userId)) {
            Recents.getTaskLoader().deleteTaskData(event.task, false);
            Slog.d("RecentsActivity", "removeTask: " + event.task.toString());
            SystemServicesProxy ssp = Recents.getSystemServices();
            if (!event.remainProcess) {
                ssp.killProcess(event.task);
            }
        }
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        if (!Recents.getSystemServices().hasDockedTask() || !event.mEmpty) {
            if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
                dismissRecentsToHome(false);
            } else {
                dismissRecentsToTargetTaskOrHome();
            }
        } else if (!event.mFromDockGesture) {
            this.mRecentsView.showEmptyView(event.msgResId);
            this.mTipView.setVisibility(4);
        }
        MetricsLogger.count(this, "overview_task_all_dismissed", 1);
    }

    public final void onBusEvent(LaunchTaskSucceededEvent event) {
        MetricsLogger.histogram(this, "overview_task_launch_index", event.taskIndexFromStackFront);
        RecentsPushEventHelper.sendRecentsEvent("hideRecents", "switchApp");
        if (Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            this.mNeedMoveRecentsToFrontOfFsGesture = false;
            RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
        }
    }

    public final void onBusEvent(LaunchTaskFailedEvent event) {
        dismissRecentsToHome(true);
        MetricsLogger.count(this, "overview_task_launch_failed", 1);
    }

    public final void onBusEvent(ScreenPinningRequestEvent event) {
        MetricsLogger.count(this, "overview_screen_pinned", 1);
    }

    public final void onBusEvent(DebugFlagsChangedEvent event) {
        finish();
    }

    public final void onBusEvent(StackViewScrolledEvent event) {
        this.mIgnoreAltTabRelease = true;
    }

    public final void onBusEvent(DockedTopTaskEvent event) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(ExitMultiModeEvent event) {
        if (this.mExitMultiModeBtn != null && this.mIsAddExitMultiModeBtn) {
            this.mExitMultiModeBtn.setVisibility(8);
        }
    }

    public final void onBusEvent(ChangeTaskLockStateEvent event) {
        Task task = event.task;
        String packageName = task.key.getComponent().getPackageName();
        if (ApplicationHelper.shouldKeeAlive(getApplicationContext(), packageName, task.key.userId)) {
            Slog.d("Enterprise", "Package " + packageName + " is protected");
            return;
        }
        task.isLocked = event.isLocked;
        try {
            ProcessManager.updateApplicationLockedState(packageName, task.key.userId, task.isLocked);
        } catch (Exception e) {
            Slog.e("RecentsActivity", "ChangeTaskLockState", e);
        }
        if (task.isLocked) {
            updateAppConfigure(getApplicationContext(), packageName, "noRestrict");
        } else if ("noRestrict".equals(queryAppConfigure(getApplicationContext(), packageName))) {
            updateAppConfigure(getApplicationContext(), packageName, "miuiAuto");
        }
        RecentsPushEventHelper.sendRecentsEvent(task.isLocked ? "lockTask" : "unLockTask", task.key.getComponent().getPackageName());
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        if (Recents.getConfiguration().getLaunchState().launchedViaFsGesture) {
            return;
        }
        if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
            this.mRecentsContainer.animate().alpha(1.0f).setStartDelay(50).setDuration(200).setInterpolator(new SineEaseInOutInterpolator()).start();
        } else {
            this.mRecentsContainer.animate().alpha(1.0f).setStartDelay(0).setDuration(150).setInterpolator(new SineEaseInOutInterpolator()).start();
        }
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        this.mRecentsContainer.animate().alpha(0.0f).setStartDelay(0).setDuration(50).setInterpolator(new SineEaseOutInterpolator()).start();
    }

    public final void onBusEvent(CleanInRecentsEvents event) {
        cleanInRecents();
    }

    public final void onBusEvent(FsGestureEnterRecentsZoomEvent event) {
        long duration = Math.max(0, 283 - event.mTimeOffset);
        this.mRecentsView.animate().setListener(null).scaleX(1.0f).scaleY(1.0f).setDuration(duration).setStartDelay(0).start();
        this.mRecentsView.startFrontTaskViewHeadFadeInAnim(duration);
        this.mRecentsContainer.animate().alpha(1.0f).setDuration(200).setInterpolator(new SineEaseInOutInterpolator()).start();
    }

    public final void onBusEvent(FsGestureEnterRecentsEvent event) {
        this.mNeedMoveRecentsToFrontOfFsGesture = true;
    }

    public final void onBusEvent(FsGestureEnterRecentsCompleteEvent event) {
        if (event.mMoveRecentsToFront && this.mNeedMoveRecentsToFrontOfFsGesture) {
            ((ActivityManager) getSystemService("activity")).moveTaskToFront(getTaskId(), 0);
        }
        this.mRecentsContainer.setAlpha(1.0f);
        RecentsEventBus.getDefault().send(new ActivitySetDummyTranslucentEvent(false));
        this.mRecentsView.setAlpha(1.0f);
        this.mRecentsView.setScaleX(1.0f);
        this.mRecentsView.setScaleY(1.0f);
        this.mRecentsView.startFrontTaskViewHeadFadeInAnim(0);
    }

    public final void onBusEvent(FsGestureSlideInEvent event) {
        if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
            this.mRecentsView.animate().alpha(1.0f).scaleX(0.95f).scaleY(0.95f).setInterpolator(Interpolators.CUBIC_EASE_OUT).setDuration(200).setStartDelay(100).start();
        } else {
            this.mRecentsView.animate().alpha(1.0f).scaleX(1.05f).scaleY(1.05f).setInterpolator(Interpolators.CUBIC_EASE_OUT).setDuration(200).setStartDelay(0).start();
        }
        this.mBackGround.animate().alpha(1.0f).setDuration(200).start();
    }

    public final void onBusEvent(FsGestureSlideOutEvent event) {
        if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
            this.mRecentsView.animate().alpha(0.0f).scaleX(0.9f).scaleY(0.9f).setInterpolator(Interpolators.CUBIC_EASE_OUT).setDuration(150).setStartDelay(0).start();
            this.mBackGround.animate().alpha(0.0f).setDuration(200).start();
            return;
        }
        this.mRecentsView.animate().alpha(0.0f).scaleX(1.1f).scaleY(1.1f).setInterpolator(Interpolators.CUBIC_EASE_OUT).setDuration(150).setStartDelay(0).start();
    }

    public final void onBusEvent(ActivitySetDummyTranslucentEvent event) {
        if (this.mSetDummyTranslucentMethod != null) {
            try {
                this.mSetDummyTranslucentMethod.invoke(this, new Object[]{Boolean.valueOf(event.mIsTranslucent)});
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e2) {
                e2.printStackTrace();
            }
        }
    }

    public final void onBusEvent(HideMemoryAndDockEvent event) {
        setupVisible();
    }

    public final void onBusEvent(ShowMemoryAndDockEvent event) {
        setupVisible();
    }

    public final void onBusEvent(StackScrollChangedEvent event) {
        int translationY;
        if (this.mRecentsRecommendView == null || this.mRecentsRecommendView.getVisibility() != 0) {
            translationY = Math.min(event.mTranslationY + Math.max(this.mRecentsView.getTaskViewPaddingView() - this.mDockBtn.getBottom(), 0), 0);
        } else {
            translationY = Math.min(event.mTranslationY, 0);
            this.mRecentsRecommendView.setTranslationY((float) translationY);
        }
        this.mDockBtn.setTranslationY((float) translationY);
        this.mTxtMemoryContainer.setTranslationY((float) translationY);
        float f = 0.0f;
        if (this.mMemoryAndClearContainer.getAlpha() != 0.0f) {
            if (!RecentsConfiguration.sCanMultiWindow) {
                f = Math.max((((float) translationY) / 100.0f) + 1.0f, 0.0f);
            }
            float alpha = f;
            this.mDockBtn.setAlpha(alpha);
            this.mTxtMemoryContainer.setAlpha(alpha);
        }
        this.mTipView.setTranslationY((float) Math.min(event.mTranslationY, 0));
    }

    public final void onBusEvent(ScrollerFlingFinishEvent event) {
        if (this.mDockBtn.getAlpha() < 1.0f || this.mTxtMemoryContainer.getAlpha() < 1.0f) {
            this.mDockBtn.setAlpha(0.0f);
            this.mTxtMemoryContainer.setAlpha(0.0f);
        }
    }

    public final void onBusEvent(NavStubViewAttachToWindowEvent event) {
        RecentsEventBus.getDefault().send(new FsGestureRecentsViewWrapperEvent(this.mRecentsView, this.mBackGround, this.mRecentsContainer));
    }

    public final void checkFsGestureOnEnterRecents() {
        boolean isHideRecentsViewByFsGesture = false;
        Recents recents = (Recents) ((Application) getApplication()).getSystemUIApplication().getComponent(Recents.class);
        if (!(recents == null || recents.getRecentsImpl() == null)) {
            isHideRecentsViewByFsGesture = recents.getRecentsImpl().getIsHideRecentsViewByFsGesture();
        }
        if (!Recents.getConfiguration().getLaunchState().launchedViaFsGesture || !isHideRecentsViewByFsGesture) {
            this.mRecentsView.setScaleX(1.0f);
            this.mRecentsView.setScaleY(1.0f);
            this.mRecentsView.setAlpha(1.0f);
            this.mRecentsView.setTranslationX(0.0f);
            this.mBackGround.setAlpha(1.0f);
            return;
        }
        this.mBackGround.setAlpha(0.0f);
        this.mRecentsView.setAlpha(0.0f);
        if (Recents.getConfiguration().getLaunchState().launchedFromHome) {
            this.mRecentsView.setScaleX(0.9f);
            this.mRecentsView.setScaleY(0.9f);
            RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
            return;
        }
        this.mRecentsView.setScaleX(1.1f);
        this.mRecentsView.setScaleY(1.1f);
        RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(0.0f, false, true));
    }

    public static String queryAppConfigure(Context ctx, String packageName) {
        String bgControl = "";
        Cursor cursor = ctx.getContentResolver().query(Uri.withAppendedPath(Uri.parse("content://com.miui.powerkeeper.configure"), "userTable"), null, "pkgName = ? AND userId = ?", new String[]{packageName, Integer.toString(CrossUserUtils.getCurrentUserId())}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                bgControl = cursor.getString(cursor.getColumnIndex("bgControl"));
            }
            cursor.close();
        }
        return bgControl;
    }

    public static void updateAppConfigure(final Context ctx, final String pkgName, final String bgControl) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                int userId = CrossUserUtils.getCurrentUserId();
                Bundle bundle = new Bundle();
                bundle.putInt("userId", userId);
                bundle.putString("pkgName", pkgName);
                bundle.putString("bgControl", bgControl);
                try {
                    ctx.getContentResolver().call(Uri.withAppendedPath(Uri.parse("content://com.miui.powerkeeper.configure"), "userTable"), "userTableupdate", null, bundle);
                } catch (IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public boolean onPreDraw() {
        this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mRecentsView.post(new Runnable() {
            public void run() {
                Recents.getSystemServices().endProlongedAnimations();
            }
        });
        return true;
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        RecentsEventBus.getDefault().dump(prefix, writer);
        Recents.getTaskLoader().dump(prefix, writer);
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("RecentsActivity");
        writer.print(" visible=");
        writer.print(this.mIsVisible ? "Y" : "N");
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mRecentsView != null) {
            this.mRecentsView.dump(prefix, writer);
        }
        if (args != null && args.length > 0) {
            if ("enableDebugRecents".equals(args[0])) {
                SystemServicesProxy.DEBUG = true;
                RecentsEventBus.DEBUG_TRACE_ALL = true;
            } else if ("disableDebugRecents".equals(args[0])) {
                SystemServicesProxy.DEBUG = false;
                RecentsEventBus.DEBUG_TRACE_ALL = false;
            }
        }
    }

    public long getFreeMemory() {
        long freeMemory;
        try {
            freeMemory = PerfShielderManager.getFreeMemory().longValue();
        } catch (Exception e) {
            Log.e("RecentsActivity", "getFreeMemory", e);
            freeMemory = Process.getFreeMemory();
        }
        Log.d("RecentsActivity", "getFreeMemory:" + freeMemory);
        return freeMemory / 1024;
    }

    public void refreshMemoryInfo() {
        String freeMemory = getFormatedMemory(Math.max(mFreeBeforeClean, getFreeMemory()), false);
        String totalMemory = getFormatedMemory(this.mTotalMemory, true);
        this.mTxtMemoryInfo1.setText(getString(R.string.status_bar_recent_memory_info1, new Object[]{freeMemory, totalMemory}));
        this.mTxtMemoryInfo2.setText(getString(R.string.status_bar_recent_memory_info2, new Object[]{freeMemory, totalMemory}));
        this.mSeparatorForMemoryInfo.setVisibility(TextUtils.isEmpty(this.mTxtMemoryInfo1.getText()) || TextUtils.isEmpty(this.mTxtMemoryInfo2.getText()) ? 8 : 0);
        this.mClearAnimView.setContentDescription(getString(R.string.accessibility_recent_task_memory_info, new Object[]{freeMemory, totalMemory}));
    }

    /* access modifiers changed from: private */
    public void endForClear() {
        RecentsEventBus.getDefault().post(new DismissAllTaskViewsEndEvent());
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                long freeAtLast = RecentsActivity.this.getFreeMemory();
                RecentsPushEventHelper.sendOneKeyCleanEvent(RecentsActivity.this.mFreeAtFirst, freeAtLast, RecentsActivity.this.mTotalMemory);
                Toast toast = Toast.makeText(RecentsActivity.this.getApplicationContext(), RecentsActivity.getToastMsg(RecentsActivity.this.getApplicationContext(), RecentsActivity.this.mFreeAtFirst, freeAtLast), 0);
                toast.getWindowParams().privateFlags |= 16;
                toast.show();
            }
        }, 300);
    }

    private void updateDockBtnVisible() {
        this.mDockBtn.setVisibility((RecentsConfiguration.sCanMultiWindow || this.mRecentsView.getStack().getStackTaskCount() <= 0 || isInMultiWindowMode() || !Utilities.supportsMultiWindow() || this.mRecentsView.getMenuView().isShowing()) ? 4 : 0);
    }

    private boolean isMemInfoShow() {
        return MiuiSettings.System.getBooleanForUser(getApplicationContext().getContentResolver(), "miui_recents_show_mem_info", false, -2);
    }

    /* access modifiers changed from: private */
    public void doClearAnim() {
        refreshMemoryInfo();
        this.mClearAnimView.animatorStart(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (RecentsActivity.this.mDismissAllTaskViewEventTrigger != null) {
                    RecentsActivity.this.mDismissAllTaskViewEventTrigger.decrement();
                    final ReferenceCountedTrigger ensureFlushTrigger = RecentsActivity.this.mDismissAllTaskViewEventTrigger;
                    RecentsActivity.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            ensureFlushTrigger.flushLastDecrementRunnables();
                        }
                    }, 300);
                    ReferenceCountedTrigger unused = RecentsActivity.this.mDismissAllTaskViewEventTrigger = null;
                }
                RecentsActivity.this.endForClear();
            }
        });
    }

    private void setupVisible() {
        boolean z = true;
        int i = 0;
        updateExitMultiModeBtnVisible(this.mIsVisible && isInMultiWindowMode());
        updateDockBtnVisible();
        updateRecentsRecommendViewVisible();
        if (isInMultiWindowMode() || this.mRecentsView.getMenuView().isShowing()) {
            z = false;
        }
        boolean isShow = z;
        int taskCount = this.mRecentsView.getStack().getStackTaskCount();
        this.mMemoryAndClearContainer.setVisibility((!isShow || taskCount <= 0) ? 4 : 0);
        this.mTxtMemoryContainer.setVisibility((!isShow || !isMemInfoShow()) ? 4 : 0);
        TextView textView = this.mTipView;
        if (!isInMultiWindowMode() || taskCount <= 0 || this.mRecentsView.getMenuView().isShowing()) {
            i = 4;
        }
        textView.setVisibility(i);
    }

    /* access modifiers changed from: private */
    public void updateRecentsRecommendViewVisible() {
        if (this.mRecentsRecommendView != null) {
            int i = 0;
            boolean z = true;
            if (isInMultiWindowMode() || this.mRecentsView.getMenuView().isShowing() || !this.mIsShowRecommend || this.mLastDeviceOrientation != 1) {
                z = false;
            }
            this.mIsRecommendVisible = z;
            RecentsRecommendView recentsRecommendView = this.mRecentsRecommendView;
            if (!this.mIsRecommendVisible) {
                i = 4;
            }
            recentsRecommendView.setVisibility(i);
        }
    }

    private void deepClean() {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                ArrayList<Task> tasks = RecentsActivity.this.mRecentsView.getStack().getStackTasks();
                ArrayList<Integer> removingTaskIds = new ArrayList<>();
                Iterator<Task> it = tasks.iterator();
                while (it.hasNext()) {
                    removingTaskIds.add(Integer.valueOf(it.next().key.id));
                }
                ProcessConfig processConfig = new ProcessConfig(1);
                processConfig.setRemoveTaskNeeded(true);
                processConfig.setRemovingTaskIdList(removingTaskIds);
                ProcessManager.kill(processConfig);
                if (RecentsActivity.this.mHandler.hasMessages(1000)) {
                    RecentsActivity.this.mHandler.removeMessages(1000);
                    RecentsActivity.this.mHandler.sendMessage(RecentsActivity.this.mHandler.obtainMessage(1000));
                }
            }
        });
    }

    public void resetHomeAlphaScale() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.isFsGestureAnimating()) {
            ssp.changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, 1.0f);
        }
    }

    public static String getToastMsg(Context context, long freeAtFirst, long freeAtLast) {
        long offset = Math.max(freeAtLast - freeAtFirst, 0);
        if (offset <= 10240) {
            return context.getResources().getString(285802628);
        }
        long memoryM = offset / 1024;
        if (memoryM < 1024) {
            return context.getResources().getString(R.string.memory_clear_result_mega, new Object[]{String.format(Locale.getDefault(), "%d", new Object[]{Long.valueOf(memoryM)})});
        }
        return context.getResources().getString(R.string.memory_clear_result_giga, new Object[]{String.format(Locale.getDefault(), "%.1f", new Object[]{Float.valueOf(((float) memoryM) / 1024.0f)})});
    }

    public String getFormatedMemory(long memoryK, boolean isCeil) {
        long memoryGTenTimes;
        long memoryM = memoryK / 1024;
        if (memoryM < 1024) {
            return memoryM + " M";
        }
        if (isCeil) {
            memoryGTenTimes = (long) (10.0d * Math.ceil(((double) memoryM) / 1024.0d));
        } else {
            memoryGTenTimes = Math.round((((double) memoryM) * 10.0d) / 1024.0d);
        }
        long memoryG = memoryGTenTimes / 10;
        if (memoryGTenTimes % 10 != 0) {
            return memoryG + "." + (memoryGTenTimes % 10) + " G";
        }
        return memoryG + " G";
    }

    private void registerDisplayListener() {
        if (Constants.IS_NOTCH) {
            ((DisplayManager) getSystemService("display")).registerDisplayListener(this.mDisplayListener, null);
            this.mDisplayListener.onDisplayChanged(0);
        }
    }

    private void unRegisterDisplayListener() {
        if (Constants.IS_NOTCH) {
            ((DisplayManager) getSystemService("display")).unregisterDisplayListener(this.mDisplayListener);
        }
    }

    public void setNotchPadding() {
        if (this.mRecentsContainer != null && this.mRecentsView != null) {
            int statusBarHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            int left = this.mRotation == 1 ? statusBarHeight : 0;
            int right = this.mRotation == 3 ? statusBarHeight : 0;
            int top = this.mRotation == 0 ? statusBarHeight : 0;
            if (isInMultiWindowMode()) {
                top = 0;
                left = 0;
            }
            if (Utilities.isAndroidPorNewer()) {
                right = 0;
                left = 0;
            }
            this.mRecentsContainer.setPadding(left, top, right, 0);
            this.mRecentsView.requstLayoutTaskStackView();
        }
    }

    private void setSystemUiVisibility() {
        this.mRecentsView.setSystemUiVisibility(772);
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        return false;
    }

    private void addRecentsRecommendViewIfNeeded() {
        if (Utilities.isShowRecentsRecommend()) {
            this.mRecentsRecommendView = (RecentsRecommendView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.recents_recommend_view, null);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
            Point point = new Point();
            ((WindowManager) getSystemService("window")).getDefaultDisplay().getRealSize(point);
            int displayWidth = Math.min(point.x, point.y);
            int marginSide = (displayWidth - (2 * ((int) ((((float) displayWidth) * getResources().getFloat(R.dimen.recents_task_rect_scale)) - ((float) getResources().getDimensionPixelSize(R.dimen.recents_task_view_padding)))))) / 3;
            lp.setMargins(marginSide, (int) Utilities.dpToPx(getResources(), 60.0f), marginSide, 0);
            this.mRecentsContainer.addView(this.mRecentsRecommendView, lp);
        }
    }
}
