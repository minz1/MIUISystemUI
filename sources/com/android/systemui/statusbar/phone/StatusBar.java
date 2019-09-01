package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.app.ActivityOptionsCompat;
import android.app.INotificationManager;
import android.app.KeyguardManager;
import android.app.KeyguardManagerCompat;
import android.app.MiuiStatusBarManager;
import android.app.Notification;
import android.app.NotificationChannelCompat;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.StatusBarManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.service.notification.StatusBarNotificationCompat;
import android.service.vr.IVrManagerCompat;
import android.telephony.PhoneStateListener;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbstractOnClickHandler;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsLoggerCompat;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibilityCompat;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarServiceCompat;
import com.android.internal.telephony.Call;
import com.android.internal.util.NotificationMessagingUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.keyguard.AODView;
import com.android.keyguard.KeyguardClockContainer;
import com.android.keyguard.KeyguardCompatibilityHelperForN;
import com.android.keyguard.KeyguardCompatibilityHelperForO;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.keyguard.faceunlock.FaceUnlockController;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.keyguard.widget.AODKeys;
import com.android.systemui.ActivityStarterDelegate;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Constants;
import com.android.systemui.CustomizedUtils;
import com.android.systemui.DejankUtils;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.DisplayCutoutCompat;
import com.android.systemui.EventLogTags;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.Interpolators;
import com.android.systemui.Logger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SwipeHelper;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUICompat;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.Util;
import com.android.systemui.analytics.JobHelper;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.content.pm.PackageManagerCompat;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.events.ScreenOffEvent;
import com.android.systemui.events.ScreenOnEvent;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.PluginFragmentListener;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.miui.ToastOverlayManager;
import com.android.systemui.miui.policy.NotificationsMonitor;
import com.android.systemui.miui.statusbar.CloudDataHelper;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.InCallUtils;
import com.android.systemui.miui.statusbar.LocalAlgoModel;
import com.android.systemui.miui.statusbar.analytics.NotificationEvent;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.notification.FoldFooterView;
import com.android.systemui.miui.statusbar.notification.FoldHeaderView;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.miui.statusbar.notification.PushEvents;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.miui.statusbar.phone.applock.AppLockHelper;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import com.android.systemui.miui.statusbar.phone.rank.RankUtil;
import com.android.systemui.miui.statusbar.policy.UsbNotificationController;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.AppTransitionFinishedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.HeaderView;
import com.android.systemui.statusbar.KeyboardShortcuts;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.KeyguardNotificationHelper;
import com.android.systemui.statusbar.LockScreenMagazineController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationFilter;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.NotificationInfo;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationSnooze;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.notification.InCallNotificationView;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.notification.NotificationInflater;
import com.android.systemui.statusbar.notification.NotificationViewWrapperCompat;
import com.android.systemui.statusbar.notification.RowInflaterTask;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DemoModeController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.policy.RemoteInputView;
import com.android.systemui.statusbar.policy.SilentModeObserverController;
import com.android.systemui.statusbar.policy.TelephonyIcons;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.util.Utils;
import com.android.systemui.util.leak.LeakDetector;
import com.android.systemui.volume.VolumeComponent;
import com.miui.systemui.annotation.Inject;
import com.miui.systemui.support.v4.app.Fragment;
import com.miui.voiptalk.service.MiuiVoipManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import miui.app.ToggleManager;
import miui.content.res.IconCustomizer;
import miui.os.Build;
import miui.security.SecurityManager;
import miui.telephony.TelephonyManager;
import miui.telephony.TelephonyManagerEx;
import miui.util.CustomizeUtil;
import miui.util.NotificationFilterHelper;
import miui.view.animation.QuarticEaseOutInterpolator;
import miui.view.animation.SineEaseOutInterpolator;

public class StatusBar extends SystemUI implements DemoMode, ActivityStarter, ActivatableNotificationView.OnActivatedListener, CommandQueue.Callbacks, DragDownHelper.DragDownCallback, ExpandableNotificationRow.ExpansionLogger, ExpandableNotificationRow.OnExpandClickListener, NotificationData.Environment, InCallNotificationView.InCallCallback, NotificationInflater.InflationCallback, VisualStabilityManager.Callback, UnlockMethodCache.OnUnlockMethodChangedListener, OnHeadsUpChangedListener, SilentModeObserverController.SilentModeListener {
    public static final Interpolator ALPHA_IN = Interpolators.ALPHA_IN;
    public static final Interpolator ALPHA_OUT = Interpolators.ALPHA_OUT;
    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES");
    public static final boolean CHATTY = DEBUG;
    public static final boolean DEBUG = Constants.DEBUG;
    public static final boolean DEBUG_GESTURES = DEBUG;
    public static final boolean DEBUG_MEDIA = DEBUG;
    public static final boolean DEBUG_MEDIA_FAKE_ARTWORK = DEBUG;
    public static final boolean DEBUG_WINDOW_STATE = DEBUG;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS;
    private static boolean ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT = false;
    public static final boolean ENABLE_REMOTE_INPUT = SystemProperties.getBoolean("debug.enable_remote_input", true);
    /* access modifiers changed from: private */
    public static String EXTRA_APP_UID = "app_uid";
    /* access modifiers changed from: private */
    public static String EXTRA_HIGH_PRIORITY_SETTING = "high_priority_setting";
    public static final boolean FORCE_REMOTE_INPUT_HISTORY = SystemProperties.getBoolean("debug.force_remoteinput_history", false);
    private static final boolean FREEFORM_WINDOW_MANAGEMENT;
    private static final boolean ONLY_CORE_APPS;
    public static final boolean SPEW = DEBUG;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    public static boolean sBootCompleted = false;
    public static boolean sGameMode = false;
    public static boolean sIsStatusBarHidden = false;
    private final String TYPE_FROM_STATUS_BAR_EXPANSION = "typefrom_status_bar_expansion";
    int[] mAbsPos = new int[2];
    protected AccessibilityManager mAccessibilityManager;
    private final BroadcastReceiver mAllUsersReceiver;
    protected boolean mAllowLockscreenRemoteInput;
    private final Runnable mAnimateCollapsePanels;
    @Inject
    protected AssistManager mAssistManager;
    private final Runnable mAutohide;
    private boolean mAutohideSuspended;
    protected BackDropView mBackdrop;
    protected ImageView mBackdropBack;
    protected ImageView mBackdropFront;
    protected IStatusBarService mBarService;
    private final BroadcastReceiver mBaseBroadcastReceiver;
    /* access modifiers changed from: private */
    @Inject
    public BatteryController mBatteryController;
    /* access modifiers changed from: private */
    public int mBatteryLevel;
    /* access modifiers changed from: private */
    public Handler mBgHandler;
    private HandlerThread mBgThread;
    protected boolean mBouncerShowing;
    BrightnessMirrorController mBrightnessMirrorController;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public long mCallBaseTime;
    /* access modifiers changed from: private */
    public String mCallState;
    private long[] mCameraLaunchGestureVibePattern;
    /* access modifiers changed from: private */
    public final Runnable mCheckBarModes;
    private final ContentObserver mCloudDataObserver;
    protected CommandQueue mCommandQueue;
    private ConfigurationController.ConfigurationListener mConfigurationListener;
    protected Context mContextForUser;
    Point mCurrentDisplaySize = new Point();
    protected final SparseArray<UserInfo> mCurrentProfiles;
    protected int mCurrentUserId;
    boolean mDarkMode;
    private final DemoModeController.DemoModeCallback mDemoCallback;
    boolean mDemoMode;
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    @Inject
    public DeviceProvisionedController mDeviceProvisionedController;
    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedListener;
    private boolean mDisableFloatNotification;
    protected boolean mDisableNotificationAlerts;
    int mDisabled1 = 0;
    int mDisabled2 = 0;
    protected DismissView mDismissView;
    protected Display mDisplay;
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private Divider.DockedStackExistsChangedListener mDockedStackExistsChangedListener;
    protected DozeScrimController mDozeScrimController;
    /* access modifiers changed from: private */
    public DozeServiceHost mDozeServiceHost;
    /* access modifiers changed from: private */
    public boolean mDozing;
    /* access modifiers changed from: private */
    public boolean mDozingRequested;
    private ExpandableNotificationRow mDraggedDownRow;
    /* access modifiers changed from: private */
    public LinearLayout mDriveModeBg;
    protected EmptyShadeView mEmptyShadeView;
    private List<String> mEnableFloatNotificationWhitelist;
    private final BroadcastReceiver mEnableNotificationsReceiver;
    View mExpandedContents;
    boolean mExpandedVisible;
    protected FaceUnlockController mFaceUnlockController;
    /* access modifiers changed from: private */
    public boolean mFadeKeyguardWhenUnlockByGxzw;
    private BroadcastReceiver mFakeArtworkReceiver;
    /* access modifiers changed from: private */
    public FalsingManager mFalsingManager;
    protected FingerprintUnlockController mFingerprintUnlockController;
    private FoldFooterView mFoldFooterView;
    private FoldHeaderView mFoldHeaderView;
    /* access modifiers changed from: private */
    public boolean mForceBlack;
    private ContentObserver mForceBlackObserver;
    private ForegroundServiceController mForegroundServiceController;
    private ContentObserver mFullScreenGestureListener;
    /* access modifiers changed from: private */
    public boolean mGameHandsFreeMode;
    private ContentObserver mGameHandsFreeObserver;
    private ContentObserver mGameModeObserver;
    private final GestureRecorder mGestureRec;
    private PowerManager.WakeLock mGestureWakeLock;
    private final View.OnClickListener mGoToLockedShadeListener;
    protected NotificationGroupManager mGroupManager;
    /* access modifiers changed from: private */
    public NotificationMenuRowPlugin.MenuItem mGutsMenuItem;
    protected H mHandler;
    private boolean mHasAnswerCall;
    /* access modifiers changed from: private */
    public boolean mHasClearAllNotifications;
    protected HeaderView mHeaderView;
    protected ArraySet<NotificationData.Entry> mHeadsUpEntriesToRemoveOnSwitch;
    protected HeadsUpManager mHeadsUpManager;
    protected boolean mHeadsUpTicker;
    protected Runnable mHideBackdropFront;
    @Inject
    protected StatusBarIconController mIconController;
    PhoneStatusBarPolicy mIconPolicy;
    private boolean mInPinnedMode;
    /* access modifiers changed from: private */
    public final DisplayInfo mInfo;
    private int mInteractingWindows;
    private BroadcastReceiver mInternalBroadcastReceiver;
    private boolean mIsDNDEnabled;
    /* access modifiers changed from: private */
    public boolean mIsFsgMode;
    /* access modifiers changed from: private */
    public boolean mIsInDriveMode;
    /* access modifiers changed from: private */
    public boolean mIsInDriveModeMask;
    /* access modifiers changed from: private */
    public boolean mIsKeyguard;
    private boolean mIsRemoved;
    private JobHelper mJobHelper;
    KeyguardBottomAreaView mKeyguardBottomArea;
    KeyguardClockContainer mKeyguardClock;
    protected boolean mKeyguardFadingAway;
    protected long mKeyguardFadingAwayDelay;
    protected long mKeyguardFadingAwayDuration;
    private boolean mKeyguardGoingAway;
    KeyguardIndicationController mKeyguardIndicationController;
    protected KeyguardManager mKeyguardManager;
    private KeyguardMonitorImpl mKeyguardMonitor;
    /* access modifiers changed from: private */
    public KeyguardNotificationHelper mKeyguardNotificationHelper;
    private boolean mKeyguardRequested;
    protected KeyguardStatusBarView mKeyguardStatusBar;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    protected ArraySet<String> mKeysKeptForRemoteInput;
    /* access modifiers changed from: private */
    public int mLastCameraLaunchSource;
    private int mLastDispatchedSystemUiVisibility = -1;
    private final Rect mLastDockedStackBounds = new Rect();
    private final Rect mLastFullscreenStackBounds = new Rect();
    private int mLastLoggedStateFingerprint;
    private NotificationListenerService.RankingMap mLatestRankingMap;
    private boolean mLaunchCameraOnFinishedGoingToSleep;
    private boolean mLaunchCameraOnScreenTurningOn;
    private Runnable mLaunchTransitionEndRunnable;
    protected boolean mLaunchTransitionFadingAway;
    protected int mLayoutDirection;
    boolean mLeaveOpenOnKeyguardHide;
    LightBarController mLightBarController;
    private Locale mLocale;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    LockScreenMagazineController mLockScreenMagazineController;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    private final SparseBooleanArray mLockscreenPublicMode;
    private final ContentObserver mLockscreenSettingsObserver;
    protected LockscreenWallpaper mLockscreenWallpaper;
    /* access modifiers changed from: private */
    public int mLogicalHeight;
    /* access modifiers changed from: private */
    public int mLogicalWidth;
    int mMaxAllowedKeyguardNotifications;
    private int mMaxKeyguardNotifications;
    private MediaController mMediaController;
    private MediaController.Callback mMediaListener;
    /* access modifiers changed from: private */
    public MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private MediaSessionManager mMediaSessionManager;
    private NotificationMessagingUtil mMessagingUtil;
    private final MetricsLogger mMetricsLogger;
    private ContentObserver mMiuiOptimizationObserver;
    /* access modifiers changed from: private */
    public MiuiStatusBarPromptController mMiuiStatusBarPrompt;
    /* access modifiers changed from: private */
    public MiuiVoipManager mMiuiVoipManager;
    int mNaturalBarHeight = -1;
    private int mNavigationBarMode;
    /* access modifiers changed from: private */
    public NavigationBarView mNavigationBarView;
    /* access modifiers changed from: private */
    public int mNavigationBarYPostion;
    @Inject
    private NetworkController mNetworkController;
    private boolean mNoAnimationOnNextBarModeChange;
    /* access modifiers changed from: private */
    public int mNotchRotation;
    private NotificationClicker mNotificationClicker;
    protected NotificationData mNotificationData;
    /* access modifiers changed from: private */
    public NotificationGuts mNotificationGutsExposed;
    /* access modifiers changed from: private */
    public NotificationIconAreaController mNotificationIconAreaController;
    private final NotificationListenerService mNotificationListener;
    @Inject
    protected NotificationLogger mNotificationLogger;
    protected NotificationPanelView mNotificationPanel;
    protected NotificationShelf mNotificationShelf;
    private final ContentObserver mNotificationStyleObserver;
    /* access modifiers changed from: private */
    public View mNotifications;
    /* access modifiers changed from: private */
    public OLEDScreenHelper mOLEDScreenHelper;
    private final NotificationStackScrollLayout.OnChildLocationsChangedListener mOnChildLocationsChangedListener;
    private RemoteViews.OnClickHandler mOnClickHandler;
    private boolean mPanelExpanded;
    private HashMap<String, NotificationData.Entry> mPendingNotifications;
    private View mPendingRemoteInputView;
    /* access modifiers changed from: private */
    public View mPendingWorkRemoteInputView;
    private PhoneStateListener mPhoneStateListener;
    int mPixelFormat;
    ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    protected PowerManager mPowerManager;
    private Configuration mPreviousConfig;
    protected QSPanel mQSPanel;
    Object mQueueLock = new Object();
    private boolean mQuietModeEnable;
    protected RecentsComponent mRecents;
    private View.OnClickListener mRecentsClickListener;
    private boolean mReinflateNotificationsOnUserSwitched;
    protected RemoteInputController mRemoteInputController;
    protected ArraySet<NotificationData.Entry> mRemoteInputEntriesToRemoveOnCollapse;
    private View mReportRejectedTouch;
    /* access modifiers changed from: private */
    public ContentResolver mResolver;
    /* access modifiers changed from: private */
    public Runnable mRunnable;
    /* access modifiers changed from: private */
    public boolean mScreenButtonDisabled;
    private ContentObserver mScreenButtonStateObserver;
    private ScreenPinningRequest mScreenPinningRequest;
    private boolean mScreenTurningOn;
    protected ScrimController mScrimController;
    protected boolean mScrimSrcModeEnabled;
    protected SecurityManager mSecurityManager;
    protected final ContentObserver mSettingsObserver;
    private boolean mShouldDisableFsgMode;
    private boolean mShouldPopup;
    protected boolean mShowLockscreenNotifications;
    /* access modifiers changed from: private */
    public final ContentObserver mShowNotificationIconObserver;
    /* access modifiers changed from: private */
    public boolean mShowNotifications;
    @Inject
    private SilentModeObserverController mSilentModeObserverController;
    private ContentObserver mSliderStatusObserver;
    private boolean mSoftInputVisible;
    protected PorterDuffXfermode mSrcOverXferMode;
    protected PorterDuffXfermode mSrcXferMode;
    protected NotificationStackScrollLayout mStackScroller;
    Runnable mStartTracing;
    protected boolean mStartedGoingToSleep;
    protected int mState;
    protected CollapsedStatusBarFragment mStatusBarFragment;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    /* access modifiers changed from: private */
    public int mStatusBarMode;
    private LogMaker mStatusBarStateLog;
    protected PhoneStatusBarView mStatusBarView;
    protected StatusBarWindowView mStatusBarWindow;
    protected StatusBarWindowManager mStatusBarWindowManager;
    private int mStatusBarWindowState = 0;
    Runnable mStopTracing;
    private List<String> mSystemForegroundWhitelist;
    int mSystemUiVisibility = 0;
    private TelephonyManager mTelephonyManager;
    private HashMap<ExpandableNotificationRow, List<ExpandableNotificationRow>> mTmpChildOrderMap;
    private final int[] mTmpInt2;
    private final Rect mTmpRect = new Rect();
    private BroadcastReceiver mToggleBroadcastReceiver;
    /* access modifiers changed from: private */
    public ToggleManager mToggleManager;
    boolean mTracking;
    int mTrackingPosition;
    /* access modifiers changed from: private */
    public final UiOffloadThread mUiOffloadThread;
    protected UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitorCallback mUpdateCallback;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    protected boolean mUseHeadsUp;
    private ContentObserver mUserExperienceObserver;
    private final ContentObserver mUserFoldObserver;
    /* access modifiers changed from: private */
    public UserManager mUserManager;
    /* access modifiers changed from: private */
    public boolean mUserSetup;
    private DeviceProvisionedController.DeviceProvisionedListener mUserSetupObserver;
    @Inject
    private UserSwitcherController mUserSwitcherController;
    /* access modifiers changed from: private */
    public final SparseBooleanArray mUsersAllowingNotifications;
    /* access modifiers changed from: private */
    public final SparseBooleanArray mUsersAllowingPrivateNotifications;
    private Vibrator mVibrator;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected VisualStabilityManager mVisualStabilityManager;
    private BroadcastReceiver mVoipPhoneStateReceiver;
    VolumeComponent mVolumeComponent;
    protected boolean mVrMode;
    private boolean mWaitingForKeyguardExit;
    private boolean mWakeUpComingFromTouch;
    private PointF mWakeUpTouchLocation;
    /* access modifiers changed from: private */
    public boolean mWakeupForNotification;
    private final ContentObserver mWakeupForNotificationObserver;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    protected int mZenMode;
    private INotificationManager sService;

    class AppMessage {
        CharSequence className;
        int num;
        String pkgName;
        int userId;

        AppMessage() {
        }
    }

    private final class DozeServiceHost implements DozeHost {
        /* access modifiers changed from: private */
        public AODView mAODView;
        private boolean mAnimateWakeup;
        private View mAodScrim;
        private final ArrayList<DozeHost.Callback> mCallbacks = new ArrayList<>();
        /* access modifiers changed from: private */
        public ViewGroup mContainer;
        private ContentObserver mContentObserver;
        private Runnable mFingerUpRunnable = new Runnable() {
            public void run() {
                DozeServiceHost.this.mContainer.setVisibility(0);
            }
        };
        private boolean mShowAodAnimate;
        private Runnable mStopAodRunnable = new Runnable() {
            public void run() {
                DozeServiceHost.this.onStopDoze();
            }
        };
        private WindowManager mViewManager;

        /* access modifiers changed from: private */
        public AODView showSecurityIdentityViewAt() {
            this.mViewManager = (WindowManager) StatusBar.this.mContext.getSystemService("window");
            LayoutInflater inflater = LayoutInflater.from(StatusBar.this.mContext);
            if (this.mAODView != null) {
                this.mViewManager.removeView(this.mAODView);
            }
            AODView frame = (AODView) inflater.inflate(R.layout.aod_mode_layout, null);
            frame.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2015, 1280, MiuiKeyguardUtils.isGxzwSensor() ? -2 : -1);
            WindowManagerCompat.setLayoutInDisplayCutoutMode(layoutParams, 1);
            KeyguardCompatibilityHelperForO.setFlag(layoutParams);
            layoutParams.setTitle("AOD");
            layoutParams.screenOrientation = 1;
            this.mViewManager.addView(frame, layoutParams);
            this.mContainer = (ViewGroup) frame.findViewById(R.id.clock_container);
            this.mAodScrim = frame.findViewById(R.id.aod_scrim);
            if (!StatusBar.this.mDozingRequested) {
                frame.setVisibility(8);
                this.mContainer.setAlpha(0.0f);
            }
            return frame;
        }

        public DozeServiceHost(Context context) {
            StatusBar.this.mContext = context.getApplicationContext();
            this.mAODView = showSecurityIdentityViewAt();
            this.mContentObserver = new ContentObserver(StatusBar.this.mHandler, StatusBar.this) {
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    AODView unused = DozeServiceHost.this.mAODView = DozeServiceHost.this.showSecurityIdentityViewAt();
                }
            };
            StatusBar.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("auto_dual_clock"), false, this.mContentObserver, -1);
            StatusBar.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("resident_timezone"), false, this.mContentObserver, -1);
            StatusBar.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("resident_id"), false, this.mContentObserver, -1);
            StatusBar.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("aod_style_index"), false, this.mContentObserver, -1);
            StatusBar.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("aod_style_index_dual"), false, this.mContentObserver, -1);
            StatusBar.this.mContext.registerReceiver(new BroadcastReceiver(StatusBar.this) {
                public void onReceive(Context context, Intent intent) {
                    AODView unused = DozeServiceHost.this.mAODView = DozeServiceHost.this.showSecurityIdentityViewAt();
                }
            }, new IntentFilter("android.intent.action.TIMEZONE_CHANGED"));
        }

        /* access modifiers changed from: private */
        public void refreshView() {
            this.mAODView = showSecurityIdentityViewAt();
        }

        public boolean isDozing() {
            return StatusBar.this.mDozingRequested;
        }

        public void setAodClockVisibility(boolean visibility) {
            this.mContainer.setAlpha(visibility ? 1.0f : 0.0f);
        }

        public String toString() {
            return "PSB.DozeServiceHost[mCallbacks=" + this.mCallbacks.size() + "]";
        }

        public void fireAodState(boolean on) {
            Iterator<DozeHost.Callback> it = new ArrayList<>(this.mCallbacks).iterator();
            while (it.hasNext()) {
                it.next().onFodInAodStateChanged(on);
            }
        }

        public void fireFingerprintPressed(boolean pressed) {
            if (StatusBar.this.mDozing && MiuiGxzwUtils.supportLowBrightnessFod()) {
                Iterator<DozeHost.Callback> it = new ArrayList<>(this.mCallbacks).iterator();
                while (it.hasNext()) {
                    it.next().onFingerprintPressed(pressed);
                }
            }
            StatusBar.this.mHandler.removeCallbacks(this.mFingerUpRunnable);
            if (pressed) {
                this.mContainer.setVisibility(4);
            } else {
                StatusBar.this.mHandler.postDelayed(this.mFingerUpRunnable, 20);
            }
        }

        public void firePowerSaveChanged(boolean active) {
            Iterator<DozeHost.Callback> it = new ArrayList<>(this.mCallbacks).iterator();
            while (it.hasNext()) {
                it.next().onPowerSaveChanged(active);
            }
        }

        public void fireAnimateState() {
            Iterator<DozeHost.Callback> it = new ArrayList<>(this.mCallbacks).iterator();
            while (it.hasNext()) {
                it.next().onAodAnimate(this.mShowAodAnimate);
            }
        }

        public boolean isAnimateShowing() {
            return this.mShowAodAnimate;
        }

        public void setNotificationAnimate(boolean show) {
            this.mShowAodAnimate = show;
        }

        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        public void startDozing() {
            if (!StatusBar.this.mDozingRequested) {
                boolean unused = StatusBar.this.mDozingRequested = true;
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().startDozing();
                    StatusBar.this.mNotificationPanel.updateGxzwState();
                }
                DozeLog.traceDozing(StatusBar.this.mContext, StatusBar.this.mDozing);
                StatusBar.this.updateDozing();
                this.mAODView.setAlpha(1.0f);
                this.mAODView.setVisibility(0);
                this.mAODView.onStartDoze();
                this.mAODView.handleUpdateView();
                if (MiuiKeyguardUtils.isGxzwSensor() && MiuiKeyguardUtils.isAodClockDisable(StatusBar.this.mContext)) {
                    this.mContainer.setAlpha(0.0f);
                } else if (!this.mAODView.isAnimateEnable() || !MiuiKeyguardUtils.isAodClockDisable(StatusBar.this.mContext)) {
                    this.mContainer.setAlpha(0.0f);
                    this.mContainer.animate().alpha(1.0f).setDuration(800).setInterpolator(new SineEaseOutInterpolator()).setStartDelay(800).start();
                } else {
                    this.mContainer.setAlpha(0.0f);
                }
            }
        }

        public void pulseWhileDozing(DozeHost.PulseCallback callback, int reason) {
            if (reason != 15) {
                callback.onPulseStarted();
                callback.onPulseFinished();
            }
        }

        public void stopDozing() {
            if (Looper.myLooper() == StatusBar.this.mHandler.getLooper()) {
                onStopDoze();
            } else {
                StatusBar.this.mHandler.postAtFrontOfQueue(this.mStopAodRunnable);
            }
        }

        /* access modifiers changed from: private */
        public void onStopDoze() {
            if (StatusBar.this.mDozingRequested) {
                boolean unused = StatusBar.this.mDozingRequested = false;
                this.mAODView.onStopDoze();
                DozeLog.traceDozing(StatusBar.this.mContext, StatusBar.this.mDozing);
                StatusBar.this.updateDozing();
                Log.d("face_unlock", "start faceunlock when stop doze");
                StatusBar.this.mUpdateMonitor.startFaceUnlock();
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().stopDozing();
                    StatusBar.this.mNotificationPanel.updateGxzwState();
                }
                removeAODView();
            }
        }

        public void removeAODView() {
            this.mContainer.setAlpha(0.0f);
            if (this.mAODView.isAttachedToWindow()) {
                this.mAODView.setVisibility(8);
            }
        }

        public void dozeTimeTick() {
            this.mAODView.handleUpdateView();
        }

        public boolean isPowerSaveActive() {
            return StatusBar.this.mBatteryController.isPowerSave() || StatusBar.this.mBatteryController.isExtremePowerSave();
        }

        public boolean isPulsingBlocked() {
            return StatusBar.this.mFingerprintUnlockController.getMode() == 1;
        }

        public void setSunImage(int index) {
            this.mAODView.setSunImage(index);
        }

        public boolean isProvisioned() {
            return true;
        }

        public boolean isBlockingDoze() {
            return false;
        }

        public void extendPulse() {
            StatusBar.this.mDozeScrimController.extendPulse();
        }

        public void setAodDimmingScrim(float scrimOpacity) {
        }

        public void setDozeScreenBrightness(int value) {
        }

        public void setAnimateWakeup(boolean animateWakeup) {
            this.mAnimateWakeup = animateWakeup;
        }

        /* access modifiers changed from: private */
        public boolean shouldAnimateWakeup() {
            return this.mAnimateWakeup;
        }
    }

    private static class FastColorDrawable extends Drawable {
        private final int mColor;

        public FastColorDrawable(int color) {
            this.mColor = -16777216 | color;
        }

        public void draw(Canvas canvas) {
            canvas.drawColor(this.mColor, PorterDuff.Mode.SRC);
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public int getOpacity() {
            return -1;
        }

        public void setBounds(int left, int top, int right, int bottom) {
        }

        public void setBounds(Rect bounds) {
        }
    }

    protected class H extends Handler {
        protected H() {
        }

        public void handleMessage(Message m) {
            int i = m.what;
            switch (i) {
                case 1000:
                    StatusBar.this.animateExpandNotificationsPanel();
                    return;
                case 1001:
                    StatusBar.this.animateCollapsePanels();
                    return;
                case 1002:
                    StatusBar.this.animateExpandSettingsPanel((String) m.obj);
                    return;
                case 1003:
                    StatusBar.this.onLaunchTransitionTimeout();
                    return;
                case 1004:
                    StatusBar.this.onUpdateFsgState();
                    return;
                case 1005:
                    SomeArgs args = (SomeArgs) m.obj;
                    StatusBar.this.updateNotificationRankingDelayed((NotificationListenerService.RankingMap) args.arg1, ((Long) args.arg2).longValue());
                    return;
                default:
                    switch (i) {
                        case 1026:
                            StatusBar.this.toggleKeyboardShortcuts(m.arg1);
                            return;
                        case 1027:
                            StatusBar.this.dismissKeyboardShortcuts();
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    private final class NotificationClicker implements View.OnClickListener {
        private NotificationClicker() {
        }

        public void onClick(View v) {
            PendingIntent pendingIntent;
            boolean afterKeyguardGone;
            View view = v;
            if (!(view instanceof ExpandableNotificationRow)) {
                Logger.fullW("StatusBar", "NotificationClicker called on a view that is not a notification row.");
                return;
            }
            StatusBar.this.wakeUpIfDozing(SystemClock.uptimeMillis(), view);
            final ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            ExpandedNotification sbn = row.getStatusBarNotification();
            if (sbn == null) {
                Logger.fullW("StatusBar", "NotificationClicker called on an unclickable notification,");
            } else if (row.getProvider() == null || !row.getProvider().isMenuVisible()) {
                Notification notification = sbn.getNotification();
                if (notification.contentIntent != null) {
                    pendingIntent = notification.contentIntent;
                } else {
                    pendingIntent = notification.fullScreenIntent;
                }
                PendingIntent intent = pendingIntent;
                String notificationKey = sbn.getKey();
                boolean afterKeyguardGone2 = false;
                boolean isHeadsUp = StatusBar.this.mHeadsUpManager != null && StatusBar.this.mHeadsUpManager.isHeadsUp(notificationKey);
                ((SystemUIStat) Dependency.get(SystemUIStat.class)).onClick(sbn, isHeadsUp, StatusBar.this.isKeyguardShowing(), StatusBar.this.mStackScroller.getVisibleNotificationIndex(row));
                if (row.isSummaryWithChildren() && !row.isGroupExpanded() && (StatusBarNotificationCompat.isAutoGroupSummary(sbn) || intent == null)) {
                    row.getExpandClickListener().onClick(row);
                } else if (intent == null) {
                    Logger.fullI("StatusBar", "click notification, no intent, key=" + sbn.getKey());
                } else {
                    row.setJustClicked(true);
                    DejankUtils.postAfterTraversal(new Runnable() {
                        public void run() {
                            row.setJustClicked(false);
                        }
                    });
                    if (intent.isActivity() && PreviewInflater.wouldLaunchResolverActivity(StatusBar.this.mContext, intent.getIntent(), StatusBar.this.mCurrentUserId)) {
                        afterKeyguardGone2 = true;
                    }
                    Logger.fullI("StatusBar", "NotificationClicker onClick notification key=" + notificationKey + " afterKeyguardGone:" + afterKeyguardGone);
                    StatusBar statusBar = StatusBar.this;
                    final boolean z = isHeadsUp;
                    final ExpandableNotificationRow expandableNotificationRow = row;
                    final String str = notificationKey;
                    AnonymousClass2 r7 = r0;
                    final ExpandedNotification expandedNotification = sbn;
                    StatusBar statusBar2 = statusBar;
                    final PendingIntent pendingIntent2 = intent;
                    AnonymousClass2 r0 = new KeyguardHostView.OnDismissAction() {
                        public boolean onDismiss() {
                            if (z) {
                                if (StatusBar.this.isPanelFullyCollapsed()) {
                                    HeadsUpManager.setIsClickedNotification(expandableNotificationRow, true);
                                }
                                StatusBar.this.mHeadsUpManager.releaseImmediately(str);
                            }
                            ExpandedNotification parentToCancel = null;
                            if (NotificationClicker.this.shouldAutoCancel(expandedNotification) && StatusBar.this.mGroupManager.isOnlyChildInGroup(expandedNotification)) {
                                ExpandedNotification summarySbn = StatusBar.this.mGroupManager.getLogicalGroupSummary(expandedNotification).getStatusBarNotification();
                                if (NotificationClicker.this.shouldAutoCancel(summarySbn)) {
                                    parentToCancel = summarySbn;
                                }
                            }
                            final ExpandedNotification summarySbn2 = parentToCancel;
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    try {
                                        ActivityManagerCompat.getService().resumeAppSwitches();
                                    } catch (RemoteException e) {
                                    }
                                    if (pendingIntent2 != null) {
                                        if (pendingIntent2.isActivity()) {
                                            int userId = pendingIntent2.getCreatorUserHandle().getIdentifier();
                                            if (LockPatternUtilsCompat.isSeparateProfileChallengeEnabled(StatusBar.this.mLockPatternUtils, userId) && StatusBar.this.mKeyguardManager.isDeviceLocked(userId) && StatusBar.this.startWorkChallengeIfNecessary(userId, pendingIntent2.getIntentSender(), str)) {
                                                return;
                                            }
                                        }
                                        ActivityOptions options = MiuiMultiWindowUtils.getActivityOptions(StatusBar.this.mContext, pendingIntent2.getCreatorPackage());
                                        try {
                                            pendingIntent2.send(null, 0, null, null, null, null, options != null ? options.toBundle() : StatusBar.this.getActivityOptions());
                                            Logger.fullI("StatusBar", "click notification, sending intent, key=" + expandedNotification.getKey());
                                        } catch (PendingIntent.CanceledException e2) {
                                            Logger.fullW("StatusBar", "Sending contentIntent failed: " + e2);
                                        }
                                        if (pendingIntent2.isActivity()) {
                                            StatusBar.this.mAssistManager.hideAssist();
                                        }
                                    }
                                    try {
                                        StatusBarServiceCompat.onNotificationClick(StatusBar.this.mBarService, str, NotificationVisibilityCompat.obtain(str, StatusBar.this.mNotificationData.getRank(str), StatusBar.this.mNotificationData.getActiveNotifications().size(), true));
                                    } catch (Exception e3) {
                                    }
                                    if (summarySbn2 != null) {
                                        StatusBar.this.mHandler.post(new Runnable() {
                                            public void run() {
                                                Runnable removeRunnable = new Runnable() {
                                                    public void run() {
                                                        StatusBar.this.performRemoveNotification(summarySbn2);
                                                    }
                                                };
                                                if (StatusBar.this.isCollapsing()) {
                                                    StatusBar.this.addPostCollapseAction(removeRunnable);
                                                } else {
                                                    removeRunnable.run();
                                                }
                                            }
                                        });
                                    }
                                }
                            };
                            if (KeyguardCompatibilityHelperForN.isUserUnlocked(StatusBar.this.mContext)) {
                                StatusBar.this.mUiOffloadThread.submit(runnable);
                            } else {
                                Runnable unused = StatusBar.this.mRunnable = runnable;
                            }
                            StatusBar.this.animateCollapsePanels(2, true, true);
                            NotificationPanelView notificationPanelView = StatusBar.this.mNotificationPanel;
                            if (!NotificationPanelView.isDefaultLockScreenTheme()) {
                                StatusBar.this.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        StatusBar.this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
                                    }
                                }, 400);
                            }
                            StatusBar.this.visibilityChanged(false);
                            return true;
                        }
                    };
                    statusBar2.dismissKeyguardThenExecute(r7, afterKeyguardGone);
                }
            } else {
                row.animateTranslateNotification(0.0f);
            }
        }

        /* access modifiers changed from: private */
        public boolean shouldAutoCancel(StatusBarNotification sbn) {
            int flags = sbn.getNotification().flags;
            if ((flags & 16) == 16 && (flags & 64) == 0) {
                return true;
            }
            return false;
        }

        public void register(ExpandableNotificationRow row, StatusBarNotification sbn) {
            row.setOnClickListener(this);
        }
    }

    private final class W extends Handler {
        W(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2001:
                    StatusBar.this.beep((String) msg.obj);
                    return;
                case 2002:
                    StatusBar.this.updateMessage((AppMessage) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public StatusBar() {
        GestureRecorder gestureRecorder;
        if (DEBUG_GESTURES) {
            gestureRecorder = new GestureRecorder("/sdcard/statusbar_gestures.dat");
        } else {
            gestureRecorder = null;
        }
        this.mGestureRec = gestureRecorder;
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mUserSetup = false;
        this.mUserSetupObserver = new DeviceProvisionedController.DeviceProvisionedListener() {
            public void onDeviceProvisionedChanged() {
            }

            public void onUserSwitched() {
                onUserSetupChanged();
            }

            public void onUserSetupChanged() {
                boolean userSetup = StatusBar.this.mDeviceProvisionedController.isUserSetup(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
                Log.d("StatusBar", String.format("User setup changed: userSetup= %s mUserSetup=%s", new Object[]{Boolean.valueOf(userSetup), Boolean.valueOf(StatusBar.this.mUserSetup)}));
                if (userSetup != StatusBar.this.mUserSetup) {
                    boolean unused = StatusBar.this.mUserSetup = userSetup;
                    if (!StatusBar.this.mUserSetup && StatusBar.this.mStatusBarView != null) {
                        StatusBar.this.animateCollapseQuickSettings();
                    }
                    if (StatusBar.this.mKeyguardBottomArea != null) {
                        StatusBar.this.mKeyguardBottomArea.setUserSetupComplete(StatusBar.this.mUserSetup);
                    }
                    StatusBar.this.updateQsExpansionEnabled();
                }
            }
        };
        this.mHandler = createHandler();
        this.mUserExperienceObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean enable = true;
                if (Settings.Secure.getIntForUser(StatusBar.this.mContext.getContentResolver(), "upload_log_pref", (int) Build.IS_DEVELOPMENT_VERSION, StatusBar.this.mCurrentUserId) != 1) {
                    enable = false;
                }
                Util.setUserExperienceProgramEnabled(enable);
            }
        };
        this.mForceBlack = false;
        this.mForceBlackObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean unused = StatusBar.this.mForceBlack = MiuiSettings.Global.getBoolean(StatusBar.this.mContext.getContentResolver(), "force_black");
                LightBarController lightBarController = StatusBar.this.mLightBarController;
                boolean z = true;
                if (!StatusBar.this.mForceBlack || StatusBar.this.mContext.getResources().getConfiguration().orientation != 1) {
                    z = false;
                }
                lightBarController.setForceBlack(z);
                StatusBar.this.mHandler.post(StatusBar.this.mCheckBarModes);
                StatusBar.this.mNotificationPanel.setForceBlack(StatusBar.this.mForceBlack);
            }
        };
        this.mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
        this.mAutohide = new Runnable() {
            public void run() {
                int requested = StatusBar.this.mSystemUiVisibility & -201326593;
                if (StatusBar.this.mSystemUiVisibility != requested) {
                    StatusBar.this.notifyUiVisibilityChanged(requested);
                }
            }
        };
        this.mSrcXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
        this.mSrcOverXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        this.mMediaListener = new MediaController.Callback() {
            public void onPlaybackStateChanged(PlaybackState state) {
                super.onPlaybackStateChanged(state);
                if (StatusBar.DEBUG_MEDIA) {
                    Log.v("StatusBar", "DEBUG_MEDIA: onPlaybackStateChanged: " + state);
                }
                if (state != null && !StatusBar.this.isPlaybackActive(state.getState())) {
                    StatusBar.this.clearCurrentMediaNotification();
                    StatusBar.this.updateMediaMetaData(true, true);
                }
            }

            public void onMetadataChanged(MediaMetadata metadata) {
                super.onMetadataChanged(metadata);
                if (StatusBar.DEBUG_MEDIA) {
                    Log.v("StatusBar", "DEBUG_MEDIA: onMetadataChanged: " + metadata);
                }
                MediaMetadata unused = StatusBar.this.mMediaMetadata = metadata;
                StatusBar.this.updateMediaMetaData(true, true);
            }
        };
        this.mOnChildLocationsChangedListener = new NotificationStackScrollLayout.OnChildLocationsChangedListener() {
        };
        this.mTmpInt2 = new int[2];
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mPendingNotifications = new HashMap<>();
        this.mGoToLockedShadeListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (StatusBar.this.mState == 1) {
                    StatusBar.this.wakeUpIfDozing(SystemClock.uptimeMillis(), v);
                    StatusBar.this.goToLockedShade(null);
                }
            }
        };
        this.mTmpChildOrderMap = new HashMap<>();
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            public void onDreamingStateChanged(boolean dreaming) {
                if (dreaming) {
                    StatusBar.this.maybeEscalateHeadsUp();
                }
            }
        };
        this.mGameModeObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean z = true;
                if (Settings.Secure.getIntForUser(StatusBar.this.mContext.getContentResolver(), "gb_notification", 0, -2) != 1) {
                    z = false;
                }
                StatusBar.sGameMode = z;
            }
        };
        this.mGameHandsFreeObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                StatusBar statusBar = StatusBar.this;
                boolean z = true;
                if (Settings.Secure.getIntForUser(StatusBar.this.mContext.getContentResolver(), "gb_handsfree", 0, -2) != 1) {
                    z = false;
                }
                boolean unused = statusBar.mGameHandsFreeMode = z;
            }
        };
        this.mNotchRotation = -1;
        this.mLogicalWidth = -1;
        this.mLogicalHeight = -1;
        this.mInfo = new DisplayInfo();
        this.mJobHelper = new JobHelper();
        this.mDockedStackExistsChangedListener = new Divider.DockedStackExistsChangedListener() {
            public void onDockedStackMinimizedChanged(boolean minimized) {
                if (!Recents.getSystemServices().hasDockedTask() || !minimized) {
                    StatusBar.this.mMiuiStatusBarPrompt.clearState(1);
                } else {
                    StatusBar.this.mMiuiStatusBarPrompt.setState(1);
                }
            }
        };
        this.mRecentsClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                StatusBar.this.toggleRecentApps();
            }
        };
        this.mToggleBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_SCREEN_BUTTONS".equals(action)) {
                    StatusBar.this.mToggleManager.performToggle(20);
                } else if ("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_LOCK".equals(action)) {
                    StatusBar.this.mToggleManager.performToggle(10);
                } else if ("com.miui.app.ExtraStatusBarManager.action_TRIGGER_TOGGLE".equals(action)) {
                    StatusBar.this.mToggleManager.performToggle(intent.getIntExtra("com.miui.app.ExtraStatusBarManager.extra_TOGGLE_ID", -1));
                }
            }
        };
        this.mHideBackdropFront = new Runnable() {
            public void run() {
                if (StatusBar.DEBUG_MEDIA) {
                    Log.v("StatusBar", "DEBUG_MEDIA: removing fade layer");
                }
                StatusBar.this.mBackdropFront.setVisibility(4);
                StatusBar.this.mBackdropFront.animate().cancel();
                StatusBar.this.mBackdropFront.setImageDrawable(null);
            }
        };
        this.mAnimateCollapsePanels = new Runnable() {
            public void run() {
                StatusBar.this.animateCollapsePanels();
            }
        };
        this.mCheckBarModes = new Runnable() {
            public void run() {
                StatusBar.this.checkBarModes();
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (StatusBar.DEBUG) {
                    Log.v("StatusBar", "onReceive: " + intent);
                }
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                    KeyboardShortcuts.dismiss();
                    StatusBar.this.showReturnToInCallScreenButtonIfNeed();
                    StatusBar.this.mHeadsUpManager.removeHeadsUpNotification();
                    if (StatusBar.this.mRemoteInputController != null) {
                        StatusBar.this.mRemoteInputController.closeRemoteInputs();
                    }
                    if (StatusBar.this.isCurrentProfile(getSendingUserId())) {
                        int flags = 0;
                        String reason = intent.getStringExtra("reason");
                        if (reason != null && reason.equals("recentapps")) {
                            flags = 0 | 2;
                        }
                        StatusBar.this.animateCollapsePanels(flags);
                        if (reason != null && reason.equals("homekey")) {
                            AnalyticsHelper.trackStatusBarCollapse("click_home_button");
                        }
                    }
                } else if ("android.app.action.SHOW_DEVICE_MONITORING_DIALOG".equals(action)) {
                    StatusBar.this.mQSPanel.showDeviceMonitoringDialog();
                }
            }
        };
        this.mInternalBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.LEAVE_INCALL_SCREEN_DURING_CALL".equals(action)) {
                    if (TelephonyManager.getDefault().getCallState() == 0 && StatusBar.this.mMiuiVoipManager.getCallState() == 0) {
                        String unused = StatusBar.this.mCallState = "";
                        StatusBar.this.hideReturnToInCallScreenButton();
                    } else {
                        String unused2 = StatusBar.this.mCallState = intent.getStringExtra("call_state");
                        long unused3 = StatusBar.this.mCallBaseTime = intent.getLongExtra("base_time", 0);
                        if (!InCallUtils.isInCallNotificationHeadsUp(StatusBar.this.mContext, StatusBar.this.mHeadsUpManager)) {
                            StatusBar.this.showReturnToInCallScreenButton(StatusBar.this.mCallState, StatusBar.this.mCallBaseTime);
                        }
                    }
                    if (InCallUtils.isInCallScreenShowing(StatusBar.this.mContext) || InCallUtils.isCallScreenShowing(StatusBar.this.mContext)) {
                        StatusBar.this.mMiuiStatusBarPrompt.makeReturnToInCallScreenButtonGone();
                    }
                } else if ("android.intent.action.ENTER_INCALL_SCREEN_DURING_CALL".equals(action)) {
                    StatusBar.this.hideReturnToInCallScreenButton();
                    String unused4 = StatusBar.this.mCallState = "";
                    if (InCallUtils.needShowReturnToInVoipCallScreenButton(StatusBar.this.mContext)) {
                        Log.v("StatusBar", "needShowReturnToInVoipCallScreenButton");
                        String unused5 = StatusBar.this.mCallState = StatusBar.this.mMiuiVoipManager.getExtraCallState();
                        long unused6 = StatusBar.this.mCallBaseTime = StatusBar.this.mMiuiVoipManager.getCallBaseTime();
                        StatusBar.this.showReturnToInCallScreenButton(StatusBar.this.mCallState, StatusBar.this.mCallBaseTime);
                    }
                } else if ("miui.intent.action.MIUI_REGION_CHANGED".equals(action)) {
                    TelephonyIcons.updateDataTypeMiuiRegion(StatusBar.this.mContext, SystemProperties.get("ro.miui.mcc", ""));
                    if (StatusBar.this.mHeaderView != null) {
                        StatusBar.this.mHeaderView.regionChanged();
                    }
                } else if ("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode".equals(action)) {
                    String state = intent.getStringExtra("EXTRA_STATE");
                    if (state == null) {
                        state = "drivemode_standby";
                    }
                    if (state.equals("drivemode_standby")) {
                        boolean unused7 = StatusBar.this.mIsInDriveModeMask = true;
                    } else if (state.equals("drivemode_idle")) {
                        boolean unused8 = StatusBar.this.mIsInDriveModeMask = false;
                    }
                    boolean unused9 = StatusBar.this.mIsInDriveMode = true;
                    StatusBar.this.mMiuiStatusBarPrompt.showReturnToDriveModeView(true, StatusBar.this.mIsInDriveModeMask);
                    StatusBar.this.updateDriveMode();
                } else if ("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode".equals(action)) {
                    boolean unused10 = StatusBar.this.mIsInDriveMode = false;
                    StatusBar.this.mMiuiStatusBarPrompt.showReturnToDriveModeView(false, false);
                    StatusBar.this.updateDriveMode();
                } else if ("com.miui.app.ExtraStatusBarManager.action_refresh_notification".equals(action)) {
                    String pkg = intent.getStringExtra("app_packageName");
                    String messageId = intent.getStringExtra("messageId");
                    String changeImportancePkg = intent.getStringExtra("change_importance");
                    if (!TextUtils.isEmpty(changeImportancePkg)) {
                        StatusBar.this.changePkgImportance(changeImportancePkg, intent.getIntExtra("new_value", 0));
                    } else if (intent.getBooleanExtra("com.miui.app.ExtraStatusBarManager.extra_forbid_notification", false)) {
                        StatusBar.this.filterPackageNotifications(pkg);
                        if (!TextUtils.equals(intent.getSender(), StatusBar.this.mContext.getPackageName())) {
                            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onBlock(pkg, messageId);
                        }
                    } else if (intent.getBooleanExtra(StatusBar.EXTRA_HIGH_PRIORITY_SETTING, false)) {
                        int uid = intent.getIntExtra(StatusBar.EXTRA_APP_UID, -1);
                        Log.d("StatusBar", "update high priority: pkg=" + pkg + ", uid=" + uid);
                        if (uid >= 0) {
                            RankUtil.updateHighPriorityMap(pkg, uid);
                            StatusBar.this.mHandler.post(new Runnable() {
                                public void run() {
                                    StatusBar.this.updateNotifications();
                                }
                            });
                        }
                    } else if (!TextUtils.isEmpty(pkg)) {
                        AppMessage appMsg = new AppMessage();
                        appMsg.pkgName = pkg;
                        appMsg.className = "";
                        appMsg.userId = 0;
                        appMsg.num = 0;
                        StatusBar.this.mBgHandler.obtainMessage(2002, appMsg).sendToTarget();
                    }
                } else if ("com.miui.app.ExtraStatusBarManager.action_remove_keyguard_notification".equals(action)) {
                    int keyCode = intent.getIntExtra("com.miui.app.ExtraStatusBarManager.extra_notification_key", 0);
                    int click = intent.getIntExtra("com.miui.app.ExtraStatusBarManager.extra_notification_click", 0);
                    if (keyCode == 0) {
                        Log.d("StatusBar", "keyCode == 0 CLEAR_KEYGUARD_NOTIFICATION");
                        StatusBar.this.mKeyguardNotificationHelper.clear();
                        return;
                    }
                    StatusBar.this.mKeyguardNotificationHelper.remove(keyCode, null);
                    for (NotificationData.Entry entry : StatusBar.this.mNotificationData.getActiveNotifications()) {
                        if (keyCode == entry.key.hashCode()) {
                            ExpandedNotification n = entry.notification;
                            Log.d("StatusBar", "keycode=" + keyCode + ";click=" + click + ";pkg=" + n.getPackageName() + ";id=" + n.getId());
                            if (click == 1) {
                                entry.row.callOnClick();
                            } else {
                                StatusBar.this.onNotificationClear(n);
                            }
                        }
                    }
                }
            }
        };
        this.mDemoCallback = new DemoModeController.DemoModeCallback() {
            public void onDemoModeChanged(String command, Bundle args) {
                StatusBar.this.dispatchDemoCommand(command, args);
            }
        };
        this.mFakeArtworkReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (StatusBar.DEBUG) {
                    Log.v("StatusBar", "onReceive: " + intent);
                }
                if ("fake_artwork".equals(intent.getAction()) && StatusBar.DEBUG_MEDIA_FAKE_ARTWORK) {
                    StatusBar.this.updateMediaMetaData(true, true);
                }
            }
        };
        this.mStartTracing = new Runnable() {
            public void run() {
                StatusBar.this.vibrate();
                SystemClock.sleep(250);
                Log.d("StatusBar", "startTracing");
                Debug.startMethodTracing("/data/statusbar-traces/trace");
                StatusBar.this.mHandler.postDelayed(StatusBar.this.mStopTracing, 10000);
            }
        };
        this.mStopTracing = new Runnable() {
            public void run() {
                Debug.stopMethodTracing();
                Log.d("StatusBar", "stopTracing");
                StatusBar.this.vibrate();
            }
        };
        this.mFadeKeyguardWhenUnlockByGxzw = false;
        this.mGroupManager = new NotificationGroupManager();
        this.mVisualStabilityManager = new VisualStabilityManager();
        this.mCurrentUserId = 0;
        this.mCurrentProfiles = new SparseArray<>();
        this.mLayoutDirection = -1;
        this.mHeadsUpEntriesToRemoveOnSwitch = new ArraySet<>();
        this.mRemoteInputEntriesToRemoveOnCollapse = new ArraySet<>();
        this.mKeysKeptForRemoteInput = new ArraySet<>();
        this.mUseHeadsUp = false;
        this.mHeadsUpTicker = false;
        this.mDisableNotificationAlerts = false;
        this.mLockscreenPublicMode = new SparseBooleanArray();
        this.mUsersAllowingPrivateNotifications = new SparseBooleanArray();
        this.mUsersAllowingNotifications = new SparseBooleanArray();
        this.mNotificationClicker = new NotificationClicker();
        this.mDeviceProvisionedListener = new DeviceProvisionedController.DeviceProvisionedListener() {
            public void onDeviceProvisionedChanged() {
                StatusBar.this.updateNotifications();
            }

            public void onUserSwitched() {
                onUserSetupChanged();
            }

            public void onUserSetupChanged() {
                if (StatusBar.this.mDozeServiceHost != null) {
                    StatusBar.this.mDozeServiceHost.refreshView();
                }
            }
        };
        this.mMiuiOptimizationObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean disabled = true;
                if (Settings.Secure.getIntForUser(StatusBar.this.mContext.getContentResolver(), "miui_optimization", 1, -2) != 0) {
                    disabled = false;
                }
                Util.setMiuiOptimizationDisabled(disabled);
                if (StatusBar.this.mNotifications != null) {
                    StatusBar.this.mShowNotificationIconObserver.onChange(false);
                }
            }
        };
        this.mSettingsObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                StatusBar.this.setZenMode(Settings.Global.getInt(StatusBar.this.mContext.getContentResolver(), "zen_mode", 0));
                StatusBar.this.updateLockscreenNotificationSetting();
            }
        };
        this.mShowNotificationIconObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean z;
                StatusBar statusBar = StatusBar.this;
                if (Util.showCtsSpecifiedColor()) {
                    z = true;
                } else {
                    z = MiuiStatusBarManager.isShowNotificationIconForUser(StatusBar.this.mContext, StatusBar.this.mCurrentUserId);
                }
                boolean unused = statusBar.mShowNotifications = z;
                StatusBar.this.mNotificationIconAreaController.setShowNotificationIcon(StatusBar.this.mShowNotifications);
                StatusBar.this.updateNotifications();
                StatusBar.this.updateNotificationIconsLayout();
            }
        };
        this.mWakeupForNotification = true;
        this.mWakeupForNotificationObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean unused = StatusBar.this.mWakeupForNotification = MiuiKeyguardUtils.isWakeupForNotification(StatusBar.this.mContext.getContentResolver());
            }
        };
        this.mLockscreenSettingsObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                StatusBar.this.mUsersAllowingPrivateNotifications.clear();
                StatusBar.this.mUsersAllowingNotifications.clear();
                StatusBar.this.updateLockscreenNotificationSetting();
                StatusBar.this.updateNotifications();
            }
        };
        this.mNotificationStyleObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                int defStyle;
                if (Constants.SHOW_NOTIFICATION_HEADER) {
                    defStyle = 1;
                } else {
                    defStyle = 0;
                }
                if (NotificationUtil.isNotificationStyleChanged(Settings.System.getIntForUser(StatusBar.this.mContext.getContentResolver(), "status_bar_notification_style", defStyle, StatusBar.this.mCurrentUserId))) {
                    StatusBar.this.updateNotificationsOnDensityOrFontScaleChanged();
                }
            }
        };
        this.mUserFoldObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                int lastFold = -1;
                int userFold = Constants.IS_INTERNATIONAL ? -1 : Settings.Global.getInt(StatusBar.this.mContext.getContentResolver(), "user_fold", 0);
                if (NotificationUtil.isUserFold()) {
                    lastFold = 1;
                }
                if (userFold == 0 || userFold != lastFold) {
                    NotificationUtil.userFold(userFold);
                    if (NotificationUtil.isUserFold()) {
                        StatusBar.this.inflateFoldView();
                    } else {
                        StatusBar.this.mStackScroller.removeFoldView();
                    }
                    StatusBar.this.updateNotifications();
                }
            }
        };
        this.mCloudDataObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                CloudDataHelper.updateAll(StatusBar.this.mContext);
            }
        };
        this.mScreenButtonStateObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                StatusBar statusBar = StatusBar.this;
                boolean z = false;
                if (Settings.Secure.getIntForUser(StatusBar.this.mResolver, "screen_buttons_state", 0, StatusBar.this.mCurrentUserId) != 0) {
                    z = true;
                }
                boolean unused = statusBar.mScreenButtonDisabled = z;
                StatusBar.this.processScreenBtnDisableNotification();
            }
        };
        this.mSliderStatusObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                if (Settings.System.getIntForUser(StatusBar.this.mContext.getContentResolver(), "sc_status", 1, 0) == 0 && StatusBar.this.mNotificationPanel != null) {
                    if (StatusBar.this.mNotificationPanel.isTracking() || !StatusBar.this.mNotificationPanel.isFullyCollapsed()) {
                        StatusBar.this.mNotificationPanel.stopTrackingAndCollapsed();
                        StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
                    }
                }
            }
        };
        this.mOnClickHandler = new AbstractOnClickHandler() {
            public boolean onClickHandler(final View view, final PendingIntent pendingIntent, final Intent fillInIntent) {
                StatusBar.this.wakeUpIfDozing(SystemClock.uptimeMillis(), view);
                if (handleRemoteInput(view, pendingIntent, fillInIntent)) {
                    return true;
                }
                if (StatusBar.DEBUG) {
                    Log.v("StatusBar", "Notification click handler invoked for intent: " + pendingIntent);
                }
                logActionClick(view);
                try {
                    ActivityManagerCompat.getService().resumeAppSwitches();
                } catch (RemoteException e) {
                }
                if (!pendingIntent.isActivity()) {
                    return superOnClickHandler(view, pendingIntent, fillInIntent);
                }
                StatusBar.this.dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                    public boolean onDismiss() {
                        try {
                            ActivityManagerCompat.getService().resumeAppSwitches();
                        } catch (RemoteException e) {
                        }
                        boolean handled = AnonymousClass76.this.superOnClickHandler(view, pendingIntent, fillInIntent);
                        if (handled) {
                            StatusBar.this.animateCollapsePanels(2, true);
                            StatusBar.this.visibilityChanged(false);
                            StatusBar.this.mAssistManager.hideAssist();
                        }
                        return handled;
                    }
                }, PreviewInflater.wouldLaunchResolverActivity(StatusBar.this.mContext, pendingIntent.getIntent(), StatusBar.this.mCurrentUserId));
                return true;
            }

            private void logActionClick(View view) {
                ViewParent parent = view.getParent();
                String key = getNotificationKeyForParent(parent);
                if (key == null) {
                    Log.w("StatusBar", "Couldn't determine notification for click.");
                    return;
                }
                int index = -1;
                if (view.getId() == 16908663 && parent != null && (parent instanceof ViewGroup)) {
                    index = ((ViewGroup) parent).indexOfChild(view);
                }
                try {
                    StatusBarServiceCompat.onNotificationActionClick(StatusBar.this.mBarService, key, index, NotificationVisibilityCompat.obtain(key, StatusBar.this.mNotificationData.getRank(key), StatusBar.this.mNotificationData.getActiveNotifications().size(), true));
                } catch (Exception e) {
                }
            }

            private String getNotificationKeyForParent(ViewParent parent) {
                while (parent != null) {
                    if (parent instanceof ExpandableNotificationRow) {
                        return ((ExpandableNotificationRow) parent).getStatusBarNotification().getKey();
                    }
                    parent = parent.getParent();
                }
                return null;
            }

            /* access modifiers changed from: private */
            public boolean superOnClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
                return super.onClickHandler(view, pendingIntent, fillInIntent, 1);
            }

            private boolean handleRemoteInput(View view, PendingIntent pendingIntent, Intent fillInIntent) {
                View view2 = view;
                Object tag = NotificationViewWrapperCompat.getRemoteInputTag(view);
                RemoteInput[] inputs = null;
                if (tag instanceof RemoteInput[]) {
                    inputs = (RemoteInput[]) tag;
                }
                if (inputs == null) {
                    return false;
                }
                RemoteInput input = null;
                for (RemoteInput i : inputs) {
                    if (i.getAllowFreeFormInput()) {
                        input = i;
                    }
                }
                if (input == null) {
                    return false;
                }
                ExpandableNotificationRow row = null;
                ViewParent p = view.getParent();
                RemoteInputView riv = null;
                while (true) {
                    if (p == null) {
                        break;
                    }
                    if (p instanceof View) {
                        View pv = (View) p;
                        if (pv.isRootNamespace()) {
                            riv = findRemoteInputView(pv);
                            break;
                        }
                    }
                    p = p.getParent();
                }
                while (true) {
                    if (p == null) {
                        break;
                    } else if (p instanceof ExpandableNotificationRow) {
                        row = (ExpandableNotificationRow) p;
                        break;
                    } else {
                        p = p.getParent();
                    }
                }
                if (row == null) {
                    return false;
                }
                row.setUserExpanded(true);
                if (!StatusBar.this.mAllowLockscreenRemoteInput) {
                    int userId = pendingIntent.getCreatorUserHandle().getIdentifier();
                    if (StatusBar.this.isLockscreenPublicMode(userId)) {
                        StatusBar.this.onLockedRemoteInput(row, view2);
                        return true;
                    } else if (StatusBar.this.mUserManager.getUserInfo(userId).isManagedProfile() && StatusBar.this.mKeyguardManager.isDeviceLocked(userId)) {
                        StatusBar.this.onLockedWorkRemoteInput(userId, row, view2);
                        return true;
                    }
                }
                if (riv == null) {
                    riv = findRemoteInputView(row.getPrivateLayout().getExpandedChild());
                    if (riv == null) {
                        return false;
                    }
                    if (!row.getPrivateLayout().getExpandedChild().isShown()) {
                        StatusBar.this.onMakeExpandedVisibleForRemoteInput(row, view2);
                        return true;
                    }
                }
                int width = view.getWidth();
                if (view2 instanceof TextView) {
                    TextView tv = (TextView) view2;
                    if (tv.getLayout() != null) {
                        width = Math.min(width, ((int) tv.getLayout().getLineWidth(0)) + tv.getCompoundPaddingLeft() + tv.getCompoundPaddingRight());
                    }
                }
                int cx = view.getLeft() + (width / 2);
                int cy = view.getTop() + (view.getHeight() / 2);
                int w = riv.getWidth();
                int h = riv.getHeight();
                riv.setRevealParameters(cx, cy, Math.max(Math.max(cx + cy, (h - cy) + cx), Math.max((w - cx) + cy, (w - cx) + (h - cy))));
                riv.setPendingIntent(pendingIntent);
                riv.setRemoteInput(inputs, input);
                riv.focusAnimated();
                return true;
            }

            private RemoteInputView findRemoteInputView(View v) {
                if (v == null) {
                    return null;
                }
                return (RemoteInputView) v.findViewWithTag(RemoteInputView.VIEW_TAG);
            }
        };
        this.mBaseBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    StatusBar.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    StatusBar.this.mContextForUser = StatusBar.this.getContextForUser(StatusBar.this.mCurrentUserId);
                    StatusBar.this.updateCurrentProfilesCache();
                    Log.v("StatusBar", "userId " + StatusBar.this.mCurrentUserId + " is in the house");
                    StatusBar.this.updateLockscreenNotificationSetting();
                    StatusBar.this.userSwitched(StatusBar.this.mCurrentUserId);
                    StatusBar.this.mToggleManager.updateAllToggles(StatusBar.this.mCurrentUserId);
                } else if ("android.intent.action.USER_ADDED".equals(action)) {
                    StatusBar.this.updateCurrentProfilesCache();
                } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                    List<ActivityManager.RecentTaskInfo> recentTask = null;
                    try {
                        recentTask = ActivityManagerCompat.getRecentTasks(1, 5, StatusBar.this.mCurrentUserId);
                    } catch (RemoteException e) {
                    }
                    if (recentTask != null && recentTask.size() > 0) {
                        UserInfo user = StatusBar.this.mUserManager.getUserInfo(recentTask.get(0).userId);
                        if (user != null && user.isManagedProfile()) {
                            Toast toast = Toast.makeText(StatusBar.this.mContext, R.string.managed_profile_foreground_toast, 0);
                            TextView text = (TextView) toast.getView().findViewById(16908299);
                            text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.stat_sys_managed_profile_status, 0, 0, 0);
                            text.setCompoundDrawablePadding(StatusBar.this.mContext.getResources().getDimensionPixelSize(R.dimen.managed_profile_toast_padding));
                            toast.show();
                        }
                        if (user != null) {
                            StatusBar.this.mIconPolicy.profileChanged(user.getUserHandle().getIdentifier());
                        }
                    }
                } else if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    ((NotificationManager) StatusBar.this.mContext.getSystemService("notification")).cancel(5);
                    Settings.Secure.putInt(StatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                    if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                        StatusBar.this.animateCollapsePanels(2, true);
                        StatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                    }
                } else if ("com.android.systemui.statusbar.work_challenge_unlocked_notification_action".equals(action)) {
                    IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
                    String notificationKey = intent.getStringExtra("android.intent.extra.INDEX");
                    if (intentSender != null) {
                        try {
                            StatusBar.this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e2) {
                        }
                    }
                    if (notificationKey != null) {
                        try {
                            StatusBarServiceCompat.onNotificationClick(StatusBar.this.mBarService, notificationKey, NotificationVisibilityCompat.obtain(notificationKey, StatusBar.this.mNotificationData.getRank(notificationKey), StatusBar.this.mNotificationData.getActiveNotifications().size(), true));
                        } catch (Exception e3) {
                        }
                    }
                }
            }
        };
        this.mAllUsersReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action) && StatusBar.this.isCurrentProfile(getSendingUserId())) {
                    StatusBar.this.mUsersAllowingPrivateNotifications.clear();
                    StatusBar.this.updateLockscreenNotificationSetting();
                    StatusBar.this.updateNotifications();
                } else if ("android.intent.action.DEVICE_LOCKED_CHANGED".equals(action)) {
                    if (userId != StatusBar.this.mCurrentUserId && StatusBar.this.isCurrentProfile(userId)) {
                        StatusBar.this.onWorkChallengeChanged();
                    }
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    if (StatusBar.this.mRunnable != null) {
                        StatusBar.this.mUiOffloadThread.submit(StatusBar.this.mRunnable);
                        Runnable unused = StatusBar.this.mRunnable = null;
                    }
                } else if ("android.intent.action.APPLICATION_MESSAGE_QUERY".equals(action)) {
                    boolean requestFirstTime = intent.getBooleanExtra("com.miui.extra_update_request_first_time", false);
                    Log.d("StatusBar", "recevie broadbcast ACTION_APPLICATION_MESSAGE_QUERY, requestFirstTime=" + requestFirstTime);
                    if (requestFirstTime) {
                        Iterator<NotificationData.Entry> it = StatusBar.this.mNotificationData.getNeedsUpdateBadgeNumNotifications().iterator();
                        while (it.hasNext()) {
                            StatusBar.this.updateAppBadgeNum(it.next().notification);
                        }
                    }
                }
            }
        };
        this.mEnableNotificationsReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (!checkSender(intent.getSender())) {
                    Log.d("StatusBar", "enable notifications receiver: invalid sender");
                    return;
                }
                String pkg = intent.getStringExtra("pkg");
                boolean enabled = intent.getBooleanExtra("enabled", true);
                if (checkParams(pkg)) {
                    Log.d("StatusBar", "enable notifications receiver: pkg=" + pkg + ", enabled=" + enabled);
                    NotificationFilterHelper.enableNotifications(context, pkg, enabled);
                }
            }

            private boolean checkSender(String pkg) {
                return TextUtils.equals("com.android.systemui", pkg) || TextUtils.equals("com.xiaomi.xmsf", pkg);
            }

            private boolean checkParams(String pkg) {
                if (!TextUtils.isEmpty(pkg)) {
                    return true;
                }
                Log.d("StatusBar", "enable notifications receiver: empty pkg");
                return false;
            }
        };
        this.mNotificationListener = new NotificationListenerService() {
            public void onListenerConnected() {
                if (StatusBar.DEBUG) {
                    Log.d("StatusBar", "onListenerConnected");
                }
                final StatusBarNotification[] notifications = getActiveNotifications();
                if (notifications == null) {
                    Log.w("StatusBar", "onListenerConnected unable to get active notifications.");
                    return;
                }
                final NotificationListenerService.RankingMap currentRanking = getCurrentRanking();
                StatusBar.this.mHandler.post(new Runnable() {
                    public void run() {
                        for (StatusBarNotification sbn : notifications) {
                            sbn.getNotification().extraNotification.setEnableFloat(false);
                            boolean unused = StatusBar.this.handleNotification(sbn, currentRanking, false);
                        }
                    }
                });
            }

            public void onNotificationPosted(final StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
                if (StatusBar.DEBUG) {
                    Log.d("StatusBar", "onNotificationPosted: " + sbn);
                }
                if (sbn != null) {
                    StatusBar.this.mHandler.post(new Runnable() {
                        public void run() {
                            StatusBar.this.processForRemoteInput(sbn.getNotification());
                            String key = sbn.getKey();
                            StatusBar.this.mKeysKeptForRemoteInput.remove(key);
                            boolean isUpdate = StatusBar.this.mNotificationData.get(key) != null;
                            Logger.fullI("StatusBar", "onNotificationPosted key:" + key + " isUpdate:" + isUpdate);
                            if (StatusBar.ENABLE_CHILD_NOTIFICATIONS || !StatusBar.this.mGroupManager.isChildInGroupWithSummary(sbn)) {
                                boolean unused = StatusBar.this.handleNotification(sbn, rankingMap, isUpdate);
                                return;
                            }
                            if (StatusBar.DEBUG) {
                                Log.d("StatusBar", "Ignoring group child due to existing summary: " + sbn);
                            }
                            if (isUpdate) {
                                StatusBar.this.removeNotification(key, rankingMap);
                            } else {
                                StatusBar.this.mNotificationData.updateRanking(rankingMap);
                            }
                        }
                    });
                }
            }

            public void onNotificationRemoved(StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
                if (StatusBar.DEBUG) {
                    Log.d("StatusBar", "onNotificationRemoved: " + sbn);
                }
                if (sbn != null) {
                    final String key = sbn.getKey();
                    StatusBar.this.mHandler.post(new Runnable() {
                        public void run() {
                            Logger.fullI("StatusBar", "onNotificationRemoved: " + key);
                            StatusBar.this.removeNotification(key, rankingMap);
                        }
                    });
                }
            }

            public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
                Logger.fullI("StatusBar", "onRankingUpdate");
                if (rankingMap != null) {
                    StatusBar.this.mHandler.removeMessages(1005);
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = rankingMap;
                    args.arg2 = Long.valueOf(SystemClock.uptimeMillis());
                    StatusBar.this.mHandler.sendMessageDelayed(StatusBar.this.mHandler.obtainMessage(1005, args), 200);
                }
            }
        };
    }

    static {
        boolean onlyCoreApps;
        boolean freeformWindowManagement;
        boolean z = true;
        if (Build.VERSION.SDK_INT > 23) {
            z = SystemProperties.getBoolean("debug.child_notifs", true);
        } else if (!miui.os.Build.IS_DEBUGGABLE || !SystemProperties.getBoolean("debug.child_notifs", false)) {
            z = false;
        }
        ENABLE_CHILD_NOTIFICATIONS = z;
        try {
            IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            onlyCoreApps = packageManager.isOnlyCoreApps();
            freeformWindowManagement = PackageManagerCompat.hasSystemFeature(packageManager, "android.software.freeform_window_management", 0);
        } catch (RemoteException e) {
            onlyCoreApps = false;
            freeformWindowManagement = false;
        }
        ONLY_CORE_APPS = onlyCoreApps;
        FREEFORM_WINDOW_MANAGEMENT = freeformWindowManagement;
    }

    public void onSilentModeChanged(boolean enabled) {
        this.mQuietModeEnable = enabled;
        if (MiuiSettings.SilenceMode.isSupported) {
            this.mIsDNDEnabled = MiuiSettings.SilenceMode.isDNDEnabled(this.mContext);
            this.mIconPolicy.updateSilentModeIcon();
            this.mShouldPopup = MiuiSettings.SilenceMode.showNotification(this.mContext);
            return;
        }
        this.mIconPolicy.setQuietMode(enabled);
    }

    public void start() {
        int i;
        this.mContext.setTheme(R.style.Theme);
        this.mBgHandler = createBgHandler();
        this.mResolver = this.mContext.getContentResolver();
        RecentsEventBus.getDefault().register(this);
        FoldBucketHelper.init();
        this.mOLEDScreenHelper = new OLEDScreenHelper(this.mContext);
        this.mKeyguardMonitor = (KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class);
        this.mSecurityManager = (SecurityManager) this.mContext.getSystemService("security");
        this.mMiuiStatusBarPrompt = (MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class);
        this.mForegroundServiceController = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        updateDisplaySize();
        this.mScrimSrcModeEnabled = this.mContext.getResources().getBoolean(R.bool.config_status_bar_scrim_behind_use_src);
        this.mTelephonyManager = TelephonyManager.getDefault();
        this.mMiuiVoipManager = MiuiVoipManager.getInstance(this.mContext);
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mToggleManager = ToggleManager.createInstance(this.mContext, this.mCurrentUserId);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        DateTimeView.setReceiverHandler((Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        putComponent(StatusBar.class, this);
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mNotificationData = new NotificationData(this);
        this.mMessagingUtil = new NotificationMessagingUtil(this.mContext);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedListener);
        registerContentObserver();
        this.mRecents = (RecentsComponent) getComponent(Recents.class);
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(this.mLocale);
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mCommandQueue = (CommandQueue) getComponent(CommandQueue.class);
        this.mCommandQueue.addCallbacks(this);
        int[] switches = new int[9];
        ArrayList<IBinder> binders = new ArrayList<>();
        ArrayList<String> iconSlots = new ArrayList<>();
        ArrayList<StatusBarIcon> icons = new ArrayList<>();
        Rect fullscreenStackBounds = new Rect();
        Rect dockedStackBounds = new Rect();
        try {
            SystemUICompat.registerStatusBar(this.mBarService, this.mCommandQueue, iconSlots, icons, switches, binders, fullscreenStackBounds, dockedStackBounds);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        this.mSettingsObserver.onChange(false);
        this.mCommandQueue.disable(switches[0], switches[6], false);
        ArrayList<StatusBarIcon> icons2 = icons;
        ArrayList<String> iconSlots2 = iconSlots;
        ArrayList<IBinder> binders2 = binders;
        int[] switches2 = switches;
        setSystemUiVisibility(switches[1], switches[7], switches[8], -1, fullscreenStackBounds, dockedStackBounds);
        topAppWindowChanged(switches2[2] != 0);
        setImeWindowStatus(binders2.get(0), switches2[3], switches2[4], switches2[5] != 0);
        int N = iconSlots2.size();
        for (int i2 = 0; i2 < N; i2++) {
            this.mCommandQueue.setIcon(iconSlots2.get(i2), icons2.get(i2));
        }
        this.mKeyguardNotificationHelper = new KeyguardNotificationHelper(this.mContext, this.mGroupManager);
        try {
            this.mNotificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
        } catch (RemoteException e2) {
            Log.e("StatusBar", "Unable to register notification listener", e2);
        }
        if (DEBUG) {
            Log.d("StatusBar", String.format("init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x", new Object[]{Integer.valueOf(icons2.size()), Integer.valueOf(switches2[0]), Integer.valueOf(switches2[1]), Integer.valueOf(switches2[2]), Integer.valueOf(switches2[3])}));
        }
        this.mContextForUser = getContextForUser(this.mCurrentUserId);
        this.mPreviousConfig = new Configuration(this.mContext.getResources().getConfiguration());
        setHeadsUpUser(this.mCurrentUserId);
        AnalyticsHelper.registerReceiver(this.mContext);
        IntentFilter enableNotificationsFilter = new IntentFilter();
        enableNotificationsFilter.addAction("miui.intent.action.systemui.ENABLE_NOTIFICATIONS");
        this.mContext.registerReceiver(this.mEnableNotificationsReceiver, enableNotificationsFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, filter);
        IntentFilter internalFilter = new IntentFilter();
        internalFilter.addAction("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        internalFilter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        internalFilter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, internalFilter, "com.android.systemui.permission.SELF", null);
        IntentFilter allUsersFilter = new IntentFilter();
        allUsersFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        allUsersFilter.addAction("android.intent.action.DEVICE_LOCKED_CHANGED");
        allUsersFilter.addAction("android.intent.action.USER_UNLOCKED");
        allUsersFilter.addAction("android.intent.action.APPLICATION_MESSAGE_QUERY");
        this.mContext.registerReceiverAsUser(this.mAllUsersReceiver, UserHandle.ALL, allUsersFilter, null, null);
        updateCurrentProfilesCache();
        IVrManagerCompat.registerListener(new IVrManagerCompat.IVrManagerCompatCallbacks() {
            public void onVrStateChanged(boolean enabled) {
                StatusBar.this.mVrMode = enabled;
            }
        });
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        updateHeadsUpSetting();
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mIconController);
        this.mGameHandsFreeObserver.onChange(false);
        this.mGameModeObserver.onChange(false);
        this.mNotificationStyleObserver.onChange(false);
        this.mUserFoldObserver.onChange(false);
        this.mCloudDataObserver.onChange(false);
        this.mScreenButtonStateObserver.onChange(false);
        this.mWakeupForNotificationObserver.onChange(false);
        this.mUserExperienceObserver.onChange(false);
        this.mMiuiOptimizationObserver.onChange(false);
        this.mSilentModeObserverController.addCallback(this);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
        if (Constants.SUPPORT_AOD) {
            this.mDozeServiceHost = new DozeServiceHost(this.mContext);
            putComponent(DozeHost.class, this.mDozeServiceHost);
            Dependency.setHost(this.mDozeServiceHost);
        }
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
        ((ActivityStarterDelegate) Dependency.get(ActivityStarterDelegate.class)).setActivityStarterImpl(this);
        this.mConfigurationListener = new ConfigurationController.ConfigurationListener() {
            public void onConfigChanged(Configuration newConfig) {
                StatusBar.this.onConfigurationChanged(newConfig);
            }

            public void onDensityOrFontScaleChanged() {
                StatusBar.this.onDensityOrFontScaleChanged();
            }
        };
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mConfigurationListener);
        Settings.Global.putInt(this.mResolver, "hide_nav_bar", 0);
        Settings.Global.putInt(this.mResolver, "can_nav_bar_hide", 0);
        Settings.Global.putInt(this.mResolver, "force_immersive_nav_bar", 0);
        Settings.Global.putInt(this.mResolver, "charging_sounds_enabled", 0);
        Settings.Global.putInt(this.mResolver, "music_in_white_list", 0);
        Settings.System.putInt(this.mResolver, "sysui_tuner_demo_on", 0);
        if (Build.VERSION.SDK_INT >= 28 && Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, -2) == -1) {
            int value = Settings.Secure.getIntForUser(this.mResolver, "aod_mode", -1, -2);
            if (value != -1) {
                Settings.Secure.putIntForUser(this.mResolver, "doze_always_on", value, -2);
            }
            Settings.Secure.putIntForUser(this.mResolver, "doze_enabled", 0, -2);
        }
        if (!miui.os.Build.DEVICE.equals("cepheus") || Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_index_update", 0, -2) != 0) {
        } else {
            int oldIndex = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_style_index", -1, -2);
            if (oldIndex > 15 || oldIndex < 0) {
            } else {
                IntentFilter intentFilter = enableNotificationsFilter;
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "aod_style_index", new int[]{3, 2, 6, 5, 1, 4, 7, 8, 9, 10, 0, 11, 12, 13, 14, 15}[oldIndex], -2);
            }
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "aod_index_update", 1, -2);
        }
        if (Settings.Secure.getIntForUser(this.mResolver, "aod_time_update", 0, -2) == 0) {
            if (Settings.Secure.getIntForUser(this.mResolver, AODKeys.AOD_MODE, 0, -2) == 1) {
                Settings.Secure.putIntForUser(this.mResolver, "need_reset_aod_time", 1, -2);
                if (Settings.Secure.getIntForUser(this.mResolver, "aod_mode_time", 0, -2) == 1) {
                    int startTime = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_start", 360, -2);
                    int endTime = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_end", 1440, -2);
                    Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "aod_start", startTime, -2);
                    Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "aod_end", endTime, -2);
                    i = 1;
                } else {
                    i = 1;
                }
            } else {
                i = 1;
                Settings.Secure.putIntForUser(this.mResolver, "aod_mode_time", 1, -2);
            }
            Settings.Secure.putIntForUser(this.mResolver, "aod_time_update", i, -2);
        }
        if (CustomizeUtil.HAS_NOTCH) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_black"), false, this.mForceBlackObserver, -1);
            this.mForceBlackObserver.onChange(false);
        }
        enableNotificaitonIfNeed();
        runInBackground();
        ((Divider) getComponent(Divider.class)).registerDockedStackExistsChangedListener(this.mDockedStackExistsChangedListener);
        if (CustomizeUtil.HAS_NOTCH) {
            ((DisplayManager) this.mContext.getSystemService("display")).registerDisplayListener(new DisplayManager.DisplayListener() {
                public void onDisplayRemoved(int displayId) {
                }

                public void onDisplayChanged(int displayId) {
                    StatusBar.this.mDisplay.getDisplayInfo(StatusBar.this.mInfo);
                    int rotation = StatusBar.this.mInfo.rotation;
                    int height = StatusBar.this.mInfo.logicalHeight;
                    int width = StatusBar.this.mInfo.logicalWidth;
                    if (StatusBar.this.mNotchRotation != rotation || StatusBar.this.mLogicalWidth != width || StatusBar.this.mLogicalHeight != height) {
                        int unused = StatusBar.this.mNotchRotation = rotation;
                        int unused2 = StatusBar.this.mLogicalWidth = width;
                        int unused3 = StatusBar.this.mLogicalHeight = height;
                        StatusBar.this.updateStatusBarPading();
                    }
                }

                public void onDisplayAdded(int displayId) {
                }
            }, this.mHandler);
        }
        this.mMiuiStatusBarPrompt.dealWithRecordState();
        this.mJobHelper.startJob(this.mContext);
        ((ToastOverlayManager) Dependency.get(ToastOverlayManager.class)).setup(this.mContext, getStatusBarWindow());
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        if (!event.inMultiWindow) {
            this.mDockedStackExistsChangedListener.onDockedStackMinimizedChanged(false);
        }
    }

    public final void onBusEvent(ScreenOffEvent event) {
        notifyHeadsUpScreenOff();
        finishBarAnimations();
        resetUserExpandedStates();
        if (!TextUtils.isEmpty(this.mCallState)) {
            this.mMiuiStatusBarPrompt.makeReturnToInCallScreenButtonGone();
        }
        this.mOLEDScreenHelper.stop(false);
    }

    public final void onBusEvent(ScreenOnEvent event) {
        if (!TextUtils.isEmpty(this.mCallState)) {
            if (InCallUtils.isInCallScreenShowing(this.mContext) || InCallUtils.isCallScreenShowing(this.mContext)) {
                this.mCallState = "";
                hideReturnToInCallScreenButton();
            } else {
                this.mMiuiStatusBarPrompt.makeReturnToInCallScreenButtonVisible();
            }
        }
        this.mOLEDScreenHelper.start(true);
    }

    /* access modifiers changed from: private */
    public void changeNavBarViewState() {
        if (this.mIsFsgMode) {
            Log.d("StatusBar", "NOTICE: full screen gesture function close");
            removeNavBarView();
        } else {
            Log.d("StatusBar", "NOTICE: full screen gesture function open");
            addNavBarView();
        }
        updateStatusBarPading();
    }

    public boolean isFullScreenGestureMode() {
        return this.mIsFsgMode;
    }

    private void removeNavBarView() {
        if (this.mNavigationBarView != null && !this.mIsRemoved) {
            this.mIsRemoved = true;
            try {
                this.mWindowManager.removeView(this.mNavigationBarView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addNavBarView() {
        if (this.mIsRemoved) {
            Log.d("StatusBar", "NOTICE: navbarview is removed");
            this.mIsRemoved = false;
            if (this.mNavigationBarView == null) {
                Log.d("StatusBar", "NOTICE: navbarview is nulll");
                return;
            }
            prepareNavigationBarView();
            if (!this.mNavigationBarView.isAttachedToWindow()) {
                try {
                    this.mNavigationBarView.setLayoutDirection(2);
                    this.mWindowManager.addView(this.mNavigationBarView, getNavigationBarLayoutParams());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2019, 8650856, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        this.mNavigationBarView.getRecentsButton().setOnClickListener(this.mRecentsClickListener);
    }

    private void addNavigationBar() {
        if (DEBUG) {
            Log.v("StatusBar", "addNavigationBar: about to add " + this.mNavigationBarView);
        }
        if (this.mNavigationBarView != null) {
            prepareNavigationBarView();
            this.mNavigationBarView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    int[] location = new int[2];
                    StatusBar.this.mNavigationBarView.getLocationOnScreen(location);
                    int unused = StatusBar.this.mNavigationBarYPostion = location[1];
                }
            });
            if (!this.mNavigationBarView.isAttachedToWindow()) {
                try {
                    this.mWindowManager.addView(this.mNavigationBarView, getNavigationBarLayoutParams());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getNavigationBarYPosition() {
        return this.mNavigationBarYPostion;
    }

    /* access modifiers changed from: protected */
    public void onBootCompleted() {
        super.onBootCompleted();
        sBootCompleted = true;
        this.mMiuiVoipManager.init();
        Log.d("StatusBar", "boot complete");
    }

    /* access modifiers changed from: private */
    public Context getContextForUser(int userId) {
        Context contextForUser = this.mContext;
        if (userId < 0) {
            return contextForUser;
        }
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 4, new UserHandle(userId));
        } catch (PackageManager.NameNotFoundException e) {
            return contextForUser;
        }
    }

    private void enableNotificaitonIfNeed() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        Set<String> oldEnabledList = sp.getStringSet("notification_fored_enabled_list", new HashSet());
        Set<String> newEnabledList = new HashSet<>(NotificationFilterHelper.getNotificationForcedEnabledList());
        if (!newEnabledList.equals(oldEnabledList)) {
            sp.edit().putStringSet("notification_fored_enabled_list", newEnabledList).apply();
            newEnabledList.removeAll(oldEnabledList);
            for (String enableNotifications : newEnabledList) {
                NotificationFilterHelper.enableNotifications(this.mContextForUser, enableNotifications, true);
            }
        }
    }

    private void runInBackground() {
        ((PackageScoreCache) Dependency.get(PackageScoreCache.class)).asyncUpdate();
        this.mBgHandler.post(new Runnable() {
            public void run() {
                PushEvents.restoreLocalModel(StatusBar.this.mContext);
            }
        });
    }

    private void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_show_notifications"), false, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_notification_icon"), false, this.mShowNotificationIconObserver, -1);
        if (ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_remote_input"), false, this.mSettingsObserver, -1);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("gb_notification"), false, this.mGameModeObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("gb_handsfree"), false, this.mGameHandsFreeObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_optimization"), false, this.mMiuiOptimizationObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_notification_style"), false, this.mNotificationStyleObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("user_fold"), false, this.mUserFoldObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(CloudDataHelper.URI_CLOUD_ALL_DATA_NOTIFY, false, this.mCloudDataObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("wakeup_for_keyguard_notification"), false, this.mWakeupForNotificationObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("screen_buttons_state"), false, this.mScreenButtonStateObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("sc_status"), false, this.mSliderStatusObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("upload_log_pref"), false, this.mUserExperienceObserver, -1);
    }

    /* access modifiers changed from: protected */
    public void makeStatusBarView() {
        Context context = this.mContext;
        updateDisplaySize();
        updateResources(false);
        loadWhiteList();
        CustomizedUtils.setCustomized(this.mContext);
        TelephonyIcons.initDataTypeName(context);
        inflateStatusBarWindow(context);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(getStatusBarWindowTouchListener());
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R.id.notification_panel);
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R.id.notification_stack_scroller);
        this.mNotificationLogger.setUp(this, this.mNotificationData, this.mStackScroller);
        this.mNotificationPanel.setStatusBar(this);
        this.mNotificationPanel.setGroupManager(this.mGroupManager);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R.id.keyguard_header);
        this.mNotificationIconAreaController = SystemUIFactory.getInstance().createNotificationIconAreaController(context, this);
        inflateShelf();
        this.mNotificationIconAreaController.setupShelf(this.mNotificationShelf);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mNotificationIconAreaController);
        FragmentHostManager.get(this.mStatusBarWindow).addTagListener("CollapsedStatusBarFragment", new FragmentHostManager.FragmentListener() {
            public void onFragmentViewCreated(String tag, Fragment fragment) {
                StatusBar.this.mStatusBarFragment = (CollapsedStatusBarFragment) fragment;
                StatusBar.this.mStatusBarFragment.initNotificationIconArea(StatusBar.this.mNotificationIconAreaController);
                StatusBar.this.mStatusBarView = (PhoneStatusBarView) fragment.getView();
                View unused = StatusBar.this.mNotifications = StatusBar.this.mStatusBarView.findViewById(R.id.notification_icon_area);
                LinearLayout unused2 = StatusBar.this.mDriveModeBg = (LinearLayout) StatusBar.this.mStatusBarView.findViewById(R.id.drivemodebg);
                StatusBar.this.mStatusBarView.setBar(StatusBar.this);
                StatusBar.this.mStatusBarView.setPanel(StatusBar.this.mNotificationPanel);
                StatusBar.this.mStatusBarView.setScrimController(StatusBar.this.mScrimController);
                StatusBar.this.mMiuiStatusBarPrompt.setHandler(StatusBar.this.mStatusBarView.getHandler());
                StatusBar.this.mMiuiStatusBarPrompt.showReturnToDriveModeView(StatusBar.this.mMiuiStatusBarPrompt.isShowingState(2), StatusBar.this.mIsInDriveModeMask);
                if (!TextUtils.isEmpty(StatusBar.this.mCallState)) {
                    StatusBar.this.showReturnToInCallScreenButton(StatusBar.this.mCallState, StatusBar.this.mCallBaseTime);
                }
                StatusBar.this.mStatusBarWindow.setStatusBarView(StatusBar.this.mStatusBarView);
                StatusBar.this.setAreThereNotifications();
                StatusBar.this.checkBarModes();
                StatusBar.this.mShowNotificationIconObserver.onChange(false);
                if (Constants.IS_NOTCH) {
                    StatusBar.this.mStatusBarFragment.setNotch();
                }
                StatusBar.this.updateDriveMode();
                StatusBar.this.updateStatusBarPading();
                StatusBar.this.mOLEDScreenHelper.setStatusBarView(StatusBar.this.mStatusBarView);
            }

            public void onFragmentViewDestroyed(String tag, Fragment fragment) {
            }
        }).getFragmentManager().beginTransaction().replace(R.id.status_bar_container, new CollapsedStatusBarFragment(), "CollapsedStatusBarFragment").commit();
        if (!ActivityManager.isHighEndGfx()) {
            this.mStatusBarWindow.setBackground(null);
            this.mNotificationPanel.setBackground(new FastColorDrawable(context.getColor(17170443)));
        }
        this.mHeadsUpManager = new HeadsUpManager(context, this.mStatusBarWindow, this.mGroupManager);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mHeadsUpManager);
        this.mHeadsUpManager.setBar(this);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mHeadsUpManager.addListener(this.mGroupManager);
        this.mHeadsUpManager.addListener(this.mVisualStabilityManager);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mNotificationData.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setHeadsUpManager(this.mHeadsUpManager);
        this.mHeadsUpManager.setVisualStabilityManager(this.mVisualStabilityManager);
        try {
            boolean showNav = this.mWindowManagerService.hasNavigationBar();
            if (DEBUG) {
                Log.v("StatusBar", "hasNavigationBar=" + showNav);
            }
            if (showNav) {
                createNavigationBar();
                this.mFullScreenGestureListener = new ContentObserver(this.mHandler) {
                    public void onChange(boolean selfChange) {
                        boolean unused = StatusBar.this.mIsFsgMode = MiuiSettings.Global.getBoolean(StatusBar.this.mContext.getContentResolver(), "force_fsg_nav_bar");
                        StatusBar.this.changeNavBarViewState();
                        StatusBar.this.processScreenBtnDisableNotification();
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, this.mFullScreenGestureListener);
                this.mFullScreenGestureListener.onChange(false);
                if (this.mIsFsgMode) {
                    this.mIsRemoved = true;
                } else {
                    addNavigationBar();
                }
            }
        } catch (RemoteException e) {
        }
        this.mPixelFormat = -1;
        this.mStackScroller.setLongPressListener(getNotificationLongClicker());
        this.mStackScroller.setMenuPressListener(getNotificationMenuClicker());
        this.mStackScroller.setStatusBar(this);
        this.mStackScroller.setGroupManager(this.mGroupManager);
        this.mStackScroller.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setOnGroupChangeListener(this.mStackScroller);
        this.mVisualStabilityManager.setVisibilityLocationProvider(this.mStackScroller);
        inflateEmptyShadeView();
        inflateDismissView();
        this.mExpandedContents = this.mStackScroller;
        this.mBackdrop = (BackDropView) this.mStatusBarWindow.findViewById(R.id.backdrop);
        this.mBackdropFront = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_front);
        this.mBackdropBack = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_back);
        this.mKeyguardClock = (KeyguardClockContainer) this.mStatusBarWindow.findViewById(R.id.keyguard_clock_view);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) this.mStatusBarWindow.findViewById(R.id.keyguard_bottom_area);
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, this.mNotificationPanel);
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mLockScreenMagazineController = SystemUIFactory.getInstance().createKeyguardWallpaperCarouselController(this.mContext, this.mNotificationPanel, this);
        this.mNotificationPanel.setKeyguardWallpaperCarouselController(this.mLockScreenMagazineController);
        this.mKeyguardBottomArea.setKeyguardIndicationController(this.mKeyguardIndicationController);
        setAreThereNotifications();
        this.mBatteryController.addCallback(new BatteryController.BatteryStateChangeCallback() {
            public void onPowerSaveChanged(boolean isPowerSave) {
                StatusBar.this.mHandler.post(StatusBar.this.mCheckBarModes);
                if (StatusBar.this.mDozeServiceHost != null) {
                    StatusBar.this.mDozeServiceHost.firePowerSaveChanged(isPowerSave || StatusBar.this.mBatteryController.isExtremePowerSave());
                }
            }

            public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
                StatusBar.this.mHandler.post(StatusBar.this.mCheckBarModes);
                if (StatusBar.this.mDozeServiceHost != null) {
                    StatusBar.this.mDozeServiceHost.firePowerSaveChanged(isExtremePowerSave || StatusBar.this.mBatteryController.isPowerSave());
                }
            }

            public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
                int unused = StatusBar.this.mBatteryLevel = level;
            }

            public void onBatteryStyleChanged(int batteryStyle) {
            }
        });
        this.mLightBarController = new LightBarController();
        this.mLightBarController.mStatusBar = this;
        View headsUpScrim = this.mStatusBarWindow.findViewById(R.id.heads_up_scrim);
        this.mScrimController = SystemUIFactory.getInstance().createScrimController(this.mLightBarController, (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_behind), (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_in_front), headsUpScrim, this.mLockscreenWallpaper);
        if (this.mScrimSrcModeEnabled) {
            Runnable runnable = new Runnable() {
                public void run() {
                    boolean asSrc = StatusBar.this.mBackdrop.getVisibility() != 0;
                    StatusBar.this.mScrimController.setDrawBehindAsSrc(asSrc);
                    StatusBar.this.mStackScroller.setDrawBackgroundAsSrc(asSrc);
                }
            };
            this.mBackdrop.setOnVisibilityChangedRunnable(runnable);
            runnable.run();
        }
        this.mHeadsUpManager.addListener(this.mScrimController);
        this.mStackScroller.setScrimController(this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(this.mScrimController, context);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        this.mKeyguardBottomArea.setStatusBar(this);
        this.mKeyguardBottomArea.setUserSetupComplete(this.mUserSetup);
        if (UserManager.get(this.mContext).isUserSwitcherEnabled()) {
            createUserSwitcher();
        }
        View container = this.mStatusBarWindow.findViewById(R.id.qs_frame);
        if (container != null) {
            FragmentHostManager fragmentHostManager = FragmentHostManager.get(container);
            fragmentHostManager.getFragmentManager().beginTransaction().replace(R.id.qs_frame, new QSFragment(), QS.TAG).commit();
            new PluginFragmentListener(container, QS.TAG, QSFragment.class, QS.class).startListening();
            final QSTileHost qsh = SystemUIFactory.getInstance().createQSTileHost(this.mContext, this, this.mIconController);
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow);
            fragmentHostManager.addTagListener(QS.TAG, new FragmentHostManager.FragmentListener() {
                public void onFragmentViewCreated(String tag, Fragment f) {
                    QS qs = (QS) f;
                    if (qs instanceof QSFragment) {
                        QSFragment qsFragment = (QSFragment) qs;
                        qsFragment.setHost(qsh);
                        qsFragment.setBrightnessMirror(StatusBar.this.mBrightnessMirrorController);
                        StatusBar.this.mQSPanel = qsFragment.getQsPanel();
                        StatusBar.this.mHeaderView = (HeaderView) qsFragment.getHeaderView();
                        StatusBar.this.mHeaderView.themeChanged();
                        StatusBar.this.mHeaderView.regionChanged();
                    }
                }

                public void onFragmentViewDestroyed(String tag, Fragment fragment) {
                }
            });
        }
        this.mReportRejectedTouch = this.mStatusBarWindow.findViewById(R.id.report_rejected_touch);
        if (this.mReportRejectedTouch != null) {
            updateReportRejectedTouchVisibility();
            this.mReportRejectedTouch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Uri session = StatusBar.this.mFalsingManager.reportRejectedTouch();
                    if (session != null) {
                        StringWriter message = new StringWriter();
                        message.write("Build info: ");
                        message.write(SystemProperties.get("ro.build.description"));
                        message.write("\nSerial number: ");
                        message.write(SystemProperties.get("ro.serialno"));
                        message.write("\n");
                        PrintWriter falsingPw = new PrintWriter(message);
                        FalsingLog.dump(falsingPw);
                        falsingPw.flush();
                        StatusBar.this.startActivityDismissingKeyguard(Intent.createChooser(new Intent("android.intent.action.SEND").setType("*/*").putExtra("android.intent.extra.SUBJECT", "Rejected touch report").putExtra("android.intent.extra.STREAM", session).putExtra("android.intent.extra.TEXT", message.toString()), "Share rejected touch report").addFlags(268435456), true, true);
                    }
                }
            });
        }
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (!pm.isScreenOn()) {
            onBusEvent(new ScreenOffEvent());
        }
        this.mGestureWakeLock = pm.newWakeLock(10, "GestureWakeLock");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        int[] pattern = this.mContext.getResources().getIntArray(R.array.config_cameraLaunchGestureVibePattern);
        this.mCameraLaunchGestureVibePattern = new long[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            this.mCameraLaunchGestureVibePattern[i] = (long) pattern[i];
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter internalFilter = new IntentFilter();
        internalFilter.addAction("android.intent.action.LEAVE_INCALL_SCREEN_DURING_CALL");
        internalFilter.addAction("android.intent.action.ENTER_INCALL_SCREEN_DURING_CALL");
        internalFilter.addAction("miui.intent.action.MIUI_REGION_CHANGED");
        internalFilter.addAction("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode");
        internalFilter.addAction("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode");
        internalFilter.addAction("com.miui.app.ExtraStatusBarManager.action_refresh_notification");
        internalFilter.addAction("com.miui.app.ExtraStatusBarManager.action_remove_keyguard_notification");
        context.registerReceiverAsUser(this.mInternalBroadcastReceiver, UserHandle.ALL, internalFilter, "miui.permission.USE_INTERNAL_GENERAL_API", this.mHandler);
        if (DEBUG_MEDIA_FAKE_ARTWORK) {
            IntentFilter demoFilter = new IntentFilter();
            demoFilter.addAction("fake_artwork");
            context.registerReceiverAsUser(this.mFakeArtworkReceiver, UserHandle.ALL, demoFilter, "android.permission.DUMP", null);
        }
        ((DemoModeController) Dependency.get(DemoModeController.class)).addCallback(this.mDemoCallback);
        IntentFilter toggleFilter = new IntentFilter();
        toggleFilter.addAction("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_SCREEN_BUTTONS");
        toggleFilter.addAction("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_LOCK");
        toggleFilter.addAction("com.miui.app.ExtraStatusBarManager.action_TRIGGER_TOGGLE");
        Context context2 = this.mContext;
        context2.registerReceiverAsUser(this.mToggleBroadcastReceiver, UserHandle.ALL, toggleFilter, "com.android.SystemUI.permission.TIGGER_TOGGLE", this.mHandler);
        this.mDeviceProvisionedController.addCallback(this.mUserSetupObserver);
        this.mUserSetupObserver.onUserSetupChanged();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
    }

    /* access modifiers changed from: protected */
    public void createNavigationBar() {
        this.mNavigationBarView = (NavigationBarView) View.inflate(this.mContext, R.layout.navigation_bar, null);
        this.mNavigationBarView.disableChangeBg(CustomizeUtil.forceLayoutHideNavigation(Util.getTopActivityPkg(this.mContext, true)));
        this.mNavigationBarView.setDisabledFlags(this.mDisabled1);
        this.mNavigationBarView.setBar(this);
        this.mOLEDScreenHelper.setNavigationBarView(this.mNavigationBarView);
    }

    /* access modifiers changed from: protected */
    public View.OnTouchListener getStatusBarWindowTouchListener() {
        return new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                StatusBar.this.checkUserAutohide(v, event);
                StatusBar.this.checkRemoteInputOutside(event);
                if (event.getAction() == 0 && StatusBar.this.mExpandedVisible && !StatusBar.this.mNotificationPanel.isQsDetailShowing()) {
                    StatusBar.this.animateCollapsePanels();
                }
                return StatusBar.this.mStatusBarWindow.onTouchEvent(event);
            }
        };
    }

    private void loadWhiteList() {
        this.mSystemForegroundWhitelist = Arrays.asList(this.mContext.getResources().getStringArray(R.array.system_foreground_notification_whitelist));
        this.mEnableFloatNotificationWhitelist = Arrays.asList(this.mContext.getResources().getStringArray(R.array.avoid_disturb_app_whitelist));
    }

    private void inflateShelf() {
        this.mNotificationShelf = (NotificationShelf) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_shelf, this.mStackScroller, false);
        this.mNotificationShelf.setOnActivatedListener(this);
        this.mStackScroller.setShelf(this.mNotificationShelf);
        this.mNotificationShelf.setOnClickListener(this.mGoToLockedShadeListener);
        this.mNotificationShelf.setStatusBarState(this.mState);
        this.mNotificationShelf.setViewType(1);
    }

    /* access modifiers changed from: protected */
    public void onDensityOrFontScaleChanged() {
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSwitchingUser()) {
            updateNotificationsOnDensityOrFontScaleChanged();
        } else {
            this.mReinflateNotificationsOnUserSwitched = true;
        }
        this.mScrimController.onDensityOrFontScaleChanged();
        if (this.mStatusBarView != null) {
            this.mStatusBarView.onDensityOrFontScaleChanged();
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.onDensityOrFontScaleChanged();
        }
        inflateSignalClusters();
        this.mNotificationIconAreaController.onDensityOrFontScaleChanged(this.mContext);
        inflateDismissView();
        updateClearAll();
        inflateEmptyShadeView();
        updateEmptyShadeView();
        this.mStatusBarKeyguardViewManager.onDensityOrFontScaleChanged();
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
        ((UserSwitcherController) Dependency.get(UserSwitcherController.class)).onDensityOrFontScaleChanged();
        if (this.mKeyguardUserSwitcher != null) {
            this.mKeyguardUserSwitcher.onDensityOrFontScaleChanged();
        }
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.refreshView();
        }
    }

    /* access modifiers changed from: private */
    public void updateNotificationsOnDensityOrFontScaleChanged() {
        updateNotificationsOnDensityOrFontScaleChanged(this.mNotificationData.getActiveNotifications());
        updateNotificationsOnDensityOrFontScaleChanged(this.mNotificationData.getFoldEntries());
    }

    private void updateNotificationsOnDensityOrFontScaleChanged(List<NotificationData.Entry> notifications) {
        for (int i = 0; i < notifications.size(); i++) {
            NotificationData.Entry entry = notifications.get(i);
            entry.notification.getNotification().extraNotification.setEnableFloat(false);
            boolean exposedGuts = this.mNotificationGutsExposed != null && entry.row.getGuts() == this.mNotificationGutsExposed;
            entry.row.onDensityOrFontScaleChanged();
            if (exposedGuts) {
                this.mNotificationGutsExposed = entry.row.getGuts();
                this.mNotificationGutsExposed.setExposed(true, false);
                bindGuts(entry.row, this.mGutsMenuItem);
            }
        }
    }

    private void inflateSignalClusters() {
        reinflateSignalCluster(this.mKeyguardStatusBar);
    }

    public static SignalClusterView reinflateSignalCluster(View view) {
        Context context = view.getContext();
        SignalClusterView signalCluster = (SignalClusterView) view.findViewById(R.id.signal_cluster);
        if (signalCluster == null) {
            return null;
        }
        ViewParent parent = signalCluster.getParent();
        if (!(parent instanceof ViewGroup)) {
            return signalCluster;
        }
        ViewGroup viewParent = (ViewGroup) parent;
        int index = viewParent.indexOfChild(signalCluster);
        viewParent.removeView(signalCluster);
        SignalClusterView newCluster = (SignalClusterView) LayoutInflater.from(context).inflate(R.layout.signal_cluster_view, viewParent, false);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewParent.getLayoutParams();
        layoutParams.setMarginsRelative(context.getResources().getDimensionPixelSize(R.dimen.signal_cluster_margin_start), 0, 0, 0);
        newCluster.setLayoutParams(layoutParams);
        viewParent.addView(newCluster, index);
        if (Constants.IS_NOTCH) {
            newCluster.setNotchEar();
        }
        return newCluster;
    }

    /* access modifiers changed from: private */
    public void inflateFoldView() {
        this.mFoldHeaderView = (FoldHeaderView) LayoutInflater.from(this.mContext).inflate(R.layout.fold_header, this.mStackScroller, false);
        this.mFoldHeaderView.setViewType(12);
        this.mFoldHeaderView.setClickListener(new FoldHeaderView.ClickListener() {
            public void onClickTips() {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.putExtra(":android:show_fragment", "com.android.settings.UserFoldFragment");
                intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
                intent.addFlags(32768);
                intent.addFlags(268435456);
                try {
                    StatusBar.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (ActivityNotFoundException e) {
                    Log.e("StatusBar", "Failed startActivityAsUser() ", e);
                }
                StatusBar.this.animateCollapsePanels(0, true);
            }
        });
        this.mFoldHeaderView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                StatusBar.this.closeFold();
            }
        });
        this.mFoldFooterView = (FoldFooterView) LayoutInflater.from(this.mContext).inflate(R.layout.fold_footer, this.mStackScroller, false);
        this.mFoldFooterView.setViewType(13);
        this.mFoldFooterView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                StatusBar.this.openFold();
            }
        });
        this.mStackScroller.setFoldView(this.mFoldHeaderView, this.mFoldFooterView);
    }

    private void inflateEmptyShadeView() {
        this.mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_no_notifications, this.mStackScroller, false);
        this.mEmptyShadeView.setViewType(2);
        this.mStackScroller.setEmptyShadeView(this.mEmptyShadeView);
    }

    private void inflateDismissView() {
        this.mDismissView = (DismissView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_dismiss_all, (ViewGroup) this.mNotificationPanel.findViewById(R.id.notification_container_parent), false);
        this.mDismissView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MetricsLogger.action(StatusBar.this.mContext, 148);
                ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_CLEAR_ALL_NOTI);
                StatusBar.this.clearAllNotifications();
            }
        });
        this.mNotificationPanel.setDismissView(this.mDismissView);
        this.mDismissView.setAccessibilityTraversalAfter(R.id.notification_stack_scroller);
        this.mStackScroller.setAccessibilityTraversalBefore(R.id.dismiss_view);
    }

    /* access modifiers changed from: protected */
    public void createUserSwitcher() {
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R.id.keyguard_user_switcher), this.mKeyguardStatusBar, this.mNotificationPanel);
    }

    /* access modifiers changed from: protected */
    public void inflateStatusBarWindow(Context context) {
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
    }

    public void clearAllNotifications() {
        this.mHasClearAllNotifications = !NotificationUtil.isFold();
        if (this.mHasClearAllNotifications) {
            updateAppBadgeNum(null);
        }
        int numChildren = this.mStackScroller.getChildCount();
        ArrayList<View> viewsToHide = new ArrayList<>(numChildren);
        final ArrayList<ExpandableNotificationRow> viewsToRemove = new ArrayList<>(numChildren);
        for (int i = 0; i < numChildren; i++) {
            View child = this.mStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                boolean parentVisible = false;
                boolean hasClipBounds = child.getClipBounds(this.mTmpRect);
                if (this.mStackScroller.canChildBeDismissed(child)) {
                    viewsToRemove.add(row);
                    if (child.getVisibility() == 0 && (!hasClipBounds || this.mTmpRect.height() > 0)) {
                        viewsToHide.add(child);
                        parentVisible = true;
                    }
                } else if (child.getVisibility() == 0 && (!hasClipBounds || this.mTmpRect.height() > 0)) {
                    parentVisible = true;
                }
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        viewsToRemove.add(childRow);
                        if (parentVisible && row.areChildrenExpanded() && this.mStackScroller.canChildBeDismissed(childRow)) {
                            boolean hasClipBounds2 = childRow.getClipBounds(this.mTmpRect);
                            if (childRow.getVisibility() == 0 && (!hasClipBounds2 || this.mTmpRect.height() > 0)) {
                                viewsToHide.add(childRow);
                            }
                        }
                    }
                }
            }
        }
        if (viewsToRemove.isEmpty() != 0) {
            closeFoldIfNeeded();
        }
        addPostCollapseAction(new Runnable() {
            public void run() {
                ArrayList<ExpandedNotification> clearNotifications = new ArrayList<>();
                StatusBar.this.mStackScroller.setDismissAllInProgress(false);
                Iterator it = viewsToRemove.iterator();
                while (it.hasNext()) {
                    ExpandableNotificationRow rowToRemove = (ExpandableNotificationRow) it.next();
                    if (StatusBar.this.mStackScroller.canChildBeDismissed(rowToRemove)) {
                        clearNotifications.add(rowToRemove.getEntry().notification);
                        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onRemoveAll(rowToRemove.getStatusBarNotification());
                        StatusBar.this.removeNotification(rowToRemove.getEntry().key, null);
                    } else {
                        rowToRemove.resetTranslation();
                    }
                }
                ArrayList<NotificationData.Entry> foldEntries = StatusBar.this.mNotificationData.getFoldEntries();
                int numChildren = foldEntries.size();
                int clearableFoldEntriesNum = 0;
                for (int i = 0; i < numChildren; i++) {
                    NotificationData.Entry entry = foldEntries.get(i);
                    if (entry.row != null && entry.row.isClearable()) {
                        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onRemoveAll(foldEntries.get(i).notification);
                        clearableFoldEntriesNum++;
                    }
                }
                AnalyticsHelper.trackNotificationClearAll(clearNotifications.size() + clearableFoldEntriesNum);
                if (StatusBar.this.mHasClearAllNotifications) {
                    try {
                        StatusBar.this.mBarService.onClearAllNotifications(StatusBar.this.mCurrentUserId);
                    } catch (Exception e) {
                    }
                } else {
                    Iterator<ExpandedNotification> it2 = clearNotifications.iterator();
                    while (it2.hasNext()) {
                        StatusBar.this.onNotificationClear(it2.next());
                    }
                }
                if (StatusBar.this.mNotificationData.getActiveNotifications().size() == 0) {
                    StatusBar.this.mStackScroller.doExpandCollapseAnimation(false, 1500);
                }
            }
        });
        performDismissAllAnimations(viewsToHide);
    }

    private void performDismissAllAnimations(ArrayList<View> hideAnimatedList) {
        Runnable animationFinishAction = new Runnable() {
            public void run() {
                StatusBar.this.closeFoldIfNeeded();
            }
        };
        this.mStackScroller.setDismissAllInProgress(true);
        List<View> realHideAnimatedList = new ArrayList<>();
        Iterator<View> it = hideAnimatedList.iterator();
        while (it.hasNext()) {
            View v = it.next();
            if ((v instanceof ExpandableNotificationRow) && this.mStackScroller.isInUserVisibleArea((ExpandableNotificationRow) v)) {
                realHideAnimatedList.add(v);
            }
        }
        if (realHideAnimatedList.size() == 0 && hideAnimatedList.size() > 0) {
            realHideAnimatedList.add(hideAnimatedList.get(0));
        }
        Log.i("StatusBar", String.format("ignored %d rows when dismiss all", new Object[]{Integer.valueOf(hideAnimatedList.size() - realHideAnimatedList.size())}));
        this.mStackScroller.dispatchDismissAllToChild(realHideAnimatedList, animationFinishAction);
    }

    /* access modifiers changed from: protected */
    public void setZenMode(int mode) {
        if (isDeviceProvisioned()) {
            this.mZenMode = mode;
            updateNotifications();
        }
    }

    /* access modifiers changed from: protected */
    public void startKeyguard() {
        Trace.beginSection("StatusBar#startKeyguard");
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        KeyguardViewMediator keyguardViewMediator2 = keyguardViewMediator;
        FingerprintUnlockController fingerprintUnlockController = new FingerprintUnlockController(this.mContext, this.mDozeScrimController, keyguardViewMediator2, this.mScrimController, this, UnlockMethodCache.getInstance(this.mContext));
        this.mFingerprintUnlockController = fingerprintUnlockController;
        FaceUnlockController faceUnlockController = new FaceUnlockController(this.mContext, this.mDozeScrimController, keyguardViewMediator2, this.mScrimController, this, UnlockMethodCache.getInstance(this.mContext));
        this.mFaceUnlockController = faceUnlockController;
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, getBouncerContainer(), this.mScrimController, this.mFingerprintUnlockController, this.mFaceUnlockController);
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mKeyguardIndicationController.setUserInfoController((UserInfoController) Dependency.get(UserInfoController.class));
        this.mFingerprintUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mFaceUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputController.addCallback(this.mStatusBarKeyguardViewManager);
        this.mLockScreenMagazineController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputController.addCallback(new RemoteInputController.Callback() {
            public void onRemoteInputSent(final NotificationData.Entry entry) {
                if (StatusBar.FORCE_REMOTE_INPUT_HISTORY && StatusBar.this.mKeysKeptForRemoteInput.contains(entry.key)) {
                    StatusBar.this.removeNotification(entry.key, null);
                } else if (StatusBar.this.mRemoteInputEntriesToRemoveOnCollapse.contains(entry)) {
                    StatusBar.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (StatusBar.this.mRemoteInputEntriesToRemoveOnCollapse.remove(entry)) {
                                StatusBar.this.removeNotification(entry.key, null);
                            }
                        }
                    }, 200);
                }
            }

            public void onRemoteInputActive(boolean active) {
            }
        });
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mLightBarController.setFingerprintUnlockController(this.mFingerprintUnlockController);
        Trace.endSection();
    }

    /* access modifiers changed from: protected */
    public View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    /* access modifiers changed from: protected */
    public ViewGroup getBouncerContainer() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(17105351);
        }
        return this.mNaturalBarHeight;
    }

    /* access modifiers changed from: protected */
    public boolean toggleSplitScreenMode(int metricsDockAction, int metricsUndockAction) {
        if (this.mRecents == null) {
            return false;
        }
        if (WindowManagerProxy.getInstance().getDockSide() == -1) {
            return this.mRecents.dockTopTask(-1, 0, null, metricsDockAction);
        }
        Divider divider = (Divider) getComponent(Divider.class);
        if (divider != null && divider.isMinimized()) {
            return false;
        }
        RecentsEventBus.getDefault().send(new UndockingTaskEvent());
        if (metricsUndockAction != -1) {
            MetricsLogger.action(this.mContext, metricsUndockAction);
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void awakenDreams() {
        SystemServicesProxy.getInstance(this.mContext).awakenDreamsAsync();
    }

    private void wakeUpForNotification(NotificationData.Entry entry) {
        if (this.mWakeupForNotification && entry.notification.isClearable() && shouldShowOnKeyguard(entry)) {
            if ((this.mDeviceInteractive && !this.mDozing) || this.mIsDNDEnabled) {
                return;
            }
            if (!KeyguardUpdateMonitor.getInstance(this.mContext).isPsensorDisabled()) {
                KeyguardUpdateMonitor.getInstance(this.mContext).registerSeneorsForKeyguard(new KeyguardUpdateMonitor.SensorsChangeCallback() {
                    public void onChange(boolean isInSuspectMode) {
                        if (!isInSuspectMode) {
                            StatusBar.this.wakeUpForNotificationInternal();
                        } else {
                            Log.e("miui_keyguard", "not wake up for notification because in suspect mode");
                        }
                        KeyguardUpdateMonitor.getInstance(StatusBar.this.mContext).unregisterSeneorsForKeyguard();
                    }
                });
            } else if (!MiuiKeyguardUtils.isNonUI()) {
                wakeUpForNotificationInternal();
            } else {
                Log.e("miui_keyguard", "not wake up for notification in nonui mode");
            }
        }
    }

    /* access modifiers changed from: private */
    public void wakeUpForNotificationInternal() {
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:NOTIFICATION");
        KeyguardUpdateMonitor.sWakeupByNotification = true;
        com.android.keyguard.analytics.AnalyticsHelper.record("keyguard_screenon_by_notification");
    }

    public void addNotification(ExpandedNotification notification, NotificationListenerService.RankingMap ranking) throws InflationException {
        String key = notification.getKey();
        if (!filterNotification(notification)) {
            Log.d("StatusBar", "addNotification key=" + key);
            this.mHasClearAllNotifications = false;
            wrapNotificationEvent(notification, null);
            this.mNotificationData.updateRanking(ranking);
            NotificationData.Entry shadeEntry = createNotificationViews(notification);
            shadeEntry.hideSensitiveByAppLock = isHideSensitiveByAppLock(notification);
            shadeEntry.needUpdateBadgeNum = NotificationUtil.needStatBadgeNum(notification);
            boolean isHeadsUped = shouldPeek(shadeEntry);
            Logger.fullI("StatusBar", "addNotification isHeadsUped:" + isHeadsUped);
            if (!isHeadsUped && notification.getNotification().fullScreenIntent != null) {
                if (shouldSuppressFullScreenIntent(key)) {
                    Logger.fullI("StatusBar", "No Fullscreen intent: suppressed by DND: " + key);
                } else if (this.mNotificationData.getImportance(shadeEntry) < 4) {
                    Logger.fullI("StatusBar", "No Fullscreen intent: not important enough: " + key);
                } else {
                    awakenDreams();
                    Logger.fullI("StatusBar", "Notification has fullScreenIntent; sending fullScreenIntent: " + key);
                    try {
                        EventLog.writeEvent(36002, key);
                        notification.getNotification().fullScreenIntent.send();
                        shadeEntry.notifyFullScreenIntentLaunched();
                        MetricsLogger.count(this.mContext, "note_fullscreen", 1);
                    } catch (PendingIntent.CanceledException e) {
                        Logger.fullE("StatusBar", "throw exception when sending full screen intent" + e);
                    }
                }
            }
            Logger.fullI("StatusBar", "addNotification key=" + key);
            abortExistingInflation(key);
            this.mForegroundServiceController.addNotification(notification, this.mNotificationData.getImportance(shadeEntry));
            this.mPendingNotifications.put(key, shadeEntry);
            wakeUpForNotification(shadeEntry);
            LocalAlgoModel.updateLocalModelIfNeed(this.mContext, notification, this.mBgHandler);
            LocalAlgoModel.uploadLocalAlgoModelIfNeed(this.mContext, this.mBgHandler);
            ((NotificationsMonitor) Dependency.get(NotificationsMonitor.class)).notifyNotificationAdded(notification);
        }
    }

    private boolean isHideSensitiveByAppLock(ExpandedNotification notification) {
        return AppLockHelper.shouldShowPublicNotificationByAppLock(this.mContext, this.mSecurityManager, notification.getPackageName(), AppLockHelper.getCurrentUserIdIfNeeded(notification.getUserId(), this.mCurrentUserId));
    }

    private boolean filterNotification(ExpandedNotification notification) {
        boolean isFilter = false;
        if (!NotificationUtil.canSendNotificationForTargetPkg(notification)) {
            notification.getNotification().extraNotification.setTargetPkg(null);
        }
        if (getAppNotificationFlag(notification, false) == 3) {
            isFilter = true;
        } else if ((notification.getNotification().flags & 64) != 0 && this.mSystemForegroundWhitelist.contains(notification.getPackageName())) {
            isFilter = true;
        } else if (((UsbNotificationController) Dependency.get(UsbNotificationController.class)).needDisableUsbNotification(notification)) {
            isFilter = true;
        } else if (notification != null && ((notification.getPackageName().equalsIgnoreCase("com.mediatek.selfregister") || notification.getPackageName().equalsIgnoreCase("com.mediatek.deviceregister")) && (notification.getNotification().flags & 268435456) != 0)) {
            isFilter = true;
        }
        if (isFilter) {
            onNotificationClear(notification);
        }
        if (isFilter) {
            Log.d("StatusBar", String.format("filter Notification key=%s", new Object[]{notification.getKey()}));
        }
        return isFilter;
    }

    private int getAppNotificationFlag(ExpandedNotification notification, boolean useBasePkg) {
        String channelId = NotificationCompat.getChannelId(notification.getNotification());
        if (TextUtils.isEmpty(channelId)) {
            return NotificationFilterHelper.getAppFlag(this.mContextForUser, notification.getPackageName(), NotificationUtil.getUid(notification), true ^ TextUtils.isEmpty(notification.getNotification().extraNotification.getTargetPkg()));
        }
        return NotificationFilterHelper.getChannelFlag(this.mContextForUser, useBasePkg && !"android".equals(notification.getNotification().extraNotification.getTargetPkg()) ? notification.getBasePkg() : notification.getPackageName(), channelId, NotificationUtil.getUid(notification), true ^ TextUtils.isEmpty(notification.getNotification().extraNotification.getTargetPkg()));
    }

    private void wrapNotificationEvent(ExpandedNotification notification, ExpandedNotification oldNotification) {
        NotificationEvent event = notification.getNotificationEvent();
        event.setStatusBar(this);
        event.setGroupManager(this.mGroupManager);
        if (oldNotification == null) {
            event.setCreateTimeStamp(System.currentTimeMillis());
            return;
        }
        event.setCreateTimeStamp(oldNotification.getNotificationEvent().getCreateTimeStamp());
        event.setVersion(oldNotification.getNotificationEvent().getVersion() + 1);
    }

    private void abortExistingInflation(String key) {
        if (this.mPendingNotifications.containsKey(key)) {
            this.mPendingNotifications.get(key).abortTask();
            this.mPendingNotifications.remove(key);
        }
        NotificationData.Entry addedEntry = this.mNotificationData.get(key);
        if (addedEntry != null) {
            addedEntry.abortTask();
        }
    }

    private void addEntry(NotificationData.Entry shadeEntry) {
        if (shouldPeek(shadeEntry)) {
            this.mHeadsUpManager.showNotification(shadeEntry);
            setNotificationShown(shadeEntry.notification);
        }
        addNotificationViews(shadeEntry);
        setAreThereNotifications();
    }

    public void handleInflationException(StatusBarNotification notification, Exception e) {
        handleNotificationError(notification, e.getMessage());
    }

    public void onAsyncInflationFinished(NotificationData.Entry entry) {
        this.mPendingNotifications.remove(entry.key);
        boolean isNew = this.mNotificationData.get(entry.key) == null;
        if (isNew && !entry.row.isRemoved()) {
            addEntry(entry);
            if (entry.notification != null && !entry.notification.isFold()) {
                this.mBgHandler.obtainMessage(2001, entry.notification.getKey()).sendToTarget();
            }
        } else if (!isNew && entry.row.hasLowPriorityStateUpdated()) {
            this.mVisualStabilityManager.onLowPriorityUpdated(entry);
            updateNotificationShade();
        }
        entry.row.setLowPriorityStateUpdated(false);
        if (entry.needUpdateBadgeNum) {
            updateAppBadgeNum(entry.notification);
        }
        if (!isNew) {
            updateHeadsUp(entry.key, entry, shouldPeek(entry), alertAgain(entry, entry.notification.getNotification()));
        }
        if (isKeyguardShowing() && shouldShowOnKeyguard(entry)) {
            if (isNew) {
                this.mKeyguardNotificationHelper.add(entry);
            } else {
                this.mKeyguardNotificationHelper.update(entry);
            }
        }
    }

    public void updateStatusBarPading() {
        if (this.mStatusBarView != null) {
            int paddingStart = 0;
            int paddingEnd = 0;
            if (CustomizeUtil.HAS_NOTCH) {
                int rotation = this.mDisplay.getRotation();
                if (rotation == 1) {
                    paddingStart = DisplayCutoutCompat.getSafeInsetLeft(this, this.mInfo);
                } else if (rotation == 3) {
                    paddingEnd = DisplayCutoutCompat.getSafeInsetRight(this, this.mInfo);
                }
            }
            this.mStatusBarView.setPadding(paddingStart, 0, paddingEnd, 0);
        }
    }

    private boolean shouldSuppressFullScreenIntent(String key) {
        if (isDeviceInVrMode() || isVrMode()) {
            return true;
        }
        if (this.mPowerManager.isInteractive()) {
            return this.mNotificationData.shouldSuppressScreenOn(key);
        }
        return this.mNotificationData.shouldSuppressScreenOff(key);
    }

    /* access modifiers changed from: protected */
    public void updateNotificationRanking(NotificationListenerService.RankingMap ranking) {
        Log.d("StatusBar", "updateNotificationRanking");
        this.mNotificationData.updateRanking(ranking);
        updateNotificationViewsOnly();
    }

    /* access modifiers changed from: private */
    public void updateNotificationRankingDelayed(NotificationListenerService.RankingMap ranking, long messageReceiveTime) {
        Log.d("StatusBar", "updateNotificationRankingDelayed messageReceiveTime=" + messageReceiveTime);
        if (this.mNotificationData.updateRankingDelayed(ranking, messageReceiveTime)) {
            updateNotificationViewsOnly();
        }
    }

    public void removeNotification(String key, NotificationListenerService.RankingMap ranking) {
        CharSequence[] newHistory;
        boolean deferRemoval = false;
        abortExistingInflation(key);
        if (this.mHeadsUpManager.isHeadsUp(key)) {
            deferRemoval = !this.mHeadsUpManager.removeNotification(key, true);
        }
        if (key.equals(this.mMediaNotificationKey)) {
            clearCurrentMediaNotification();
            updateMediaMetaData(true, true);
        }
        String pkg = null;
        if (FORCE_REMOTE_INPUT_HISTORY && this.mRemoteInputController.isSpinning(key)) {
            NotificationData.Entry entry = this.mNotificationData.get(key);
            StatusBarNotification sbn = entry.notification;
            Notification.Builder b = NotificationCompat.recoverBuilder(this.mContext, sbn.getNotification().clone());
            CharSequence[] oldHistory = sbn.getNotification().extras.getCharSequenceArray("android.remoteInputHistory");
            if (oldHistory == null) {
                newHistory = new CharSequence[1];
            } else {
                newHistory = new CharSequence[(oldHistory.length + 1)];
                for (int i = 0; i < oldHistory.length; i++) {
                    newHistory[i + 1] = oldHistory[i];
                }
            }
            newHistory[0] = String.valueOf(entry.remoteInputText);
            NotificationCompat.setRemoteInputHistory(b, newHistory);
            Notification newNotification = b.build();
            newNotification.contentView = sbn.getNotification().contentView;
            newNotification.bigContentView = sbn.getNotification().bigContentView;
            newNotification.headsUpContentView = sbn.getNotification().headsUpContentView;
            boolean updated = handleNotification(sbn, null, true);
            if (!updated) {
                deferRemoval = false;
            }
            if (updated) {
                this.mKeysKeptForRemoteInput.add(entry.key);
                return;
            }
        }
        if (deferRemoval) {
            this.mLatestRankingMap = ranking;
            this.mHeadsUpEntriesToRemoveOnSwitch.add(this.mHeadsUpManager.getEntry(key));
            return;
        }
        NotificationData.Entry entry2 = this.mNotificationData.get(key);
        if (entry2 != null) {
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onVisibilityChanged(entry2, false);
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).postExposeEventIfNeed(entry2);
        }
        if (entry2 == null || !this.mRemoteInputController.isRemoteInputActive(entry2) || entry2.row == null || entry2.row.isDismissed()) {
            if (entry2 != null) {
                this.mForegroundServiceController.removeNotification(entry2.notification);
            }
            if (!(entry2 == null || entry2.row == null)) {
                entry2.row.setRemoved();
                this.mStackScroller.cleanUpViewState(entry2.row);
            }
            handleGroupSummaryRemoved(key, ranking);
            ExpandedNotification old = removeNotificationViews(key, ranking);
            Log.d("StatusBar", "removeNotification key=" + key + " old=" + old);
            if (old != null) {
                if (!this.mHasClearAllNotifications && !old.isFold()) {
                    updateAppBadgeNum(old);
                }
                if (!hasActiveNotifications() && NotificationUtil.isFold()) {
                    closeFold();
                }
            }
            setAreThereNotifications();
            if (isKeyguardShowing()) {
                if (entry2 != null) {
                    pkg = entry2.notification.getPackageName();
                }
                this.mKeyguardNotificationHelper.remove(key.hashCode(), pkg);
            }
            return;
        }
        this.mLatestRankingMap = ranking;
        this.mRemoteInputEntriesToRemoveOnCollapse.add(entry2);
    }

    private void handleGroupSummaryRemoved(String key, NotificationListenerService.RankingMap ranking) {
        NotificationData.Entry entry = this.mNotificationData.get(key);
        if (entry != null && entry.row != null && entry.row.isSummaryWithChildren() && (StatusBarNotificationCompat.getOverrideGroupKey(entry.notification) == null || entry.row.isDismissed())) {
            List<ExpandableNotificationRow> notificationChildren = entry.row.getNotificationChildren();
            ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow row = notificationChildren.get(i);
                if ((row.getStatusBarNotification().getNotification().flags & 64) == 0) {
                    toRemove.add(row);
                    row.setKeepInParent(true);
                    row.setRemoved();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void performRemoveNotification(ExpandedNotification n) {
        NotificationData.Entry entry = this.mNotificationData.get(n.getKey());
        if (this.mRemoteInputController.isRemoteInputActive(entry)) {
            this.mRemoteInputController.removeRemoteInput(entry, null);
        }
        onNotificationClear(n);
        if (FORCE_REMOTE_INPUT_HISTORY && this.mKeysKeptForRemoteInput.contains(n.getKey())) {
            this.mKeysKeptForRemoteInput.remove(n.getKey());
        }
        removeNotification(n.getKey(), null);
    }

    /* access modifiers changed from: private */
    public void updateNotificationShade() {
        boolean deviceSensitive;
        if (this.mStackScroller != null) {
            if (isCollapsing()) {
                addPostCollapseAction(new Runnable() {
                    public void run() {
                        StatusBar.this.updateNotificationShade();
                    }
                });
                return;
            }
            ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            ArrayList<ExpandableNotificationRow> toShow = new ArrayList<>(activeNotifications.size());
            int N = activeNotifications.size();
            int i = 0;
            while (true) {
                deviceSensitive = true;
                if (i >= N) {
                    break;
                }
                NotificationData.Entry ent = activeNotifications.get(i);
                if (!ent.row.isDismissed() && !ent.row.isRemoved()) {
                    int userId = ent.notification.getUserId();
                    boolean devicePublic = isLockscreenPublicMode(this.mCurrentUserId);
                    boolean userPublic = devicePublic || isLockscreenPublicMode(userId);
                    boolean needsRedaction = needsRedaction(ent);
                    boolean hideSensitive = userPublic && needsRedaction;
                    if (ent.hideSensitive != hideSensitive) {
                        ent.hideSensitive = hideSensitive;
                        if (isKeyguardShowing() && shouldShowOnKeyguard(ent)) {
                            this.mKeyguardNotificationHelper.update(ent);
                        }
                    }
                    boolean sensitive = hideSensitive || ent.hideSensitiveByAppLock;
                    if (!devicePublic || userAllowsPrivateNotificationsInPublic(this.mCurrentUserId)) {
                        deviceSensitive = false;
                    }
                    if (sensitive) {
                        updatePublicContentView(ent);
                    }
                    ent.row.setSensitive(sensitive, deviceSensitive);
                    ent.row.setNeedsRedaction(needsRedaction);
                    if (this.mGroupManager.isChildInGroupWithSummary(ent.row.getStatusBarNotification())) {
                        ExpandableNotificationRow summary = this.mGroupManager.getGroupSummary((StatusBarNotification) ent.row.getStatusBarNotification());
                        List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(summary);
                        if (orderedChildren == null) {
                            orderedChildren = new ArrayList<>();
                            this.mTmpChildOrderMap.put(summary, orderedChildren);
                        }
                        orderedChildren.add(ent.row);
                    } else {
                        toShow.add(ent.row);
                    }
                }
                i++;
            }
            ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>();
            for (int i2 = 0; i2 < this.mStackScroller.getChildCount(); i2++) {
                View child = this.mStackScroller.getChildAt(i2);
                if (!toShow.contains(child) && (child instanceof ExpandableNotificationRow)) {
                    toRemove.add((ExpandableNotificationRow) child);
                }
            }
            Iterator<ExpandableNotificationRow> it = toRemove.iterator();
            while (it.hasNext()) {
                ExpandableNotificationRow remove = it.next();
                if (this.mGroupManager.isChildInGroupWithSummary(remove.getStatusBarNotification())) {
                    this.mStackScroller.setChildTransferInProgress(true);
                }
                if (remove.isSummaryWithChildren() && remove.isGroupExpanded()) {
                    remove.getExpandClickListener().onClick(remove);
                }
                this.mStackScroller.removeView(remove);
                this.mStackScroller.setChildTransferInProgress(false);
            }
            removeNotificationChildren();
            for (int i3 = 0; i3 < toShow.size(); i3++) {
                View v = toShow.get(i3);
                if (v.getParent() == null) {
                    this.mVisualStabilityManager.notifyViewAddition(v);
                    this.mStackScroller.addView(v);
                }
            }
            addNotificationChildrenAndSort();
            changeViewPosition(true);
            int j = 0;
            for (int i4 = 0; i4 < this.mStackScroller.getChildCount(); i4++) {
                View child2 = this.mStackScroller.getChildAt(i4);
                if (child2 instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow targetChild = toShow.get(j);
                    if (child2 != targetChild) {
                        if (this.mVisualStabilityManager.canReorderNotification(targetChild)) {
                            this.mStackScroller.changeViewPosition(targetChild, i4);
                        } else {
                            this.mVisualStabilityManager.addReorderingAllowedCallback(this);
                        }
                    }
                    j++;
                }
            }
            List<ExpandableNotificationRow> visibleRows = new ArrayList<>();
            for (int i5 = 0; i5 < this.mStackScroller.getChildCount(); i5++) {
                View child3 = this.mStackScroller.getChildAt(i5);
                if ((child3 instanceof ExpandableNotificationRow) && child3.getVisibility() != 8) {
                    visibleRows.add((ExpandableNotificationRow) child3);
                }
            }
            int i6 = 0;
            while (i6 < visibleRows.size()) {
                ExpandableNotificationRow row = visibleRows.get(i6);
                row.setIsFirstRow(i6 == 0);
                ExpandableNotificationRow expandableNotificationRow = null;
                row.setHasExtraTopPadding(needExtraTopPadding(row, i6 == 0 ? null : visibleRows.get(i6 - 1)));
                if (i6 != visibleRows.size() - 1) {
                    expandableNotificationRow = visibleRows.get(i6 + 1);
                }
                row.setHasExtraBottomPadding(needExtraBottomPadding(row, expandableNotificationRow));
                row.setNeedDrawBgDivider(i6 != 0 && !visibleRows.get(i6 + -1).isMediaNotification(), i6 != visibleRows.size() - 1);
                i6++;
            }
            this.mVisualStabilityManager.onReorderingFinished();
            this.mTmpChildOrderMap.clear();
            updateRowStates();
            changeViewPosition(false);
            updateSpeedBumpIndex();
            updateClearAll();
            updateEmptyShadeView();
            KeyguardClockContainer keyguardClockContainer = this.mKeyguardClock;
            boolean z = !this.mNotificationPanel.isNoVisibleNotifications();
            if (this.mState != 1) {
                deviceSensitive = false;
            }
            keyguardClockContainer.updateClockView(z, deviceSensitive);
            updateQsExpansionEnabled();
            this.mNotificationIconAreaController.updateNotificationIcons(this.mNotificationData);
        }
    }

    private boolean needExtraTopPadding(ExpandableNotificationRow row, ExpandableNotificationRow lastRow) {
        if (row.isMediaNotification()) {
            return false;
        }
        if (!row.isCustomViewNotification() && lastRow != null && !lastRow.isMediaNotification()) {
            return false;
        }
        return true;
    }

    private boolean needExtraBottomPadding(ExpandableNotificationRow row, ExpandableNotificationRow nextRow) {
        if (row.isMediaNotification()) {
            return false;
        }
        if (!row.isCustomViewNotification() && nextRow != null && !nextRow.isMediaNotification()) {
            return false;
        }
        return true;
    }

    private void changeViewPosition(boolean top) {
        if (!top) {
            this.mStackScroller.changeViewPosition(this.mEmptyShadeView, this.mStackScroller.getChildCount() - 1);
            this.mStackScroller.changeViewPosition(this.mNotificationShelf, this.mStackScroller.getChildCount() - 2);
            if (NotificationUtil.isUserFold()) {
                this.mStackScroller.changeViewPosition(this.mFoldFooterView, this.mStackScroller.getChildCount() - 3);
            }
        } else if (NotificationUtil.isUserFold()) {
            this.mStackScroller.changeViewPosition(this.mFoldHeaderView, 0);
        }
    }

    private boolean needsRedaction(NotificationData.Entry ent) {
        boolean redactedLockscreen = (userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) ^ true) || (userAllowsPrivateNotificationsInPublic(ent.notification.getUserId()) ^ true);
        boolean notificationRequestsRedaction = ent.notification.getNotification().visibility == 0;
        if (packageHasVisibilityOverride(ent.notification.getKey())) {
            return true;
        }
        if (!notificationRequestsRedaction || !redactedLockscreen) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void updateQsExpansionEnabled() {
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        boolean z = true;
        if (!isDeviceProvisioned() || ((!this.mUserSetup && this.mUserSwitcherController != null && this.mUserSwitcherController.isSimpleUserSwitcher()) || (this.mDisabled2 & 4) != 0 || (this.mDisabled2 & 1) != 0 || this.mDozing || ONLY_CORE_APPS)) {
            z = false;
        }
        notificationPanelView.setQsExpansionEnabled(z);
    }

    private void addNotificationChildrenAndSort() {
        boolean orderChanged = false;
        for (int i = 0; i < this.mStackScroller.getChildCount(); i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow parent = (ExpandableNotificationRow) view;
                List<ExpandableNotificationRow> children = parent.getNotificationChildren();
                List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(parent);
                int childIndex = 0;
                while (orderedChildren != null && childIndex < orderedChildren.size()) {
                    ExpandableNotificationRow childView = orderedChildren.get(childIndex);
                    if (children == null || !children.contains(childView)) {
                        if (childView.getParent() != null) {
                            Log.wtf("StatusBar", "trying to add a notification child that already has a parent. class:" + childView.getParent().getClass() + "\n child: " + childView);
                            ((ViewGroup) childView.getParent()).removeView(childView);
                        }
                        this.mVisualStabilityManager.notifyViewAddition(childView);
                        parent.addChildNotification(childView, childIndex);
                        this.mStackScroller.notifyGroupChildAdded(childView);
                    }
                    childIndex++;
                }
                orderChanged |= parent.applyChildOrder(orderedChildren, this.mVisualStabilityManager, this);
            }
        }
        if (orderChanged) {
            this.mStackScroller.generateChildOrderChangedEvent();
        }
    }

    private void removeNotificationChildren() {
        ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>();
        for (int i = 0; i < this.mStackScroller.getChildCount(); i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow parent = (ExpandableNotificationRow) view;
                List<ExpandableNotificationRow> children = parent.getNotificationChildren();
                List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(parent);
                if (children != null) {
                    toRemove.clear();
                    for (ExpandableNotificationRow childRow : children) {
                        if ((orderedChildren == null || !orderedChildren.contains(childRow)) && !childRow.keepInParent()) {
                            toRemove.add(childRow);
                        }
                    }
                    Iterator<ExpandableNotificationRow> it = toRemove.iterator();
                    while (it.hasNext()) {
                        ExpandableNotificationRow remove = it.next();
                        parent.removeChildNotification(remove);
                        if (this.mNotificationData.get(remove.getStatusBarNotification().getKey()) == null) {
                            this.mStackScroller.notifyGroupChildRemoved(remove, parent.getChildrenContainer());
                        }
                    }
                }
            }
        }
    }

    public void addQsTile(ComponentName tile) {
        this.mQSPanel.getHost().addTile(tile);
    }

    public void remQsTile(ComponentName tile) {
        this.mQSPanel.getHost().removeTile(tile);
    }

    public void clickTile(ComponentName tile) {
        this.mQSPanel.clickTile(tile);
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

    private boolean packageHasVisibilityOverride(String key) {
        return this.mNotificationData.getVisibilityOverride(key) == 0;
    }

    public void updateClearAll() {
        boolean showDismissView = true;
        if (this.mState == 1 || !this.mExpandedVisible || this.mHeadsUpManager.hasPinnedHeadsUp() || this.mHeadsUpManager.isHeadsUpGoingAway() || !hasActiveClearableNotifications() || this.mNotificationPanel.isQsDetailShowing()) {
            showDismissView = false;
        }
        this.mNotificationPanel.updateDismissView(showDismissView);
    }

    private boolean hasActiveClearableNotifications() {
        int childCount = this.mStackScroller.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.mStackScroller.getChildAt(i);
            if ((child instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) child).canViewBeDismissed()) {
                return true;
            }
        }
        return hasActiveClearableFoldNotifications();
    }

    private boolean hasActiveClearableFoldNotifications() {
        ArrayList<NotificationData.Entry> foldEntries = this.mNotificationData.getFoldEntries();
        int numChildren = foldEntries.size();
        for (int i = 0; i < numChildren; i++) {
            NotificationData.Entry entry = foldEntries.get(i);
            if (entry.row != null && entry.row.isClearable()) {
                return true;
            }
        }
        return false;
    }

    private void updateEmptyShadeView() {
        boolean z = false;
        boolean showEmptyShadeView = this.mState != 1 && this.mNotificationData.getActiveNotifications().size() == 0;
        if (NotificationUtil.isUserFold()) {
            if (this.mFoldHeaderView.getVisibility() == 8 && this.mFoldFooterView.getVisibility() == 8) {
                z = true;
            }
            showEmptyShadeView &= z;
        }
        this.mNotificationPanel.showEmptyShadeView(showEmptyShadeView);
    }

    private void updateSpeedBumpIndex() {
        int N = this.mStackScroller.getChildCount();
        boolean noAmbient = false;
        int currentIndex = 0;
        int speedBumpIndex = 0;
        for (int i = 0; i < N; i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view.getVisibility() != 8 && (view instanceof ExpandableNotificationRow)) {
                currentIndex++;
                if (!this.mNotificationData.isAmbient(((ExpandableNotificationRow) view).getStatusBarNotification().getKey())) {
                    speedBumpIndex = currentIndex;
                }
            }
        }
        if (speedBumpIndex == N) {
            noAmbient = true;
        }
        this.mStackScroller.updateSpeedBumpIndex(speedBumpIndex, noAmbient);
    }

    public static boolean isTopLevelChild(NotificationData.Entry entry) {
        return entry.row.getParent() instanceof NotificationStackScrollLayout;
    }

    private void updateNotificationViewsOnly() {
        handleFoldViewVisibility();
        updateNotificationShade();
    }

    public void updateNotifications() {
        this.mNotificationData.filterAndSort();
        updateNotificationViewsOnly();
    }

    private void handleFoldViewVisibility() {
        if (NotificationUtil.isUserFold() && this.mNotificationPanel != null) {
            boolean showFoldFooter = false;
            boolean showFoldHeader = NotificationUtil.isFold() && !isKeyguardShowing() && !this.mNotificationPanel.isPanelVisibleBecauseOfHeadsUp();
            if (!showFoldHeader && !this.mNotificationPanel.isPanelVisibleBecauseOfHeadsUp() && this.mNotificationData.getFoldCount() > 0 && !isKeyguardShowing()) {
                showFoldFooter = true;
            }
            this.mStackScroller.setFoldViewVisibility(showFoldHeader, showFoldFooter);
        }
    }

    /* access modifiers changed from: private */
    public void openFold() {
        if (NotificationUtil.isFoldAnimating()) {
            Log.i("StatusBar", "openFold fold animating, cancel opening fold");
            return;
        }
        ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_OPEN_FOLD);
        boolean z = true;
        NotificationUtil.fold(true);
        NotificationUtil.setFoldAnimating(true);
        NotificationUtil.lastQsCovered(this.mStackScroller.isQsCovered());
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onOpenFold(this.mStackScroller);
        updateNotifications();
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (this.mNotificationData.getActiveNotifications().size() <= 4) {
            z = false;
        }
        notificationStackScrollLayout.onOpenFold(z);
    }

    /* access modifiers changed from: private */
    public void closeFold() {
        if (NotificationUtil.isFoldAnimating()) {
            Log.i("StatusBar", "closeFold fold animating, cancel closing fold");
            return;
        }
        ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_CLOSE_FOLD);
        NotificationUtil.fold(false);
        NotificationUtil.setFoldAnimating(true);
        Iterator<NotificationData.Entry> it = this.mNotificationData.getActiveNotifications().iterator();
        while (it.hasNext()) {
            it.next().row.setUserExpanded(false, true);
        }
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onCloseFold(this.mStackScroller);
        closeAndSaveGuts(true, true, true, -1, -1, true);
        updateNotifications();
        this.mStackScroller.onCloseFold();
    }

    public void requestNotificationUpdate() {
        updateNotifications();
    }

    /* access modifiers changed from: protected */
    public void setAreThereNotifications() {
        boolean z = true;
        if (SPEW) {
            boolean clearable = hasActiveNotifications() && hasActiveClearableNotifications();
            Log.d("StatusBar", "setAreThereNotifications: N=" + this.mNotificationData.getActiveNotifications().size() + " any=" + hasActiveNotifications() + " clearable=" + clearable);
        }
        if (this.mStatusBarView != null) {
            final View nlo = this.mStatusBarView.findViewById(R.id.notification_lights_out);
            boolean showDot = hasActiveNotifications() && !areLightsOn();
            if (nlo.getAlpha() != 1.0f) {
                z = false;
            }
            if (showDot != z) {
                float f = 0.0f;
                if (showDot) {
                    nlo.setAlpha(0.0f);
                    nlo.setVisibility(0);
                }
                ViewPropertyAnimator animate = nlo.animate();
                if (showDot) {
                    f = 1.0f;
                }
                animate.alpha(f).setDuration(showDot ? 750 : 250).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(showDot ? null : new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator _a) {
                        nlo.setVisibility(8);
                    }
                }).start();
            }
        }
        findAndUpdateMediaNotifications();
    }

    public void findAndUpdateMediaNotifications() {
        int i;
        boolean metaDataChanged = false;
        synchronized (this.mNotificationData) {
            ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            NotificationData.Entry mediaNotification = null;
            MediaController controller = null;
            int i2 = 0;
            while (true) {
                i = 3;
                if (i2 >= N) {
                    break;
                }
                NotificationData.Entry entry = activeNotifications.get(i2);
                if (entry.isMediaNotification()) {
                    MediaSession.Token token = (MediaSession.Token) entry.notification.getNotification().extras.getParcelable("android.mediaSession");
                    if (token != null) {
                        MediaController aController = new MediaController(this.mContext, token);
                        if (3 == getMediaControllerPlaybackState(aController)) {
                            if (DEBUG_MEDIA) {
                                Log.v("StatusBar", "DEBUG_MEDIA: found mediastyle controller matching " + entry.notification.getKey());
                            }
                            mediaNotification = entry;
                            controller = aController;
                        }
                    } else {
                        continue;
                    }
                }
                i2++;
            }
            if (mediaNotification == null && this.mMediaSessionManager != null) {
                for (MediaController aController2 : this.mMediaSessionManager.getActiveSessionsForUser(null, -1)) {
                    if (i == getMediaControllerPlaybackState(aController2)) {
                        String pkg = aController2.getPackageName();
                        int i3 = 0;
                        while (true) {
                            if (i3 >= N) {
                                break;
                            }
                            NotificationData.Entry entry2 = activeNotifications.get(i3);
                            if (entry2.notification.getPackageName().equals(pkg)) {
                                if (DEBUG_MEDIA) {
                                    Log.v("StatusBar", "DEBUG_MEDIA: found controller matching " + entry2.notification.getKey());
                                }
                                controller = aController2;
                                mediaNotification = entry2;
                            } else {
                                i3++;
                            }
                        }
                    }
                    i = 3;
                }
            }
            if (controller != null && !sameSessions(this.mMediaController, controller)) {
                clearCurrentMediaNotification();
                this.mMediaController = controller;
                this.mMediaController.registerCallback(this.mMediaListener);
                this.mMediaMetadata = this.mMediaController.getMetadata();
                if (DEBUG_MEDIA) {
                    Log.v("StatusBar", "DEBUG_MEDIA: insert listener, receive metadata: " + this.mMediaMetadata);
                }
                if (mediaNotification != null) {
                    this.mMediaNotificationKey = mediaNotification.notification.getKey();
                    if (DEBUG_MEDIA) {
                        Log.v("StatusBar", "DEBUG_MEDIA: Found new media notification: key=" + this.mMediaNotificationKey + " controller=" + this.mMediaController);
                    }
                }
                metaDataChanged = true;
            }
        }
        if (metaDataChanged) {
            updateNotifications();
        }
        updateMediaMetaData(metaDataChanged, true);
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public boolean isPlaybackActive(int state) {
        if (state == 1 || state == 7 || state == 0) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void clearCurrentMediaNotification() {
        this.mMediaNotificationKey = null;
        this.mMediaMetadata = null;
        if (this.mMediaController != null) {
            if (DEBUG_MEDIA) {
                Log.v("StatusBar", "DEBUG_MEDIA: Disconnecting from old controller: " + this.mMediaController.getPackageName());
            }
            this.mMediaController.unregisterCallback(this.mMediaListener);
        }
        this.mMediaController = null;
    }

    private boolean sameSessions(MediaController a, MediaController b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.controlsSameSession(b);
    }

    public void updateMediaMetaData(boolean metaDataChanged, boolean allowEnterAnimation) {
        Trace.beginSection("StatusBar#updateMediaMetaData");
        if (this.mBackdrop == null) {
            Trace.endSection();
        } else if (this.mLaunchTransitionFadingAway) {
            this.mBackdrop.setVisibility(4);
            Trace.endSection();
        } else {
            if (DEBUG_MEDIA) {
                Log.v("StatusBar", "DEBUG_MEDIA: updating album art for notification " + this.mMediaNotificationKey + " metadata=" + this.mMediaMetadata + " metaDataChanged=" + metaDataChanged + " state=" + this.mState);
            }
            Drawable artworkDrawable = null;
            if (!(this.mMediaMetadata == null || 0 == 0)) {
                artworkDrawable = new BitmapDrawable(this.mBackdropBack.getResources(), null);
            }
            boolean hideBecauseOccluded = this.mStatusBarKeyguardViewManager != null && this.mStatusBarKeyguardViewManager.isOccluded();
            if (((artworkDrawable != null) || DEBUG_MEDIA_FAKE_ARTWORK) && !((this.mState == 0 && 0 == 0) || this.mFingerprintUnlockController.getMode() == 2 || hideBecauseOccluded)) {
                if (this.mBackdrop.getVisibility() != 0) {
                    this.mBackdrop.setVisibility(0);
                    if (allowEnterAnimation) {
                        this.mBackdrop.setAlpha(0.002f);
                        this.mBackdrop.animate().alpha(1.0f);
                    } else {
                        this.mBackdrop.animate().cancel();
                        this.mBackdrop.setAlpha(1.0f);
                    }
                    this.mStatusBarWindowManager.setBackdropShowing(true);
                    metaDataChanged = true;
                    if (DEBUG_MEDIA) {
                        Log.v("StatusBar", "DEBUG_MEDIA: Fading in album artwork");
                    }
                }
                if (metaDataChanged) {
                    if (this.mBackdropBack.getDrawable() != null) {
                        this.mBackdropFront.setImageDrawable(this.mBackdropBack.getDrawable().getConstantState().newDrawable(this.mBackdropFront.getResources()).mutate());
                        if (this.mScrimSrcModeEnabled) {
                            this.mBackdropFront.getDrawable().mutate().setXfermode(this.mSrcOverXferMode);
                        }
                        this.mBackdropFront.setAlpha(1.0f);
                        this.mBackdropFront.setVisibility(0);
                    } else {
                        this.mBackdropFront.setVisibility(4);
                    }
                    if (DEBUG_MEDIA_FAKE_ARTWORK) {
                        int c = -16777216 | ((int) (Math.random() * 1.6777215E7d));
                        Log.v("StatusBar", String.format("DEBUG_MEDIA: setting new color: 0x%08x", new Object[]{Integer.valueOf(c)}));
                        this.mBackdropBack.setBackgroundColor(-1);
                        this.mBackdropBack.setImageDrawable(new ColorDrawable(c));
                    } else {
                        this.mBackdropBack.setImageDrawable(artworkDrawable);
                    }
                    if (this.mScrimSrcModeEnabled) {
                        this.mBackdropBack.getDrawable().mutate().setXfermode(this.mSrcXferMode);
                    }
                    if (this.mBackdropFront.getVisibility() == 0) {
                        if (DEBUG_MEDIA) {
                            Log.v("StatusBar", "DEBUG_MEDIA: Crossfading album artwork from " + this.mBackdropFront.getDrawable() + " to " + this.mBackdropBack.getDrawable());
                        }
                        this.mBackdropFront.animate().setDuration(250).alpha(0.0f).withEndAction(this.mHideBackdropFront);
                    }
                }
            } else if (this.mBackdrop.getVisibility() != 8) {
                if (DEBUG_MEDIA) {
                    Log.v("StatusBar", "DEBUG_MEDIA: Fading out album artwork");
                }
                if (this.mFingerprintUnlockController.getMode() == 2 || hideBecauseOccluded) {
                    this.mBackdrop.setVisibility(8);
                    this.mBackdropBack.setImageDrawable(null);
                    this.mStatusBarWindowManager.setBackdropShowing(false);
                } else {
                    this.mStatusBarWindowManager.setBackdropShowing(false);
                    this.mBackdrop.animate().alpha(0.002f).setInterpolator(Interpolators.ACCELERATE_DECELERATE).setDuration(300).setStartDelay(0).withEndAction(new Runnable() {
                        public void run() {
                            StatusBar.this.mBackdrop.setVisibility(8);
                            StatusBar.this.mBackdropFront.animate().cancel();
                            StatusBar.this.mBackdropBack.setImageDrawable(null);
                            StatusBar.this.mHandler.post(StatusBar.this.mHideBackdropFront);
                        }
                    });
                    if (this.mKeyguardFadingAway) {
                        this.mBackdrop.animate().setDuration(this.mKeyguardFadingAwayDuration / 2).setStartDelay(this.mKeyguardFadingAwayDelay).setInterpolator(Interpolators.LINEAR).start();
                    }
                }
            }
            Trace.endSection();
        }
    }

    private void updateReportRejectedTouchVisibility() {
        if (this.mReportRejectedTouch != null) {
            this.mReportRejectedTouch.setVisibility((this.mState != 1 || !this.mFalsingManager.isReportingEnabled()) ? 4 : 0);
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        this.mSoftInputVisible = (vis & 2) != 0;
    }

    public void disable(int state1, int state2, boolean animate) {
        int i = state1;
        int i2 = state2;
        boolean z = true;
        boolean z2 = animate & (this.mStatusBarWindowState != 2);
        EventLog.writeEvent(30099, i);
        int old1 = this.mDisabled1;
        boolean diff1 = i ^ old1;
        this.mDisabled1 = i;
        int old2 = this.mDisabled2;
        int diff2 = i2 ^ old2;
        this.mDisabled2 = i2;
        if (DEBUG) {
            Log.d("StatusBar", String.format("disable1: 0x%08x -> 0x%08x (diff1: 0x%08x)", new Object[]{Integer.valueOf(old1), Integer.valueOf(state1), Integer.valueOf(diff1)}));
            Log.d("StatusBar", String.format("disable2: 0x%08x -> 0x%08x (diff2: 0x%08x)", new Object[]{Integer.valueOf(old2), Integer.valueOf(state2), Integer.valueOf(diff2)}));
        }
        StringBuilder flagdbg = new StringBuilder();
        flagdbg.append("disable<");
        flagdbg.append((i & 65536) != 0 ? 'E' : 'e');
        char c = ' ';
        flagdbg.append(diff1 & true ? '!' : ' ');
        flagdbg.append((i & 131072) != 0 ? 'I' : 'i');
        flagdbg.append(true & diff1 ? '!' : ' ');
        flagdbg.append((i & 262144) != 0 ? 'A' : 'a');
        flagdbg.append(diff1 & true ? '!' : ' ');
        char c2 = 'S';
        flagdbg.append((i & 1048576) != 0 ? 'S' : 's');
        flagdbg.append(diff1 & true ? '!' : ' ');
        flagdbg.append((4194304 & i) != 0 ? 'B' : 'b');
        flagdbg.append(true & diff1 ? '!' : ' ');
        flagdbg.append((2097152 & i) != 0 ? 'H' : 'h');
        flagdbg.append(true & diff1 ? '!' : ' ');
        flagdbg.append((i & 16777216) != 0 ? 'R' : 'r');
        flagdbg.append(diff1 & true ? '!' : ' ');
        flagdbg.append((8388608 & i) != 0 ? 'C' : 'c');
        flagdbg.append(true & diff1 ? '!' : ' ');
        if ((33554432 & i) == 0) {
            c2 = 's';
        }
        flagdbg.append(c2);
        flagdbg.append(true & diff1 ? '!' : ' ');
        flagdbg.append("> disable2<");
        flagdbg.append((i2 & 1) != 0 ? 'Q' : 'q');
        flagdbg.append((diff2 & 1) != 0 ? '!' : ' ');
        flagdbg.append((i2 & 2) != 0 ? 'I' : 'i');
        flagdbg.append((diff2 & 2) != 0 ? '!' : ' ');
        flagdbg.append((i2 & 4) != 0 ? 'N' : 'n');
        flagdbg.append((diff2 & 4) != 0 ? '!' : ' ');
        flagdbg.append((i2 & 8) != 0 ? 'G' : 'g');
        flagdbg.append((diff2 & 8) != 0 ? '!' : ' ');
        flagdbg.append((i2 & 16) != 0 ? 'R' : 'r');
        if ((diff2 & 16) != 0) {
            c = '!';
        }
        flagdbg.append(c);
        flagdbg.append('>');
        Log.d("StatusBar", flagdbg.toString());
        if ((diff1 && true) && (65536 & i) != 0) {
            animateCollapsePanels();
        }
        if ((diff1 && true) && (16777216 & i) != 0) {
            this.mHandler.removeMessages(1020);
            this.mHandler.sendEmptyMessage(1020);
        }
        if ((true && diff1) && this.mNavigationBarView != null) {
            this.mNavigationBarView.setDisabledFlags(i);
        }
        if (diff1 && true) {
            this.mDisableNotificationAlerts = (i & 262144) != 0;
            updateHeadsUpSetting();
        }
        if ((diff2 & 1) != 0) {
            updateQsExpansionEnabled();
        }
        if ((diff2 & 4) != 0) {
            updateQsExpansionEnabled();
            if ((i2 & 4) != 0) {
                animateCollapsePanels();
            }
        }
        sIsStatusBarHidden = (this.mDisabled1 & 256) != 0;
        if (diff1 && true) {
            boolean isEnter = false;
            if (sIsStatusBarHidden) {
                isEnter = true;
            }
            if (this.mIsFsgMode) {
                Intent intent = new Intent();
                intent.setPackage("com.android.systemui");
                intent.setAction("com.android.systemui.fullscreen.statechange");
                intent.putExtra("isEnter", isEnter);
                this.mContext.sendBroadcast(intent);
            }
        }
        if (diff1 && true) {
            if ((this.mDisabled1 & 1024) == 0) {
                z = false;
            }
            this.mDisableFloatNotification = z;
        }
    }

    public int getFlagDisable1() {
        return this.mDisabled1;
    }

    private void updateHeadsUpSetting() {
        boolean wasUsing = this.mUseHeadsUp;
        this.mUseHeadsUp = !this.mDisableNotificationAlerts;
        StringBuilder sb = new StringBuilder();
        sb.append("heads up is ");
        sb.append(this.mUseHeadsUp ? "enabled" : "disabled");
        Log.d("StatusBar", sb.toString());
        if (wasUsing != this.mUseHeadsUp && !this.mUseHeadsUp) {
            Log.d("StatusBar", "dismissing any existing heads up notification on disable event");
            this.mHeadsUpManager.releaseAllImmediately();
        }
    }

    public void recomputeDisableFlags(boolean animate) {
        this.mCommandQueue.recomputeDisableFlags(animate);
    }

    /* access modifiers changed from: protected */
    public H createHandler() {
        return new H();
    }

    private W createBgHandler() {
        this.mBgThread = new HandlerThread("StatusBar", 10);
        this.mBgThread.start();
        return new W(this.mBgThread.getLooper());
    }

    public void startActivity(Intent intent, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, false, dismissShade);
    }

    public void startActivity(Intent intent, boolean onlyProvisioned, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, dismissShade);
    }

    public void startActivity(Intent intent, boolean dismissShade, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, dismissShade, callback);
    }

    public void setQsExpanded(boolean expanded) {
        int i;
        this.mStatusBarWindowManager.setQsExpanded(expanded);
        KeyguardClockContainer keyguardClockContainer = this.mKeyguardClock;
        if (expanded) {
            i = 4;
        } else {
            i = 0;
        }
        keyguardClockContainer.setImportantForAccessibility(i);
    }

    public boolean isGoingToNotificationShade() {
        return this.mLeaveOpenOnKeyguardHide;
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return getBarState() == 1;
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public String getCurrentMediaNotificationKey() {
        return this.mMediaNotificationKey;
    }

    public boolean isScrimSrcModeEnabled() {
        return this.mScrimSrcModeEnabled;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    public void onUnlockMethodStateChanged() {
        logStateToEventlog();
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        this.mInPinnedMode = inPinnedMode;
        if (inPinnedMode) {
            this.mStatusBarWindowManager.setHeadsUpShowing(true);
            this.mStatusBarWindowManager.setForceStatusBarVisible(true);
            if (this.mNotificationPanel.isFullyCollapsed()) {
                this.mNotificationPanel.requestLayout();
                this.mStatusBarWindowManager.setForceWindowCollapsed(true);
                this.mNotificationPanel.post(new Runnable() {
                    public void run() {
                        StatusBar.this.mStatusBarWindowManager.setForceWindowCollapsed(false);
                    }
                });
            }
            updateFsgState();
        } else if (!this.mNotificationPanel.isFullyCollapsed() || this.mNotificationPanel.isTracking()) {
            this.mStatusBarWindowManager.setHeadsUpShowing(false);
            updateFsgState();
        } else {
            this.mHeadsUpManager.setHeadsUpGoingAway(true);
            this.mStackScroller.runAfterAnimationFinished(new Runnable() {
                public void run() {
                    if (!StatusBar.this.mHeadsUpManager.hasPinnedHeadsUp()) {
                        StatusBar.this.mStatusBarWindowManager.setHeadsUpShowing(false);
                        StatusBar.this.mHeadsUpManager.setHeadsUpGoingAway(false);
                    }
                    StatusBar.this.removeRemoteInputEntriesKeptUntilCollapsed();
                }
            });
        }
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        dismissVolumeDialog();
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        if (isHeadsUp || !this.mHeadsUpEntriesToRemoveOnSwitch.contains(entry)) {
            updateNotificationRanking(null);
        } else {
            removeNotification(entry.key, this.mLatestRankingMap);
            this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
            if (this.mHeadsUpEntriesToRemoveOnSwitch.isEmpty()) {
                this.mLatestRankingMap = null;
            }
        }
        if (!isHeadsUp) {
            sendExitFloatingIntent(entry.notification);
        }
    }

    private void sendExitFloatingIntent(ExpandedNotification sbn) {
        if (sbn != null && sbn.getNotification().extraNotification.getExitFloatingIntent() != null) {
            try {
                Log.d("StatusBar", "Notification has exitFloatingIntent; sending exitFloatingIntent");
                sbn.getNotification().extraNotification.getExitFloatingIntent().send();
            } catch (PendingIntent.CanceledException e) {
                Log.e("StatusBar", "floating intent send occur exception", e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateHeadsUp(String key, NotificationData.Entry entry, boolean shouldPeek, boolean alertAgain) {
        if (isHeadsUp(key)) {
            if (!shouldPeek) {
                this.mHeadsUpManager.removeNotification(key, false);
            } else {
                this.mHeadsUpManager.updateNotification(entry, alertAgain);
            }
        } else if (shouldPeek && alertAgain) {
            this.mHeadsUpManager.showNotification(entry);
        }
    }

    /* access modifiers changed from: protected */
    public void setHeadsUpUser(int newUserId) {
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.setUser(newUserId);
        }
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpManager.isHeadsUp(key);
    }

    /* access modifiers changed from: protected */
    public boolean isSnoozedPackage(StatusBarNotification sbn) {
        return this.mHeadsUpManager.isSnoozed(sbn.getPackageName());
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mPanelExpanded = isExpanded;
        this.mStatusBarWindowManager.setPanelExpanded(isExpanded);
        this.mVisualStabilityManager.setPanelExpanded(isExpanded);
        if (isExpanded && getBarState() != 1) {
            if (DEBUG) {
                Log.v("StatusBar", "clearing notification effects from setPanelExpanded");
            }
            clearNotificationEffects();
        }
        if (!isExpanded) {
            removeRemoteInputEntriesKeptUntilCollapsed();
        }
    }

    /* access modifiers changed from: private */
    public void removeRemoteInputEntriesKeptUntilCollapsed() {
        for (int i = 0; i < this.mRemoteInputEntriesToRemoveOnCollapse.size(); i++) {
            NotificationData.Entry entry = this.mRemoteInputEntriesToRemoveOnCollapse.valueAt(i);
            this.mRemoteInputController.removeRemoteInput(entry, null);
            removeNotification(entry.key, this.mLatestRankingMap);
        }
        this.mRemoteInputEntriesToRemoveOnCollapse.clear();
    }

    public void onScreenTurnedOff() {
        this.mFalsingManager.onScreenOff();
    }

    public NotificationStackScrollLayout getNotificationScrollLayout() {
        return this.mStackScroller;
    }

    public boolean isPulsing() {
        return this.mDozeScrimController.isPulsing();
    }

    public void onReorderingAllowed() {
        updateNotifications();
    }

    public boolean isLaunchTransitionFadingAway() {
        return this.mLaunchTransitionFadingAway;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        return this.mNotificationPanel.hideStatusBarIconsWhenExpanded();
    }

    /* access modifiers changed from: private */
    public void updateMessage(AppMessage appMsg) {
        updateAppBadgeNum(appMsg.pkgName, appMsg.className, appMsg.num, appMsg.userId);
    }

    /* access modifiers changed from: private */
    public void beep(String key) {
        if (this.sService == null) {
            this.sService = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        }
        try {
            this.sService.getClass().getMethod("buzzBeepBlinkForNotification", new Class[]{String.class}).invoke(this.sService, new Object[]{key});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
        }
    }

    public void maybeEscalateHeadsUp() {
        for (HeadsUpManager.HeadsUpEntry entry : this.mHeadsUpManager.getAllEntries()) {
            StatusBarNotification sbn = entry.entry.notification;
            Notification notification = sbn.getNotification();
            if (notification.fullScreenIntent != null) {
                Log.d("StatusBar", "converting a heads up to fullScreen");
                try {
                    EventLog.writeEvent(36003, sbn.getKey());
                    notification.fullScreenIntent.send();
                    entry.entry.notifyFullScreenIntentLaunched();
                } catch (PendingIntent.CanceledException e) {
                    Log.e("StatusBar", "throw exception when sending full screen intent", e);
                }
            }
        }
        this.mHeadsUpManager.releaseAllImmediately();
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void handleSystemNavigationKey(int key) {
        if (SPEW) {
            Log.d("StatusBar", "handleSystemNavigationKey: " + key);
        }
        if (panelsEnabled() && this.mKeyguardMonitor.isDeviceInteractive() && ((!this.mKeyguardMonitor.isShowing() || this.mKeyguardMonitor.isOccluded()) && this.mUserSetup)) {
            if (280 == key) {
                MetricsLogger.action(this.mContext, 493);
                this.mNotificationPanel.collapse(false, 1.0f);
            } else if (281 == key) {
                MetricsLogger.action(this.mContext, 494);
                if (this.mNotificationPanel.isFullyCollapsed()) {
                    this.mNotificationPanel.expand(true);
                    MetricsLogger.count(this.mContext, "panel_open", 1);
                } else if (!this.mNotificationPanel.isInSettings() && !this.mNotificationPanel.isExpanding()) {
                    this.mNotificationPanel.flingSettings(0.0f, true);
                    MetricsLogger.count(this.mContext, "panel_open_qs", 1);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean panelsEnabled() {
        return (this.mDisabled1 & 65536) == 0 && (this.mDisabled2 & 4) == 0 && !ONLY_CORE_APPS;
    }

    /* access modifiers changed from: package-private */
    public void makeExpandedVisible(boolean force) {
        if (SPEW) {
            Log.d("StatusBar", "Make expanded visible: expanded visible=" + this.mExpandedVisible);
        }
        if (force || (!this.mExpandedVisible && panelsEnabled())) {
            this.mExpandedVisible = true;
            this.mStatusBarWindowManager.setPanelVisible(true);
            visibilityChanged(true);
            updateFsgState();
            this.mWaitingForKeyguardExit = false;
            recomputeDisableFlags(!force);
            setInteracting(1, true);
        }
    }

    /* access modifiers changed from: private */
    public void closeFoldIfNeeded() {
        if (NotificationUtil.isFold()) {
            closeFold();
            runPostCollapseRunnables();
            return;
        }
        animateCollapsePanels();
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void postAnimateForceCollapsePanels() {
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBar.this.animateCollapsePanels(0, true);
            }
        });
    }

    public void animateCollapsePanels(int flags) {
        animateCollapsePanels(flags, false, false, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force) {
        animateCollapsePanels(flags, force, false, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
        animateCollapsePanels(flags, force, delayed, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed, float speedUpFactor) {
        if (force || this.mState == 0) {
            if (SPEW) {
                Log.d("StatusBar", "animateCollapse(): mExpandedVisible=" + this.mExpandedVisible + " flags=" + flags);
            }
            if ((flags & 2) == 0 && !this.mHandler.hasMessages(1020)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            if (this.mStatusBarWindow != null && this.mNotificationPanel.canPanelBeCollapsed()) {
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mStatusBarWindow.cancelExpandHelper();
                this.mStatusBarView.collapsePanel(true, delayed, speedUpFactor);
            }
            return;
        }
        runPostCollapseRunnables();
    }

    public boolean canPanelBeCollapsed() {
        return this.mNotificationPanel.canPanelBeCollapsed();
    }

    /* access modifiers changed from: private */
    public void runPostCollapseRunnables() {
        ArrayList<Runnable> clonedList = new ArrayList<>(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = clonedList.size();
        for (int i = 0; i < size; i++) {
            clonedList.get(i).run();
        }
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void animateExpandNotificationsPanel() {
        if (SPEW) {
            Log.d("StatusBar", "animateExpand: mExpandedVisible=" + this.mExpandedVisible);
        }
        if (panelsEnabled()) {
            this.mNotificationPanel.expand(true);
        }
    }

    public void animateExpandSettingsPanel(String subPanel) {
        if (SPEW) {
            Log.d("StatusBar", "animateExpand: mExpandedVisible=" + this.mExpandedVisible);
        }
        if (panelsEnabled() && this.mUserSetup) {
            if (subPanel != null) {
                this.mQSPanel.openDetails(subPanel);
            }
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapsePanel(true, false, 1.0f);
        }
    }

    /* access modifiers changed from: package-private */
    public void makeExpandedInvisible() {
        if (SPEW) {
            Log.d("StatusBar", "makeExpandedInvisible: mExpandedVisible=" + this.mExpandedVisible + " mExpandedVisible=" + this.mExpandedVisible);
        }
        if (this.mExpandedVisible && this.mStatusBarWindow != null) {
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onPanelCollapsed(this.mStackScroller);
            this.mStatusBarView.collapsePanel(false, false, 1.0f);
            this.mNotificationPanel.closeQs();
            this.mExpandedVisible = false;
            visibilityChanged(false);
            updateFsgState();
            this.mStatusBarWindowManager.setPanelVisible(false);
            this.mStatusBarWindowManager.setForceStatusBarVisible(false);
            closeAndSaveGuts(true, true, true, -1, -1, true);
            runPostCollapseRunnables();
            setInteracting(1, false);
            showBouncerIfKeyguard();
            recomputeDisableFlags(this.mNotificationPanel.hideStatusBarIconsWhenExpanded());
            if (!isKeyguardShowing()) {
                WindowManagerGlobal.getInstance().trimMemory(20);
            }
            this.mNotificationPanel.setBrightnessListening(false);
        }
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (DEBUG_GESTURES && event.getActionMasked() != 2) {
            EventLog.writeEvent(36000, new Object[]{Integer.valueOf(event.getActionMasked()), Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY()), Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)});
        }
        if (SPEW) {
            Log.d("StatusBar", "Touch: rawY=" + event.getRawY() + " event=" + event + " mDisabled1=" + this.mDisabled1 + " mDisabled2=" + this.mDisabled2 + " mTracking=" + this.mTracking);
        } else if (CHATTY && event.getAction() != 2) {
            Log.d("StatusBar", String.format("panel: %s at (%f, %f) mDisabled1=0x%08x mDisabled2=0x%08x", new Object[]{MotionEvent.actionToString(event.getAction()), Float.valueOf(event.getRawX()), Float.valueOf(event.getRawY()), Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)}));
        }
        if (DEBUG_GESTURES) {
            this.mGestureRec.add(event);
        }
        if (this.mStatusBarWindowState == 0) {
            if (!(event.getAction() == 1 || event.getAction() == 3) || this.mExpandedVisible) {
                setInteracting(1, true);
            } else {
                setInteracting(1, false);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    public FingerprintUnlockController getFingerprintUnlockController() {
        return this.mFingerprintUnlockController;
    }

    public void setWindowState(int window, int state) {
        boolean showing = state == 0;
        if (this.mStatusBarWindow != null && window == 1 && this.mStatusBarWindowState != state) {
            this.mStatusBarWindowState = state;
            if (DEBUG_WINDOW_STATE) {
                Log.d("StatusBar", "Status bar " + StatusBarManager.windowStateToString(state));
            }
            if (!showing && this.mState == 0) {
                this.mStatusBarView.collapsePanel(false, false, 1.0f);
            }
        }
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
        int i;
        int i2 = mask;
        int oldVal = this.mSystemUiVisibility;
        int newVal = ((~i2) & oldVal) | (vis & i2);
        int diff = newVal ^ oldVal;
        if (DEBUG) {
            Log.d("StatusBar", String.format("setSystemUiVisibility vis=%s mask=%s oldVal=%s newVal=%s diff=%s", new Object[]{Integer.toHexString(vis), Integer.toHexString(mask), Integer.toHexString(oldVal), Integer.toHexString(newVal), Integer.toHexString(diff)}));
        }
        boolean sbModeChanged = false;
        if (diff != 0) {
            this.mSystemUiVisibility = newVal;
            if ((diff & 1) != 0) {
                setAreThereNotifications();
            }
            if ((vis & 268435456) != 0) {
                this.mSystemUiVisibility &= -268435457;
                this.mNoAnimationOnNextBarModeChange = true;
            }
            int i3 = -1;
            if (this.mNavigationBarView == null) {
                i = -1;
            } else {
                i = -1;
                i3 = computeNavigationBarMode(oldVal, newVal, 134217728, Integer.MIN_VALUE, 32768);
            }
            int nbMode = i3;
            if (this.mNavigationBarView != null && this.mNavigationBarView.isForceImmersive() && (newVal & 2) != 0 && (nbMode == 0 || nbMode == 3)) {
                nbMode = 1;
            }
            int sbMode = computeStatusBarMode(oldVal, newVal);
            boolean nbModeChanged = nbMode != i;
            boolean sbModeChanged2 = sbMode != i;
            if (sbModeChanged2 && sbMode != this.mStatusBarMode) {
                this.mStatusBarMode = sbMode;
                checkBarModes();
                this.mOLEDScreenHelper.onStatusBarModeChanged(this.mStatusBarMode);
            }
            if (nbModeChanged && nbMode != this.mNavigationBarMode) {
                this.mNavigationBarMode = nbMode;
                if (this.mStackScroller != null) {
                    this.mStackScroller.setLastNavigationBarMode(this.mNavigationBarMode);
                }
                if (this.mNavigationBarView != null) {
                    this.mNavigationBarView.getBarTransitions().transitionTo(nbMode, true);
                    this.mNavigationBarView.setDisabledFlags(this.mDisabled1, true);
                }
            }
            if (sbModeChanged2 || nbModeChanged) {
                if (this.mStatusBarMode == 1 || this.mNavigationBarMode == 1) {
                    scheduleAutohide();
                } else {
                    cancelAutohide();
                }
            }
            if ((vis & 536870912) != 0) {
                this.mSystemUiVisibility &= -536870913;
            }
            notifyUiVisibilityChanged(this.mSystemUiVisibility);
            sbModeChanged = sbModeChanged2;
        }
        final int maskF = i2;
        final int dockedStackVisF = dockedStackVis;
        final int fullscreenStackVisF = Build.VERSION.SDK_INT == 23 ? vis : fullscreenStackVis;
        final boolean sbModeChangedF = sbModeChanged;
        final Rect dockedStackBoundsF = dockedStackBounds;
        final Rect fullscreenStackBoundsF = fullscreenStackBounds;
        AnonymousClass42 r18 = r0;
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        AnonymousClass42 r0 = new Runnable() {
            public void run() {
                StatusBar.this.mLightBarController.onSystemUiVisibilityChanged(fullscreenStackVisF, dockedStackVisF, maskF, fullscreenStackBoundsF, dockedStackBoundsF, sbModeChangedF, StatusBar.this.mStatusBarMode);
            }
        };
        statusBarWindowView.post(r0);
    }

    private int computeNavigationBarMode(int oldVis, int newVis, int transientFlag, int translucentFlag, int transparentFlag) {
        int oldMode = navigationBarMode(oldVis, transientFlag, translucentFlag, transparentFlag);
        int newMode = navigationBarMode(newVis, transientFlag, translucentFlag, transparentFlag);
        if (oldMode == newMode) {
            return -1;
        }
        return newMode;
    }

    private int navigationBarMode(int vis, int transientFlag, int translucentFlag, int transparentFlag) {
        if ((vis & transientFlag) != 0) {
            return 1;
        }
        if ((vis & translucentFlag) != 0) {
            return 2;
        }
        if ((vis & transparentFlag) != 0) {
            return 4;
        }
        if ((vis & 1) != 0) {
            return 3;
        }
        return 0;
    }

    public int getNavigationBarMode() {
        return this.mNavigationBarMode;
    }

    /* access modifiers changed from: protected */
    public int computeStatusBarMode(int oldVal, int newVal) {
        return computeBarMode(oldVal, newVal, 67108864, 1073741824, 8);
    }

    /* access modifiers changed from: protected */
    public BarTransitions getStatusBarTransitions() {
        return this.mStatusBarView.getBarTransitions();
    }

    /* access modifiers changed from: protected */
    public int computeBarMode(int oldVis, int newVis, int transientFlag, int translucentFlag, int transparentFlag) {
        int oldMode = barMode(oldVis, transientFlag, translucentFlag, transparentFlag);
        int newMode = barMode(newVis, transientFlag, translucentFlag, transparentFlag);
        if (oldMode == newMode) {
            return -1;
        }
        return newMode;
    }

    /* access modifiers changed from: package-private */
    public void checkBarModes() {
        if (!this.mDemoMode) {
            if (this.mStatusBarView != null) {
                checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, getStatusBarTransitions());
            }
            this.mNoAnimationOnNextBarModeChange = false;
        }
    }

    private int barMode(int vis, int transientFlag, int translucentFlag, int transparentFlag) {
        int lightsOutTransparent = 1 | transparentFlag;
        if ((vis & transientFlag) != 0) {
            return 1;
        }
        if ((vis & lightsOutTransparent) == lightsOutTransparent) {
            return 6;
        }
        if ((vis & transparentFlag) != 0) {
            return 4;
        }
        if ((vis & translucentFlag) != 0) {
            return 2;
        }
        if ((vis & 1) != 0) {
            return 3;
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public void checkBarMode(int mode, int windowState, BarTransitions transitions) {
        boolean anim = false;
        boolean powerSave = this.mBatteryController.isPowerSave() || this.mBatteryController.isExtremePowerSave();
        if (!this.mNoAnimationOnNextBarModeChange && this.mDeviceInteractive && windowState != 2 && !powerSave) {
            anim = true;
        }
        if (this.mForceBlack && !this.mExpandedVisible && this.mContext.getResources().getConfiguration().orientation == 1) {
            mode = 0;
        }
        transitions.transitionTo(mode, anim);
    }

    private void finishBarAnimations() {
        if (this.mStatusBarView != null) {
            this.mStatusBarView.getBarTransitions().finishAnimations();
        }
    }

    public void setInteracting(int barWindow, boolean interacting) {
        int i;
        boolean z = false;
        if (((this.mInteractingWindows & barWindow) != 0) != interacting) {
            z = true;
        }
        boolean changing = z;
        if (interacting) {
            i = this.mInteractingWindows | barWindow;
        } else {
            i = this.mInteractingWindows & (~barWindow);
        }
        this.mInteractingWindows = i;
        if (this.mInteractingWindows != 0) {
            suspendAutohide();
        } else {
            resumeSuspendedAutohide();
        }
        if (changing && interacting && barWindow == 2) {
            dismissVolumeDialog();
        }
        checkBarModes();
    }

    private void dismissVolumeDialog() {
        if (this.mVolumeComponent != null) {
            this.mVolumeComponent.dismissNow();
        }
    }

    private void resumeSuspendedAutohide() {
        if (this.mAutohideSuspended) {
            scheduleAutohide();
            this.mHandler.postDelayed(this.mCheckBarModes, 500);
        }
    }

    private void suspendAutohide() {
        this.mHandler.removeCallbacks(this.mAutohide);
        this.mHandler.removeCallbacks(this.mCheckBarModes);
        this.mAutohideSuspended = (this.mSystemUiVisibility & 201326592) != 0;
    }

    private void cancelAutohide() {
        this.mAutohideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutohide);
    }

    private void scheduleAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 3000);
    }

    /* access modifiers changed from: package-private */
    public void checkUserAutohide(View v, MotionEvent event) {
        if ((this.mSystemUiVisibility & 201326592) != 0 && event.getAction() == 4 && event.getX() == 0.0f && event.getY() == 0.0f && !this.mRemoteInputController.isRemoteInputActive()) {
            userAutohide();
        }
    }

    /* access modifiers changed from: private */
    public void checkRemoteInputOutside(MotionEvent event) {
        if (event.getAction() == 4 && event.getX() == 0.0f && event.getY() == 0.0f && this.mRemoteInputController.isRemoteInputActive()) {
            this.mRemoteInputController.closeRemoteInputs();
        }
    }

    private void userAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 350);
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public void setLightsOn(boolean on) {
        Log.v("StatusBar", "setLightsOn(" + on + ")");
        if (on) {
            setSystemUiVisibility(0, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
            return;
        }
        setSystemUiVisibility(1, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
    }

    /* access modifiers changed from: private */
    public void notifyUiVisibilityChanged(int vis) {
        try {
            if (this.mLastDispatchedSystemUiVisibility != vis) {
                this.mWindowManagerService.statusBarVisibilityChanged(vis);
                this.mLastDispatchedSystemUiVisibility = vis;
            }
        } catch (RemoteException e) {
        }
    }

    public void topAppWindowChanged(boolean showMenu) {
        if (SPEW) {
            StringBuilder sb = new StringBuilder();
            sb.append(showMenu ? "showing" : "hiding");
            sb.append(" the MENU button");
            Log.d("StatusBar", sb.toString());
        }
        if (showMenu) {
            setLightsOn(true);
        }
    }

    public static String viewInfo(View v) {
        return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom() + ") " + v.getWidth() + "x" + v.getHeight() + "]";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpandedVisible=" + this.mExpandedVisible + ", mTrackingPosition=" + this.mTrackingPosition);
            StringBuilder sb = new StringBuilder();
            sb.append("  mTracking=");
            sb.append(this.mTracking);
            pw.println(sb.toString());
            pw.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            pw.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        pw.print("  mPendingNotifications=");
        if (this.mPendingNotifications.size() == 0) {
            pw.println("null");
        } else {
            for (NotificationData.Entry entry : this.mPendingNotifications.values()) {
                pw.println(entry.notification);
            }
        }
        pw.print("  mInteractingWindows=");
        pw.println(this.mInteractingWindows);
        pw.println("  mSystemUiVisibility=" + Integer.toHexString(this.mSystemUiVisibility));
        pw.println(String.format("  disable1=0x%08x disable2=0x%08x", new Object[]{Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)}));
        pw.print("  mStatusBarWindowState=");
        pw.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        pw.print("  mStatusBarMode=");
        pw.println(BarTransitions.modeToString(this.mStatusBarMode));
        pw.print("  mDozing=");
        pw.println(this.mDozing);
        pw.print("  mZenMode=");
        pw.println(Settings.Global.zenModeToString(this.mZenMode));
        pw.print("  mUseHeadsUp=");
        pw.println(this.mUseHeadsUp);
        if (this.mStatusBarView != null) {
            dumpBarTransitions(pw, "mStatusBarView", this.mStatusBarView.getBarTransitions());
        }
        pw.print("  mMediaSessionManager=");
        pw.println(this.mMediaSessionManager);
        pw.print("  mMediaNotificationKey=");
        pw.println(this.mMediaNotificationKey);
        pw.print("  mMediaController=");
        pw.print(this.mMediaController);
        if (this.mMediaController != null) {
            pw.print(" state=" + this.mMediaController.getPlaybackState());
        }
        pw.println();
        pw.print("  mMediaMetadata=");
        pw.print(this.mMediaMetadata);
        if (this.mMediaMetadata != null) {
            pw.print(" title=" + this.mMediaMetadata.getText("android.media.metadata.TITLE"));
        }
        pw.println();
        this.mOLEDScreenHelper.dump(fd, pw, args);
        pw.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            pw.println("    mNotificationPanel=" + this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug(""));
            pw.print("      ");
            this.mNotificationPanel.dump(fd, pw, args);
            this.mStackScroller.dump(fd, pw, args);
        }
        DozeLog.dump(pw);
        synchronized (this.mNotificationData) {
            this.mNotificationData.dump(pw, "  ");
        }
        if (DEBUG_GESTURES) {
            pw.print("  status bar gestures: ");
            this.mGestureRec.dump(fd, pw, args);
        }
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.dump(fd, pw, args);
        } else {
            pw.println("  mHeadsUpManager: null");
        }
        if (this.mGroupManager != null) {
            this.mGroupManager.dump(fd, pw, args);
        } else {
            pw.println("  mGroupManager: null");
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext) != null) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dump(fd, pw, args);
        }
        FalsingManager.getInstance(this.mContext).dump(pw);
        FalsingLog.dump(pw);
        pw.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry2 : Prefs.getAll(this.mContext).entrySet()) {
            pw.print("  ");
            pw.print(entry2.getKey());
            pw.print("=");
            pw.println(entry2.getValue());
        }
        pw.println("AppNotificationSettings:");
        for (Map.Entry<String, ?> entry3 : NotificationFilterHelper.getSharedPreferences(this.mContext).getAll().entrySet()) {
            pw.print("  ");
            pw.print(entry3.getKey());
            pw.print("=");
            pw.println(entry3.getValue());
        }
        pw.print("  mNavigationBarView=");
        if (this.mNavigationBarView == null) {
            pw.println("null");
        } else {
            this.mNavigationBarView.dump(fd, pw, args);
        }
    }

    static void dumpBarTransitions(PrintWriter pw, String var, BarTransitions transitions) {
        pw.print("  ");
        pw.print(var);
        pw.print(".BarTransitions.mMode=");
        pw.println(BarTransitions.modeToString(transitions.getMode()));
    }

    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        makeStatusBarView();
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mRemoteInputController = new RemoteInputController(this.mHeadsUpManager);
        this.mStatusBarWindowManager.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    /* access modifiers changed from: package-private */
    public void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
        if (DEBUG_GESTURES) {
            this.mGestureRec.tag("display", String.format("%dx%d", new Object[]{Integer.valueOf(this.mDisplayMetrics.widthPixels), Integer.valueOf(this.mDisplayMetrics.heightPixels)}));
        }
    }

    /* access modifiers changed from: package-private */
    public float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, dismissShade, null);
    }

    public void startActivityDismissingKeyguard(final Intent intent, boolean onlyProvisioned, boolean dismissShade, final ActivityStarter.Callback callback) {
        if (!onlyProvisioned || isDeviceProvisioned()) {
            executeRunnableDismissingKeyguard(new Runnable() {
                public void run() {
                    StatusBar.this.mAssistManager.hideAssist();
                    intent.setFlags(335544320);
                    int result = -96;
                    ActivityOptions options = new ActivityOptions(StatusBar.this.getActivityOptions());
                    if (intent == KeyguardBottomAreaView.INSECURE_CAMERA_INTENT) {
                        ActivityOptionsCompat.setRotationAnimationHint(options, 3);
                    }
                    try {
                        result = ActivityManagerCompat.getService().startActivityAsUser(null, StatusBar.this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(StatusBar.this.mContext.getContentResolver()), null, null, 0, 268435456, null, options.toBundle(), UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("StatusBar", "Unable to start activity", e);
                    }
                    if (callback != null) {
                        callback.onActivityStarted(result);
                    }
                }
            }, new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.onActivityStarted(-96);
                    }
                }
            }, dismissShade, PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mCurrentUserId), true);
        }
    }

    public void readyForKeyguardDone() {
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void executeRunnableDismissingKeyguard(final Runnable runnable, Runnable cancelAction, final boolean dismissShade, boolean afterKeyguardGone, final boolean deferred) {
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
            public boolean onDismiss() {
                if (runnable != null) {
                    if (!StatusBar.this.isKeyguardShowing() || !StatusBar.this.mStatusBarKeyguardViewManager.isOccluded()) {
                        AsyncTask.execute(runnable);
                    } else {
                        StatusBar.this.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
                    }
                }
                if (dismissShade) {
                    if (StatusBar.this.mExpandedVisible) {
                        StatusBar.this.animateCollapsePanels(2, true, true);
                    } else {
                        StatusBar.this.mHandler.post(new Runnable() {
                            public void run() {
                                StatusBar.this.runPostCollapseRunnables();
                            }
                        });
                    }
                } else if (StatusBar.this.isInLaunchTransition() && StatusBar.this.mNotificationPanel.isLaunchTransitionFinished()) {
                    StatusBar.this.mHandler.post(new Runnable() {
                        public void run() {
                            StatusBar.this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
                        }
                    });
                }
                return deferred;
            }
        }, cancelAction, afterKeyguardGone);
    }

    /* access modifiers changed from: private */
    public void changePkgImportance(String pkg, int newValue) {
        this.mNotificationData.changeImportance(pkg, newValue);
        updateNotifications();
        ArrayList<NotificationData.Entry> entries = this.mNotificationData.getPkgNotifications(pkg);
        if (!entries.isEmpty()) {
            updateAppBadgeNum(entries.get(0).notification);
        }
    }

    public void resetUserExpandedStates() {
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int notificationCount = activeNotifications.size();
        for (int i = 0; i < notificationCount; i++) {
            NotificationData.Entry entry = activeNotifications.get(i);
            if (entry.row != null) {
                entry.row.resetUserExpansion();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, boolean afterKeyguardGone) {
        dismissKeyguardThenExecute(action, null, afterKeyguardGone);
    }

    private void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, Runnable cancelAction, boolean afterKeyguardGone) {
        if (isKeyguardShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(action, cancelAction, afterKeyguardGone);
        } else {
            action.onDismiss();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG) {
            Log.v("StatusBar", "configuration changed: " + this.mContext.getResources().getConfiguration());
        }
        if (this.mPreviousConfig == null) {
            this.mPreviousConfig = new Configuration(newConfig);
        }
        int changes = this.mPreviousConfig.updateFrom(newConfig);
        boolean themeChange = Util.isThemeResourcesChanged(changes, newConfig.extraConfig.themeChangedFlags);
        boolean localeChanged = (changes & 4) != 0;
        updateResources(themeChange);
        updateDisplaySize();
        updateRowStates();
        if (CustomizeUtil.HAS_NOTCH) {
            this.mForceBlackObserver.onChange(false);
        }
        this.mScreenPinningRequest.onConfigurationChanged();
        if (themeChange) {
            IconCustomizer.clearCache();
            updateNotificationsOnDensityOrFontScaleChanged();
            inflateDismissView();
            if (NotificationUtil.isUserFold()) {
                this.mStackScroller.removeFoldView();
                inflateFoldView();
            }
            if (this.mHeaderView != null) {
                this.mHeaderView.themeChanged();
            }
        } else if (localeChanged) {
            updateNotificationsOnDensityOrFontScaleChanged();
        }
        this.mOLEDScreenHelper.onConfigurationChanged();
        if (NotificationUtil.isFoldAnimating()) {
            NotificationUtil.setFoldAnimating(false);
        }
    }

    public void userSwitched(int newUserId) {
        setHeadsUpUser(newUserId);
        animateCollapsePanels();
        updatePublicMode();
        this.mNotificationData.filterAndSort();
        if (this.mReinflateNotificationsOnUserSwitched) {
            updateNotificationsOnDensityOrFontScaleChanged();
            this.mReinflateNotificationsOnUserSwitched = false;
        }
        this.mMiuiOptimizationObserver.onChange(false);
        this.mWakeupForNotificationObserver.onChange(false);
        this.mNotificationStyleObserver.onChange(false);
        updateNotificationShade();
        clearCurrentMediaNotification();
        setLockscreenUser(newUserId);
        Intent intent = new Intent("android.intent.action.APPLICATION_MESSAGE_QUERY");
        intent.putExtra("com.miui.extra_update_request_first_time", true);
        this.mContext.sendBroadcast(intent);
        if (Build.VERSION.SDK_INT >= 28 && Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, newUserId) == -1) {
            int value = Settings.Secure.getIntForUser(this.mResolver, "aod_mode", -1, newUserId);
            if (value != -1) {
                Settings.Secure.putIntForUser(this.mResolver, "doze_always_on", value, newUserId);
            }
            Settings.Secure.putIntForUser(this.mResolver, "doze_enabled", 0, newUserId);
        }
    }

    /* access modifiers changed from: protected */
    public void setLockscreenUser(int newUserId) {
        if (this.mLockscreenWallpaper != null) {
            this.mLockscreenWallpaper.setCurrentUser(newUserId);
        }
        this.mScrimController.setCurrentUser(newUserId);
        updateMediaMetaData(true, false);
    }

    /* access modifiers changed from: package-private */
    public void updateResources(boolean isThemeChanged) {
        loadDimens(this.mContext.getResources());
        if (this.mNotificationPanel != null) {
            this.mNotificationPanel.updateResources(isThemeChanged);
        }
        if (this.mQSPanel != null) {
            this.mQSPanel.updateResources(isThemeChanged);
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.updateResources();
        }
        if (this.mLockScreenMagazineController != null) {
            this.mLockScreenMagazineController.updateResources(isThemeChanged);
        }
    }

    /* access modifiers changed from: protected */
    public void loadDimens(Resources res) {
        int oldBarHeight = this.mNaturalBarHeight;
        this.mNaturalBarHeight = res.getDimensionPixelSize(R.dimen.status_bar_height);
        if (!(this.mStatusBarWindowManager == null || this.mNaturalBarHeight == oldBarHeight)) {
            this.mStatusBarWindowManager.setBarHeight(this.mNaturalBarHeight);
        }
        this.mMaxAllowedKeyguardNotifications = res.getInteger(R.integer.keyguard_max_notification_count);
        if (DEBUG) {
            Log.v("StatusBar", "defineSlots");
        }
    }

    /* access modifiers changed from: protected */
    public void handleVisibleToUserChanged(boolean visibleToUser) {
        if (visibleToUser) {
            handleVisibleToUserChangedImpl(visibleToUser);
            this.mNotificationLogger.startNotificationLogging();
            return;
        }
        this.mNotificationLogger.stopNotificationLogging();
        handleVisibleToUserChangedImpl(visibleToUser);
    }

    private void handleVisibleToUserChangedImpl(boolean visibleToUser) {
        if (visibleToUser) {
            try {
                boolean pinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
                boolean clearNotificationEffects = !isPanelFullyCollapsed() && (this.mState == 0 || this.mState == 2);
                int notificationLoad = this.mNotificationData.getActiveNotifications().size();
                if (pinnedHeadsUp && isPanelFullyCollapsed()) {
                    notificationLoad = 1;
                }
                this.mBarService.onPanelRevealed(clearNotificationEffects, notificationLoad);
            } catch (RemoteException e) {
            }
        } else {
            this.mBarService.onPanelHidden();
        }
    }

    public void onKeyguardOccludedChanged(boolean keyguardOccluded) {
        this.mNotificationPanel.onKeyguardOccludedChanged(keyguardOccluded);
    }

    private void logStateToEventlog() {
        boolean isShowing = isKeyguardShowing();
        boolean isOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean canSkipBouncer = this.mUnlockMethodCache.canSkipBouncer();
        int stateFingerprint = getLoggingFingerprint(this.mState, isShowing, isOccluded, isBouncerShowing, isSecure, canSkipBouncer);
        if (stateFingerprint != this.mLastLoggedStateFingerprint) {
            if (this.mStatusBarStateLog == null) {
                this.mStatusBarStateLog = new LogMaker(0);
            }
            MetricsLoggerCompat.write(this.mContext, this.mMetricsLogger, this.mStatusBarStateLog.setCategory(isBouncerShowing ? 197 : 196).setType(isShowing ? 1 : 2).setSubtype(isSecure));
            EventLogTags.writeSysuiStatusBarState(this.mState, isShowing ? 1 : 0, isOccluded ? 1 : 0, isBouncerShowing ? 1 : 0, isSecure ? 1 : 0, canSkipBouncer ? 1 : 0);
            this.mLastLoggedStateFingerprint = stateFingerprint;
        }
    }

    private static int getLoggingFingerprint(int statusBarState, boolean keyguardShowing, boolean keyguardOccluded, boolean bouncerShowing, boolean secure, boolean currentlyInsecure) {
        return (statusBarState & 255) | ((keyguardShowing) << true) | ((keyguardOccluded) << true) | ((bouncerShowing) << true) | ((secure) << true) | ((currentlyInsecure) << true);
    }

    /* access modifiers changed from: package-private */
    public void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250, VIBRATION_ATTRIBUTES);
    }

    public void collapsePanels() {
        makeExpandedInvisible();
    }

    public void postQSRunnableDismissingKeyguard(final Runnable runnable) {
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBar.this.mLeaveOpenOnKeyguardHide = true;
                StatusBar.this.executeRunnableDismissingKeyguard(new Runnable() {
                    public void run() {
                        StatusBar.this.mHandler.post(runnable);
                    }
                }, null, false, false, false);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(final PendingIntent intent) {
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBar.this.startPendingIntentDismissingKeyguard(intent);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(final Intent intent, int delay) {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                StatusBar.this.handleStartActivityDismissingKeyguard(intent, true);
            }
        }, (long) delay);
    }

    /* access modifiers changed from: private */
    public void handleStartActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned) {
        if (intent != null) {
            startActivityDismissingKeyguard(intent, onlyProvisioned, true);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        View notifications;
        int barMode = 1;
        if (command.equals("enter")) {
            this.mDemoMode = true;
        } else if (command.equals("exit")) {
            this.mDemoMode = false;
            checkBarModes();
        } else if (!this.mDemoMode) {
            dispatchDemoCommand("enter", new Bundle());
        }
        boolean modeChange = command.equals("enter") || command.equals("exit");
        if ((modeChange || command.equals("volume")) && this.mVolumeComponent != null) {
            this.mVolumeComponent.dispatchDemoCommand(command, args);
        }
        if (modeChange || command.equals("clock")) {
            dispatchDemoCommandToView(command, args, R.id.clock);
        }
        if (modeChange || command.equals("battery")) {
            this.mBatteryController.dispatchDemoCommand(command, args);
        }
        if (modeChange || command.equals("status")) {
            this.mIconController.dispatchDemoCommand(command, args);
        }
        if (this.mNetworkController != null && (modeChange || command.equals("network"))) {
            this.mNetworkController.dispatchDemoCommand(command, args);
        }
        if (modeChange || command.equals("notifications")) {
            if (this.mStatusBarView == null) {
                notifications = null;
            } else {
                notifications = this.mStatusBarView.findViewById(R.id.notification_icon_area);
            }
            if (notifications != null) {
                notifications.setVisibility(this.mDemoMode ? 4 : 0);
            }
        }
        if (command.equals("bars")) {
            String mode = args.getString("mode");
            if ("opaque".equals(mode)) {
                barMode = 0;
            } else if ("translucent".equals(mode)) {
                barMode = 2;
            } else if (!"semi-transparent".equals(mode)) {
                barMode = "transparent".equals(mode) ? 4 : "warning".equals(mode) ? 5 : -1;
            }
            if (barMode != -1 && this.mStatusBarView != null) {
                this.mStatusBarView.getBarTransitions().transitionTo(barMode, true);
            }
        }
    }

    private void dispatchDemoCommandToView(String command, Bundle args, int id) {
        if (this.mStatusBarView != null) {
            View v = this.mStatusBarView.findViewById(id);
            if (v instanceof DemoMode) {
                ((DemoMode) v).dispatchDemoCommand(command, args);
            }
        }
    }

    public int getBarState() {
        return this.mState;
    }

    public boolean isPanelFullyCollapsed() {
        return this.mNotificationPanel.isFullyCollapsed();
    }

    public boolean isQSFullyCollapsed() {
        return this.mNotificationPanel.isQSFullyCollapsed();
    }

    public void showKeyguard() {
        this.mKeyguardRequested = true;
        updateIsKeyguard();
    }

    public boolean hideKeyguard() {
        this.mKeyguardRequested = false;
        return updateIsKeyguard();
    }

    public void setKeyguardTransparent() {
        if (this.mState == 1) {
            this.mStatusBarWindowManager.setKeygaurdTransparent(true);
            if (this.mDozeServiceHost != null) {
                this.mDozeServiceHost.removeAODView();
            }
        }
    }

    private boolean updateIsKeyguard() {
        boolean shouldBeKeyguard = true;
        boolean keyguardForDozing = this.mDozingRequested && !this.mDeviceInteractive;
        if (!this.mKeyguardRequested && !keyguardForDozing) {
            shouldBeKeyguard = false;
        }
        if (!shouldBeKeyguard) {
            return hideKeyguardImpl();
        }
        showKeyguardImpl();
        return false;
    }

    public void showKeyguardImpl() {
        this.mIsKeyguard = true;
        if (this.mLaunchTransitionFadingAway) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        if (this.mFadeKeyguardWhenUnlockByGxzw) {
            this.mNotificationPanel.animate().cancel();
            this.mFadeKeyguardWhenUnlockByGxzw = false;
            this.mNotificationPanel.setAlpha(1.0f);
            finishKeyguardFadingAway();
            this.mFingerprintUnlockController.finishKeyguardFadingAway();
            this.mStatusBarWindowManager.setKeyguardFadingAway(false);
        }
        this.mHandler.removeMessages(1003);
        if (this.mUserSwitcherController == null || !this.mUserSwitcherController.useFullscreenUserSwitcher()) {
            setBarState(1);
        } else {
            setBarState(3);
        }
        updateKeyguardState(false, false);
        if (this.mState == 1) {
            instantExpandNotificationsPanel();
        } else if (this.mState == 3) {
            instantCollapseNotificationPanel();
        }
        this.mLeaveOpenOnKeyguardHide = false;
        if (this.mDraggedDownRow != null) {
            this.mDraggedDownRow.setUserLocked(false);
            this.mDraggedDownRow.notifyHeightChanged(false);
            this.mDraggedDownRow = null;
        }
        this.mPendingRemoteInputView = null;
        this.mAssistManager.onLockscreenShown();
    }

    /* access modifiers changed from: private */
    public void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        runLaunchTransitionEndRunnable();
        this.mLaunchTransitionFadingAway = false;
        this.mScrimController.forceHideScrims(false);
        updateMediaMetaData(true, true);
    }

    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing();
    }

    public void addPostCollapseAction(Runnable r) {
        this.mPostCollapseRunnables.add(r);
    }

    public void onKeyguardDone() {
        this.mKeyguardNotificationHelper.clear();
    }

    public boolean isInLaunchTransition() {
        return this.mNotificationPanel.isLaunchTransitionRunning() || this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(final Runnable beforeFading, Runnable endRunnable) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = endRunnable;
        Runnable hideRunnable = new Runnable() {
            public void run() {
                StatusBar.this.mLaunchTransitionFadingAway = true;
                if (beforeFading != null) {
                    beforeFading.run();
                }
                StatusBar.this.mScrimController.forceHideScrims(true);
                StatusBar.this.updateMediaMetaData(false, true);
                StatusBar.this.mNotificationPanel.setAlpha(1.0f);
                StatusBar.this.mStackScroller.setParentNotFullyVisible(true);
                StatusBar.this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100).setDuration(300).withLayer().withEndAction(new Runnable() {
                    public void run() {
                        StatusBar.this.onLaunchTransitionFadingEnded();
                    }
                });
                StatusBar.this.mCommandQueue.appTransitionStarting(SystemClock.uptimeMillis(), 120, true);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(hideRunnable);
        } else {
            hideRunnable.run();
        }
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.notifyStartFading();
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0).setDuration(96).setInterpolator(ScrimController.KEYGUARD_FADE_OUT_INTERPOLATOR).start();
    }

    public void fadeKeyguardWhenUnlockByGxzw(final Runnable finished) {
        this.mNotificationPanel.notifyStartFading();
        this.mFadeKeyguardWhenUnlockByGxzw = true;
        fadeViewWhenUnlockByGxzw(this.mNotificationPanel, new Runnable() {
            public void run() {
                boolean unused = StatusBar.this.mFadeKeyguardWhenUnlockByGxzw = false;
                if (finished != null) {
                    finished.run();
                }
            }
        });
    }

    private void fadeViewWhenUnlockByGxzw(View view, final Runnable finished) {
        view.animate().alpha(0.0f).setStartDelay(0).setDuration(300).setInterpolator(new QuarticEaseOutInterpolator()).setListener(new Animator.AnimatorListener() {
            private boolean cancel = false;

            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                if (!this.cancel && finished != null) {
                    finished.run();
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.cancel = true;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        }).start();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000);
    }

    /* access modifiers changed from: private */
    public void onLaunchTransitionTimeout() {
        Log.w("StatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mNotificationPanel.resetViews();
    }

    private void runLaunchTransitionEndRunnable() {
        if (this.mLaunchTransitionEndRunnable != null) {
            Runnable r = this.mLaunchTransitionEndRunnable;
            this.mLaunchTransitionEndRunnable = null;
            r.run();
        }
    }

    public boolean hideKeyguardImpl() {
        this.mIsKeyguard = false;
        Trace.beginSection("StatusBar#hideKeyguard");
        boolean staying = this.mLeaveOpenOnKeyguardHide;
        setBarState(0);
        View viewToClick = null;
        if (this.mLeaveOpenOnKeyguardHide) {
            this.mLeaveOpenOnKeyguardHide = false;
            this.mNotificationPanel.animateToFullShade(calculateGoingToFullShadeDelay());
            this.mDismissView.setVisibility(4);
            if (this.mDraggedDownRow != null) {
                this.mDraggedDownRow.setUserLocked(false);
                this.mDraggedDownRow = null;
            }
            viewToClick = this.mPendingRemoteInputView;
            this.mPendingRemoteInputView = null;
        } else if (!this.mNotificationPanel.isCollapsing()) {
            instantCollapseNotificationPanel();
        }
        updateKeyguardState(staying, false);
        if (viewToClick != null && viewToClick.isAttachedToWindow()) {
            viewToClick.callOnClick();
        }
        if (this.mQSPanel != null) {
            this.mQSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
        this.mNotificationPanel.animate().cancel();
        this.mNotificationPanel.setAlpha(1.0f);
        Trace.endSection();
        return staying;
    }

    private void releaseGestureWakeLock() {
        if (this.mGestureWakeLock.isHeld()) {
            this.mGestureWakeLock.release();
        }
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void keyguardGoingAway() {
        this.mKeyguardGoingAway = true;
        this.mKeyguardMonitor.notifyKeyguardGoingAway(true);
        this.mCommandQueue.appTransitionPending(true);
    }

    public void setKeyguardFadingAway(long startTime, long delay, long fadeoutDuration) {
        long j = delay;
        long j2 = fadeoutDuration;
        boolean z = true;
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = j;
        this.mKeyguardFadingAwayDuration = j2;
        this.mWaitingForKeyguardExit = false;
        this.mCommandQueue.appTransitionStarting((startTime + j2) - 120, 120, true);
        if (j2 <= 0) {
            z = false;
        }
        recomputeDisableFlags(z);
        this.mCommandQueue.appTransitionStarting(startTime - 120, 120, true);
        this.mKeyguardMonitor.notifyKeyguardFadingAway(j, j2);
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardGoingAway = false;
        this.mKeyguardMonitor.notifyKeyguardDoneFading();
    }

    public void stopWaitingForKeyguardExit() {
        this.mWaitingForKeyguardExit = false;
    }

    private void updatePublicMode() {
        boolean devicePublic = isKeyguardShowing() && this.mStatusBarKeyguardViewManager.isSecure(this.mCurrentUserId);
        Log.d("StatusBar", "updatePublicMode() showingKeyguard=" + showingKeyguard + ",devicePublic=" + devicePublic);
        int userId = this.mCurrentProfiles.size() - 1;
        while (true) {
            int i = userId;
            if (i >= 0) {
                int userId2 = this.mCurrentProfiles.valueAt(i).id;
                boolean isProfilePublic = devicePublic;
                if (!devicePublic && userId2 != this.mCurrentUserId && LockPatternUtilsCompat.isSeparateProfileChallengeEnabled(this.mLockPatternUtils, userId2) && this.mStatusBarKeyguardViewManager.isSecure(userId2)) {
                    isProfilePublic = this.mKeyguardManager.isDeviceLocked(userId2);
                    Log.d("StatusBar", "updatePublicMode() isProfilePublic=" + isProfilePublic);
                }
                setLockscreenPublicMode(isProfilePublic, userId2);
                userId = i - 1;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
        Trace.beginSection("StatusBar#updateKeyguardState");
        boolean z = true;
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            this.mNotificationPanel.resetViews();
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(true, fromShadeLocked);
            }
            this.mStatusBarView.removePendingHideExpandedRunnables();
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(false, goingToFullShade || this.mState == 2 || fromShadeLocked);
            }
        }
        if (this.mState == 1 || this.mState == 2) {
            this.mScrimController.setKeyguardShowing(true);
        } else {
            this.mScrimController.setKeyguardShowing(false);
        }
        this.mNotificationPanel.setBarState(this.mState, this.mKeyguardFadingAway, goingToFullShade);
        updateDozingState();
        updatePublicMode();
        updateStackScrollerState(goingToFullShade, fromShadeLocked);
        updateNotifications();
        checkBarModes();
        if (this.mState == 1) {
            z = false;
        }
        updateMediaMetaData(false, z);
        this.mKeyguardMonitor.notifyKeyguardState(isKeyguardShowing(), this.mUnlockMethodCache.isMethodSecure(), this.mStatusBarKeyguardViewManager.isOccluded());
        Trace.endSection();
    }

    private void updateDozingState() {
        Trace.beginSection("StatusBar#updateDozingState");
        boolean animate = !this.mDozing && this.mDozeServiceHost != null && this.mDozeServiceHost.shouldAnimateWakeup();
        this.mNotificationPanel.setDozing(this.mDozing, animate);
        this.mStackScroller.setDark(this.mDozing, animate, this.mWakeUpTouchLocation);
        this.mScrimController.setDozing(this.mDozing);
        this.mKeyguardIndicationController.setDozing(this.mDozing);
        this.mNotificationPanel.setDark(this.mDozing, animate);
        updateQsExpansionEnabled();
        this.mDozeScrimController.setDozing(this.mDozing, animate);
        updateRowStates();
        Trace.endSection();
    }

    public void updateStackScrollerState(boolean goingToFullShade, boolean fromShadeLocked) {
        boolean publicMode;
        if (this.mStackScroller != null) {
            boolean z = true;
            if (this.mState != 1) {
                z = false;
            }
            boolean onKeyguard = z;
            Log.d("StatusBar", "updateStackScrollerState() publicMode=" + publicMode + ",isKeyguardShowing=" + isKeyguardShowing());
            this.mStackScroller.setHideSensitive(publicMode, goingToFullShade);
            this.mStackScroller.setDimmed(onKeyguard, fromShadeLocked);
            this.mStackScroller.setExpandingEnabled(NotificationUtil.isExpandingEnabled(onKeyguard));
            ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
            this.mStackScroller.setActivatedChild(null);
            if (activatedChild != null) {
                activatedChild.makeInactive(false);
            }
        }
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent event) {
        if (this.mState != 1 || !this.mStatusBarKeyguardViewManager.interceptMediaKey(event)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUnlockOnMenuPressed() {
        return this.mDeviceInteractive && this.mState != 0 && this.mStatusBarKeyguardViewManager.shouldDismissOnMenuPressed();
    }

    public boolean onMenuPressed() {
        if (!shouldUnlockOnMenuPressed()) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    public void endAffordanceLaunch() {
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
    }

    public void closeQs() {
        if (this.mNotificationPanel != null) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBar.this.mNotificationPanel.animateCloseQs();
                }
            });
        }
    }

    public boolean onBackPressed() {
        if (this.mStatusBarKeyguardViewManager.onBackPressed()) {
            return true;
        }
        if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs();
            }
            return true;
        } else if (!this.mNotificationPanel.isInCenterScreen()) {
            this.mNotificationPanel.resetViews();
            return true;
        } else if (NotificationUtil.isUserFold() && NotificationUtil.isFold() && !isKeyguardShowing()) {
            closeFold();
            return true;
        } else if (this.mState != 1 && this.mState != 2) {
            animateCollapsePanels();
            AnalyticsHelper.trackStatusBarCollapse("click_back_button");
            return true;
        } else if (this.mKeyguardUserSwitcher.hideIfNotSimple(true)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean onSpacePressed() {
        if (!this.mDeviceInteractive || this.mState == 0) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncerIfKeyguard() {
        if (this.mState == 1 || this.mState == 2) {
            showBouncer();
        }
    }

    /* access modifiers changed from: protected */
    public void showBouncer() {
        this.mWaitingForKeyguardExit = isKeyguardShowing();
        this.mStatusBarKeyguardViewManager.dismiss();
    }

    private void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.expand(false);
    }

    private void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
    }

    public void onActivated(ActivatableNotificationView view) {
        this.mLockscreenGestureLogger.write(this.mContext, 192, 0, 0);
        this.mKeyguardIndicationController.showTransientIndication((int) R.string.notification_tap_again);
        ActivatableNotificationView previousView = this.mStackScroller.getActivatedChild();
        if (previousView != null) {
            previousView.makeInactive(true);
        }
        this.mStackScroller.setActivatedChild(view);
    }

    public void setBarState(int state) {
        if (!(state == 0 || state == 1)) {
            Slog.w("StatusBar", "setBarState: illegal state, state = " + state, new Throwable());
        }
        if (state != this.mState && this.mVisible && (state == 2 || (state == 0 && isGoingToNotificationShade()))) {
            clearNotificationEffects();
        }
        if (state == 1) {
            removeRemoteInputEntriesKeptUntilCollapsed();
            maybeEscalateHeadsUp();
        } else if (this.mIsInDriveMode) {
            this.mMiuiStatusBarPrompt.setState(2);
        }
        this.mState = state;
        updateDriveMode();
        this.mGroupManager.setStatusBarState(state);
        this.mHeadsUpManager.setStatusBarState(state);
        this.mFalsingManager.setStatusBarState(state);
        this.mStatusBarWindowManager.setStatusBarState(state);
        this.mStackScroller.setStatusBarState(state);
        updateReportRejectedTouchVisibility();
        updateDozing();
        this.mNotificationShelf.setStatusBarState(state);
    }

    public void onActivationReset(ActivatableNotificationView view) {
        if (view == this.mStackScroller.getActivatedChild()) {
            this.mKeyguardIndicationController.hideTransientIndication();
            this.mStackScroller.setActivatedChild(null);
        }
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onPanelCollapsed(this.mStackScroller);
        runPostCollapseRunnables();
        if (!isPanelFullyCollapsed()) {
            this.mStatusBarWindowManager.setStatusBarFocusable(true);
        }
        if (NotificationUtil.isFold()) {
            closeFold();
            NotificationUtil.setFoldAnimating(false);
        }
        ((PackageScoreCache) Dependency.get(PackageScoreCache.class)).asyncUpdate();
    }

    public void onTrackingStopped(boolean expand) {
        if ((this.mState == 1 || this.mState == 2) && !expand && !this.mUnlockMethodCache.canSkipBouncer()) {
            if (MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext)) {
                this.mUpdateMonitor.startFaceUnlock(true);
            }
            showBouncerIfKeyguard();
        }
    }

    /* access modifiers changed from: protected */
    public int getMaxKeyguardNotifications(boolean recompute) {
        if (!recompute) {
            return this.mMaxKeyguardNotifications;
        }
        this.mMaxKeyguardNotifications = Math.max(1, this.mNotificationPanel.computeMaxKeyguardNotifications(this.mMaxAllowedKeyguardNotifications));
        return this.mMaxKeyguardNotifications;
    }

    public int getMaxKeyguardNotifications() {
        return getMaxKeyguardNotifications(false);
    }

    public NavigationBarView getNavigationBarView() {
        return this.mNavigationBarView;
    }

    public boolean onDraggedDown(View startingChild, int dragLengthY) {
        if (this.mState != 1 || !hasActiveNotifications() || (isDozing() && !isPulsing())) {
            return false;
        }
        this.mLockscreenGestureLogger.write(this.mContext, 187, (int) (((float) dragLengthY) / this.mDisplayMetrics.density), 0);
        goToLockedShade(startingChild);
        if (startingChild instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) startingChild).onExpandedByGesture(true);
        }
        return true;
    }

    public void onDragDownReset() {
        this.mStackScroller.setDimmed(true, true);
        this.mStackScroller.resetScrollPosition();
        this.mStackScroller.resetCheckSnoozeLeavebehind();
    }

    public void onCrossedThreshold(boolean above) {
        this.mStackScroller.setDimmed(!above, true);
    }

    public void onTouchSlopExceeded() {
        this.mStackScroller.removeLongPressCallback();
        this.mStackScroller.checkSnoozeLeavebehind();
    }

    public void setEmptyDragAmount(float amount) {
        this.mNotificationPanel.setEmptyDragAmount(amount);
    }

    public void goToLockedShade(View expandView) {
        if ((this.mDisabled2 & 4) == 0) {
            int userId = this.mCurrentUserId;
            ExpandableNotificationRow row = null;
            if (expandView instanceof ExpandableNotificationRow) {
                row = (ExpandableNotificationRow) expandView;
                row.setUserExpanded(true, true);
                row.setGroupExpansionChanging(true);
                if (row.getStatusBarNotification() != null) {
                    userId = row.getStatusBarNotification().getUserId();
                }
            }
            boolean fullShadeNeedsBouncer = !userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) || !this.mShowLockscreenNotifications || this.mFalsingManager.shouldEnforceBouncer();
            if (!isLockscreenPublicMode(userId) || !fullShadeNeedsBouncer) {
                this.mNotificationPanel.animateToFullShade(0);
                this.mDismissView.setVisibility(4);
                setBarState(2);
                updateKeyguardState(false, false);
            } else {
                this.mLeaveOpenOnKeyguardHide = true;
                showBouncerIfKeyguard();
                this.mDraggedDownRow = row;
                this.mPendingRemoteInputView = null;
            }
        }
    }

    public void onLockedNotificationImportanceChange(KeyguardHostView.OnDismissAction dismissAction) {
        this.mLeaveOpenOnKeyguardHide = true;
        dismissKeyguardThenExecute(dismissAction, true);
    }

    /* access modifiers changed from: protected */
    public void onLockedRemoteInput(ExpandableNotificationRow row, View clicked) {
        this.mLeaveOpenOnKeyguardHide = true;
        showBouncer();
        this.mPendingRemoteInputView = clicked;
    }

    /* access modifiers changed from: protected */
    public void onMakeExpandedVisibleForRemoteInput(ExpandableNotificationRow row, final View clickedView) {
        if (isKeyguardShowing()) {
            onLockedRemoteInput(row, clickedView);
            return;
        }
        row.setUserExpanded(true);
        row.getPrivateLayout().setOnExpandedVisibleListener(new Runnable() {
            public void run() {
                clickedView.performClick();
            }
        });
    }

    /* access modifiers changed from: protected */
    public boolean startWorkChallengeIfNecessary(int userId, IntentSender intendSender, String notificationKey) {
        this.mPendingWorkRemoteInputView = null;
        Intent newIntent = KeyguardManagerCompat.createConfirmDeviceCredentialIntent(this.mKeyguardManager, null, null, userId);
        if (newIntent == null) {
            return false;
        }
        Intent callBackIntent = new Intent("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        callBackIntent.putExtra("android.intent.extra.INTENT", intendSender);
        callBackIntent.putExtra("android.intent.extra.INDEX", notificationKey);
        callBackIntent.setPackage(this.mContext.getPackageName());
        newIntent.putExtra("android.intent.extra.INTENT", PendingIntent.getBroadcast(this.mContext, 0, callBackIntent, 1409286144).getIntentSender());
        try {
            ActivityManagerCompat.startConfirmDeviceCredentialIntent(newIntent, null);
        } catch (RemoteException e) {
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onLockedWorkRemoteInput(int userId, ExpandableNotificationRow row, View clicked) {
        animateCollapsePanels();
        startWorkChallengeIfNecessary(userId, null, null);
        this.mPendingWorkRemoteInputView = clicked;
    }

    private boolean isAnyProfilePublicMode() {
        for (int i = this.mCurrentProfiles.size() - 1; i >= 0; i--) {
            if (isLockscreenPublicMode(this.mCurrentProfiles.valueAt(i).id)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void onWorkChallengeChanged() {
        updatePublicMode();
        updateNotifications();
        if (this.mPendingWorkRemoteInputView != null && !isAnyProfilePublicMode()) {
            final Runnable clickPendingViewRunnable = new Runnable() {
                public void run() {
                    View pendingWorkRemoteInputView = StatusBar.this.mPendingWorkRemoteInputView;
                    if (pendingWorkRemoteInputView != null) {
                        ViewParent p = pendingWorkRemoteInputView.getParent();
                        while (!(p instanceof ExpandableNotificationRow)) {
                            if (p != null) {
                                p = p.getParent();
                            } else {
                                return;
                            }
                        }
                        final ExpandableNotificationRow row = (ExpandableNotificationRow) p;
                        ViewParent viewParent = row.getParent();
                        if (viewParent instanceof NotificationStackScrollLayout) {
                            final NotificationStackScrollLayout scrollLayout = (NotificationStackScrollLayout) viewParent;
                            row.makeActionsVisibile();
                            row.post(new Runnable() {
                                public void run() {
                                    Runnable finishScrollingCallback = new Runnable() {
                                        public void run() {
                                            StatusBar.this.mPendingWorkRemoteInputView.callOnClick();
                                            View unused = StatusBar.this.mPendingWorkRemoteInputView = null;
                                            scrollLayout.setFinishScrollingCallback(null);
                                        }
                                    };
                                    if (scrollLayout.scrollTo(row)) {
                                        scrollLayout.setFinishScrollingCallback(finishScrollingCallback);
                                    } else {
                                        finishScrollingCallback.run();
                                    }
                                }
                            });
                        }
                    }
                }
            };
            this.mNotificationPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    if (StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarWindow().getHeight() != StatusBar.this.mNotificationPanel.mStatusBar.getStatusBarHeight()) {
                        StatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        StatusBar.this.mNotificationPanel.post(clickPendingViewRunnable);
                    }
                }
            });
            instantExpandNotificationsPanel();
        }
    }

    public void onExpandClicked(NotificationData.Entry clickedEntry, boolean nowExpanded) {
        this.mHeadsUpManager.setExpanded(clickedEntry, nowExpanded);
    }

    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStackScroller.onGoToKeyguard();
            setBarState(1);
            updateKeyguardState(false, true);
        }
    }

    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        this.mBouncerShowing = bouncerShowing;
        if (this.mStatusBarView != null) {
            this.mStatusBarView.setBouncerShowing(bouncerShowing);
        }
        recomputeDisableFlags(true);
    }

    public void onStartedGoingToSleep() {
        this.mStartedGoingToSleep = true;
        this.mUpdateMonitor.stopFaceUnlock();
        this.mNotificationPanel.onStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep() {
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mLaunchCameraOnScreenTurningOn = false;
        this.mStartedGoingToSleep = false;
        this.mDeviceInteractive = false;
        this.mWakeUpComingFromTouch = false;
        this.mWakeUpTouchLocation = null;
        this.mStackScroller.setAnimationsEnabled(false);
        this.mVisualStabilityManager.setScreenOn(false);
        updateVisibleToUser();
        this.mNotificationPanel.setTouchDisabled(true);
        this.mStatusBarWindow.cancelCurrentTouch();
        if (this.mLaunchCameraOnFinishedGoingToSleep) {
            this.mLaunchCameraOnFinishedGoingToSleep = false;
            this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBar.this.onCameraLaunchGestureDetected(StatusBar.this.mLastCameraLaunchSource);
                }
            });
        }
        updateIsKeyguard();
    }

    public void onStartedWakingUp() {
        this.mDeviceInteractive = true;
        this.mStackScroller.setAnimationsEnabled(true);
        this.mVisualStabilityManager.setScreenOn(true);
        this.mNotificationPanel.setTouchDisabled(false);
        this.mNotificationPanel.onStartedWakingUp();
        updateVisibleToUser();
        updateIsKeyguard();
    }

    public void onScreenTurningOn() {
        this.mScreenTurningOn = true;
        this.mFalsingManager.onScreenTurningOn();
        if (this.mLaunchCameraOnScreenTurningOn) {
            this.mNotificationPanel.launchCamera(false, this.mLastCameraLaunchSource);
            this.mLaunchCameraOnScreenTurningOn = false;
        }
    }

    private void vibrateForCameraGesture() {
        this.mVibrator.vibrate(this.mCameraLaunchGestureVibePattern, -1);
    }

    public void onScreenTurnedOn() {
        this.mScreenTurningOn = false;
        this.mDozeScrimController.onScreenTurnedOn();
    }

    public void showScreenPinningRequest(int taskId) {
        if (!this.mKeyguardMonitor.isShowing()) {
            showScreenPinningRequest(taskId, true);
        }
    }

    public void showScreenPinningRequest(int taskId, boolean allowCancel) {
        this.mScreenPinningRequest.showPrompt(taskId, allowCancel);
    }

    public boolean hasActiveNotifications() {
        return !this.mNotificationData.getActiveNotifications().isEmpty();
    }

    public void wakeUpIfDozing(long time, View where) {
        if (this.mDozing) {
            ((PowerManager) this.mContext.getSystemService("power")).wakeUp(time, "com.android.systemui:NODOZE");
            this.mWakeUpComingFromTouch = true;
            where.getLocationInWindow(this.mTmpInt2);
            this.mWakeUpTouchLocation = new PointF((float) (this.mTmpInt2[0] + (where.getWidth() / 2)), (float) (this.mTmpInt2[1] + (where.getHeight() / 2)));
            this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
            this.mFalsingManager.onScreenOnFromTouch();
        }
    }

    public void appTransitionCancelled() {
        RecentsEventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    public void appTransitionFinished() {
        RecentsEventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    public void onCameraLaunchGestureDetected(int source) {
        this.mLastCameraLaunchSource = source;
        if (this.mStartedGoingToSleep) {
            this.mLaunchCameraOnFinishedGoingToSleep = true;
            return;
        }
        if (this.mNotificationPanel.canCameraGestureBeLaunched(isKeyguardShowing() && this.mExpandedVisible)) {
            if (!this.mDeviceInteractive) {
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE");
                this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
            }
            vibrateForCameraGesture();
            if (!isKeyguardShowing()) {
                startActivity(KeyguardBottomAreaView.INSECURE_CAMERA_INTENT, true);
            } else {
                if (!this.mDeviceInteractive) {
                    this.mScrimController.dontAnimateBouncerChangesUntilNextFrame();
                    this.mGestureWakeLock.acquire(6000);
                }
                if (this.mScreenTurningOn || this.mStatusBarKeyguardViewManager.isScreenTurnedOn()) {
                    this.mNotificationPanel.launchCamera(this.mDeviceInteractive, source);
                } else {
                    this.mLaunchCameraOnScreenTurningOn = true;
                }
            }
        }
    }

    public void notifyFpAuthModeChanged() {
        updateDozing();
    }

    /* access modifiers changed from: private */
    public void updateDozing() {
        Trace.beginSection("StatusBar#updateDozing");
        this.mDozing = this.mDozingRequested || this.mFingerprintUnlockController.getMode() == 2;
        if (this.mFingerprintUnlockController.getMode() == 1) {
            this.mDozing = false;
        }
        this.mStatusBarWindowManager.setDozing(this.mDozing);
        this.mStatusBarKeyguardViewManager.setDozing(this.mDozing);
        updateDozingState();
        Trace.endSection();
    }

    public boolean isKeyguardShowing() {
        return this.mStatusBarKeyguardViewManager.isShowing();
    }

    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisionedController.isDeviceProvisioned();
    }

    public boolean isDeviceInVrMode() {
        return this.mVrMode;
    }

    /* access modifiers changed from: private */
    public void processScreenBtnDisableNotification() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (!this.mScreenButtonDisabled || this.mIsFsgMode) {
            notificationManager.cancelAsUser(null, R.drawable.screen_button_notification_icon, new UserHandle(this.mCurrentUserId));
            return;
        }
        Notification screenButtonNotification = NotificationCompat.newBuilder(this.mContext, NotificationChannels.SCREENBUTTON).setWhen(System.currentTimeMillis()).setShowWhen(true).setOngoing(true).setSmallIcon(R.drawable.screen_button_notification_icon).setContentTitle(this.mContext.getString(R.string.screen_button_notification_title)).setContentText(this.mContext.getString(285802685)).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_SCREEN_BUTTONS"), 0)).build();
        screenButtonNotification.extraNotification.setTargetPkg("android");
        notificationManager.notifyAsUser(null, R.drawable.screen_button_notification_icon, screenButtonNotification, new UserHandle(this.mCurrentUserId));
    }

    /* access modifiers changed from: private */
    public boolean handleNotification(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, boolean isUpdate) {
        boolean success = true;
        Notification n = sbn.getNotification();
        if (!NotificationUtil.hasSmallIcon(n) || (n.isGroupSummary() && NotificationUtil.isPkgWontAutoBundle(sbn.getPackageName()))) {
            Logger.fullI("StatusBar", "do not process notification. key=" + sbn.getKey());
            return false;
        }
        ExpandedNotification newSbn = new ExpandedNotification(sbn);
        newSbn.setImportance(NotificationUtil.getPkgImportance(this.mContext, newSbn.getPackageName()));
        try {
            Logger.fullI("StatusBar", "handleNotification key:" + sbn.getKey() + " isUpdate:" + isUpdate);
            if (isUpdate) {
                updateNotification(newSbn, rankingMap);
            } else {
                addNotification(newSbn, rankingMap);
            }
        } catch (InflationException e) {
            handleInflationException(sbn, e);
            success = false;
        }
        return success;
    }

    /* access modifiers changed from: private */
    public void updateCurrentProfilesCache() {
        synchronized (this.mCurrentProfiles) {
            this.mCurrentProfiles.clear();
            if (this.mUserManager != null) {
                for (UserInfo user : this.mUserManager.getProfiles(this.mCurrentUserId)) {
                    this.mCurrentProfiles.put(user.id, user);
                }
            }
        }
    }

    public boolean isNotificationForCurrentProfiles(StatusBarNotification n) {
        int thisUserId = this.mCurrentUserId;
        int notificationUserId = n.getUserId();
        if (DEBUG) {
            Log.v("StatusBar", String.format("%s: current userid: %d, notification userid: %d", new Object[]{n, Integer.valueOf(thisUserId), Integer.valueOf(notificationUserId)}));
        }
        return isCurrentProfile(notificationUserId);
    }

    /* access modifiers changed from: protected */
    public void setNotificationShown(StatusBarNotification n) {
        setNotificationsShown(new String[]{n.getKey()});
    }

    public void setNotificationsShown(String[] keys) {
        try {
            this.mNotificationListener.setNotificationsShown(keys);
        } catch (RuntimeException e) {
            Log.d("StatusBar", "failed setNotificationsShown: ", e);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isCurrentProfile(int userId) {
        boolean z;
        synchronized (this.mCurrentProfiles) {
            if (userId != -1) {
                try {
                    if (this.mCurrentProfiles.get(userId) == null) {
                        z = false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            z = true;
        }
        return z;
    }

    public NotificationGroupManager getGroupManager() {
        return this.mGroupManager;
    }

    public IStatusBarService getBarService() {
        return this.mBarService;
    }

    public void setNotificationSnoozed(StatusBarNotification sbn, NotificationSwipeActionHelper.SnoozeOption snoozeOption) {
        if (snoozeOption.criterion != null) {
            NotificationCompat.snoozeNotification(this.mNotificationListener, sbn.getKey(), snoozeOption.criterion.getId());
        } else {
            NotificationCompat.snoozeNotification(this.mNotificationListener, sbn.getKey(), (long) (snoozeOption.snoozeForMinutes * 60 * 1000));
        }
    }

    private void bindGuts(ExpandableNotificationRow row, NotificationMenuRowPlugin.MenuItem item) {
        final ExpandableNotificationRow expandableNotificationRow = row;
        row.inflateGuts();
        row.setGutsView(item);
        final ExpandedNotification sbn = row.getStatusBarNotification();
        expandableNotificationRow.setTag(sbn.getPackageName());
        final NotificationGuts guts = row.getGuts();
        guts.setClosedListener(new NotificationGuts.OnGutsClosedListener() {
            public void onGutsClosed(NotificationGuts g) {
                if (!g.willBeRemoved() && !expandableNotificationRow.isRemoved()) {
                    StatusBar.this.mStackScroller.onHeightChanged(expandableNotificationRow, !StatusBar.this.isPanelFullyCollapsed());
                }
                if (StatusBar.this.mNotificationGutsExposed == g) {
                    NotificationGuts unused = StatusBar.this.mNotificationGutsExposed = null;
                    NotificationMenuRowPlugin.MenuItem unused2 = StatusBar.this.mGutsMenuItem = null;
                }
            }

            public void onGutsCloseAnimationEnd() {
                if (NotificationUtil.showMiuiStyle() && !expandableNotificationRow.isRemoved()) {
                    expandableNotificationRow.updateLargeIconVisibility(true);
                }
            }
        });
        View gutsView = item.getGutsView();
        if (gutsView instanceof NotificationSnooze) {
            NotificationSnooze snoozeGuts = (NotificationSnooze) gutsView;
            snoozeGuts.setSnoozeListener(this.mStackScroller.getSwipeActionHelper());
            snoozeGuts.setStatusBarNotification(sbn);
            snoozeGuts.setSnoozeOptions(row.getEntry().snoozeCriteria);
            guts.setHeightChangedListener(new NotificationGuts.OnHeightChangedListener() {
                public void onHeightChanged(NotificationGuts guts) {
                    StatusBar.this.mStackScroller.onHeightChanged(expandableNotificationRow, expandableNotificationRow.isShown());
                }
            });
        }
        if (gutsView instanceof NotificationFilter) {
            ((NotificationFilter) gutsView).bindNotification(sbn, new NotificationFilter.ClickListener() {
                public void onClickConfirm(View v) {
                    StatusBar.this.saveAndCloseNotificationMenu(expandableNotificationRow, guts, v);
                    StatusBar.this.saveFiler(sbn);
                }

                public void onClickCancel(View v) {
                    StatusBar.this.saveAndCloseNotificationMenu(expandableNotificationRow, guts, v);
                    StatusBar.this.resetViewForFilter();
                }
            });
        }
        if (gutsView instanceof NotificationInfo) {
            NotificationInfo info = (NotificationInfo) gutsView;
            UserHandle userHandle = sbn.getUser();
            INotificationManager iNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            String pkg = sbn.getPackageName();
            ArraySet<NotificationChannelCompat> channels = new ArraySet<>();
            if (row.getEntry().channel == null) {
                row.getEntry().channel = this.mNotificationData.getChannel(row.getEntry().key);
            }
            channels.add(row.getEntry().channel);
            if (row.isSummaryWithChildren()) {
                List<ExpandableNotificationRow> childrenRows = row.getNotificationChildren();
                int numChildren = childrenRows.size();
                int i = 0;
                while (i < numChildren) {
                    ExpandableNotificationRow childRow = childrenRows.get(i);
                    NotificationChannelCompat childChannel = childRow.getEntry().channel;
                    StatusBarNotification childSbn = childRow.getStatusBarNotification();
                    List<ExpandableNotificationRow> childrenRows2 = childrenRows;
                    if (childSbn.getUser().equals(userHandle) && childSbn.getPackageName().equals(pkg)) {
                        channels.add(childChannel);
                    }
                    i++;
                    childrenRows = childrenRows2;
                }
            }
            int importance = row.getEntry().channel.getImportance();
            final ExpandableNotificationRow expandableNotificationRow2 = expandableNotificationRow;
            final NotificationGuts notificationGuts = guts;
            final NotificationMenuRowPlugin.MenuItem menuItem = item;
            ArrayList arrayList = new ArrayList(channels);
            final UserHandle userHandle2 = userHandle;
            AnonymousClass84 r0 = new NotificationInfo.ClickListener() {
                public void onClickSettings(View v, NotificationChannelCompat channel, int appUid) {
                    StatusBar.this.saveAndCloseNotificationMenu(expandableNotificationRow2, notificationGuts, v);
                    StatusBar.this.onClickMenuSettings(expandableNotificationRow2, menuItem);
                }

                public void onClickDone(View v) {
                    StatusBar.this.saveAndCloseNotificationMenu(expandableNotificationRow2, notificationGuts, v);
                }

                public void onClickCheckSave(final Runnable saveImportance) {
                    if (!StatusBar.this.isLockscreenPublicMode(userHandle2.getIdentifier()) || !(StatusBar.this.mState == 1 || StatusBar.this.mState == 2)) {
                        saveImportance.run();
                    } else {
                        StatusBar.this.onLockedNotificationImportanceChange(new KeyguardHostView.OnDismissAction() {
                            public boolean onDismiss() {
                                saveImportance.run();
                                return true;
                            }
                        });
                    }
                }
            };
            info.bindNotification(iNotificationManager, arrayList, importance, sbn, r0);
        }
    }

    /* access modifiers changed from: private */
    public void saveAndCloseNotificationMenu(ExpandableNotificationRow row, NotificationGuts guts, View done) {
        guts.resetFalsingCheck();
        int[] rowLocation = new int[2];
        int[] doneLocation = new int[2];
        row.getLocationOnScreen(rowLocation);
        done.getLocationOnScreen(doneLocation);
        closeAndSaveGuts(false, false, true, (doneLocation[0] - rowLocation[0]) + (done.getWidth() / 2), (doneLocation[1] - rowLocation[1]) + (done.getHeight() / 2), true);
    }

    /* access modifiers changed from: protected */
    public SwipeHelper.LongPressListener getNotificationLongClicker() {
        return new SwipeHelper.LongPressListener() {
            public boolean onLongPress(View v, int x, int y, NotificationMenuRowPlugin.MenuItem item) {
                if (!(v instanceof ExpandableNotificationRow)) {
                    return false;
                }
                if (v.getWindowToken() == null) {
                    Log.e("StatusBar", "Trying to show notification guts, but not attached to window");
                    return false;
                }
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                if (row.isDark()) {
                    return false;
                }
                if (row.isExpandable() && !NotificationUtil.isExpandingEnabled(StatusBar.this.isKeyguardShowing())) {
                    row.getExpandClickListener().onClick(row);
                    return true;
                } else if (NotificationUtil.isExpandingEnabled(StatusBar.this.isKeyguardShowing())) {
                    return StatusBar.this.updateGutsState(row, x, y, item);
                } else {
                    return false;
                }
            }
        };
    }

    /* access modifiers changed from: protected */
    public SwipeHelper.MenuPressListener getNotificationMenuClicker() {
        return new SwipeHelper.MenuPressListener() {
            public boolean onMenuPress(View v, int x, int y, NotificationMenuRowPlugin.MenuItem item) {
                return StatusBar.this.updateGutsState((ExpandableNotificationRow) v, x, y, item);
            }
        };
    }

    /* access modifiers changed from: private */
    public void onClickMenuSettings(ExpandableNotificationRow row, NotificationMenuRowPlugin.MenuItem item) {
        if (item.getGutsView() instanceof NotificationInfo) {
            NotificationData.Entry entry = row.getEntry();
            String pkg = entry.notification.getPackageName();
            int userId = UserHandle.getUserId(NotificationUtil.getUid(entry.notification));
            String messageId = NotificationUtil.getMessageId(entry.notification);
            PackageManager packageManager = this.mContext.getPackageManager();
            boolean started = false;
            if (NotificationUtil.isHybrid(entry.notification)) {
                Intent intent = new Intent(APP_NOTIFICATION_PREFS_CATEGORY_INTENT).setPackage(pkg);
                List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
                if (resolveInfos.size() > 0) {
                    ActivityInfo activityInfo = resolveInfos.get(0).activityInfo;
                    intent.setClassName(activityInfo.packageName, activityInfo.name);
                    intent.addFlags(32768);
                    intent.addFlags(268435456);
                    intent.putExtra("appName", "");
                    intent.putExtra("packageName", pkg);
                    intent.putExtra("userId", userId);
                    intent.putExtra("messageId", messageId);
                    intent.putExtra("notificationId", "");
                    intent.putExtra("miui.category", NotificationUtil.getCategory(entry.notification));
                    try {
                        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                        started = true;
                    } catch (ActivityNotFoundException e) {
                        Log.e("StatusBar", "Failed startActivityAsUser() ", e);
                    }
                }
            }
            if (!started) {
                Intent intent2 = new Intent("android.intent.action.MAIN");
                intent2.setClassName("com.android.settings", "com.android.settings.Settings$NotificationFilterActivity");
                intent2.addFlags(32768);
                intent2.addFlags(268435456);
                intent2.putExtra("appName", row.getEntry().notification.getAppName());
                intent2.putExtra("packageName", pkg);
                intent2.putExtra("userId", userId);
                intent2.putExtra("messageId", messageId);
                intent2.putExtra("notificationId", "");
                try {
                    this.mContext.startActivityAsUser(intent2, UserHandle.CURRENT);
                } catch (ActivityNotFoundException e2) {
                    Log.e("StatusBar", "Failed startActivityAsUser() ", e2);
                }
            }
            animateCollapsePanels(0, true);
        }
    }

    /* access modifiers changed from: private */
    public boolean updateGutsState(ExpandableNotificationRow row, int x, int y, NotificationMenuRowPlugin.MenuItem item) {
        if (row.areGutsExposed()) {
            closeAndSaveGuts(false, false, true, -1, -1, true);
            return false;
        }
        ExpandableNotificationRow expandableNotificationRow = row;
        NotificationMenuRowPlugin.MenuItem menuItem = item;
        bindGuts(expandableNotificationRow, menuItem);
        NotificationGuts guts = expandableNotificationRow.getGuts();
        if (guts == null) {
            return false;
        }
        MetricsLogger.action(this.mContext, 204);
        guts.setVisibility(4);
        final ExpandableNotificationRow expandableNotificationRow2 = expandableNotificationRow;
        final NotificationGuts notificationGuts = guts;
        final int i = x;
        final int i2 = y;
        final NotificationMenuRowPlugin.MenuItem menuItem2 = menuItem;
        AnonymousClass87 r0 = new Runnable() {
            public void run() {
                if (expandableNotificationRow2.getWindowToken() == null) {
                    Log.e("StatusBar", "Trying to show notification guts, but not attached to window");
                    return;
                }
                StatusBar.this.closeAndSaveGuts(true, true, true, -1, -1, false);
                boolean needsFalsingProtection = false;
                notificationGuts.setVisibility(0);
                if (NotificationUtil.showMiuiStyle()) {
                    expandableNotificationRow2.updateLargeIconVisibility(false);
                }
                Animator a = ViewAnimationUtils.createCircularReveal(notificationGuts, i, i2, 0.0f, (float) Math.hypot((double) Math.max(notificationGuts.getWidth() - i, i), (double) Math.max(notificationGuts.getHeight() - i2, i2)));
                a.setDuration(360);
                a.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
                a.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        expandableNotificationRow2.resetTranslation();
                        notificationGuts.setIsAnimating(false);
                    }
                });
                a.start();
                notificationGuts.setIsAnimating(true);
                if (StatusBar.this.mState == 1 && !StatusBar.this.mAccessibilityManager.isTouchExplorationEnabled()) {
                    needsFalsingProtection = true;
                }
                notificationGuts.setExposed(true, needsFalsingProtection);
                expandableNotificationRow2.closeRemoteInput();
                StatusBar.this.mStackScroller.onHeightChanged(expandableNotificationRow2, true);
                NotificationGuts unused = StatusBar.this.mNotificationGutsExposed = notificationGuts;
                NotificationMenuRowPlugin.MenuItem unused2 = StatusBar.this.mGutsMenuItem = menuItem2;
            }
        };
        guts.post(r0);
        return true;
    }

    /* access modifiers changed from: private */
    public void saveFiler(ExpandedNotification notification) {
        String pkg = notification.getFoldPackageName();
        if (NotificationUtil.isUserFold()) {
            int targetImportance = NotificationUtil.isFold() ? 1 : -1;
            NotificationUtil.setPkgImportance(this.mContext, pkg, targetImportance);
            this.mNotificationData.changeImportance(pkg, targetImportance);
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onSetImportance(notification, targetImportance);
        } else {
            blockAndTrackNotification(notification);
        }
        if (!NotificationUtil.isFold() || !shouldCloseFoldWhenSaveFiler(pkg)) {
            updateNotifications();
        } else {
            closeFold();
        }
        updateAppBadgeNum(notification);
    }

    private boolean shouldCloseFoldWhenSaveFiler(String pkg) {
        Iterator<NotificationData.Entry> it = this.mNotificationData.getActiveNotifications().iterator();
        while (it.hasNext()) {
            if (!it.next().notification.getFoldPackageName().equals(pkg)) {
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void updateAppBadgeNum(ExpandedNotification notification) {
        if (notification != null) {
            int num = 0;
            int userId = notification.getUser().getIdentifier();
            String pkgName = notification.getPackageName();
            CharSequence className = NotificationUtil.getMessageClassName(notification);
            if (NotificationFilterHelper.isAllowed(this.mContext, notification, "_message")) {
                ArrayList<NotificationData.Entry> entries = this.mNotificationData.getPkgNotifications(pkgName);
                if (NotificationUtil.isMissedCallNotification(notification)) {
                    Iterator<NotificationData.Entry> it = entries.iterator();
                    while (it.hasNext()) {
                        NotificationData.Entry entry = it.next();
                        if (NotificationUtil.isMissedCallNotification(entry.notification) && needStatBadgeNum(entry, notification)) {
                            num += entry.notification.getNotification().extraNotification.getMessageCount();
                        }
                    }
                    pkgName = "com.android.contacts";
                    className = ".activities.TwelveKeyDialer";
                } else {
                    Iterator<NotificationData.Entry> it2 = entries.iterator();
                    while (it2.hasNext()) {
                        NotificationData.Entry entry2 = it2.next();
                        if (entry2.notification.getPackageName().equals(pkgName) && TextUtils.equals(NotificationUtil.getMessageClassName(entry2.notification), className) && needStatBadgeNum(entry2, notification)) {
                            num += entry2.notification.getNotification().extraNotification.getMessageCount();
                        }
                    }
                }
            }
            updateAppBadgeNum(pkgName, className, num, userId);
            return;
        }
        updateAppBadgeNum(null, null, 0, 0);
    }

    private boolean needStatBadgeNum(NotificationData.Entry entry, ExpandedNotification notification) {
        return UserHandle.isSameUser(entry.notification.getUid(), notification.getUid()) && NotificationUtil.needStatBadgeNum(entry) && !entry.isMediaNotification() && !this.mGroupManager.isSummaryHasChildren(entry.notification);
    }

    private void updateAppBadgeNum(String packageName, CharSequence className, int num, int userId) {
        String componentName;
        if (packageName == null) {
            componentName = "";
        } else {
            componentName = packageName + "/" + className;
        }
        Intent intent = new Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE");
        intent.putExtra("android.intent.extra.update_application_message_text", num > 0 ? String.valueOf(num) : null);
        intent.putExtra("android.intent.extra.update_application_component_name", componentName);
        intent.putExtra("userId", userId);
        intent.putExtra("targetPkg", className);
        intent.putExtra("miui.intent.extra.application_show_corner", NotificationFilterHelper.isAllowed(this.mContext, packageName, "_message"));
        intent.setPackage("com.miui.home");
        Log.d("StatusBar", "update app badge num: " + componentName + ",num=" + num + ",isAllowed=" + isAllowed + ",userId=" + userId);
        if (userId == -1) {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    private void blockAndTrackNotification(ExpandedNotification notification) {
        String pkgName = notification.getPackageName();
        NotificationFilterHelper.enableNotifications(this.mContextForUser, pkgName, NotificationUtil.getUid(notification), false);
        if (NotificationUtil.isXmsfCategory(notification)) {
            Intent intent = new Intent("com.xiaomi.xmsf.action.SET_SHIELD_STATUS");
            intent.setComponent(new ComponentName("com.xiaomi.xmsf", "com.xiaomi.xmsf.typedNotificationShield.NotificationShieldService"));
            intent.putExtra("key.setShield.type", NotificationUtil.getCategory(notification));
            intent.putExtra("key.setShield.status", true);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        }
        filterPackageNotifications(pkgName);
    }

    /* access modifiers changed from: private */
    public void filterPackageNotifications(String pkgName) {
        Iterator<NotificationData.Entry> it = this.mNotificationData.getPkgNotifications(pkgName).iterator();
        while (it.hasNext()) {
            filterNotification(it.next().notification);
        }
    }

    /* access modifiers changed from: private */
    public void resetViewForFilter() {
    }

    public NotificationGuts getExposedGuts() {
        return this.mNotificationGutsExposed;
    }

    public void closeAndSaveGuts(boolean removeLeavebehinds, boolean force, boolean removeControls, int x, int y, boolean resetMenu) {
        if (this.mNotificationGutsExposed != null) {
            this.mNotificationGutsExposed.closeControls(removeLeavebehinds, removeControls, x, y, force);
        }
        if (resetMenu) {
            this.mStackScroller.resetExposedMenuView(false, true);
        }
    }

    public void toggleSplitScreen() {
        toggleSplitScreenMode(-1, -1);
    }

    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    public void dismissKeyboardShortcutsMenu() {
        this.mHandler.removeMessages(1027);
        this.mHandler.sendEmptyMessage(1027);
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
        this.mHandler.removeMessages(1026);
        this.mHandler.obtainMessage(1026, deviceId, 0).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void toggleKeyboardShortcuts(int deviceId) {
        KeyboardShortcuts.toggle(this.mContext, deviceId);
    }

    /* access modifiers changed from: protected */
    public void dismissKeyboardShortcuts() {
        KeyboardShortcuts.dismiss();
    }

    public void setLockscreenPublicMode(boolean publicMode, int userId) {
        this.mLockscreenPublicMode.put(userId, publicMode);
    }

    public boolean isLockscreenPublicMode(int userId) {
        if (userId == -1) {
            return this.mLockscreenPublicMode.get(this.mCurrentUserId, false);
        }
        return this.mLockscreenPublicMode.get(userId, false);
    }

    public boolean userAllowsNotificationsInPublic(int userHandle) {
        boolean allowed = true;
        if (isCurrentProfile(userHandle) && userHandle != this.mCurrentUserId) {
            return true;
        }
        if (this.mUsersAllowingNotifications.indexOfKey(userHandle) >= 0) {
            return this.mUsersAllowingNotifications.get(userHandle);
        }
        boolean allowedByUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0, userHandle) != 0;
        boolean allowedByDpm = adminAllowsKeyguardFeature(userHandle, 4);
        if (!allowedByUser || !allowedByDpm) {
            allowed = false;
        }
        this.mUsersAllowingNotifications.append(userHandle, allowed);
        return allowed;
    }

    public boolean userAllowsPrivateNotificationsInPublic(int userHandle) {
        boolean allowed = true;
        if (userHandle == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(userHandle) >= 0) {
            return this.mUsersAllowingPrivateNotifications.get(userHandle);
        }
        boolean allowedByUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, userHandle) != 0;
        boolean allowedByDpm = adminAllowsKeyguardFeature(userHandle, 8);
        if (!allowedByUser || !allowedByDpm) {
            allowed = false;
        }
        this.mUsersAllowingPrivateNotifications.append(userHandle, allowed);
        return allowed;
    }

    private boolean adminAllowsKeyguardFeature(int userHandle, int feature) {
        boolean z = true;
        if (userHandle == -1) {
            return true;
        }
        if ((this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, userHandle) & feature) != 0) {
            z = false;
        }
        return z;
    }

    public boolean shouldHideNotifications(int userId) {
        return (isLockscreenPublicMode(userId) && !userAllowsNotificationsInPublic(userId)) || (userId != this.mCurrentUserId && shouldHideNotifications(this.mCurrentUserId));
    }

    public boolean shouldHideNotifications(String key) {
        return isLockscreenPublicMode(this.mCurrentUserId) && this.mNotificationData.getVisibilityOverride(key) == -1;
    }

    public boolean isSecurelyLocked(int userId) {
        return isLockscreenPublicMode(userId);
    }

    public void onNotificationClear(ExpandedNotification notification) {
        this.mNotificationData.performRemoveNotification(notification);
    }

    public void onPanelLaidOut() {
        if (this.mState == 1 && getMaxKeyguardNotifications(false) != getMaxKeyguardNotifications(true)) {
            updateRowStates();
        }
    }

    /* access modifiers changed from: protected */
    public void inflateViews(final NotificationData.Entry entry, ViewGroup parent) {
        final ExpandedNotification sbn = entry.notification;
        if (entry.row != null) {
            entry.reset();
            updateNotification(entry, sbn, entry.row);
            return;
        }
        new RowInflaterTask().inflate(this.mContext, parent, entry, new RowInflaterTask.RowInflationFinishedListener() {
            public void onInflationFinished(ExpandableNotificationRow row) {
                StatusBar.this.bindRow(entry, row);
                StatusBar.this.updateNotification(entry, sbn, row);
            }
        });
    }

    /* access modifiers changed from: private */
    public void bindRow(NotificationData.Entry entry, final ExpandableNotificationRow row) {
        row.setExpansionLogger(this, entry.notification.getKey());
        row.setGroupManager(this.mGroupManager);
        row.setHeadsUpManager(this.mHeadsUpManager);
        row.setRemoteInputController(this.mRemoteInputController);
        row.setOnExpandClickListener(this);
        row.setRemoteViewClickHandler(this.mOnClickHandler);
        row.setInflationCallback(this);
        row.setInCallCallback(this);
        row.setAppName(entry.notification.getAppName());
        row.setOnDismissRunnable(new Runnable() {
            public void run() {
                ((SystemUIStat) Dependency.get(SystemUIStat.class)).onRemove(row, StatusBar.this.mIsKeyguard ? "keyguard" : "statusbar");
                StatusBar.this.performRemoveNotification(row.getStatusBarNotification());
            }
        });
        row.setDescendantFocusability(393216);
        if (ENABLE_REMOTE_INPUT) {
            row.setDescendantFocusability(131072);
        }
    }

    /* access modifiers changed from: private */
    public void updateNotification(NotificationData.Entry entry, StatusBarNotification sbn, ExpandableNotificationRow row) {
        row.setNeedsRedaction(needsRedaction(entry));
        boolean useIncreasedHeadsUp = false;
        boolean isLowPriority = this.mNotificationData.isAmbient(sbn.getKey()) && !sbn.getNotification().isGroupSummary();
        boolean isUpdate = this.mNotificationData.get(entry.key) != null;
        boolean wasLowPriority = row.isLowPriority();
        row.setIsLowPriority(isLowPriority);
        row.setLowPriorityStateUpdated(isUpdate && wasLowPriority != isLowPriority);
        this.mNotificationClicker.register(row, sbn);
        entry.targetSdk = entry.notification.getTargetSdk();
        row.setLegacy(entry.targetSdk >= 9 && entry.targetSdk < 21);
        entry.autoRedacted = entry.notification.getNotification().publicVersion == null;
        entry.row = row;
        entry.row.setOnActivatedListener(this);
        if (this.mMessagingUtil.isImportantMessaging(sbn, this.mNotificationData.getImportance(entry)) && this.mPanelExpanded) {
            useIncreasedHeadsUp = true;
        }
        row.setUseIncreasedCollapsedHeight(shouldUseIncreaedColleapsedHeight(sbn));
        row.setUseIncreasedHeadsUpHeight(useIncreasedHeadsUp);
        row.updateNotification(entry);
    }

    private boolean shouldUseIncreaedColleapsedHeight(StatusBarNotification sbn) {
        return !sbn.getNotification().extras.containsKey("android.template") && sbn.getNotification().extras.containsKey("android.progress");
    }

    /* access modifiers changed from: private */
    public void processForRemoteInput(Notification n) {
        if (ENABLE_REMOTE_INPUT && n.extras != null && n.extras.containsKey("android.wearable.EXTENSIONS") && (n.actions == null || n.actions.length == 0)) {
            List<Notification.Action> actions = new Notification.WearableExtender(n).getActions();
            int numActions = actions.size();
            Notification.Action viableAction = null;
            for (int i = 0; i < numActions; i++) {
                Notification.Action action = actions.get(i);
                if (action != null) {
                    RemoteInput[] remoteInputs = action.getRemoteInputs();
                    if (remoteInputs == null) {
                        continue;
                    } else {
                        int length = remoteInputs.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                break;
                            } else if (remoteInputs[i2].getAllowFreeFormInput()) {
                                viableAction = action;
                                break;
                            } else {
                                i2++;
                            }
                        }
                        if (viableAction != null) {
                            break;
                        }
                    }
                }
            }
            if (viableAction != null) {
                Notification.Builder rebuilder = NotificationCompat.recoverBuilder(this.mContext, n);
                rebuilder.setActions(new Notification.Action[]{viableAction});
                rebuilder.build();
            }
        }
    }

    public void startPendingIntentDismissingKeyguard(final PendingIntent intent) {
        if (isDeviceProvisioned()) {
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                public boolean onDismiss() {
                    new Thread() {
                        public void run() {
                            try {
                                ActivityManagerCompat.getService().resumeAppSwitches();
                            } catch (RemoteException e) {
                            }
                            try {
                                intent.send(null, 0, null, null, null, null, StatusBar.this.getActivityOptions());
                            } catch (PendingIntent.CanceledException e2) {
                                Log.w("StatusBar", "Sending intent failed: " + e2);
                            }
                            if (intent.isActivity()) {
                                StatusBar.this.mAssistManager.hideAssist();
                            }
                        }
                    }.start();
                    StatusBar.this.animateCollapsePanels(2, true, true);
                    StatusBar.this.visibilityChanged(false);
                    return true;
                }
            }, intent.isActivity() && PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent.getIntent(), this.mCurrentUserId));
        }
    }

    /* access modifiers changed from: protected */
    public Bundle getActivityOptions() {
        ActivityOptions options = ActivityOptions.makeBasic();
        ActivityOptionsCompat.setLaunchStackId(options, 1, 4, -1);
        return options.toBundle();
    }

    /* access modifiers changed from: protected */
    public void visibilityChanged(boolean visible) {
        if (this.mVisible != visible) {
            this.mVisible = visible;
            if (!visible) {
                closeAndSaveGuts(true, true, true, -1, -1, true);
            }
        }
        updateVisibleToUser();
    }

    /* access modifiers changed from: protected */
    public void updateVisibleToUser() {
        boolean oldVisibleToUser = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible && this.mDeviceInteractive;
        if (oldVisibleToUser != this.mVisibleToUser) {
            handleVisibleToUserChanged(this.mVisibleToUser);
        }
    }

    private void updateFsgState() {
        this.mHandler.removeMessages(1004);
        this.mHandler.sendEmptyMessageDelayed(1004, 10);
    }

    /* access modifiers changed from: private */
    public void onUpdateFsgState() {
        boolean shouldDisable = this.mExpandedVisible && !this.mInPinnedMode;
        if (this.mIsFsgMode && !this.mIsKeyguard && shouldDisable != this.mShouldDisableFsgMode) {
            Utils.updateFsgState(this.mContext, "typefrom_status_bar_expansion", shouldDisable);
        }
        this.mShouldDisableFsgMode = shouldDisable;
    }

    public void clearNotificationEffects() {
        try {
            this.mBarService.clearNotificationEffects();
        } catch (RemoteException e) {
        }
    }

    /* access modifiers changed from: package-private */
    public void handleNotificationError(StatusBarNotification n, String message) {
        removeNotification(n.getKey(), null);
        try {
            this.mBarService.onNotificationError(n.getPackageName(), n.getTag(), n.getId(), n.getUid(), n.getInitialPid(), message, n.getUserId());
        } catch (RemoteException e) {
        }
    }

    /* access modifiers changed from: protected */
    public ExpandedNotification removeNotificationViews(String key, NotificationListenerService.RankingMap ranking) {
        NotificationData.Entry entry = this.mNotificationData.remove(key, ranking);
        if (entry == null) {
            Log.w("StatusBar", "removeNotification for unknown key: " + key);
            return null;
        }
        updateNotifications();
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(entry);
        return entry.notification;
    }

    /* access modifiers changed from: protected */
    public NotificationData.Entry createNotificationViews(ExpandedNotification sbn) throws InflationException {
        if (DEBUG) {
            Log.d("StatusBar", "createNotificationViews(notification=" + sbn);
        }
        NotificationData.Entry entry = new NotificationData.Entry(sbn);
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackInstance(entry);
        entry.createIcons(this.mContext, sbn);
        inflateViews(entry, this.mStackScroller);
        return entry;
    }

    /* access modifiers changed from: protected */
    public void addNotificationViews(NotificationData.Entry entry) {
        if (entry != null) {
            this.mNotificationData.add(entry);
            ((NotificationsMonitor) Dependency.get(NotificationsMonitor.class)).notifyNotificationArrived(entry.notification);
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onArrive(entry.notification);
            updateNotifications();
        }
    }

    /* access modifiers changed from: protected */
    public void updateRowStates() {
        int N = this.mStackScroller.getChildCount();
        int visibleNotifications = 0;
        boolean z = false;
        boolean z2 = true;
        boolean onKeyguard = this.mState == 1;
        int maxNotifications = -1;
        if (this.mStatusBarFragment != null && onKeyguard) {
            maxNotifications = getMaxKeyguardNotifications(true);
        }
        this.mStackScroller.setMaxDisplayedNotifications(maxNotifications);
        Stack<ExpandableNotificationRow> stack = new Stack<>();
        for (int i = N - 1; i >= 0; i--) {
            View child = this.mStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                stack.push((ExpandableNotificationRow) child);
            }
        }
        while (stack.isEmpty() == 0) {
            ExpandableNotificationRow row = stack.pop();
            NotificationData.Entry entry = row.getEntry();
            boolean childNotification = this.mGroupManager.isChildInGroupWithSummary(entry.notification);
            if (onKeyguard) {
                row.setOnKeyguard(z2);
            } else {
                row.setOnKeyguard(z);
                row.setSystemExpanded((visibleNotifications != 0 || childNotification || row.getStatusBarNotification().isFold()) ? z : z2);
            }
            entry.row.setShowAmbient(isDozing());
            int userId = entry.notification.getUserId();
            boolean suppressedSummary = (!this.mGroupManager.isSummaryOfSuppressedGroup(entry.notification) || entry.row.isRemoved()) ? z : z2;
            if (!isKeyguardShowing()) {
                entry.notification.setHasShownAfterUnlock(z2);
            }
            boolean showOnKeyguard = shouldShowOnKeyguard(entry);
            if (suppressedSummary || ((isLockscreenPublicMode(userId) && !this.mShowLockscreenNotifications) || (onKeyguard && !showOnKeyguard))) {
                entry.row.setVisibility(8);
            } else {
                boolean wasGone = entry.row.getVisibility() == 8 ? z2 : z;
                if (wasGone) {
                    entry.row.setVisibility(z ? 1 : 0);
                }
                if (!childNotification && !entry.row.isRemoved()) {
                    if (wasGone) {
                        this.mStackScroller.generateAddAnimation(entry.row, !showOnKeyguard);
                    }
                    visibleNotifications++;
                }
            }
            if (row.isSummaryWithChildren()) {
                List<ExpandableNotificationRow> notificationChildren = row.getNotificationChildren();
                for (int i2 = notificationChildren.size() - 1; i2 >= 0; i2--) {
                    stack.push(notificationChildren.get(i2));
                }
            }
            z = false;
            z2 = true;
        }
        this.mNotificationPanel.setNoVisibleNotifications(visibleNotifications == 0);
    }

    public boolean shouldShowOnKeyguard(NotificationData.Entry entry) {
        return this.mShowLockscreenNotifications && !this.mNotificationData.isAmbient(entry.notification.getKey()) && isEnableKeyguard(entry);
    }

    private boolean isEnableKeyguard(NotificationData.Entry entry) {
        ExpandedNotification notification = entry.notification;
        return (entry.isMediaNotification() || NotificationUtil.isCts(notification) || (!notification.hasShownAfterUnlock() && !isLonelyAutoGroupSummaryOnKeyguard(entry) && isAllowShownOnKeyguard(notification) && notification.getNotification().extraNotification.isEnableKeyguard())) && !entry.notification.isFold();
    }

    private boolean isAllowShownOnKeyguard(ExpandedNotification notification) {
        String channelId = NotificationCompat.getChannelId(notification.getNotification());
        if (TextUtils.isEmpty(channelId)) {
            return NotificationFilterHelper.isAllowed(this.mContext, notification.getPackageName(), "_keyguard");
        }
        return NotificationFilterHelper.isAllowedWithChannel(this.mContext, "android".equals(notification.getNotification().extraNotification.getTargetPkg()) ? notification.getPackageName() : notification.getBasePkg(), channelId, "_keyguard");
    }

    private boolean isLonelyAutoGroupSummaryOnKeyguard(NotificationData.Entry entry) {
        if (!StatusBarNotificationCompat.isAutoGroupSummary(entry.notification)) {
            return false;
        }
        NotificationGroupManager.NotificationGroup group = this.mGroupManager.getNotificationGroup(entry.notification.getGroupKey());
        if (group == null) {
            return true;
        }
        Iterator<NotificationData.Entry> iterator = group.children.iterator();
        while (iterator.hasNext()) {
            NotificationData.Entry child = iterator.next();
            if (!this.mGroupManager.isSummaryOfGroup(child.notification) && isEnableKeyguard(child)) {
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void setShowLockscreenNotifications(boolean show) {
        this.mShowLockscreenNotifications = show;
    }

    /* access modifiers changed from: protected */
    public void setLockScreenAllowRemoteInput(boolean allowLockscreenRemoteInput) {
        this.mAllowLockscreenRemoteInput = allowLockscreenRemoteInput;
    }

    /* access modifiers changed from: private */
    public void updateLockscreenNotificationSetting() {
        boolean z = false;
        boolean show = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        int dpmFlags = this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId);
        setShowLockscreenNotifications(show && ((dpmFlags & 4) == 0));
        if (ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT) {
            boolean remoteInput = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_remote_input", 0, this.mCurrentUserId) != 0;
            boolean remoteInputDpm = (dpmFlags & 64) == 0;
            if (remoteInput && remoteInputDpm) {
                z = true;
            }
            setLockScreenAllowRemoteInput(z);
            return;
        }
        setLockScreenAllowRemoteInput(false);
    }

    public void updateNotification(ExpandedNotification notification, NotificationListenerService.RankingMap ranking) throws InflationException {
        if (!filterNotification(notification)) {
            Log.d("StatusBar", "updateNotification(" + notification + ")");
            String key = notification.getKey();
            if (!notification.isFold()) {
                this.mBgHandler.obtainMessage(2001, key).sendToTarget();
            }
            abortExistingInflation(key);
            NotificationData.Entry entry = this.mNotificationData.get(key);
            if (entry != null) {
                entry.hideSensitiveByAppLock = isHideSensitiveByAppLock(notification);
                this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
                this.mRemoteInputEntriesToRemoveOnCollapse.remove(entry);
                Notification notification2 = notification.getNotification();
                this.mNotificationData.updateRanking(ranking);
                ExpandedNotification oldNotification = entry.notification;
                entry.notification = notification;
                entry.needUpdateBadgeNum = NotificationUtil.needRestatBadgeNum(notification, oldNotification);
                this.mGroupManager.onEntryUpdated(entry, oldNotification);
                wrapNotificationEvent(notification, oldNotification);
                entry.updateIcons(this.mContext, notification);
                inflateViews(entry, this.mStackScroller);
                this.mForegroundServiceController.updateNotification(notification, this.mNotificationData.getImportance(entry));
                updateNotifications();
                if (!notification.isClearable()) {
                    this.mStackScroller.snapViewIfNeeded(entry.row);
                }
                allowGroupShowOnKeyguardAgain(notification);
                if (DEBUG) {
                    boolean isForCurrentUser = isNotificationForCurrentProfiles(notification);
                    StringBuilder sb = new StringBuilder();
                    sb.append("notification is ");
                    sb.append(isForCurrentUser ? "" : "not ");
                    sb.append("for you");
                    Log.d("StatusBar", sb.toString());
                }
                setAreThereNotifications();
                wakeUpForNotification(entry);
                ((NotificationsMonitor) Dependency.get(NotificationsMonitor.class)).notifyNotificationUpdated(notification);
            }
        }
    }

    private void allowGroupShowOnKeyguardAgain(ExpandedNotification notification) {
        if (this.mGroupManager.isChildInGroupWithSummary(notification)) {
            ExpandableNotificationRow summary = this.mGroupManager.getGroupSummary((StatusBarNotification) notification);
            if (summary != null && summary.getEntry() != null) {
                NotificationData.Entry summaryEntry = summary.getEntry();
                summaryEntry.notification.setHasShownAfterUnlock(false);
                NotificationGroupManager.NotificationGroup group = this.mGroupManager.getNotificationGroup(summaryEntry.notification.getGroupKey());
                if (group != null) {
                    Iterator<NotificationData.Entry> iterator = group.children.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().notification.setHasShownAfterUnlock(false);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updatePublicContentView(NotificationData.Entry entry) {
        RemoteViews publicContentView = entry.cachedPublicContentView;
        View inflatedView = entry.getPublicContentView();
        if (entry.autoRedacted && publicContentView != null && inflatedView != null) {
            String notificationHiddenText = NotificationUtil.getHiddenText(this.mContext);
            TextView titleView = (TextView) inflatedView.findViewById(16908310);
            if (titleView != null && !titleView.getText().toString().equals(notificationHiddenText)) {
                titleView.setText(notificationHiddenText);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void notifyHeadsUpScreenOff() {
        maybeEscalateHeadsUp();
    }

    private boolean alertAgain(NotificationData.Entry oldEntry, Notification newNotification) {
        return oldEntry == null || !oldEntry.hasInterrupted() || (newNotification.flags & 8) == 0;
    }

    /* access modifiers changed from: protected */
    public boolean shouldPeek(NotificationData.Entry entry) {
        return shouldPeek(entry, entry.notification);
    }

    /* access modifiers changed from: protected */
    public boolean shouldPeek(NotificationData.Entry entry, ExpandedNotification sbn) {
        boolean shouldPeek = false;
        if (!this.mUseHeadsUp || isDeviceInVrMode()) {
            Log.d("StatusBar", "No peeking: no huns or vr mode");
            return false;
        } else if (this.mNotificationData.shouldFilterOut(sbn)) {
            Log.d("StatusBar", "No peeking: filtered notification: " + sbn.getKey());
            return false;
        } else if (this.mNotificationData.shouldSuppressScreenOn(sbn.getKey())) {
            Log.d("StatusBar", "No peeking: suppressed by DND: " + sbn.getKey());
            return false;
        } else if (entry.hasJustLaunchedFullScreenIntent()) {
            Log.d("StatusBar", "No peeking: recent fullscreen: " + sbn.getKey());
            return false;
        } else if (isSnoozedPackage(sbn)) {
            Log.d("StatusBar", "No peeking: snoozed package: " + sbn.getKey());
            return false;
        } else if (this.mNotificationData.getImportance(entry) < 3 && !InCallUtils.isInCallNotification(this.mContext, sbn)) {
            Log.d("StatusBar", "No peeking: unimportant notification: " + sbn.getKey());
            return false;
        } else if (this.mExpandedVisible && !this.mHeadsUpManager.hasPinnedHeadsUp() && !this.mHeadsUpManager.isHeadsUpGoingAway()) {
            Log.d("StatusBar", "No peeking: status bar expanded: " + sbn.getKey());
            return false;
        } else if (!panelsEnabled()) {
            Log.d("StatusBar", "No peeking: disabled panel : " + sbn.getKey());
            return false;
        } else {
            if (sbn.getNotification().fullScreenIntent != null) {
                if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
                    Log.d("StatusBar", "No peeking: accessible fullscreen: " + sbn.getKey());
                    return false;
                } else if (InCallUtils.isInCallNotification(this.mContext, sbn)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "com.xiaomi.system.devicelock.locked", 0) != 0) {
                        Log.d("StatusBar", "No peeking: device locked: " + sbn.getKey());
                        return false;
                    }
                    if (!this.mStatusBarKeyguardViewManager.isShowing() || (this.mStatusBarKeyguardViewManager.isOccluded() && !InCallUtils.isInCallScreenShowing(this.mContext))) {
                        shouldPeek = true;
                    }
                    Log.d("StatusBar", "in call notification should peek: " + shouldPeek);
                    return shouldPeek;
                }
            }
            if (StatusBarNotificationCompat.isGroup(sbn) && NotificationCompat.suppressAlertingDueToGrouping(sbn.getNotification())) {
                Log.d("StatusBar", "No peeking: suppressed due to group alert behavior: " + sbn.getKey());
                return false;
            } else if (!StatusBarNotificationCompat.isAutoGroupSummary(sbn)) {
                return enableFloatNotification(sbn);
            } else {
                Log.d("StatusBar", "No peeking: auto group summary: " + sbn.getKey());
                return false;
            }
        }
    }

    private boolean enableFloatNotification(ExpandedNotification sbn) {
        if (!isDeviceProvisioned() || isKeyguardShowing() || InCallUtils.isInCallNotificationHeadsUp(this.mContext, this.mHeadsUpManager) || ((InCallUtils.isInCallScreenShowing(this.mContext) && !InCallUtils.isPhoneInCallNotificationInVideo(sbn)) || isLowStorageMode() || isVrMode() || sbn.isFold())) {
            Log.d("StatusBar", "No peeking: miui smart intercept: " + sbn.getKey());
            return false;
        }
        boolean quietModeEnable = MiuiSettings.SilenceMode.isSupported ? this.mShouldPopup : this.mQuietModeEnable;
        if (sbn.getNotification().fullScreenIntent != null) {
            if (sIsStatusBarHidden || this.mSoftInputVisible || this.mDisableFloatNotification || quietModeEnable || this.mEnableFloatNotificationWhitelist.contains(Util.getTopActivityPkg(this.mContext, true))) {
                if (!sbn.isClearable()) {
                    sbn.getNotification().extraNotification.setFloatTime(Integer.MAX_VALUE);
                }
                Log.d("StatusBar", "peeking: miui smart suspension: " + sbn.getKey());
                return true;
            }
            Log.d("StatusBar", "No peeking: has fullscreen intent: " + sbn.getKey());
            return false;
        } else if (this.mDisableFloatNotification) {
            Log.d("StatusBar", "No peeking: disable float notification: " + sbn.getKey());
            return false;
        } else if (InCallUtils.isInCallScreenShowing(this.mContext) && InCallUtils.isPhoneInCallNotificationInVideo(sbn)) {
            Log.d("StatusBar", "peeking: video in call notification: " + sbn.getKey());
            return true;
        } else if (sbn.getNotification().extraNotification.isEnableFloat() && getAppNotificationFlag(sbn, true) == 2 && !NotificationUtil.hasProgressbar(sbn) && !quietModeEnable) {
            Log.d("StatusBar", "peeking: miui permission allows: " + sbn.getKey());
            return true;
        } else if (!this.mQuietModeEnable || !InCallUtils.isInCallNotification(this.mContextForUser, sbn)) {
            Logger.fullI("StatusBar", "No peeking: " + sbn.getKey());
            return false;
        } else {
            Log.d("StatusBar", "peeking: in call notification: " + sbn.getKey());
            return true;
        }
    }

    private boolean isVrMode() {
        return 1 == Settings.System.getInt(this.mContext.getContentResolver(), "vr_mode", 0);
    }

    private boolean isLowStorageMode() {
        return SystemProperties.getBoolean("sys.is_mem_low", false);
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public void logNotificationExpansion(final String key, final boolean userAction, final boolean expanded) {
        this.mUiOffloadThread.submit(new Runnable() {
            public void run() {
                try {
                    StatusBar.this.mBarService.onNotificationExpansionChanged(key, userAction, expanded);
                } catch (RemoteException e) {
                }
            }
        });
    }

    public boolean isKeyguardSecure() {
        if (this.mStatusBarKeyguardViewManager != null) {
            return this.mStatusBarKeyguardViewManager.isSecure();
        }
        Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
        return false;
    }

    public void showAssistDisclosure() {
        if (this.mAssistManager != null) {
            this.mAssistManager.showDisclosure();
        }
    }

    public void startAssist(Bundle args) {
        if (this.mAssistManager != null) {
            this.mAssistManager.startAssist(args);
        }
    }

    public void onInCallNotificationShow() {
        if (this.mPhoneStateListener == null) {
            this.mPhoneStateListener = new PhoneStateListener() {
                public void onCallStateChanged(int state, String incomingNumber) {
                    StatusBar.this.onCallStateChanged(state);
                }
            };
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        }
        if (this.mVoipPhoneStateReceiver == null) {
            this.mVoipPhoneStateReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    StatusBar.this.onCallStateChanged(intent.getIntExtra("state", 0));
                }
            };
            this.mContext.registerReceiverAsUser(this.mVoipPhoneStateReceiver, UserHandle.ALL, new IntentFilter("com.miui.voip.action.CALL_STATE_CHANGED"), null, null);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "status_bar_in_call_notification_floating", 1, -2);
        this.mHasAnswerCall = false;
    }

    /* access modifiers changed from: private */
    public void onCallStateChanged(int state) {
        if (state == 2 && !this.mHasAnswerCall) {
            if (this.mGameHandsFreeMode) {
                this.mHeadsUpManager.removeHeadsUpNotification();
            } else {
                onExitCall();
            }
        }
    }

    public void onInCallNotificationHide() {
        if (this.mPhoneStateListener != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mPhoneStateListener = null;
        }
        if (this.mVoipPhoneStateReceiver != null) {
            this.mContext.unregisterReceiver(this.mVoipPhoneStateReceiver);
            this.mVoipPhoneStateReceiver = null;
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "status_bar_in_call_notification_floating", 0, -2);
    }

    public void onAnswerCall() {
        Log.d("StatusBar", "on answer call");
        this.mHasAnswerCall = true;
        if (this.mTelephonyManager.getCallState() != 0) {
            TelephonyManagerEx.getDefault().answerRingingCall();
        } else {
            this.mMiuiVoipManager.answerRingingCall();
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("systemUI.answer", true);
        if (!this.mGameHandsFreeMode) {
            InCallUtils.goInCallScreen(this.mContext, bundle);
        }
        this.mHeadsUpManager.removeHeadsUpNotification();
    }

    public void onEndCall() {
        Log.d("StatusBar", "on end call");
        if (this.mTelephonyManager.getCallState() != 0) {
            TelephonyManagerEx.getDefault().endCall();
        } else {
            this.mMiuiVoipManager.endCall();
        }
        this.mHeadsUpManager.removeHeadsUpNotification();
    }

    public void onExitCall() {
        Log.d("StatusBar", "on exit call");
        InCallUtils.goInCallScreen(this.mContext);
        this.mHeadsUpManager.removeHeadsUpNotification();
    }

    public void showReturnToInCallScreenButtonIfNeed() {
        if (!InCallUtils.isInCallNotificationHeadsUp(this.mContext, this.mHeadsUpManager)) {
            return;
        }
        if (1 == TelephonyManager.getDefault().getCallState() || 1 == this.mMiuiVoipManager.getCallState()) {
            showReturnToInCallScreenButton(Call.State.INCOMING.toString(), 0);
            TelephonyManagerEx.getDefault().silenceRinger();
        }
    }

    public void showReturnToInCallScreenButton(String state, long baseTime) {
        Log.d("StatusBar", "show return to in call screen button");
        this.mMiuiStatusBarPrompt.showReturnToInCallScreenButton(state, baseTime);
    }

    public void hideReturnToInCallScreenButton() {
        Log.d("StatusBar", "hide return to in call screen button");
        this.mMiuiStatusBarPrompt.hideReturnToInCallScreenButton();
    }

    public int getBatteryLevel() {
        return this.mBatteryLevel;
    }

    /* access modifiers changed from: package-private */
    public void resumeSuspendedNavBarAutohide() {
        resumeSuspendedAutohide();
    }

    /* access modifiers changed from: package-private */
    public void suspendNavBarAutohide() {
        suspendAutohide();
    }

    /* access modifiers changed from: private */
    public void updateNotificationIconsLayout() {
        int i = 0;
        boolean visible = true;
        boolean onKeyguard = this.mState == 1;
        boolean isShowingDriveMode = this.mMiuiStatusBarPrompt.isShowingState(2);
        if (!this.mShowNotifications || ((isShowingDriveMode && !onKeyguard) || this.mDemoMode)) {
            visible = false;
        }
        View view = this.mNotifications;
        if (!visible) {
            i = 4;
        }
        view.setVisibility(i);
    }

    /* access modifiers changed from: private */
    public void updateDriveMode() {
        boolean z = false;
        boolean onKeyguard = this.mState == 1;
        boolean isShowingDriveMode = this.mMiuiStatusBarPrompt.isShowingState(2);
        updateNotificationIconsLayout();
        this.mDriveModeBg.setVisibility((Constants.IS_NOTCH || !isShowingDriveMode || onKeyguard) ? 8 : 0);
        this.mLightBarController.setDriveMode(!Constants.IS_NOTCH && !onKeyguard && isShowingDriveMode);
        this.mStatusBarFragment.updateInDriveMode(!onKeyguard && isShowingDriveMode);
        MiuiStatusBarPromptController miuiStatusBarPromptController = this.mMiuiStatusBarPrompt;
        boolean z2 = !onKeyguard && isShowingDriveMode;
        if (!onKeyguard && this.mIsInDriveModeMask) {
            z = true;
        }
        miuiStatusBarPromptController.showReturnToDriveModeView(z2, z);
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
        this.mRecents.showRecentApps(triggeredFromAltTab, fromHome);
    }

    public boolean shouldHideNotificationIcons() {
        return this.mMiuiStatusBarPrompt.isShowingState(1);
    }

    public void setDarkMode(boolean isDarkMode) {
        this.mDarkMode = isDarkMode;
    }

    public void setStatus(int what, String action, Bundle ext) {
        this.mMiuiStatusBarPrompt.setStatus(what, action, ext);
    }

    public void refreshClockVisibility(boolean isNormalMode) {
        this.mStatusBarFragment.refreshClockVisibility(false, isNormalMode, true);
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
    }

    public void toggleRecentApps() {
    }

    public void appTransitionPending(boolean forced) {
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
    }

    public void showPictureInPictureMenu() {
    }
}
