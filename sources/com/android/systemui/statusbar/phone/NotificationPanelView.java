package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.MiuiStatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayerCompat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Property;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.AwesomeLockScreen;
import com.android.keyguard.KeyguardClockContainer;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardFaceUnlockView;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.charge.ChargeHelper;
import com.android.keyguard.charge.MiuiKeyguardChargingView;
import com.android.keyguard.faceunlock.FaceUnlockManager;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.keyguard.magazine.LockScreenMagazinePreView;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.negative.MiuiKeyguardMoveLeftViewContainer;
import com.android.keyguard.utils.PackageUtils;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Constants;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.LockScreenMagazineController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.android.systemui.statusbar.phone.KeyguardMoveHelper;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.QcomBoostFramework;
import com.miui.systemui.support.v4.app.Fragment;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import miui.content.res.ThemeResources;
import miui.util.CustomizeUtil;

public class NotificationPanelView extends PanelView implements View.OnClickListener, QS.HeightListener, ExpandableView.OnHeightChangedListener, KeyguardMoveHelper.Callback, StatusBarWindowManager.BlurRatioChangedListener, OnHeadsUpChangedListener, NotificationStackScrollLayout.OnEmptySpaceClickListener, NotificationStackScrollLayout.OnOverscrollTopChangedListener, NotificationStackScrollLayout.OnTopPaddingUpdateListener {
    private static final boolean DEBUG = Constants.DEBUG;
    private static final FloatProperty<NotificationPanelView> SET_DARK_AMOUNT_PROPERTY = new FloatProperty<NotificationPanelView>("mDarkAmount") {
        public void setValue(NotificationPanelView object, float value) {
            object.setDarkAmount(value);
        }

        public Float get(NotificationPanelView object) {
            return Float.valueOf(object.mDarkAmount);
        }
    };
    public static final String TAG = NotificationPanelView.class.getSimpleName();
    /* access modifiers changed from: private */
    public static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);
    public static boolean sQsExpanded;
    ContentObserver contentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean unused = NotificationPanelView.this.mExpandableUnderKeyguard = MiuiStatusBarManager.isExpandableUnderKeyguardForUser(NotificationPanelView.this.mContext, -2);
        }
    };
    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
            NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
            float unused = NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        }
    };
    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
        public void run() {
            boolean unused = NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            NotificationPanelView.this.mKeyguardClockView.setVisibility(8);
            NotificationPanelView.this.mAwesomeLockScreenContainer.setVisibility(8);
        }
    };
    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
        public void run() {
            boolean unused = NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
        }
    };
    private boolean mAnimateNextTopPaddingChange;
    private AwesomeLockScreen mAwesomeLockScreen;
    /* access modifiers changed from: private */
    public FrameLayout mAwesomeLockScreenContainer;
    private boolean mBlockTouches;
    private ChargeHelper mChargeHelper;
    private MiuiKeyguardChargingView mChargingView;
    private int mClockAnimationTarget = -1;
    private KeyguardClockPositionAlgorithm mClockPositionAlgorithm = new KeyguardClockPositionAlgorithm();
    private KeyguardClockPositionAlgorithm.Result mClockPositionResult = new KeyguardClockPositionAlgorithm.Result();
    private float mCloseHandleUnderlapSize;
    private boolean mClosingWithAlphaFadeOut;
    private boolean mCollapsedOnDown;
    private boolean mConflictingQsExpansionGesture;
    /* access modifiers changed from: private */
    public float mDarkAmount;
    private ValueAnimator mDarkAnimator;
    protected DismissView mDismissView;
    /* access modifiers changed from: private */
    public Animator mDismissViewAnimator = null;
    private int mDismissViewBottomMargin;
    private boolean mDismissViewShowUp;
    private int mDismissViewSize;
    /* access modifiers changed from: private */
    public boolean mDozing;
    private boolean mDozingOnDown;
    private float mEmptyDragAmount;
    /* access modifiers changed from: private */
    public boolean mExpandableUnderKeyguard;
    private boolean mExpandingFromHeadsUp;
    private final ChargeHelper.ExtremePowerSaveModeChangeCallback mExtremePowerSaveModeChangeCallback = new ChargeHelper.ExtremePowerSaveModeChangeCallback() {
        public void onModeChange() {
            if (!NotificationPanelView.this.mDozing) {
                NotificationPanelView.this.startLiveLockWallpaper();
            }
        }
    };
    /* access modifiers changed from: private */
    public FaceUnlockManager mFaceUnlockManager;
    private MiuiKeyguardFaceUnlockView mFaceUnlockView;
    private FalsingManager mFalsingManager;
    private boolean mFlingAfterTracking;
    private FlingAnimationUtils mFlingAnimationUtils;
    /* access modifiers changed from: private */
    public boolean mForceBlack;
    private final FragmentHostManager.FragmentListener mFragmentListener = new FragmentHostManager.FragmentListener() {
        public void onFragmentViewCreated(String tag, Fragment fragment) {
            QS unused = NotificationPanelView.this.mQs = (QS) fragment;
            NotificationPanelView.this.mQs.setPanelView(NotificationPanelView.this);
            NotificationPanelView.this.mQs.setExpandClickListener(NotificationPanelView.this);
            NotificationPanelView.this.mQs.setHeaderClickable(NotificationPanelView.this.mQsExpansionEnabled);
            NotificationPanelView.this.mQs.setKeyguardShowing(NotificationPanelView.this.mKeyguardShowing);
            NotificationPanelView.this.mQs.setOverscrolling(NotificationPanelView.this.mStackScrollerOverscrolling);
            NotificationPanelView.this.mNotificationStackScroller.setQs(NotificationPanelView.this.mQs);
            NotificationPanelView.this.updateQsExpansion();
            NotificationPanelView.this.updateDismissViewState();
        }

        public void onFragmentViewDestroyed(String tag, Fragment fragment) {
            if (fragment == NotificationPanelView.this.mQs) {
                if (NotificationPanelView.this.isQsDetailShowing()) {
                    NotificationPanelView.this.mQs.closeDetail();
                }
                QS unused = NotificationPanelView.this.mQs = null;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mGetCameraImageSucceed = false;
    private NotificationGroupManager mGroupManager;
    private boolean mHasWindowFocus;
    private boolean mHeadsUpAnimatingAway;
    private Runnable mHeadsUpExistenceChangedRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.setHeadsUpAnimatingAway(false);
            NotificationPanelView.this.notifyBarPanelExpansionChanged();
        }
    };
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private int mIndicationBottomPadding;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mIntercepting;
    private boolean mIsExpanding;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mIsFullWidth;
    private boolean mIsInteractive;
    private boolean mIsKeyguardCoverd;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    /* access modifiers changed from: private */
    public KeyguardClockContainer mKeyguardClockView;
    private KeyguardIndicationController mKeyguardIndicationController;
    /* access modifiers changed from: private */
    public MiuiKeyguardMoveLeftViewContainer mKeyguardLeftView;
    /* access modifiers changed from: private */
    public KeyguardMoveHelper mKeyguardMoveHelper;
    /* access modifiers changed from: private */
    public boolean mKeyguardOccluded;
    /* access modifiers changed from: private */
    public ImageView mKeyguardRightView;
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing;
    /* access modifiers changed from: private */
    public KeyguardStatusBarView mKeyguardStatusBar;
    /* access modifiers changed from: private */
    public float mKeyguardStatusBarAnimateAlpha = 1.0f;
    /* access modifiers changed from: private */
    public boolean mKeyguardStatusViewAnimating;
    private float mKeyguardTouchDownX;
    private float mKeyguardTouchDownY;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void updateShowingStatus(boolean showing) {
            if (!showing || !NotificationPanelView.this.mUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot()) {
                NotificationPanelView.this.mContext.getContentResolver().unregisterContentObserver(NotificationPanelView.this.mSCStatusProviderObserver);
                return;
            }
            NotificationPanelView.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("sc_event_status"), false, NotificationPanelView.this.mSCStatusProviderObserver, -1);
            NotificationPanelView.this.mSCStatusProviderObserver.onChange(true);
            int unused = NotificationPanelView.this.mSlideChoice = Settings.System.getIntForUser(NotificationPanelView.this.mContext.getContentResolver(), "miui_slider_tool_choice", 1, -2);
        }

        public void onStartedGoingToSleep(int why) {
            if (NotificationPanelView.this.mLiveLockWallpaperPlayer != null) {
                MediaPlayerCompat.seekTo(NotificationPanelView.this.mLiveLockWallpaperPlayer, 0, 3);
                NotificationPanelView.this.mLiveLockWallpaperPlayer.setVolume(0.0f, 0.0f);
            }
        }

        public void onUserSwitchComplete(int userId) {
            super.onUserSwitchComplete(userId);
            NotificationPanelView.this.mKeyguardClockView.onUserChanged();
        }

        public void onLockScreenMagazineStatusChanged() {
            if (!NotificationPanelView.this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                NotificationPanelView.this.mLeftViewBg.setBackgroundColor(NotificationPanelView.this.mUpdateMonitor.getWallpaperBlurColor());
            } else if (NotificationPanelView.this.mPreTransToLeftScreenDrawable != null) {
                NotificationPanelView.this.mLeftViewBg.setBackground(NotificationPanelView.this.mPreTransToLeftScreenDrawable);
            }
        }

        public void onLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
            boolean unused = NotificationPanelView.this.mLockScreenMagazinePreViewVisible = visible;
            NotificationPanelView.this.updateGxzwState();
        }
    };
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mLastAnnouncementWasQuickSettings;
    private String mLastCameraLaunchSource = "lockscreen_affordance";
    private int mLastDensityDpi = -1;
    private String mLastLiveLockPath = null;
    private int mLastOrientation = -1;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    /* access modifiers changed from: private */
    public String mLaunchPkg;
    private boolean mLaunchingAffordance;
    /* access modifiers changed from: private */
    public ImageView mLeftViewBg;
    private boolean mListenForHeadsUp;
    /* access modifiers changed from: private */
    public MediaPlayer mLiveLockWallpaperPlayer;
    private TextureView mLiveLockWallpaperView;
    /* access modifiers changed from: private */
    public boolean mLiveReady = false;
    /* access modifiers changed from: private */
    public LockScreenMagazineController mLockScreenMagazineController;
    private LockScreenMagazinePreView mLockScreenMagazinePreView;
    /* access modifiers changed from: private */
    public boolean mLockScreenMagazinePreViewVisible;
    private LockscreenGestureLogger mLockscreenGestureLogger = new LockscreenGestureLogger();
    private Paint mMaskPaint = new Paint();
    private List<View> mMoveListViews = new ArrayList();
    private int mNavigationBarBottomHeight;
    private boolean mNoVisibleNotifications = true;
    private View mNotchCorner;
    protected NotificationsQuickSettingsContainer mNotificationContainerParent;
    protected NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private int mOrientation = 1;
    private boolean mPanelExpanded;
    private int mPanelGravity;
    private int mPanelWidth;
    /* access modifiers changed from: private */
    public QcomBoostFramework mPerf = null;
    private int mPositionMinSideMargin;
    /* access modifiers changed from: private */
    public Drawable mPreLeftScreenDrawable;
    /* access modifiers changed from: private */
    public Drawable mPreTransToLeftScreenDrawable;
    private Configuration mPreviousConfig;
    /* access modifiers changed from: private */
    public QS mQs;
    private boolean mQsAnimatorExpand;
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    /* access modifiers changed from: private */
    public ValueAnimator mQsExpansionAnimator;
    protected boolean mQsExpansionEnabled = true;
    private boolean mQsExpansionFromOverscroll;
    protected float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    private FrameLayout mQsFrame;
    private boolean mQsFullyExpanded;
    protected int mQsMaxExpansionHeight;
    protected int mQsMinExpansionHeight;
    private boolean mQsOverscrollExpansionEnabled;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled = true;
    /* access modifiers changed from: private */
    public ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private VelocityTracker mQsVelocityTracker;
    /* access modifiers changed from: private */
    public int mSCEventStatus = 0;
    /* access modifiers changed from: private */
    public int mSCStatus = 0;
    /* access modifiers changed from: private */
    public ContentObserver mSCStatusProviderObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int unused = NotificationPanelView.this.mSCEventStatus = Settings.System.getIntForUser(NotificationPanelView.this.mContext.getContentResolver(), "sc_event_status", 0, 0);
            if (NotificationPanelView.this.mSCEventStatus != 2) {
                int unused2 = NotificationPanelView.this.mSCStatus = Settings.System.getIntForUser(NotificationPanelView.this.mContext.getContentResolver(), "sc_status", 0, 0);
                if (NotificationPanelView.this.mSCStatus != 0 || selfChange) {
                    if (NotificationPanelView.this.mSCStatus == 1) {
                        NotificationPanelView.this.mUpdateMonitor.setFaceUnlockStarted(false);
                        NotificationPanelView.this.mFaceUnlockManager.runOnFaceUnlockWorkerThread(new Runnable() {
                            public void run() {
                                NotificationPanelView.this.mFaceUnlockManager.stopFaceUnlock();
                            }
                        });
                    }
                } else if (NotificationPanelView.this.mUpdateMonitor.shouldListenForFaceUnlock()) {
                    NotificationPanelView.this.mUpdateMonitor.startFaceUnlock();
                } else if ((NotificationPanelView.this.mKeyguardOccluded && !MiuiKeyguardUtils.isSCSlideNotOpenCamera(NotificationPanelView.this.mContext)) || !NotificationPanelView.this.mKeyguardOccluded) {
                    if (NotificationPanelView.this.mSlideChoice == 1) {
                        Intent intent = new Intent();
                        intent.setFlags(276856832);
                        intent.putExtra("ShowCameraWhenLocked", true);
                        intent.putExtra("StartActivityWhenLocked", true);
                        intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
                        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                        intent.putExtra("autofocus", true);
                        intent.putExtra("fullScreen", false);
                        intent.putExtra("showActionIcons", false);
                        intent.setComponent(new ComponentName("com.android.camera", "com.android.camera.Camera"));
                        NotificationPanelView.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    } else if (NotificationPanelView.this.mSlideChoice == 2) {
                        Intent intent2 = new Intent();
                        intent2.setFlags(276824064);
                        intent2.putExtra("StartActivityWhenLocked", true);
                        intent2.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.sliderpanel.SliderPanelActivity"));
                        intent2.putExtra("onKeyguard", true);
                        intent2.putExtra("blurColor", KeyguardUpdateMonitor.getInstance(NotificationPanelView.this.mContext).getWallpaperBlurColor());
                        NotificationPanelView.this.mContext.startActivityAsUser(intent2, UserHandle.CURRENT);
                    } else if (NotificationPanelView.this.mSlideChoice == 3) {
                        String unused3 = NotificationPanelView.this.mLaunchPkg = Settings.System.getStringForUser(NotificationPanelView.this.mContext.getContentResolver(), "miui_slider_launch_pkg", -2);
                        if (NotificationPanelView.this.mLaunchPkg != null) {
                            Intent intent3 = NotificationPanelView.this.mContext.getPackageManager().getLaunchIntentForPackage(NotificationPanelView.this.mLaunchPkg);
                            if (intent3 != null) {
                                intent3.setFlags(276824064);
                                NotificationPanelView.this.mStatusBar.startActivity(intent3, true);
                            }
                        }
                    }
                }
            }
        }
    };
    KeyguardUpdateMonitor.SensorsChangeCallback mSensorsChangeCallback = new KeyguardUpdateMonitor.SensorsChangeCallback() {
        public void onChange(boolean isInSuspectMode) {
            if (isInSuspectMode) {
                Log.e("miui_keyguard", "enter suspect mode");
            }
            NotificationPanelView.this.mKeyguardMoveHelper.updateKeyguardHorizontalSwpingSlop(isInSuspectMode);
            NotificationPanelView.this.mKeyguardVerticalMoveHelper.updateKeyguardVerticalSwpingSlop(isInSuspectMode);
        }
    };
    private boolean mShowEmptyShadeView;
    private boolean mShowIconsWhenExpanded;
    /* access modifiers changed from: private */
    public int mSlideChoice = 0;
    /* access modifiers changed from: private */
    public boolean mStackScrollerOverscrolling;
    private final ValueAnimator.AnimatorUpdateListener mStatusBarAnimateAlphaListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            float unused = NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) animation.getAnimatedValue()).floatValue();
            NotificationPanelView.this.updateHeaderKeyguardAlpha();
        }
    };
    private int mStatusBarMinHeight;
    protected int mStatusBarState;
    /* access modifiers changed from: private */
    public View mSwitchSystemUser;
    private View mThemeBackgroundView;
    private int mTopPaddingAdjustment;
    private int mTopPaddingWhenQsBeingCovered;
    private boolean mTouchAtKeyguardBottomArea = false;
    private int mTouchSlop;
    private int mTrackingPointer;
    private boolean mTwoFingerQsExpandPossible;
    private int mUnlockMoveDistance;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardUpdateMonitor.WallpaperChangeCallback mWallpaperChangeCallback = new KeyguardUpdateMonitor.WallpaperChangeCallback() {
        public void onWallpaperChange(boolean succeed) {
            if (succeed) {
                NotificationPanelView.this.updateWallpaper(true);
            }
        }
    };
    private int mWallpaperType = 2;
    private ImageView mWallpaperView;

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(!DEBUG);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mQsOverscrollExpansionEnabled = getResources().getBoolean(R.bool.config_enableQuickSettingsOverscrollExpansion);
        this.mPerf = new QcomBoostFramework();
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mPreviousConfig = new Configuration();
        this.mChargeHelper = ChargeHelper.getInstance(context);
        this.mChargeHelper.registerWallpaperChangeCallback(this.mExtremePowerSaveModeChangeCallback);
        this.mUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mExpandableUnderKeyguard = MiuiStatusBarManager.isExpandableUnderKeyguard(context);
        IntentFilter userChangeFilter = new IntentFilter();
        userChangeFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean unused = NotificationPanelView.this.mExpandableUnderKeyguard = MiuiStatusBarManager.isExpandableUnderKeyguardForUser(NotificationPanelView.this.mContext, -2);
            }
        }, userChangeFilter);
        loadDimens(getResources());
    }

    /* access modifiers changed from: private */
    public void updateWallpaper(boolean wallpaperChanged) {
        this.mWallpaperType = 2;
        boolean z = true;
        if (WallpaperAuthorityUtils.isThemeLockLiveWallpaper(getContext())) {
            this.mWallpaperView.setImageDrawable(null);
            this.mWallpaperType = 3;
        } else {
            Pair<File, Drawable> wallpaperCache = KeyguardWallpaperUtils.getLockWallpaper(this.mContext);
            if (wallpaperCache != null) {
                File wallpaper = (File) wallpaperCache.first;
                Drawable preview = (Drawable) wallpaperCache.second;
                if (wallpaper.getPath().endsWith(".mp4")) {
                    if (wallpaperChanged || !TextUtils.equals(this.mLastLiveLockPath, wallpaper.getPath())) {
                        this.mWallpaperView.setImageDrawable(preview);
                        showMiLiveLockWallpaper(wallpaper);
                        this.mLastLiveLockPath = wallpaper.getPath();
                    }
                    this.mWallpaperType = 1;
                } else {
                    this.mWallpaperView.setImageDrawable(preview);
                    this.mWallpaperType = 2;
                }
            }
        }
        if (this.mWallpaperType != 1) {
            releaseLiveWallpaper();
        }
        this.mLeftViewBg.setBackgroundColor(this.mUpdateMonitor.getWallpaperBlurColor());
        this.mKeyguardBottomArea.setDarkMode(this.mUpdateMonitor.isLightWallpaperBottom());
        this.mKeyguardClockView.setDarkMode(this.mUpdateMonitor.isLightClock());
        KeyguardStatusBarView keyguardStatusBarView = this.mKeyguardStatusBar;
        if (this.mForceBlack || !this.mUpdateMonitor.isLightWallpaperStatusBar()) {
            z = false;
        }
        keyguardStatusBarView.setDarkMode(z);
        this.mLockScreenMagazinePreView.setSettingButtonDarkMode(this.mUpdateMonitor.isLightLockScreenMagazinePreSettings());
        this.mLockScreenMagazinePreView.setGlobalPreDarkMode(this.mUpdateMonitor.isLightLockScreenMagazineGlobalPre());
    }

    public void setStatusBar(StatusBar bar) {
        this.mStatusBar = bar;
        this.mKeyguardLeftView.setStatusBar(this.mStatusBar);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setClipChildren(false);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        this.mLockScreenMagazinePreView = (LockScreenMagazinePreView) findViewById(R.id.wallpaper_des);
        this.mKeyguardClockView = (KeyguardClockContainer) findViewById(R.id.keyguard_clock_view);
        this.mChargingView = (MiuiKeyguardChargingView) findViewById(R.id.battery_charging_view);
        this.mWallpaperView = (ImageView) findViewById(R.id.wallpaper);
        this.mThemeBackgroundView = findViewById(R.id.theme_background);
        this.mNotchCorner = findViewById(R.id.notch_corner);
        this.mLeftViewBg = (ImageView) findViewById(R.id.left_view_bg);
        this.mAwesomeLockScreenContainer = (FrameLayout) findViewById(R.id.awesome_lock_screen_container);
        this.mNotificationContainerParent = (NotificationsQuickSettingsContainer) findViewById(R.id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        this.mNotificationStackScroller.setOnTopPaddingUpdateListener(this);
        this.mNotificationStackScroller.setFlingAnimationUtils(this.mFlingAnimationUtils);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        this.mFaceUnlockView = (MiuiKeyguardFaceUnlockView) findViewById(R.id.miui_keyguard_face_unlock_view);
        this.mFaceUnlockView.setKeyguardFaceUnlockView(true);
        this.mKeyguardMoveHelper = new KeyguardMoveHelper(this, getContext());
        KeyguardVerticalMoveHelper keyguardVerticalMoveHelper = new KeyguardVerticalMoveHelper(this.mContext, this, this.mKeyguardClockView, this.mNotificationStackScroller, this.mFaceUnlockView);
        this.mKeyguardVerticalMoveHelper = keyguardVerticalMoveHelper;
        this.mKeyguardBottomArea.setNotificationPanelView(this);
        this.mLastOrientation = getResources().getConfiguration().orientation;
        this.mSwitchSystemUser = findViewById(R.id.switch_to_system_user);
        this.mSwitchSystemUser.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!KeyguardUpdateMonitor.isOwnerUser()) {
                    try {
                        ActivityManagerNative.getDefault().switchUser(0);
                        NotificationPanelView.this.mSwitchSystemUser.setVisibility(8);
                    } catch (RemoteException e) {
                        Log.e(NotificationPanelView.TAG, "switchUser failed", e);
                    }
                }
            }
        });
        this.mMoveListViews.add(this.mKeyguardClockView);
        this.mMoveListViews.add(this.mNotificationContainerParent);
        this.mMoveListViews.add(this.mKeyguardBottomArea);
        this.mMoveListViews.add(this.mSwitchSystemUser);
        this.mQsFrame = (FrameLayout) findViewById(R.id.qs_frame);
        this.mKeyguardLeftView = (MiuiKeyguardMoveLeftViewContainer) findViewById(R.id.keyguard_left_view);
        this.mKeyguardRightView = (ImageView) findViewById(R.id.keyguard_right_view);
        this.mIsDefaultTheme = isDefaultLockScreenTheme();
        updateWallpaper(true);
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setNotificationPanelView(this);
        }
        updateResources(false);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (MiuiKeyguardUtils.isPad() && newConfig.orientation != this.mOrientation) {
            this.mOrientation = newConfig.orientation;
            setCameraImage();
        }
    }

    private boolean shouldShowSwitchSystemUser() {
        boolean z = false;
        if (Build.VERSION.SDK_INT < 28 || KeyguardUpdateMonitor.isOwnerUser()) {
            return false;
        }
        if (KeyguardUpdateMonitor.getCurrentUser() != KeyguardUpdateMonitor.getSecondUser()) {
            z = true;
        }
        return z;
    }

    private void releaseLiveWallpaper() {
        if (this.mLiveLockWallpaperView != null) {
            removeView(this.mLiveLockWallpaperView);
            this.mLiveLockWallpaperView = null;
        }
        if (this.mLiveLockWallpaperPlayer != null) {
            final MediaPlayer wallPaperPlayer = this.mLiveLockWallpaperPlayer;
            this.mLiveLockWallpaperPlayer = null;
            AsyncTask.execute(new Runnable() {
                public void run() {
                    wallPaperPlayer.release();
                }
            });
        }
        this.mLastLiveLockPath = null;
    }

    private void showMiLiveLockWallpaper(File wallpaperFile) {
        releaseLiveWallpaper();
        this.mLiveLockWallpaperView = new TextureView(this.mContext);
        this.mLiveLockWallpaperView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.mLiveLockWallpaperPlayer = MediaPlayer.create(this.mContext, Uri.fromFile(wallpaperFile));
        int i = 0;
        this.mLiveReady = false;
        if (this.mLiveLockWallpaperPlayer != null) {
            this.mLiveLockWallpaperPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                public void onSeekComplete(MediaPlayer mp) {
                    if (NotificationPanelView.this.mLiveReady) {
                        mp.pause();
                    }
                }
            });
        } else {
            Log.e(TAG, "live lock wallpaper is null");
        }
        this.mLiveLockWallpaperView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    NotificationPanelView.this.mLiveLockWallpaperPlayer.setSurface(new Surface(surface));
                    boolean unused = NotificationPanelView.this.mLiveReady = true;
                    NotificationPanelView.this.startLiveLockWallpaper();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        addView(this.mLiveLockWallpaperView, indexOfChild(this.mWallpaperView) + 1);
        TextureView textureView = this.mLiveLockWallpaperView;
        if (!this.mKeyguardShowing) {
            i = 4;
        }
        textureView.setVisibility(i);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(QS.TAG, this.mFragmentListener);
        post(new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardStatusBar.setDarkMode(!NotificationPanelView.this.mForceBlack && NotificationPanelView.this.mUpdateMonitor.isLightWallpaperStatusBar());
            }
        });
        this.mUpdateMonitor.registerWallpaperChangeCallback(this.mWallpaperChangeCallback);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_expandable_under_keyguard"), false, this.contentObserver, -1);
        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).addBlurRatioListener(this);
        this.mUpdateMonitor.registerPhoneSignalChangeCallback();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(QS.TAG, this.mFragmentListener);
        this.mUpdateMonitor.unregisterWallpaperChangeCallback(this.mWallpaperChangeCallback);
        this.mContext.getContentResolver().unregisterContentObserver(this.contentObserver);
        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).removeBlurRatioListener(this);
        this.mUpdateMonitor.unRegisterPhoneSignalChangeCallback();
    }

    public void updateResources(boolean isThemeChanged) {
        if (isThemeChanged && isQsDetailShowing()) {
            this.mQs.closeDetail();
        }
        Resources res = getResources();
        Configuration newConfig = res.getConfiguration();
        loadDimens(res);
        updateLayout();
        this.mNotificationContainerParent.updateResources(isThemeChanged);
        if (newConfig.orientation != this.mLastOrientation) {
            this.mLastOrientation = newConfig.orientation;
            int qqs_count = res.getInteger(R.integer.quick_settings_qqs_count);
            if (newConfig.orientation == 1) {
                qqs_count = res.getInteger(R.integer.quick_settings_qqs_count_portrait);
            }
            saveValueToTunerService(qqs_count);
            resetVerticalPanelPosition();
            this.mKeyguardMoveHelper.reset(true);
        }
        if (isThemeChanged) {
            reInflateThemeBackgroundView();
            boolean isDefaultTheme = isDefaultLockScreenTheme();
            if (isDefaultTheme != this.mIsDefaultTheme) {
                String str = TAG;
                Slog.i(str, "default theme change: mIsDefaultTheme = " + this.mIsDefaultTheme + ", isDefaultTheme = " + isDefaultTheme);
            }
            this.mIsDefaultTheme = isDefaultTheme;
            Class<?> cls = getClass();
            PanelBar.LOG((Class) cls, "isDefaultTheme = " + this.mIsDefaultTheme);
            if (isKeyguardShowing()) {
                if (this.mIsDefaultTheme) {
                    removeAwesomeLockScreen();
                } else {
                    addAwesomeLockScreenIfNeed(true);
                    if (this.mIsInteractive) {
                        onAwesomeLockScreenResume();
                    }
                }
                updateWallpaper(true);
                setBarState(this.mStatusBarState, false, false);
            }
            new AsyncTask<Void, Void, Void>() {
                /* access modifiers changed from: protected */
                public Void doInBackground(Void... params) {
                    NotificationPanelView.this.mUpdateMonitor.updateWallpaperBlurColor();
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            LockScreenMagazineAnalytics.recordThemeType(this.mContext, isDefaultTheme);
        }
        if (newConfig.densityDpi != this.mLastDensityDpi) {
            reInflateKeyguardMoveLeftView();
            this.mKeyguardMoveHelper.reset(false);
            this.mUpdateMonitor.updateKeyguardWallpaperLightDimens();
        }
        this.mLastDensityDpi = newConfig.densityDpi;
        this.mKeyguardMoveHelper.onConfigurationChanged();
    }

    /* access modifiers changed from: protected */
    public void loadDimens(Resources res) {
        super.loadDimens(res);
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = res.getDimensionPixelSize(17105351);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = res.getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = res.getDimensionPixelOffset(R.dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(res);
        this.mQsFalsingThreshold = res.getDimensionPixelSize(R.dimen.qs_falsing_threshold);
        this.mPositionMinSideMargin = res.getDimensionPixelSize(R.dimen.notification_panel_min_side_margin);
        this.mIndicationBottomPadding = res.getDimensionPixelSize(R.dimen.keyguard_indication_bottom_padding);
        this.mCloseHandleUnderlapSize = res.getDimension(R.dimen.close_handle_underlap);
        this.mDismissViewSize = res.getDimensionPixelSize(R.dimen.notification_clear_all_size);
        this.mDismissViewBottomMargin = res.getDimensionPixelSize(R.dimen.notification_clear_all_bottom_margin);
        this.mPanelWidth = res.getDimensionPixelSize(R.dimen.notification_panel_width);
        this.mPanelGravity = res.getInteger(R.integer.notification_panel_layout_gravity);
    }

    /* access modifiers changed from: protected */
    public void updateLayout() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mQsFrame.getLayoutParams();
        if (!(lp.width == this.mPanelWidth && lp.gravity == this.mPanelGravity)) {
            lp.width = this.mPanelWidth;
            lp.gravity = this.mPanelGravity;
            this.mQsFrame.setLayoutParams(lp);
        }
        FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (lp2.width != this.mPanelWidth || lp2.gravity != this.mPanelGravity) {
            lp2.width = this.mPanelWidth;
            lp2.gravity = this.mPanelGravity;
            this.mNotificationStackScroller.setLayoutParams(lp2);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int i = 0;
        setIsFullWidth(this.mNotificationStackScroller.getWidth() == getWidth());
        if (this.mQs != null) {
            if (!this.mKeyguardShowing) {
                i = this.mQs.getQsMinExpansionHeight();
            }
            this.mQsMinExpansionHeight = i;
        }
        positionClockAndNotifications();
        onQsHeightChanged();
        if (!this.mQsExpanded) {
            setQsExpansion(((float) this.mQsMinExpansionHeight) + this.mLastOverscroll);
        }
        updateExpandedHeight(getExpandedHeight());
        updateHeader();
        if (this.mQsSizeChangeAnimator == null && this.mQs != null) {
            this.mQs.setHeightOverride(this.mQs.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
    }

    private void setIsFullWidth(boolean isFullWidth) {
        this.mIsFullWidth = isFullWidth;
        this.mNotificationStackScroller.setIsFullWidth(isFullWidth);
    }

    private void startQsSizeChangeAnimation(int oldHeight, int newHeight) {
        Interpolator interpolator;
        if (this.mQsSizeChangeAnimator != null) {
            oldHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        boolean maybeExpand = oldHeight < newHeight;
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(new int[]{oldHeight, newHeight});
        int duration = maybeExpand ? 300 : 400;
        if (maybeExpand) {
            interpolator = Interpolators.DECELERATE_CUBIC;
        } else {
            interpolator = Interpolators.CUBIC_EASE_IN_OUT;
        }
        this.mQsSizeChangeAnimator.setDuration((long) duration);
        this.mQsSizeChangeAnimator.setInterpolator(interpolator);
        this.mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                NotificationPanelView.this.mQs.setHeightOverride(((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue());
            }
        });
        this.mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = NotificationPanelView.this.mQsSizeChangeAnimator = null;
            }
        });
        this.mQsSizeChangeAnimator.start();
    }

    private void positionClockAndNotifications() {
        int stackScrollerPadding;
        boolean animate = this.mNotificationStackScroller.isAddOrRemoveAnimationPending();
        if (this.mStatusBarState != 1) {
            if (this.mNotificationStackScroller.isQsCovered() || this.mNotificationStackScroller.isQsBeingCovered()) {
                stackScrollerPadding = this.mQs == null ? 0 : this.mQs.getQsHeaderHeight();
            } else {
                stackScrollerPadding = (this.mQs != null ? this.mQs.getQsMinExpansionHeight() : 0) + this.mQsPeekHeight;
            }
            this.mTopPaddingAdjustment = 0;
        } else {
            this.mClockPositionAlgorithm.setup(this.mStatusBar.getMaxKeyguardNotifications(), getMaxPanelHeight(), getExpandedHeight(), this.mNotificationStackScroller.getNotGoneChildCount(), getHeight(), this.mKeyguardClockView.getClockHeight(), this.mEmptyDragAmount, 0, this.mDarkAmount, this.mKeyguardClockView.getClockVisibleHeight(), this.mKeyguardClockView.getTopMargin());
            this.mClockPositionAlgorithm.run(this.mClockPositionResult);
            stackScrollerPadding = this.mClockPositionResult.stackScrollerPadding;
            this.mTopPaddingAdjustment = this.mClockPositionResult.stackScrollerPaddingAdjustment;
        }
        this.mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);
        requestScrollerTopPaddingUpdate(animate);
    }

    public int computeMaxKeyguardNotifications(int maximum) {
        float availableSpace;
        float minPadding = (float) this.mNotificationStackScroller.getIntrinsicPadding();
        int notificationPadding = Math.max(1, getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            availableSpace = ((float) (MiuiGxzwUtils.GXZW_ICON_Y - MiuiGxzwUtils.GXZW_ICON_HEIGHT)) - minPadding;
        } else {
            int chargingViewSpace = 0;
            if (this.mChargingView.isShown()) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mChargingView.getLayoutParams();
                chargingViewSpace = this.mChargingView.getHeight() + params.topMargin + params.bottomMargin;
            }
            availableSpace = ((((float) this.mNotificationStackScroller.getHeight()) - minPadding) - ((float) this.mIndicationBottomPadding)) - ((float) chargingViewSpace);
        }
        int count = 0;
        for (int i = 0; i < this.mNotificationStackScroller.getChildCount(); i++) {
            ExpandableView child = (ExpandableView) this.mNotificationStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                if (!this.mGroupManager.isSummaryOfSuppressedGroup(row.getStatusBarNotification()) && this.mStatusBar.shouldShowOnKeyguard(row.getEntry()) && !row.isRemoved()) {
                    availableSpace -= (float) (child.getMinHeight() + notificationPadding);
                    if (availableSpace < 0.0f || count >= maximum) {
                        break;
                    }
                    count++;
                }
            }
        }
        return count;
    }

    public void animateToFullShade(long delay) {
        this.mAnimateNextTopPaddingChange = true;
        this.mNotificationStackScroller.goToFullShade(delay);
        requestLayout();
    }

    public void setQsExpansionEnabled(boolean qsExpansionEnabled) {
        this.mQsExpansionEnabled = qsExpansionEnabled;
        if (this.mQs != null) {
            this.mQs.setHeaderClickable(qsExpansionEnabled);
        }
    }

    public void resetViews() {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        if (!this.mLaunchingAffordance) {
            this.mKeyguardMoveHelper.resetImmediately();
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        closeQs();
        this.mStatusBar.closeAndSaveGuts(true, true, true, -1, -1, true);
        this.mNotificationStackScroller.resetViews();
        if (this.mStatusBarState == 1) {
            this.mLockScreenMagazineController.reset();
        }
        this.mKeyguardVerticalMoveHelper.reset();
        this.mKeyguardBottomArea.reset();
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion((float) this.mQsMinExpansionHeight);
    }

    public void animateCloseQs() {
        if (this.mQsExpansionAnimator != null) {
            if (this.mQsAnimatorExpand) {
                float height = this.mQsExpansionHeight;
                this.mQsExpansionAnimator.cancel();
                setQsExpansion(height);
            } else {
                return;
            }
        }
        flingSettings(0.0f, false);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
        }
        expand(true);
    }

    public void fling(float vel, boolean expand) {
        GestureRecorder gr = ((PhoneStatusBarView) this.mBar).mBar.getGestureRecorder();
        if (gr != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("fling ");
            sb.append(vel > 0.0f ? "open" : "closed");
            String sb2 = sb.toString();
            gr.tag(sb2, "notifications,v=" + vel);
        }
        super.fling(vel, expand);
    }

    /* access modifiers changed from: protected */
    public void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        this.mFlingAfterTracking = isTracking();
        this.mHeadsUpTouchHelper.notifyFling(!expand);
        setClosingWithAlphaFadeout(!expand && getFadeoutAlpha() == 1.0f);
        if (!expand && this.mFlingAfterTracking) {
            AnalyticsHelper.trackStatusBarCollapse("up_swipe");
        }
        super.flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEventInternal(event);
        }
        event.getText().add(getKeyguardOrLockScreenString());
        this.mLastAnnouncementWasQuickSettings = false;
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean z = false;
        if (this.mBlockTouches || isQsDetailShowing()) {
            Log.d(TAG, "NotificationPanelView not intercept");
            return false;
        }
        initDownStates(event);
        if (this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open", 1);
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
            return true;
        } else if (this.mQsOverscrollExpansionEnabled && !isFullyCollapsed() && !this.mNotificationStackScroller.isQsCovered() && onQsIntercept(event)) {
            return true;
        } else {
            boolean result = super.onInterceptTouchEvent(event);
            if (this.mKeyguardShowing) {
                if (event.getActionMasked() == 0) {
                    this.mKeyguardMoveHelper.onTouchEvent(event);
                    if (event.getY() >= ((float) this.mKeyguardBottomArea.getTop()) && this.mKeyguardBottomArea.getVisibility() == 0 && this.mKeyguardBottomArea.getAlpha() == 1.0f) {
                        z = true;
                    }
                    this.mTouchAtKeyguardBottomArea = z;
                    this.mKeyguardTouchDownX = event.getX();
                    this.mKeyguardTouchDownY = event.getY();
                }
                if (!this.mTouchAtKeyguardBottomArea || (Math.abs(event.getX() - this.mKeyguardTouchDownX) < ((float) this.mTouchSlop) && Math.abs(event.getY() - this.mKeyguardTouchDownY) < ((float) this.mTouchSlop))) {
                    return result;
                }
                return true;
            }
            return result;
        }
    }

    private boolean onQsIntercept(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        int actionMasked = event.getActionMasked();
        boolean z = true;
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    this.mIntercepting = true;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    initVelocityTracker();
                    trackMovement(event);
                    if (shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, 0.0f)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (this.mQsExpansionAnimator != null) {
                        onQsExpansionStarted();
                        this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                        this.mQsTracking = true;
                        this.mIntercepting = false;
                        this.mNotificationStackScroller.removeLongPressCallback();
                        break;
                    }
                    break;
                case 1:
                case 3:
                    trackMovement(event);
                    if (this.mQsTracking) {
                        if (event.getActionMasked() != 3) {
                            z = false;
                        }
                        flingQsWithCurrentVelocity(y, z);
                        this.mQsTracking = false;
                    }
                    this.mIntercepting = false;
                    break;
                case 2:
                    float h = y - this.mInitialTouchY;
                    trackMovement(event);
                    if (this.mQsTracking) {
                        setQsExpansion(this.mInitialHeightOnTouch + h);
                        trackMovement(event);
                        this.mIntercepting = false;
                        return true;
                    } else if (Math.abs(h) > ((float) this.mTouchSlop) && Math.abs(h) > Math.abs(x - this.mInitialTouchX) && shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, h)) {
                        this.mQsTracking = true;
                        onQsExpansionStarted();
                        notifyExpandingFinished();
                        this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                        this.mInitialTouchY = y;
                        this.mInitialTouchX = x;
                        this.mIntercepting = false;
                        this.mNotificationStackScroller.removeLongPressCallback();
                        return true;
                    }
            }
        } else {
            int upPointer = event.getPointerId(event.getActionIndex());
            if (this.mTrackingPointer == upPointer) {
                if (event.getPointerId(0) != upPointer) {
                    z = false;
                }
                int newIndex = z;
                this.mTrackingPointer = event.getPointerId((int) newIndex);
                this.mInitialTouchX = event.getX(newIndex);
                this.mInitialTouchY = event.getY(newIndex);
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isInContentBounds(float x, float y) {
        float stackScrollerX = this.mNotificationStackScroller.getX();
        return !this.mNotificationStackScroller.isBelowLastNotification(x - stackScrollerX, y) && stackScrollerX < x && x < ((float) this.mNotificationStackScroller.getWidth()) + stackScrollerX;
    }

    /* access modifiers changed from: protected */
    public boolean isInUnderlapBounds(float x, float y) {
        return ((float) getHeight()) - y < this.mCloseHandleUnderlapSize;
    }

    private void initDownStates(MotionEvent event) {
        if (event.getActionMasked() == 0) {
            boolean z = false;
            this.mOnlyAffordanceInThisMotion = false;
            this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
            this.mDozingOnDown = isDozing();
            this.mCollapsedOnDown = isFullyCollapsed();
            if (this.mCollapsedOnDown && this.mHeadsUpManager.hasPinnedHeadsUp()) {
                z = true;
            }
            this.mListenForHeadsUp = z;
        }
    }

    private void flingQsWithCurrentVelocity(float y, boolean isCancelMotionEvent) {
        float vel = getCurrentQSVelocity();
        boolean expandsQs = flingExpandsQs(vel);
        if (expandsQs) {
            logQsSwipeDown(y);
        }
        flingSettings(vel, expandsQs && !isCancelMotionEvent);
    }

    private void logQsSwipeDown(float y) {
        int gesture;
        float vel = getCurrentQSVelocity();
        if (this.mStatusBarState == 1) {
            gesture = 193;
        } else {
            gesture = 194;
        }
        this.mLockscreenGestureLogger.write(getContext(), gesture, (int) ((y - this.mInitialTouchY) / this.mStatusBar.getDisplayDensity()), (int) (vel / this.mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float vel) {
        boolean z = false;
        if (isFalseTouch()) {
            return false;
        }
        if (Math.abs(vel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            if (getQsExpansionFraction() > 0.5f) {
                z = true;
            }
            return z;
        }
        if (vel > 0.0f) {
            z = true;
        }
        return z;
    }

    private boolean isFalseTouch() {
        if (!needsAntiFalsing()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        return !this.mQsTouchAboveFalsingThreshold;
    }

    /* access modifiers changed from: protected */
    public float getQsExpansionFraction() {
        return Math.min(1.0f, (this.mQsExpansionHeight - ((float) this.mQsMinExpansionHeight)) / ((float) (getTempQsMaxExpansion() - this.mQsMinExpansionHeight)));
    }

    /* access modifiers changed from: protected */
    public float getOpeningHeight() {
        return this.mNotificationStackScroller.getOpeningHeight();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mBlockTouches || isQsDetailShowing()) {
            return false;
        }
        initDownStates(event);
        boolean z = true;
        if (this.mListenForHeadsUp && !this.mHeadsUpTouchHelper.isTrackingHeadsUp() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
        }
        boolean handled = false;
        if ((!this.mIsExpanding || this.mHintAnimationRunning) && !this.mQsExpanded && this.mStatusBar.getBarState() != 0 && !this.mDozing) {
            handled = false | this.mKeyguardMoveHelper.onTouchEvent(event);
        }
        if (this.mOnlyAffordanceInThisMotion || this.mKeyguardMoveHelper.isInLeftView() || this.mHeadsUpTouchHelper.onTouchEvent(event)) {
            return true;
        }
        if (this.mQsOverscrollExpansionEnabled && !this.mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(event)) {
            return true;
        }
        if (event.getActionMasked() == 0 && isFullyCollapsed()) {
            MetricsLogger.count(this.mContext, "panel_open", 1);
            resetVerticalPanelPosition();
            handled = true;
        }
        this.mLockScreenMagazineController.onTouchEvent(event, this.mStatusBarState, this.mInitialTouchX, this.mInitialTouchY);
        this.mKeyguardIndicationController.onTouchEvent(event, this.mStatusBarState, this.mInitialTouchX, this.mInitialTouchY);
        boolean handled2 = handled | super.onTouchEvent(event);
        if (this.mDozing) {
            z = handled2;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public boolean isExpandForbiddenInKeyguard() {
        return this.mStatusBar.isKeyguardShowing() && !this.mExpandableUnderKeyguard;
    }

    private boolean handleQsTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0 && getExpandedFraction() == 1.0f && this.mStatusBar.getBarState() != 1 && !this.mQsExpanded && this.mQsExpansionEnabled && !this.mNotificationStackScroller.isQsCovered()) {
            this.mQsTracking = true;
            this.mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(event);
        }
        if (!this.mQsExpandImmediate && this.mQsTracking) {
            onQsTouch(event);
            if (!this.mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (action == 3 || action == 1) {
            this.mConflictingQsExpansionGesture = false;
        }
        if (action == 0 && isFullyCollapsed() && this.mQsExpansionEnabled) {
            this.mTwoFingerQsExpandPossible = true;
        }
        if (this.mTwoFingerQsExpandPossible && isOpenQsEvent(event) && event.getY(event.getActionIndex()) < ((float) this.mStatusBarMinHeight)) {
            MetricsLogger.count(this.mContext, "panel_open_qs", 1);
            this.mQsExpandImmediate = true;
            requestPanelHeightUpdate();
            setListening(true);
        }
        return false;
    }

    private boolean isOpenQsEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        boolean twoFingerDrag = action == 5 && pointerCount == 2;
        boolean stylusButtonClickDrag = action == 0 && (event.isButtonPressed(32) || event.isButtonPressed(64));
        boolean mouseButtonClickDrag = action == 0 && (event.isButtonPressed(2) || event.isButtonPressed(4));
        if (twoFingerDrag || stylusButtonClickDrag || mouseButtonClickDrag) {
            return true;
        }
        return false;
    }

    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() == 0 && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1.0f)) {
            this.mFalsingManager.onQsDown();
            this.mQsTracking = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
            notifyExpandingFinished();
        }
    }

    /* access modifiers changed from: protected */
    public boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean expands = super.flingExpands(vel, vectorVel, x, y);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return expands;
    }

    /* access modifiers changed from: protected */
    public boolean hasConflictingGestures() {
        return this.mStatusBar.getBarState() != 0;
    }

    public boolean isKeyguardShowing() {
        return this.mKeyguardShowing;
    }

    /* access modifiers changed from: protected */
    public boolean shouldGestureIgnoreXTouchSlop(float x, float y) {
        return !this.mKeyguardMoveHelper.isOnAffordanceIcon(x, y);
    }

    private void onQsTouch(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float y = event.getY(pointerIndex);
        float x = event.getX(pointerIndex);
        float h = y - this.mInitialTouchY;
        int actionMasked = event.getActionMasked();
        boolean z = false;
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    this.mQsTracking = true;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    onQsExpansionStarted();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    initVelocityTracker();
                    trackMovement(event);
                    return;
                case 1:
                case 3:
                    this.mQsTracking = false;
                    this.mTrackingPointer = -1;
                    trackMovement(event);
                    if (getQsExpansionFraction() != 0.0f || y >= this.mInitialTouchY) {
                        if (event.getActionMasked() == 3) {
                            z = true;
                        }
                        flingQsWithCurrentVelocity(y, z);
                    } else {
                        refreshNotificationStackScrollerVisible();
                    }
                    if (this.mQsVelocityTracker != null) {
                        this.mQsVelocityTracker.recycle();
                        this.mQsVelocityTracker = null;
                        return;
                    }
                    return;
                case 2:
                    if (!isExpandForbiddenInKeyguard()) {
                        setQsExpansion(this.mInitialHeightOnTouch + h);
                        if (h >= ((float) getFalsingThreshold())) {
                            this.mQsTouchAboveFalsingThreshold = true;
                        }
                        trackMovement(event);
                        return;
                    }
                    return;
                default:
                    return;
            }
        } else {
            int upPointer = event.getPointerId(event.getActionIndex());
            if (this.mTrackingPointer == upPointer) {
                if (event.getPointerId(0) == upPointer) {
                    z = true;
                }
                int newIndex = z;
                float newY = event.getY((int) newIndex);
                float newX = event.getX(newIndex);
                this.mTrackingPointer = event.getPointerId(newIndex);
                this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                this.mInitialTouchY = newY;
                this.mInitialTouchX = newX;
            }
        }
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mQsFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        if (this.mQsOverscrollExpansionEnabled && !this.mNotificationStackScroller.isQsCovered()) {
            cancelQsAnimation();
            if (!this.mQsExpansionEnabled) {
                amount = 0.0f;
            }
            float rounded = amount >= 1.0f ? amount : 0.0f;
            boolean z = false;
            setOverScrolling(rounded != 0.0f && isRubberbanded);
            if (rounded != 0.0f) {
                z = true;
            }
            this.mQsExpansionFromOverscroll = z;
            this.mLastOverscroll = rounded;
            updateQsState();
            setQsExpansion(((float) this.mQsMinExpansionHeight) + rounded);
        }
    }

    public void flingTopOverscroll(float velocity, boolean open) {
        if (this.mQsOverscrollExpansionEnabled && !this.mNotificationStackScroller.isQsCovered()) {
            float f = 0.0f;
            this.mLastOverscroll = 0.0f;
            this.mQsExpansionFromOverscroll = false;
            setQsExpansion(this.mQsExpansionHeight);
            if (this.mQsExpansionEnabled || !open) {
                f = velocity;
            }
            flingSettings(f, open && this.mQsExpansionEnabled, new Runnable() {
                public void run() {
                    boolean unused = NotificationPanelView.this.mStackScrollerOverscrolling = false;
                    NotificationPanelView.this.setOverScrolling(false);
                    NotificationPanelView.this.updateQsState();
                }
            }, false);
        }
    }

    public void onScrollerTopPaddingUpdate(int topPadding) {
        this.mTopPaddingWhenQsBeingCovered = topPadding;
        positionClockAndNotifications();
        float fraction = 0.0f;
        if (this.mQs.getQsMinExpansionHeight() > this.mQs.getQsHeaderHeight()) {
            fraction = (((float) (this.mQs.getQsMinExpansionHeight() - topPadding)) * 1.0f) / ((float) (this.mQs.getQsMinExpansionHeight() - this.mQs.getQsHeaderHeight()));
        }
        if (this.mQs != null && this.mQs.getQsContent() != null && this.mQs.getQsContent().isShown()) {
            this.mQs.getQsContent().setScaleX(1.0f - (0.100000024f * fraction));
            this.mQs.getQsContent().setScaleY(1.0f - (0.100000024f * fraction));
            this.mQs.getQsContent().setAlpha(1.0f - (1.0f * fraction));
        }
    }

    /* access modifiers changed from: private */
    public void setOverScrolling(boolean overscrolling) {
        this.mStackScrollerOverscrolling = overscrolling;
        if (this.mQs != null) {
            this.mQs.setOverscrolling(overscrolling);
        }
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    /* access modifiers changed from: protected */
    public void onQsExpansionStarted(int overscrollAmount) {
        cancelQsAnimation();
        cancelHeightAnimator();
        setQsExpansion(this.mQsExpansionHeight - ((float) overscrollAmount));
        requestPanelHeightUpdate();
        this.mNotificationStackScroller.checkSnoozeLeavebehind();
    }

    private void setQsExpanded(boolean expanded) {
        boolean changed = this.mQsExpanded != expanded;
        if (!expanded && this.mIsKeyguardCoverd) {
            this.mIsKeyguardCoverd = false;
            LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Uncovered");
        }
        if (changed) {
            this.mQsExpanded = expanded;
            sQsExpanded = this.mQsExpanded;
            updateQsState();
            requestPanelHeightUpdate();
            this.mFalsingManager.setQsExpanded(expanded);
            this.mStatusBar.setQsExpanded(expanded);
            this.mNotificationContainerParent.setQsExpanded(expanded);
            if (MiuiKeyguardUtils.isGxzwSensor()) {
                updateGxzwState();
            }
            if (this.mQsExpanded && this.mLockScreenMagazineController != null && (this.mLockScreenMagazineController.isSwitchAnimating() || this.mLockScreenMagazinePreViewVisible)) {
                this.mLockScreenMagazineController.reset();
            }
            refreshNotificationStackScrollerVisible();
        }
    }

    public void setBarState(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        int oldState = this.mStatusBarState;
        boolean keyguardShowing = statusBarState == 1;
        setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);
        this.mStatusBarState = statusBarState;
        setKeyguardOtherViewVisibility(statusBarState);
        if (keyguardShowing && !this.mKeyguardShowing) {
            updateWallpaper(false);
            if (!this.mGetCameraImageSucceed) {
                setCameraImage();
            }
            setLockScreenMagazineLeftPreImage();
            this.mNotificationStackScroller.resetIsQsCovered(false);
        }
        this.mKeyguardShowing = keyguardShowing;
        if (this.mQs != null) {
            this.mQs.setKeyguardShowing(this.mKeyguardShowing);
        }
        if (oldState == 1 && (goingToFullShade || statusBarState == 2)) {
            animateKeyguardStatusBarOut();
            this.mQs.animateHeaderSlidingIn(this.mStatusBarState == 2 ? 0 : this.mStatusBar.calculateGoingToFullShadeDelay());
        } else if (oldState == 2 && statusBarState == 1) {
            animateKeyguardStatusBarIn(360);
            this.mQs.animateHeaderSlidingOut();
        } else {
            this.mKeyguardStatusBar.setAlpha(1.0f);
            this.mKeyguardStatusBar.setVisibility(keyguardShowing ? 0 : 4);
            if (keyguardShowing && oldState != this.mStatusBarState) {
                this.mKeyguardBottomArea.onKeyguardShowingChanged();
                if (this.mQs != null) {
                    this.mQs.hideImmediately();
                }
            }
        }
        if (keyguardShowing) {
            updateDozingVisibilities(false);
        }
        updateNotchCornerVisibility();
        resetVerticalPanelPosition();
        updateQsState();
    }

    public static boolean isDefaultLockScreenTheme() {
        return !ThemeResources.getSystem().containsAwesomeLockscreenEntry("manifest.xml");
    }

    private void setCameraImage() {
        new AsyncTask<Void, Void, Drawable>() {
            /* access modifiers changed from: protected */
            public Drawable doInBackground(Void... params) {
                if (NotificationPanelView.this.mUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot()) {
                    return PackageUtils.getDrawableFromPackage(NotificationPanelView.this.mContext, "com.android.camera", MiuiKeyguardUtils.getCameraImageName());
                }
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Drawable result) {
                if (result != null) {
                    if (MiuiKeyguardUtils.isPad()) {
                        NotificationPanelView.this.mKeyguardRightView.setBackground(result);
                    } else {
                        NotificationPanelView.this.mKeyguardRightView.setImageDrawable(result);
                    }
                    boolean unused = NotificationPanelView.this.mGetCameraImageSucceed = true;
                    return;
                }
                PanelBar.LOG(NotificationPanelView.TAG, "set default camera image");
                if (MiuiKeyguardUtils.isPad()) {
                    NotificationPanelView.this.mKeyguardRightView.setBackgroundResource(R.drawable.camera_preview);
                } else {
                    NotificationPanelView.this.mKeyguardRightView.setImageResource(R.drawable.camera_preview);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void setLockScreenMagazineLeftPreImage() {
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... params) {
                if (!NotificationPanelView.this.mUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot() || !NotificationPanelView.this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                    Drawable unused = NotificationPanelView.this.mPreTransToLeftScreenDrawable = null;
                    Drawable unused2 = NotificationPanelView.this.mPreLeftScreenDrawable = null;
                } else {
                    Drawable unused3 = NotificationPanelView.this.mPreTransToLeftScreenDrawable = PackageUtils.getDrawableFromPackage(NotificationPanelView.this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, NotificationPanelView.this.mLockScreenMagazineController.getPreTransToLeftScreenDrawableResName());
                    Drawable unused4 = NotificationPanelView.this.mPreLeftScreenDrawable = PackageUtils.getDrawableFromPackage(NotificationPanelView.this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, NotificationPanelView.this.mLockScreenMagazineController.getPreLeftScreenDrawableResName());
                }
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                if (NotificationPanelView.this.mPreLeftScreenDrawable != null) {
                    NotificationPanelView.this.mKeyguardLeftView.setPreBackgroundDrawable(NotificationPanelView.this.mPreLeftScreenDrawable);
                }
                if (NotificationPanelView.this.mPreTransToLeftScreenDrawable != null) {
                    NotificationPanelView.this.mLeftViewBg.setBackground(NotificationPanelView.this.mPreTransToLeftScreenDrawable);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void animateKeyguardStatusBarOut() {
        long j;
        long j2;
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{this.mKeyguardStatusBar.getAlpha(), 0.0f});
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            j = this.mStatusBar.getKeyguardFadingAwayDelay();
        } else {
            j = 0;
        }
        anim.setStartDelay(j);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            j2 = this.mStatusBar.getKeyguardFadingAwayDuration() / 2;
        } else {
            j2 = 360;
        }
        anim.setDuration(j2);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        anim.start();
    }

    private void animateKeyguardStatusBarIn(long duration) {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        anim.setDuration(duration);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.start();
    }

    private void setKeyguardBottomAreaVisibility(int statusBarState, boolean goingToFullShade) {
        this.mKeyguardBottomArea.animate().cancel();
        if (goingToFullShade) {
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
        } else if (statusBarState == 1 || statusBarState == 2) {
            this.mKeyguardBottomArea.setVisibility(this.mIsDefaultTheme ? 0 : 4);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            this.mKeyguardBottomArea.setVisibility(8);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        }
    }

    private void setKeyguardOtherViewVisibility(int statusBarState) {
        boolean keyguardShowing = true;
        int i = 0;
        if (statusBarState != 1) {
            keyguardShowing = false;
        }
        int i2 = 4;
        this.mWallpaperView.setVisibility((statusBarState == 0 || !this.mIsDefaultTheme) ? 4 : 0);
        updateThemeBackgroundVisibility(statusBarState);
        if (this.mLiveLockWallpaperView != null) {
            this.mLiveLockWallpaperView.setVisibility(keyguardShowing ? 0 : 4);
            if (this.mLiveLockWallpaperPlayer != null) {
                if (keyguardShowing) {
                    if (!this.mLiveLockWallpaperPlayer.isPlaying()) {
                        startLiveLockWallpaper();
                    }
                } else if (this.mLiveLockWallpaperPlayer.isPlaying()) {
                    this.mLiveLockWallpaperPlayer.pause();
                }
            }
        }
        this.mKeyguardLeftView.setVisibility(keyguardShowing ? 0 : 4);
        ImageView imageView = this.mKeyguardRightView;
        if (keyguardShowing) {
            i2 = 0;
        }
        imageView.setVisibility(i2);
        View view = this.mSwitchSystemUser;
        if (!keyguardShowing || !shouldShowSwitchSystemUser()) {
            i = 8;
        }
        view.setVisibility(i);
        refreshNotificationStackScrollerVisible();
    }

    private void updateThemeBackgroundVisibility(int statusBarState) {
        this.mThemeBackgroundView.setVisibility(statusBarState == 0 ? 0 : 4);
    }

    private void updateNotchCornerVisibility() {
        if (CustomizeUtil.HAS_NOTCH) {
            this.mNotchCorner.setVisibility((!this.mForceBlack || !this.mKeyguardShowing) ? 8 : 0);
        }
    }

    /* access modifiers changed from: protected */
    public void notifyBarPanelExpansionChanged() {
        super.notifyBarPanelExpansionChanged();
        refreshNotificationStackScrollerVisible();
    }

    public void refreshNotificationStackScrollerVisible() {
        this.mNotificationStackScroller.setVisibility((this.mStatusBarState != 1 || this.mIsDefaultTheme || this.mQsTracking || this.mQsExpanded) ? 0 : 4);
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        if ((keyguardFadingAway || this.mStatusBarState != 1 || statusBarState == 1) && !goingToFullShade) {
            int i = 4;
            if (this.mStatusBarState == 2 && statusBarState == 1) {
                this.mKeyguardClockView.animate().cancel();
                KeyguardClockContainer keyguardClockContainer = this.mKeyguardClockView;
                if (this.mIsDefaultTheme) {
                    i = 0;
                }
                keyguardClockContainer.setVisibility(i);
                addAwesomeLockScreenIfNeed();
                this.mKeyguardStatusViewAnimating = true;
                this.mKeyguardClockView.setAlpha(0.0f);
                this.mKeyguardClockView.animate().alpha(1.0f).setStartDelay(0).setDuration(320).setInterpolator(Interpolators.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
            } else if (statusBarState == 1) {
                this.mKeyguardClockView.animate().cancel();
                this.mKeyguardStatusViewAnimating = false;
                KeyguardClockContainer keyguardClockContainer2 = this.mKeyguardClockView;
                if (this.mIsDefaultTheme) {
                    i = 0;
                }
                keyguardClockContainer2.setVisibility(i);
                addAwesomeLockScreenIfNeed();
                this.mKeyguardClockView.setAlpha(1.0f);
            } else {
                this.mKeyguardClockView.animate().cancel();
                this.mKeyguardStatusViewAnimating = false;
                this.mLockScreenMagazineController.reset();
                this.mKeyguardClockView.setVisibility(8);
                this.mKeyguardClockView.setAlpha(1.0f);
                removeAwesomeLockScreen();
            }
        } else {
            this.mKeyguardClockView.animate().cancel();
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardClockView.animate().alpha(0.0f).setStartDelay(0).setDuration(160).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable);
            if (keyguardFadingAway) {
                this.mKeyguardClockView.animate().setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).start();
            }
        }
    }

    private void addAwesomeLockScreenIfNeed() {
        addAwesomeLockScreenIfNeed(false);
    }

    private void addAwesomeLockScreenIfNeed(boolean force) {
        if ((this.mAwesomeLockScreen == null && !this.mIsDefaultTheme) || force) {
            this.mAwesomeLockScreen = new AwesomeLockScreen(this.mContext, this.mStatusBar, this, this.mBar);
            this.mAwesomeLockScreenContainer.removeAllViews();
            this.mAwesomeLockScreenContainer.addView(this.mAwesomeLockScreen);
        }
        if (this.mAwesomeLockScreen != null) {
            this.mAwesomeLockScreenContainer.setVisibility(0);
        }
    }

    private void onAwesomeLockScreenResume() {
        if (this.mAwesomeLockScreen != null) {
            this.mAwesomeLockScreen.onResume(false);
        }
    }

    /* access modifiers changed from: private */
    public void onAwesomeLockScreenPause() {
        if (this.mAwesomeLockScreen != null) {
            this.mAwesomeLockScreen.onPause();
        }
    }

    private void removeAwesomeLockScreen() {
        if (this.mAwesomeLockScreen != null) {
            this.mAwesomeLockScreen.onPause();
            this.mAwesomeLockScreenContainer.removeAllViews();
            this.mAwesomeLockScreen = null;
            this.mAwesomeLockScreenContainer.setVisibility(8);
        }
    }

    /* access modifiers changed from: private */
    public void updateQsState() {
        this.mNotificationStackScroller.setQsExpanded(this.mQsExpanded);
        this.mNotificationStackScroller.setScrollingEnabled(this.mStatusBarState != 1 && (!this.mQsExpanded || this.mQsExpansionFromOverscroll));
        updateEmptyShadeView();
        if (this.mKeyguardUserSwitcher != null && this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            this.mKeyguardUserSwitcher.hideIfNotSimple(true);
        }
        if (this.mQs != null) {
            this.mQs.setExpanded(this.mQsExpanded);
        }
    }

    /* access modifiers changed from: private */
    public void setQsExpansion(float height) {
        if (DEBUG) {
            String str = TAG;
            Log.d(str, "setQsExpansion height=" + height);
        }
        float height2 = Math.min(Math.max(height, (float) this.mQsMinExpansionHeight), (float) this.mQsMaxExpansionHeight);
        this.mQsFullyExpanded = !isFullyCollapsed() && height2 == ((float) this.mQsMaxExpansionHeight) && this.mQsMaxExpansionHeight != 0;
        if (this.mQsFullyExpanded && this.mIsKeyguardCoverd != this.mQsFullyExpanded) {
            this.mIsKeyguardCoverd = true;
            LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Covered");
        }
        if (height2 > ((float) this.mQsMinExpansionHeight) && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height2 <= ((float) this.mQsMinExpansionHeight) && this.mQsExpanded) {
            setQsExpanded(false);
            if (this.mLastAnnouncementWasQuickSettings && !this.mTracking && !isCollapsing()) {
                announceForAccessibility(getKeyguardOrLockScreenString());
                this.mLastAnnouncementWasQuickSettings = false;
            }
        }
        this.mQsExpansionHeight = height2;
        updateQsExpansion();
        updateDismissViewState();
        requestScrollerTopPaddingUpdate(false);
        if (this.mKeyguardShowing) {
            updateHeaderKeyguardAlpha();
        }
        if (this.mStatusBarState == 2 || this.mStatusBarState == 1) {
            if (height2 <= 0.0f) {
                setListening(false);
            } else if (height2 >= ((float) this.mQsMaxExpansionHeight)) {
                setListening(true);
            }
            updateKeyguardBottomAreaAlpha();
        }
        if (height2 != 0.0f && this.mQsFullyExpanded && !this.mLastAnnouncementWasQuickSettings) {
            announceForAccessibility(getContext().getString(R.string.accessibility_desc_quick_settings));
            this.mLastAnnouncementWasQuickSettings = true;
        }
        if (this.mQsFullyExpanded && this.mFalsingManager.shouldEnforceBouncer()) {
            this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
        }
        if (DEBUG) {
            invalidate();
        }
    }

    /* access modifiers changed from: private */
    public void updateDismissViewState() {
        boolean z = false;
        boolean dismissViewShowUp = getAppearFraction() > 0.8f && !isQsDetailShowing();
        if (this.mDismissViewShowUp != dismissViewShowUp) {
            if (this.mDismissViewAnimator != null) {
                this.mDismissViewAnimator.cancel();
            }
            if (!dismissViewShowUp) {
                z = true;
            }
            this.mDismissViewAnimator = createDismissViewAnimator(z);
            this.mDismissViewAnimator.start();
        }
        this.mDismissViewShowUp = dismissViewShowUp;
    }

    private Animator createDismissViewAnimator(boolean collapse) {
        DismissView dismissView = this.mDismissView;
        Property property = View.ALPHA;
        float[] fArr = new float[2];
        fArr[0] = this.mDismissView.getAlpha();
        float f = 0.0f;
        fArr[1] = collapse ? 0.0f : 1.0f;
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(dismissView, property, fArr);
        DismissView dismissView2 = this.mDismissView;
        Property property2 = View.TRANSLATION_Y;
        float[] fArr2 = new float[2];
        fArr2[0] = this.mDismissView.getTranslationY();
        if (collapse) {
            f = (float) (getHeight() - this.mDismissView.getTop());
        }
        fArr2[1] = f;
        ObjectAnimator transAnim = ObjectAnimator.ofFloat(dismissView2, property2, fArr2);
        AnimatorSet ret = new AnimatorSet();
        ret.setDuration(collapse ? 150 : 300);
        ret.setInterpolator(collapse ? Interpolators.ACCELERATE : Interpolators.DECELERATE_CUBIC);
        ret.playTogether(new Animator[]{alphaAnim, transAnim});
        ret.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Animator unused = NotificationPanelView.this.mDismissViewAnimator = null;
                super.onAnimationEnd(animation);
            }
        });
        return ret;
    }

    /* access modifiers changed from: protected */
    public void updateQsExpansion() {
        if (this.mQs != null) {
            this.mQs.setQsExpansion(getQsExpansionFraction(), getHeaderTranslation(), getAppearFraction());
        }
    }

    public boolean isQSFullyCollapsed() {
        if (!this.mKeyguardShowing) {
            return isFullyCollapsed();
        }
        return this.mQs == null || this.mQs.isQSFullyCollapsed();
    }

    private String getKeyguardOrLockScreenString() {
        if (this.mQs != null && this.mQs.isCustomizing()) {
            return getContext().getString(R.string.accessibility_desc_quick_settings_edit);
        }
        if (this.mStatusBarState == 1) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        }
        return getContext().getString(R.string.accessibility_desc_notification_shade);
    }

    private float calculateQsTopPadding() {
        int max;
        if (this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted))) {
            int maxNotifications = this.mClockPositionResult.stackScrollerPadding - this.mClockPositionResult.stackScrollerPaddingAdjustment;
            int maxQs = getTempQsMaxExpansion();
            if (this.mStatusBarState == 1) {
                max = Math.max(maxNotifications, maxQs);
            } else {
                max = maxQs;
            }
            return (float) ((int) interpolate(getExpandedFraction(), (float) this.mQsMinExpansionHeight, (float) max));
        } else if (this.mQsSizeChangeAnimator != null) {
            return (float) ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        } else {
            if (this.mKeyguardShowing) {
                return interpolate(getQsExpansionFraction(), (float) this.mNotificationStackScroller.getIntrinsicPadding(), (float) this.mQsMaxExpansionHeight);
            }
            if (this.mNotificationStackScroller.isQsCovered()) {
                return (float) this.mQs.getQsHeaderHeight();
            }
            if (this.mNotificationStackScroller.isQsBeingCovered()) {
                return (float) this.mTopPaddingWhenQsBeingCovered;
            }
            return this.mQsExpansionHeight;
        }
    }

    /* access modifiers changed from: protected */
    public void requestScrollerTopPaddingUpdate(boolean animate) {
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        float calculateQsTopPadding = calculateQsTopPadding();
        boolean z = true;
        boolean z2 = this.mAnimateNextTopPaddingChange || animate;
        if (!this.mKeyguardShowing || (!this.mQsExpandImmediate && (!this.mIsExpanding || !this.mQsExpandedWhenExpandingStarted))) {
            z = false;
        }
        notificationStackScrollLayout.updateTopPadding(calculateQsTopPadding, z2, z);
        this.mAnimateNextTopPaddingChange = false;
    }

    private void trackMovement(MotionEvent event) {
        if (this.mQsVelocityTracker != null) {
            this.mQsVelocityTracker.addMovement(event);
        }
        this.mLastTouchX = event.getX();
        this.mLastTouchY = event.getY();
    }

    private void initVelocityTracker() {
        if (this.mQsVelocityTracker != null) {
            this.mQsVelocityTracker.recycle();
        }
        this.mQsVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentQSVelocity() {
        if (this.mQsVelocityTracker == null) {
            return 0.0f;
        }
        this.mQsVelocityTracker.computeCurrentVelocity(1000);
        return this.mQsVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        if (this.mQsExpansionAnimator != null) {
            this.mQsExpansionAnimator.cancel();
        }
    }

    public void flingSettings(float vel, boolean expand) {
        flingSettings(vel, expand, null, false);
    }

    /* access modifiers changed from: protected */
    public void flingSettings(float vel, boolean expand, final Runnable onFinishRunnable, boolean isClick) {
        float target = (float) (expand ? this.mQsMaxExpansionHeight : this.mQsMinExpansionHeight);
        if (target == this.mQsExpansionHeight) {
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
            }
            return;
        }
        if (this.mPerf != null) {
            this.mPerf.perfHint(4224, this.mContext.getPackageName(), -1, 1);
        }
        boolean oppositeDirection = false;
        if ((vel > 0.0f && !expand) || (vel < 0.0f && expand)) {
            vel = 0.0f;
            oppositeDirection = true;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mQsExpansionHeight, target});
        if (isClick) {
            animator.setInterpolator(Interpolators.TOUCH_RESPONSE);
            animator.setDuration(368);
        } else {
            this.mFlingAnimationUtils.apply((Animator) animator, this.mQsExpansionHeight, target, vel);
        }
        if (oppositeDirection) {
            animator.setDuration(350);
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.setQsExpansion(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (NotificationPanelView.this.mPerf != null) {
                    NotificationPanelView.this.mPerf.perfLockRelease();
                }
                NotificationPanelView.this.mNotificationStackScroller.resetCheckSnoozeLeavebehind();
                ValueAnimator unused = NotificationPanelView.this.mQsExpansionAnimator = null;
                if (onFinishRunnable != null) {
                    onFinishRunnable.run();
                }
            }
        });
        animator.start();
        this.mQsExpansionAnimator = animator;
        this.mQsAnimatorExpand = expand;
    }

    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff) {
        boolean z = false;
        if (!this.mQsExpansionEnabled || this.mCollapsedOnDown || this.mNotificationStackScroller.isQsCovered()) {
            return false;
        }
        if (this.mQsExpanded && this.mKeyguardShowing && isXWithinQsFrame(x)) {
            return true;
        }
        boolean onQuickQs = isInQuickQsArea(x, y);
        if (!this.mQsExpanded) {
            return onQuickQs;
        }
        if (onQuickQs || (yDiff < 0.0f && isInQsArea(x, y))) {
            z = true;
        }
        return z;
    }

    private boolean isInQuickQsArea(float x, float y) {
        boolean z = false;
        if (this.mKeyguardShowing) {
            if (isXWithinQsFrame(x) && y >= ((float) this.mKeyguardStatusBar.getTop()) && y <= ((float) this.mKeyguardStatusBar.getBottom())) {
                z = true;
            }
            return z;
        }
        if (isXWithinQsFrame(x) && y >= ((float) this.mQs.getHeader().getTop()) && y <= ((float) (this.mQs.getHeader().getTop() + this.mQs.getQsMinExpansionHeight()))) {
            z = true;
        }
        return z;
    }

    private boolean isInQsArea(float x, float y) {
        return isXWithinQsFrame(x) && (y <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || y <= this.mQs.getView().getY() + ((float) this.mQs.getView().getHeight()));
    }

    private boolean isXWithinQsFrame(float x) {
        return x >= this.mQsFrame.getX() && x <= this.mQsFrame.getX() + ((float) this.mQsFrame.getWidth());
    }

    /* access modifiers changed from: protected */
    public boolean isScrolledToBottom() {
        boolean z = true;
        if (isInSettings()) {
            return true;
        }
        if (this.mStatusBar.getBarState() != 1 && !this.mNotificationStackScroller.isScrolledToBottom()) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public int getMaxPanelHeight() {
        int maxHeight;
        int min = this.mStatusBarMinHeight;
        if (this.mStatusBar.getBarState() != 1 && this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            min = Math.max(min, (int) (((float) this.mQsMinExpansionHeight) + getOverExpansionAmount()));
        }
        if (this.mQsExpandImmediate != 0 || this.mQsExpanded || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            maxHeight = calculatePanelHeightQsExpanded();
        } else {
            maxHeight = calculatePanelHeightShade();
        }
        return Math.max(maxHeight, min);
    }

    public boolean isInSettings() {
        return this.mQsExpanded;
    }

    public boolean isExpanding() {
        return this.mIsExpanding;
    }

    /* access modifiers changed from: protected */
    public void onHeightUpdated(float expandedHeight) {
        float panelHeightQsCollapsed;
        if (DEBUG) {
            String str = TAG;
            Log.d(str, "onHeightUpdated expandedHeight=" + expandedHeight);
        }
        if (!this.mQsExpanded || this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            positionClockAndNotifications();
        }
        if (this.mQsExpandImmediate || (this.mQsExpanded && !this.mQsTracking && this.mQsExpansionAnimator == null && !this.mQsExpansionFromOverscroll)) {
            if (this.mKeyguardShowing) {
                panelHeightQsCollapsed = expandedHeight / ((float) getMaxPanelHeight());
            } else {
                float panelHeightQsCollapsed2 = (float) (this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getLayoutMinHeight());
                panelHeightQsCollapsed = (expandedHeight - panelHeightQsCollapsed2) / (((float) calculatePanelHeightQsExpanded()) - panelHeightQsCollapsed2);
            }
            setQsExpansion(((float) this.mQsMinExpansionHeight) + (((float) (getTempQsMaxExpansion() - this.mQsMinExpansionHeight)) * panelHeightQsCollapsed));
        }
        updateExpandedHeight(expandedHeight);
        updateHeader();
        updateNotificationTranslucency();
        updatePanelExpanded();
        updateStatusBarWindowBlur();
        this.mStatusBar.getStatusBarView().setVisibility(this.mExpandedHeight > 30.0f ? 4 : 0);
        this.mNotificationStackScroller.setShadeExpanded(!isFullyCollapsed());
        if (DEBUG) {
            invalidate();
        }
    }

    private void updateStatusBarWindowBlur() {
        float blurRatio;
        if (this.mStatusBarState == 0) {
            float blurFraction = Math.min(Math.max(0.0f, (this.mExpandedHeight * 1.0f) / getPeekHeight()), 1.0f);
            if (this.mFlingAfterTracking || isTracking()) {
                blurRatio = Interpolators.DECELERATE_QUINT.getInterpolation(blurFraction);
            } else {
                blurRatio = blurFraction;
            }
            ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setBlurRatio(blurRatio);
            return;
        }
        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setBlurRatio(0.0f);
    }

    private void updatePanelExpanded() {
        boolean isExpanded = !isFullyCollapsed();
        if (this.mPanelExpanded != isExpanded) {
            this.mHeadsUpManager.setIsExpanded(isExpanded);
            this.mStatusBar.setPanelExpanded(isExpanded);
            this.mPanelExpanded = isExpanded;
        }
    }

    private int getTempQsMaxExpansion() {
        return this.mQsMaxExpansionHeight;
    }

    private int calculatePanelHeightShade() {
        return (int) (((float) ((this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mTopPaddingAdjustment)) + this.mNotificationStackScroller.getTopPaddingOverflow());
    }

    private int calculatePanelHeightQsExpanded() {
        int i;
        float notificationHeight = (float) (this.mNotificationStackScroller.getContentHeight() - this.mNotificationStackScroller.getTopPadding());
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShowEmptyShadeView) {
            notificationHeight = (float) this.mNotificationStackScroller.getEmptyShadeViewHeight();
        }
        int maxQsHeight = this.mQsMaxExpansionHeight;
        if (this.mQsSizeChangeAnimator != null) {
            maxQsHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        if (this.mStatusBarState == 1) {
            i = this.mClockPositionResult.stackScrollerPadding - this.mTopPaddingAdjustment;
        } else {
            i = 0;
        }
        float totalHeight = ((float) Math.max(maxQsHeight, i)) + notificationHeight + this.mNotificationStackScroller.getTopPaddingOverflow();
        if (totalHeight > ((float) this.mNotificationStackScroller.getHeight())) {
            totalHeight = Math.max((float) (this.mNotificationStackScroller.getLayoutMinHeight() + maxQsHeight), (float) this.mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private void updateNotificationTranslucency() {
        float alpha = 1.0f;
        if (this.mClosingWithAlphaFadeOut && !this.mExpandingFromHeadsUp && !this.mHeadsUpManager.hasPinnedHeadsUp()) {
            alpha = getFadeoutAlpha();
        }
        if (!isQsDetailShowing()) {
            this.mNotificationStackScroller.setAlpha(alpha);
        }
    }

    private float getFadeoutAlpha() {
        return (float) Math.pow((double) Math.max(0.0f, Math.min((getNotificationsTopY() + ((float) this.mNotificationStackScroller.getFirstItemMinHeight())) / ((float) this.mQsMinExpansionHeight), 1.0f)), 0.75d);
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionAmount() {
        return this.mNotificationStackScroller.getCurrentOverScrollAmount(true);
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionPixels() {
        return this.mNotificationStackScroller.getCurrentOverScrolledPixels(true);
    }

    private void updateHeader() {
        if (this.mStatusBar.getBarState() == 1) {
            updateHeaderKeyguardAlpha();
        }
        updateQsExpansion();
        updateDismissViewState();
    }

    /* access modifiers changed from: protected */
    public float getHeaderTranslation() {
        if (this.mStatusBar.getBarState() == 1) {
            return 0.0f;
        }
        return Math.min(0.0f, NotificationUtils.interpolate((float) (-this.mQsMinExpansionHeight), 0.0f, this.mNotificationStackScroller.getAppearFraction(this.mExpandedHeight)));
    }

    private float getAppearFraction() {
        return Math.max(Math.min(1.0f, this.mNotificationStackScroller.getAppearFraction(this.mExpandedHeight)), 0.0f);
    }

    private float getKeyguardContentsAlpha() {
        float alpha;
        if (this.mStatusBar.getBarState() == 1) {
            alpha = getNotificationsTopY() / ((float) (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance));
        } else {
            alpha = getNotificationsTopY() / ((float) this.mKeyguardStatusBar.getHeight());
        }
        return (float) Math.pow((double) MathUtils.constrain(alpha, 0.0f, 1.0f), 0.75d);
    }

    /* access modifiers changed from: private */
    public void updateHeaderKeyguardAlpha() {
        float alpha = Math.min(getKeyguardContentsAlpha(), 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f)) * this.mKeyguardStatusBarAnimateAlpha;
        this.mKeyguardStatusBar.setAlpha(alpha);
        this.mKeyguardStatusBar.setVisibility((this.mKeyguardStatusBar.getAlpha() == 0.0f || this.mDozing || !this.mKeyguardShowing) ? 4 : 0);
        this.mLockScreenMagazineController.setWallPaperViewsAlpha(alpha);
    }

    private void updateKeyguardBottomAreaAlpha() {
        float alpha = Math.min(getKeyguardContentsAlpha(), 1.0f - getQsExpansionFraction());
        this.mKeyguardBottomArea.setAlpha(alpha);
        int i = 4;
        if (this.mStatusBarState == 1 || this.mStatusBarState == 2) {
            this.mKeyguardBottomArea.setVisibility((!this.mIsDefaultTheme || alpha == 0.0f) ? 4 : 0);
        }
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        if (alpha != 0.0f) {
            i = 0;
        }
        keyguardBottomAreaView.setImportantForAccessibility(i);
        invalidate();
    }

    private float getNotificationsTopY() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return this.mNotificationStackScroller.getNotificationsTopY();
    }

    /* access modifiers changed from: protected */
    public void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (this.mQsExpanded) {
            onQsExpansionStarted();
        }
        setHeaderListening(true);
        setBrightnessListening(true);
    }

    /* access modifiers changed from: protected */
    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mHeadsUpManager.onExpandingFinished();
        this.mIsExpanding = false;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public void run() {
                    NotificationPanelView.this.setListening(false);
                    NotificationPanelView.this.setBrightnessListening(false);
                    NotificationPanelView.this.onAwesomeLockScreenPause();
                }
            });
            postOnAnimation(new Runnable() {
                public void run() {
                    NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mTwoFingerQsExpandPossible = false;
        this.mIsExpansionFromHeadsUp = false;
        this.mNotificationStackScroller.setTrackingHeadsUp(false);
        this.mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    /* access modifiers changed from: private */
    public void setListening(boolean listening) {
        if (this.mQs != null) {
            this.mQs.setListening(listening);
        }
    }

    public void setBrightnessListening(boolean listening) {
        if (this.mQs != null) {
            this.mQs.setBrightnessListening(listening);
        }
    }

    public void setHeaderListening(boolean listening) {
        if (this.mQs != null) {
            this.mQs.setHeaderListening(listening);
        }
    }

    public void expand(boolean animate) {
        super.expand(animate);
        if (this.mStatusBarState == 1 && this.mIsInteractive) {
            onAwesomeLockScreenResume();
        }
    }

    /* access modifiers changed from: protected */
    public void setOverExpansion(float overExpansion, boolean isPixels) {
        if (!this.mConflictingQsExpansionGesture && !this.mQsExpandImmediate && !this.mNotificationStackScroller.isQsCovered() && this.mStatusBar.getBarState() != 1) {
            this.mNotificationStackScroller.setOnHeightChangedListener(null);
            if (isPixels) {
                this.mNotificationStackScroller.setOverScrolledPixels(overExpansion, true, false);
            } else {
                this.mNotificationStackScroller.setOverScrollAmount(overExpansion, true, false);
            }
            this.mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onTrackingStarted() {
        this.mFalsingManager.onTrackingStarted();
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            this.mKeyguardMoveHelper.animateHideLeftRightIcon();
        }
        this.mNotificationStackScroller.onPanelTrackingStarted();
    }

    /* access modifiers changed from: protected */
    public void onTrackingStopped(boolean expand) {
        this.mFalsingManager.onTrackingStopped();
        super.onTrackingStopped(expand);
        if (expand) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        this.mNotificationStackScroller.onPanelTrackingStopped();
        if (!expand) {
            return;
        }
        if ((this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) && !this.mHintAnimationRunning) {
            this.mKeyguardMoveHelper.animateShowLeftRightIcon();
            this.mKeyguardMoveHelper.reset(true);
        }
    }

    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {
        ExpandableNotificationRow firstRow;
        if (view != null || !this.mQsExpanded) {
            ExpandableView firstChildNotGone = this.mNotificationStackScroller.getFirstChildNotGone();
            if (firstChildNotGone instanceof ExpandableNotificationRow) {
                firstRow = (ExpandableNotificationRow) firstChildNotGone;
            } else {
                firstRow = null;
            }
            if (firstRow != null && (view == firstRow || firstRow.getNotificationParent() == firstRow)) {
                requestScrollerTopPaddingUpdate(false);
            }
            requestPanelHeightUpdate();
        }
    }

    public void onReset(ExpandableView view) {
    }

    public void onQsHeightChanged() {
        int oldMaxHeight = this.mQsMaxExpansionHeight;
        this.mQsMaxExpansionHeight = this.mQs != null ? this.mQs.getDesiredHeight() : 0;
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = (float) this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
            if (this.mQsMaxExpansionHeight != oldMaxHeight) {
                startQsSizeChangeAnimation(oldMaxHeight, this.mQsMaxExpansionHeight);
            }
        }
    }

    private void saveValueToTunerService(final int qqs_count) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ((TunerService) Dependency.get(TunerService.class)).setValue("sysui_qqs_count", qqs_count);
            }
        });
    }

    private void reInflateThemeBackgroundView() {
        int index = indexOfChild(this.mThemeBackgroundView);
        removeView(this.mThemeBackgroundView);
        this.mThemeBackgroundView = LayoutInflater.from(getContext()).inflate(R.layout.notification_panel_window_bg, null, false);
        addView(this.mThemeBackgroundView, index);
        updateThemeBackgroundVisibility(this.mStatusBarState);
    }

    private void reInflateKeyguardMoveLeftView() {
        int index = indexOfChild(this.mKeyguardLeftView);
        removeView(this.mKeyguardLeftView);
        int i = 0;
        this.mKeyguardLeftView = (MiuiKeyguardMoveLeftViewContainer) LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_left_view_container, null, false);
        addView(this.mKeyguardLeftView, index);
        this.mKeyguardLeftView.setStatusBar(this.mStatusBar);
        MiuiKeyguardMoveLeftViewContainer miuiKeyguardMoveLeftViewContainer = this.mKeyguardLeftView;
        if (this.mStatusBarState != 1) {
            i = 4;
        }
        miuiKeyguardMoveLeftViewContainer.setVisibility(i);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mNavigationBarBottomHeight = insets.getStableInsetBottom();
        updateMaxHeadsUpTranslation();
        return insets;
    }

    private void updateMaxHeadsUpTranslation() {
        this.mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), this.mNavigationBarBottomHeight);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection != this.mOldLayoutDirection) {
            this.mKeyguardMoveHelper.onRtlPropertiesChanged();
            this.mOldLayoutDirection = layoutDirection;
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.expand_indicator) {
            onQsExpansionStarted();
            if (this.mQsExpanded) {
                flingSettings(0.0f, false, null, true);
            } else if (this.mQsExpansionEnabled) {
                this.mLockscreenGestureLogger.write(getContext(), 195, 0, 0);
                flingSettings(0.0f, true, null, true);
            }
        }
    }

    public void onAnimationToSideStarted(boolean rightPage, float translation, float vel) {
        boolean start = getLayoutDirection() == 1 ? rightPage : !rightPage;
        this.mIsLaunchTransitionRunning = true;
        this.mLaunchAnimationEndRunnable = null;
        float displayDensity = this.mStatusBar.getDisplayDensity();
        int lengthDp = Math.abs((int) (translation / displayDensity));
        int velocityDp = Math.abs((int) (vel / displayDensity));
        if (start) {
            this.mLockscreenGestureLogger.write(getContext(), 190, lengthDp, velocityDp);
            this.mFalsingManager.onLeftAffordanceOn();
        } else {
            if ("lockscreen_affordance".equals(this.mLastCameraLaunchSource)) {
                this.mLockscreenGestureLogger.write(getContext(), 189, lengthDp, velocityDp);
            }
            this.mFalsingManager.onCameraOn();
            this.mKeyguardBottomArea.launchCamera(this.mLastCameraLaunchSource);
        }
        this.mStatusBar.startLaunchTransitionTimeout();
        this.mBlockTouches = true;
    }

    public void triggerAction(boolean rightPage, float translation, float vel) {
        if (rightPage) {
            this.mKeyguardBottomArea.launchCamera(this.mLastCameraLaunchSource);
        } else if (this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            this.mKeyguardBottomArea.launchLockScreenMagazine(this.mLastCameraLaunchSource);
        }
    }

    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        if (this.mLaunchAnimationEndRunnable != null) {
            this.mLaunchAnimationEndRunnable.run();
            this.mLaunchAnimationEndRunnable = null;
        }
        this.mStatusBar.readyForKeyguardDone();
    }

    public float getMaxTranslationDistance() {
        return (float) Math.hypot((double) getWidth(), (double) getHeight());
    }

    public void onSwipingStarted() {
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
        this.mQsTracking = false;
    }

    public void onSwipingAborted() {
        this.mFalsingManager.onAffordanceSwipingAborted();
    }

    public KeyguardAffordanceView getLeftIcon() {
        return this.mKeyguardBottomArea.getLeftView();
    }

    public KeyguardAffordanceView getRightIcon() {
        return this.mKeyguardBottomArea.getRightView();
    }

    public boolean isInCenterScreen() {
        return this.mKeyguardMoveHelper.isInCenterScreen();
    }

    public MiuiKeyguardMoveLeftViewContainer getLeftView() {
        return this.mKeyguardLeftView;
    }

    public View getLeftViewBg() {
        return this.mLeftViewBg;
    }

    public View getRightView() {
        return this.mKeyguardRightView;
    }

    public boolean isKeyguardWallpaperCarouselSwitchAnimating() {
        return this.mLockScreenMagazineController.isSwitchAnimating();
    }

    public List<View> getLockScreenView() {
        return this.mMoveListViews;
    }

    public boolean needsAntiFalsing() {
        return this.mStatusBarState == 1;
    }

    public void startFaceUnlockByMove() {
        Log.d(TAG, "startFaceUnlockByMove");
        this.mUpdateMonitor.startFaceUnlock();
    }

    public void stopFaceUnlockByMove() {
        this.mUpdateMonitor.stopFaceUnlock();
    }

    public View getFaceUnlockView() {
        return this.mFaceUnlockView;
    }

    /* access modifiers changed from: protected */
    public float getPeekHeight() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return (float) this.mNotificationStackScroller.getPeekHeight();
        }
        return (float) this.mQsMinExpansionHeight;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUseDismissingAnimation() {
        return this.mStatusBarState != 0 && (!this.mStatusBar.isKeyguardCurrentlySecure() || !isTracking());
    }

    /* access modifiers changed from: protected */
    public boolean isTrackingBlocked() {
        return this.mConflictingQsExpansionGesture && this.mQsExpanded;
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQs != null && (this.mQs.isCustomizing() || this.mQs.isShowingDetail());
    }

    public void closeQsDetail() {
        this.mQs.closeDetail();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return this.mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return this.mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable r) {
        this.mLaunchAnimationEndRunnable = r;
    }

    public void setEmptyDragAmount(float amount) {
        float factor = 0.8f;
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            factor = 0.4f;
        } else if (!this.mStatusBar.hasActiveNotifications()) {
            factor = 0.4f;
        }
        this.mEmptyDragAmount = amount * factor;
        positionClockAndNotifications();
    }

    private static float interpolate(float t, float start, float end) {
        return ((1.0f - t) * start) + (t * end);
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (dozing != this.mDozing) {
            this.mDozing = dozing;
            if (this.mStatusBarState == 1) {
                updateDozingVisibilities(animate);
            }
        }
    }

    private void updateDozingVisibilities(boolean animate) {
        if (this.mDozing) {
            this.mKeyguardStatusBar.setVisibility(4);
            this.mKeyguardBottomArea.setDozing(this.mDozing, animate);
            return;
        }
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardBottomArea.setDozing(this.mDozing, animate);
        if (animate) {
            animateKeyguardStatusBarIn(700);
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void showEmptyShadeView(boolean emptyShadeViewVisible) {
        this.mShowEmptyShadeView = emptyShadeViewVisible;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        this.mNotificationStackScroller.updateEmptyShadeView(this.mShowEmptyShadeView && !this.mQsExpanded);
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onStartedWakingUp() {
        this.mIsInteractive = true;
        this.mKeyguardClockView.updateTimeAndBatteryInfo();
        this.mKeyguardMoveHelper.resetImmediately();
        onAwesomeLockScreenResume();
        registerSeneorsForKeyguard();
        startLiveLockWallpaper();
    }

    /* access modifiers changed from: private */
    public void startLiveLockWallpaper() {
        if (this.mLiveLockWallpaperPlayer != null && this.mLiveReady && this.mIsInteractive) {
            try {
                if (!this.mChargeHelper.isExtremePowerModeEnabled(this.mContext)) {
                    this.mLiveLockWallpaperPlayer.start();
                    this.mLiveLockWallpaperPlayer.setVolume(1.0f, 1.0f);
                    return;
                }
                MediaPlayerCompat.seekTo(this.mLiveLockWallpaperPlayer, 0, 3);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    public void onStartedGoingToSleep() {
        this.mIsInteractive = false;
        onAwesomeLockScreenPause();
        this.mUpdateMonitor.unregisterSeneorsForKeyguard();
    }

    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    /* access modifiers changed from: protected */
    public boolean onMiddleClicked() {
        switch (this.mStatusBar.getBarState()) {
            case 0:
                post(this.mPostCollapseRunnable);
                AnalyticsHelper.trackStatusBarCollapse("click_black_area");
                return false;
            case 1:
                if (!this.mDozingOnDown) {
                    this.mLockscreenGestureLogger.write(getContext(), 188, 0, 0);
                }
                return true;
            case 2:
                if (!this.mQsExpanded) {
                    this.mStatusBar.goToKeyguard();
                }
                return true;
            default:
                return true;
        }
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (this.mKeyguardShowing && child == this.mNotificationContainerParent) {
            float maskAlpha = 1.0f - Math.min(getKeyguardContentsAlpha(), 1.0f - getQsExpansionFraction());
            int wallpaperBlurColor = this.mUpdateMonitor.getWallpaperBlurColor();
            if (maskAlpha > 0.0f && wallpaperBlurColor != -1) {
                this.mMaskPaint.reset();
                this.mMaskPaint.setColor(wallpaperBlurColor);
                this.mMaskPaint.setAlpha((int) (255.0f * maskAlpha));
                canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), this.mMaskPaint);
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        this.mNotificationStackScroller.setInHeadsUpPinnedMode(inPinnedMode);
        if (inPinnedMode) {
            this.mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
            return;
        }
        setHeadsUpAnimatingAway(true);
        this.mNotificationStackScroller.runAfterAnimationFinished(this.mHeadsUpExistenceChangedRunnable);
    }

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        this.mHeadsUpAnimatingAway = headsUpAnimatingAway;
        this.mNotificationStackScroller.setHeadsUpAnimatingAway(headsUpAnimatingAway);
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mNotificationStackScroller.onHeadsUpPinned(headsUp);
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        this.mNotificationStackScroller.onHeadsUpUnPinned(headsUp);
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        this.mNotificationStackScroller.onHeadsUpStateChanged(entry, isHeadsUp);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        super.setHeadsUpManager(headsUpManager);
        this.mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManager, this.mNotificationStackScroller, this, this.mStatusBar);
    }

    public void setTrackingHeadsUp(boolean tracking) {
        if (tracking) {
            this.mNotificationStackScroller.setTrackingHeadsUp(true);
            this.mExpandingFromHeadsUp = true;
        }
    }

    /* access modifiers changed from: protected */
    public void onClosingFinished() {
        super.onClosingFinished();
        resetVerticalPanelPosition();
        setClosingWithAlphaFadeout(false);
        if (!(this.mQs == null || this.mQs.getQsContent() == null || this.mExpandedHeight != 0.0f)) {
            this.mQs.getQsContent().setScaleY(1.0f);
            this.mQs.getQsContent().setScaleX(1.0f);
            this.mQs.getQsContent().setAlpha(1.0f);
        }
        if (this.mNotificationStackScroller != null) {
            this.mNotificationStackScroller.setScaleX(1.0f);
            this.mNotificationStackScroller.setScaleY(1.0f);
        }
    }

    private void setClosingWithAlphaFadeout(boolean closing) {
        this.mClosingWithAlphaFadeOut = closing;
        this.mNotificationStackScroller.forceNoOverlappingRendering(closing);
    }

    private void resetVerticalPanelPosition() {
        setVerticalPanelTranslation(0.0f);
    }

    /* access modifiers changed from: protected */
    public void setVerticalPanelTranslation(float translation) {
        this.mNotificationStackScroller.setTranslationX(translation);
        this.mQsFrame.setTranslationX(translation);
    }

    /* access modifiers changed from: protected */
    public void updateExpandedHeight(float expandedHeight) {
        if (this.mTracking) {
            this.mNotificationStackScroller.setExpandingVelocity(getCurrentExpandVelocity());
        }
        this.mNotificationStackScroller.setExpandedHeight(expandedHeight);
        updateKeyguardBottomAreaAlpha();
        updateStatusBarIcons();
    }

    public boolean isFullWidth() {
        return this.mIsFullWidth;
    }

    private void updateStatusBarIcons() {
        boolean showIconsWhenExpanded = isFullWidth() && getExpandedHeight() < getOpeningHeight();
        if (showIconsWhenExpanded && this.mNoVisibleNotifications && isOnKeyguard()) {
            showIconsWhenExpanded = false;
        }
        if (showIconsWhenExpanded != this.mShowIconsWhenExpanded) {
            this.mShowIconsWhenExpanded = showIconsWhenExpanded;
            this.mStatusBar.recomputeDisableFlags(false);
        }
    }

    private boolean isOnKeyguard() {
        return this.mStatusBar.getBarState() == 1;
    }

    public void setPanelScrimMinFraction(float minFraction) {
        this.mBar.panelScrimMinFractionChanged(minFraction);
    }

    /* access modifiers changed from: protected */
    public boolean isPanelVisibleBecauseOfHeadsUp() {
        return this.mHeadsUpManager.hasPinnedHeadsUp() || this.mHeadsUpAnimatingAway;
    }

    public boolean hasOverlappingRendering() {
        return !this.mDozing;
    }

    public void launchCamera(boolean animate, int source) {
        boolean z = true;
        if (source == 1) {
            this.mLastCameraLaunchSource = "power_double_tap";
        } else if (source == 0) {
            this.mLastCameraLaunchSource = "wiggle_gesture";
        } else {
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        if (!isFullyCollapsed()) {
            this.mLaunchingAffordance = true;
            setLaunchingAffordance(true);
        } else {
            animate = false;
        }
        KeyguardMoveHelper keyguardMoveHelper = this.mKeyguardMoveHelper;
        if (getLayoutDirection() != 1) {
            z = false;
        }
        keyguardMoveHelper.launchAffordance(animate, z);
    }

    public void onAffordanceLaunchEnded() {
        this.mLaunchingAffordance = false;
        setLaunchingAffordance(false);
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        updateFullyVisibleState(false);
    }

    public void notifyStartFading() {
        updateFullyVisibleState(true);
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        updateFullyVisibleState(false);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        this.mHasWindowFocus = hasWindowFocus;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            updateGxzwState();
        }
    }

    private void updateFullyVisibleState(boolean forceNotFullyVisible) {
        this.mNotificationStackScroller.setParentNotFullyVisible((!forceNotFullyVisible && getAlpha() == 1.0f && getVisibility() == 0) ? false : true);
    }

    private void setLaunchingAffordance(boolean launchingAffordance) {
        getLeftIcon().setLaunchingAffordance(launchingAffordance);
        getRightIcon().setLaunchingAffordance(launchingAffordance);
    }

    public boolean canCameraGestureBeLaunched(boolean keyguardIsShowing) {
        ResolveInfo resolveInfo = this.mKeyguardBottomArea.resolveCameraIntent();
        String packageToLaunch = (resolveInfo == null || resolveInfo.activityInfo == null) ? null : resolveInfo.activityInfo.packageName;
        return packageToLaunch != null && (keyguardIsShowing || !isForegroundApp(packageToLaunch)) && !this.mKeyguardMoveHelper.isSwipingInProgress();
    }

    private boolean isForegroundApp(String pkgName) {
        return pkgName.equals(Util.getTopActivityPkg(getContext()));
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        boolean portrait = this.mContext.getResources().getConfiguration().orientation == 1;
        if (isFullWidth() && this.mShowIconsWhenExpanded) {
            return false;
        }
        if (!isPanelVisibleBecauseOfHeadsUp() || !Constants.IS_NOTCH || !portrait) {
            return true;
        }
        return false;
    }

    public void setTouchDisabled(boolean disabled) {
        super.setTouchDisabled(disabled);
        if (disabled && this.mKeyguardMoveHelper.isSwipingInProgress() && !this.mIsLaunchTransitionRunning) {
            this.mKeyguardMoveHelper.resetImmediately();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        Object[] objArr = new Object[14];
        objArr[0] = Float.valueOf(this.mQsExpansionHeight);
        objArr[1] = Integer.valueOf(this.mQsMinExpansionHeight);
        objArr[2] = Integer.valueOf(this.mQsMaxExpansionHeight);
        objArr[3] = this.mIntercepting ? "T" : "f";
        objArr[4] = this.mPanelExpanded ? "T" : "f";
        objArr[5] = this.mQsExpanded ? "T" : "f";
        objArr[6] = this.mQsFullyExpanded ? "T" : "f";
        objArr[7] = this.mKeyguardShowing ? "T" : "f";
        objArr[8] = this.mBlockTouches ? "T" : "f";
        objArr[9] = this.mOnlyAffordanceInThisMotion ? "T" : "f";
        objArr[10] = this.mHeadsUpTouchHelper.isTrackingHeadsUp() ? "T" : "f";
        objArr[11] = this.mConflictingQsExpansionGesture ? "T" : "f";
        objArr[12] = this.mIsExpansionFromHeadsUp ? "T" : "f";
        objArr[13] = this.mClockPositionAlgorithm.toString();
        pw.println(String.format("      [NotificationPanelView: mQsExpansionHeight=%f mQsMinExpansionHeight=%d mQsMaxExpansionHeight=%d mIntercepting=%s mPanelExpanded=%s mQsExpanded=%s mQsFullyExpanded=%s mKeyguardShowing=%s mBlockTouches=%s mOnlyAffordanceInThisMotion=%s isTrackingHeadsUp=%s mConflictingQsExpansionGesture=%s mIsExpansionFromHeadsUp=%s mClockPositionAlgorithm=%s]", objArr));
    }

    public void setDark(boolean dark, boolean animate) {
        float darkAmount = dark ? 1.0f : 0.0f;
        if (this.mDarkAmount != darkAmount) {
            if (this.mDarkAnimator != null && this.mDarkAnimator.isRunning()) {
                this.mDarkAnimator.cancel();
            }
            if (animate) {
                this.mDarkAnimator = ObjectAnimator.ofFloat(this, SET_DARK_AMOUNT_PROPERTY, new float[]{darkAmount});
                this.mDarkAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
                this.mDarkAnimator.setDuration(200);
                this.mDarkAnimator.start();
            } else {
                setDarkAmount(darkAmount);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setDarkAmount(float amount) {
        this.mDarkAmount = amount;
        positionClockAndNotifications();
    }

    public void setNoVisibleNotifications(boolean noNotifications) {
        this.mNoVisibleNotifications = noNotifications;
        if (this.mQs != null) {
            this.mQs.setHasNotifications(!noNotifications);
        }
    }

    public boolean isNoVisibleNotifications() {
        return this.mNoVisibleNotifications;
    }

    public void setDismissView(DismissView dismissView) {
        int index = -1;
        if (this.mDismissView != null) {
            index = this.mNotificationContainerParent.indexOfChild(this.mDismissView);
            this.mNotificationContainerParent.removeView(this.mDismissView);
        }
        this.mDismissView = dismissView;
        this.mNotificationContainerParent.addView(this.mDismissView, index);
    }

    public void updateDismissView(boolean visible) {
        int i;
        boolean hasExtraRange = true;
        int i2 = 0;
        if ((this.mDismissView.getVisibility() == 0) != visible) {
            this.mDismissView.setVisibility(visible ? 0 : 4);
        }
        boolean landscape = getResources().getConfiguration().orientation == 2;
        if (!visible || landscape) {
            hasExtraRange = false;
        }
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (hasExtraRange) {
            i = this.mDismissViewBottomMargin;
        } else {
            i = 0;
        }
        if (hasExtraRange) {
            i2 = this.mDismissViewSize + (this.mDismissViewBottomMargin * 2);
        }
        notificationStackScrollLayout.setExtraBottomRange(i, i2);
    }

    public void setForceBlack(boolean forceBlack) {
        this.mForceBlack = forceBlack;
        updateNotchCornerVisibility();
        this.mKeyguardStatusBar.setDarkMode(!this.mForceBlack && this.mUpdateMonitor.isLightWallpaperStatusBar());
    }

    public void setKeyguardWallpaperCarouselController(LockScreenMagazineController lockScreenMagazineController) {
        this.mLockScreenMagazineController = lockScreenMagazineController;
        this.mMoveListViews.add(this.mLockScreenMagazineController.getWallPaperDes());
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mKeyguardIndicationController = keyguardIndicationController;
    }

    public void onKeyguardOccludedChanged(boolean keyguardOccluded) {
        this.mKeyguardOccluded = keyguardOccluded;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            updateGxzwState();
        }
    }

    public void updateGxzwState() {
        boolean canShow;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            boolean z = false;
            if (Dependency.getHost() == null || !Dependency.getHost().isDozing()) {
                boolean moveHelperCanShow = this.mKeyguardMoveHelper.canShowGxzw();
                boolean bottomAreaCanShow = this.mKeyguardBottomArea.canShowGxzw();
                boolean bouncer = MiuiGxzwManager.getInstance().isBouncer();
                boolean ignoreFocusChange = MiuiGxzwManager.getInstance().isIgnoreFocusChange();
                boolean isShowingChargeAnimationWindow = MiuiGxzwManager.getInstance().isShowingChargeAnimationWindow();
                boolean isShowFodInBouncer = MiuiGxzwManager.getInstance().isShowFodInBouncer();
                if (bouncer) {
                    canShow = isShowFodInBouncer && (this.mHasWindowFocus || ignoreFocusChange) && !isShowingChargeAnimationWindow;
                } else {
                    canShow = (!this.mQsExpanded && !this.mKeyguardOccluded && moveHelperCanShow && bottomAreaCanShow && !this.mLockScreenMagazinePreViewVisible && !isShowingChargeAnimationWindow) && (this.mHasWindowFocus || ignoreFocusChange);
                }
                if (MiuiGxzwManager.getInstance().isShouldShowGxzwIconInKeyguard() != canShow) {
                    Log.i(TAG, "updateGxzwState: mQsExpanded = " + this.mQsExpanded + ", mKeyguardOccluded = " + this.mKeyguardOccluded + ", moveHelperCanShow = " + moveHelperCanShow + ", bottomAreaCanShow = " + bottomAreaCanShow + ", bouncer = " + bouncer + ", mHasWindowFocus = " + this.mHasWindowFocus + ", ignoreFocusChange = " + ignoreFocusChange + ", mLockScreenMagazinePreViewVisible = " + this.mLockScreenMagazinePreViewVisible + ", isShowFodInBouncer = " + isShowFodInBouncer + ", isShowingChargeAnimationWindow = " + isShowingChargeAnimationWindow);
                }
                MiuiGxzwManager instance = MiuiGxzwManager.getInstance();
                if (!canShow) {
                    z = true;
                }
                instance.dismissGxzwIconView(z);
            } else {
                if (!MiuiGxzwManager.getInstance().isShouldShowGxzwIconInKeyguard()) {
                    Log.i(TAG, "updateGxzwState: dozing");
                }
                MiuiGxzwManager.getInstance().dismissGxzwIconView(false);
            }
        }
    }

    public void registerSeneorsForKeyguard() {
        this.mUpdateMonitor.registerSeneorsForKeyguard(this.mSensorsChangeCallback);
    }

    public void inflateLeftView() {
        if (this.mKeyguardLeftView != null) {
            this.mKeyguardLeftView.inflateLeftView();
        }
    }

    public void onBlurRatioChanged(float blurRatio) {
        this.mThemeBackgroundView.setAlpha(blurRatio);
    }
}
