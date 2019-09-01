package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.app.admin.DevicePolicyManagerCompat;
import android.app.trust.TrustManager;
import android.app.trust.TrustManagerCompat;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.preference.PreferenceManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardStateCallbackCompat;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.keyguard.BoostFrameworkHelper;
import com.android.keyguard.KeyguardCompatibilityHelperForO;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.LatencyTracker;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.faceunlock.FaceUnlockController;
import com.android.keyguard.faceunlock.FaceUnlockManager;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.smartcover.SmartCoverHelper;
import com.android.keyguard.wallpaper.KeyguardWallpaperHelper;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.events.ScreenOnEvent;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.statusbar.phone.FingerprintUnlockController;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.util.Utils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import miui.security.SecurityManager;

public class KeyguardViewMediator extends SystemUI {
    public static final int STRONG_AUTH_REQUIRED_AFTER_TIMEOUT = (Build.VERSION.SDK_INT > 25 ? 16 : 256);
    /* access modifiers changed from: private */
    public static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(606076928);
    public static long sScreenOnDelay;
    public final int OFF_BECAUSE_OF_ADMIN = 1;
    public final int OFF_BECAUSE_OF_TIMEOUT = 3;
    public final int OFF_BECAUSE_OF_USER = 2;
    private AlarmManager mAlarmManager;
    /* access modifiers changed from: private */
    public AudioManager mAudioManager;
    /* access modifiers changed from: private */
    public MiuiBleUnlockHelper mBleUnlockHelper;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(intent.getAction())) {
                int sequence = intent.getIntExtra("seq", 0);
                Log.d("KeyguardViewMediator", "received DELAYED_KEYGUARD_ACTION with seq = " + sequence + ", mDelayedShowingSequence = " + KeyguardViewMediator.this.mDelayedShowingSequence);
                synchronized (KeyguardViewMediator.this) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == sequence) {
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
            } else if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK".equals(intent.getAction())) {
                int sequence2 = intent.getIntExtra("seq", 0);
                int userId = intent.getIntExtra("android.intent.extra.USER_ID", 0);
                if (userId != 0) {
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.mDelayedProfileShowingSequence == sequence2) {
                            KeyguardViewMediator.this.lockProfile(userId);
                        }
                    }
                }
            } else if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                synchronized (KeyguardViewMediator.this) {
                    boolean unused = KeyguardViewMediator.this.mShuttingDown = true;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public int mDelayedProfileShowingSequence;
    /* access modifiers changed from: private */
    public int mDelayedShowingSequence;
    /* access modifiers changed from: private */
    public boolean mDeviceInteractive;
    private final DismissCallbackRegistry mDismissCallbackRegistry = new DismissCallbackRegistry();
    /* access modifiers changed from: private */
    public Display mDisplay;
    private IKeyguardDrawnCallback mDrawnCallback;
    private IKeyguardExitCallback mExitSecureCallback;
    /* access modifiers changed from: private */
    public boolean mExternallyEnabled = true;
    private FaceUnlockController mFaceUnlockController;
    /* access modifiers changed from: private */
    public FaceUnlockManager mFaceUnlockManager;
    private FingerprintUnlockController mFingerprintUnlockController;
    private long mFpAuthTime = 0;
    ContentObserver mFullScreenGestureObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            boolean unused = KeyguardViewMediator.this.mIsFullScreenGestureOpened = MiuiSettings.Global.getBoolean(KeyguardViewMediator.this.mContext.getContentResolver(), "force_fsg_nav_bar");
            MiuiKeyguardUtils.setIsFullScreenGestureOpened(KeyguardViewMediator.this.mIsFullScreenGestureOpened);
        }
    };
    private volatile boolean mGoingToSleep;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1:
                    KeyguardViewMediator.this.handleShow((Bundle) msg.obj);
                    return;
                case 2:
                    KeyguardViewMediator.this.handleHide();
                    return;
                case 3:
                    KeyguardViewMediator.this.handleReset();
                    return;
                case 4:
                    Trace.beginSection("KeyguardViewMediator#handleMessage VERIFY_UNLOCK");
                    KeyguardViewMediator.this.handleVerifyUnlock();
                    Trace.endSection();
                    return;
                case 5:
                    KeyguardViewMediator.this.handleNotifyFinishedGoingToSleep();
                    return;
                case 6:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNING_ON");
                    KeyguardViewMediator.this.handleNotifyScreenTurningOn((IKeyguardDrawnCallback) msg.obj);
                    Trace.endSection();
                    return;
                case 7:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE");
                    KeyguardViewMediator.this.handleKeyguardDone();
                    Trace.endSection();
                    return;
                case 8:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_DRAWING");
                    KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                    Trace.endSection();
                    return;
                case 9:
                    Trace.beginSection("KeyguardViewMediator#handleMessage SET_OCCLUDED");
                    KeyguardViewMediator keyguardViewMediator = KeyguardViewMediator.this;
                    boolean z2 = msg.arg1 != 0;
                    if (msg.arg2 != 0) {
                        z = true;
                    }
                    keyguardViewMediator.handleSetOccluded(z2, z);
                    Trace.endSection();
                    return;
                case 10:
                    synchronized (KeyguardViewMediator.this) {
                        KeyguardViewMediator.this.doKeyguardLocked((Bundle) msg.obj);
                    }
                    return;
                case 11:
                    KeyguardViewMediator.this.handleDismiss((IKeyguardDismissCallback) msg.obj);
                    return;
                case 12:
                    Trace.beginSection("KeyguardViewMediator#handleMessage START_KEYGUARD_EXIT_ANIM");
                    StartKeyguardExitAnimParams params = (StartKeyguardExitAnimParams) msg.obj;
                    KeyguardViewMediator.this.handleStartKeyguardExitAnimation(params.startTime, params.fadeoutDuration);
                    FalsingManager.getInstance(KeyguardViewMediator.this.mContext).onSucccessfulUnlock();
                    Trace.endSection();
                    return;
                case 13:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_PENDING_TIMEOUT");
                    Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                    KeyguardViewMediator.this.mViewMediatorCallback.readyForKeyguardDone();
                    Trace.endSection();
                    return;
                case 14:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_STARTED_WAKING_UP");
                    KeyguardViewMediator.this.handleNotifyStartedWakingUp();
                    Trace.endSection();
                    return;
                case 15:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNED_ON");
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOn();
                    Trace.endSection();
                    return;
                case 16:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOff();
                    return;
                case 17:
                    KeyguardViewMediator.this.handleNotifyStartedGoingToSleep();
                    return;
                case 18:
                    Trace.beginSection("KeyguardViewMediator#handleMessage SET_SWITCHING_USER");
                    KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardViewMediator.this.mContext);
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    instance.setSwitchingUser(z);
                    Trace.endSection();
                    return;
                case 19:
                    Slog.w("KeyguardViewMediator", "fw call startKeyguardExitAnimation timeout");
                    KeyguardViewMediator.this.startKeyguardExitAnimation(SystemClock.uptimeMillis(), 0);
                    return;
                default:
                    return;
            }
        }
    };
    private Animation mHideAnimation;
    /* access modifiers changed from: private */
    public final Runnable mHideAnimationFinishedRunnable = new Runnable() {
        public void run() {
            boolean unused = KeyguardViewMediator.this.mHideAnimationRunning = false;
            KeyguardViewMediator.this.tryKeyguardDone();
        }
    };
    /* access modifiers changed from: private */
    public boolean mHideAnimationRun = false;
    /* access modifiers changed from: private */
    public boolean mHideAnimationRunning = false;
    private boolean mHideLockForLid;
    private boolean mHiding;
    private Runnable mInitAllRunnable = new Runnable() {
        public void run() {
            if (KeyguardViewMediator.this.mFaceUnlockManager.needInitFaceUnlock() && KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot()) {
                KeyguardViewMediator.this.mFaceUnlockManager.initAll();
                if (!KeyguardViewMediator.this.mFaceUnlockManager.isValidFeature()) {
                    Slog.e("face_unlock", "face data is valid");
                    Settings.Secure.putIntForUser(KeyguardViewMediator.this.mContext.getContentResolver(), "face_unlock_valid_feature", 0, KeyguardUpdateMonitor.getCurrentUser());
                    KeyguardViewMediator.this.sendReEntryFaceUnlockNotification();
                }
            }
        }
    };
    private boolean mInputRestricted;
    /* access modifiers changed from: private */
    public boolean mIsFullScreenGestureOpened = false;
    /* access modifiers changed from: private */
    public KeyguardDisplayManager mKeyguardDisplayManager;
    /* access modifiers changed from: private */
    public boolean mKeyguardDonePending = false;
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        public void run() {
            Trace.beginSection("KeyguardViewMediator.mKeyGuardGoingAwayRunnable");
            Log.d("KeyguardViewMediator", "keyguardGoingAway");
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.keyguardGoingAway();
            KeyguardViewMediator.this.mUpdateMonitor.setKeyguardGoingAway(true);
            KeyguardViewMediator.this.mHandler.removeMessages(19);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(19, 1000);
            synchronized (KeyguardViewMediator.this) {
                long unused = KeyguardViewMediator.this.mKeyguardGoingAwayTime = System.currentTimeMillis();
            }
            if (!MiuiKeyguardUtils.isGxzwSensor() || !MiuiGxzwManager.getInstance().isFodFastUnlock()) {
                KeyguardViewMediator.this.keyguardGoingAway();
            } else {
                KeyguardViewMediator.this.startKeyguardExitAnimation(SystemClock.uptimeMillis(), 0);
            }
            Trace.endSection();
        }
    };
    /* access modifiers changed from: private */
    public long mKeyguardGoingAwayTime = 0;
    private final ArrayList<IKeyguardStateCallbackCompat> mKeyguardStateCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public Sensor mLargeAreaTouchSensor = null;
    /* access modifiers changed from: private */
    public SensorEventListener mLargeAreaTouchSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (event.values != null && event.values[0] == 1.0f && KeyguardViewMediator.this.mShowing) {
                Slog.i("KeyguardViewMediator", "keyguard_screen_off_reason:large area touch");
                KeyguardViewMediator.this.mPM.goToSleep(SystemClock.uptimeMillis());
            }
        }

        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private boolean mLockLater;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    /* access modifiers changed from: private */
    public int mLockSoundStreamId;
    /* access modifiers changed from: private */
    public float mLockSoundVolume;
    /* access modifiers changed from: private */
    public SoundPool mLockSounds;
    /* access modifiers changed from: private */
    public boolean mLockWhenSimRemoved;
    private Runnable mMigrateFilesRunnable = new Runnable() {
        public void run() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(KeyguardViewMediator.this.mContext);
            if (!sp.getBoolean("pref_key_has_migrate_files", false)) {
                sp.edit().putBoolean("pref_key_has_migrate_files", KeyguardViewMediator.this.migrateKeyguardFiles()).commit();
            }
        }
    };
    private boolean mNeedToReshowWhenReenabled = false;
    /* access modifiers changed from: private */
    public boolean mOccluded = false;
    /* access modifiers changed from: private */
    public PowerManager mPM;
    private boolean mPendingLock;
    private boolean mPendingReset;
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    /* access modifiers changed from: private */
    public boolean mReadyForKeyEvent = false;
    private boolean mScreeningOn;
    /* access modifiers changed from: private */
    public boolean mSendKeyEventScreenOn = false;
    /* access modifiers changed from: private */
    public SensorManager mSensorManager;
    private PowerManager.WakeLock mShowKeyguardWakeLock;
    private final BroadcastReceiver mShowUnlockScreenReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("xiaomi.intent.action.SHOW_SECURE_KEYGUARD".equals(intent.getAction()) && KeyguardViewMediator.this.mShowing) {
                final boolean resetLockScreen = false;
                if (intent.getBooleanExtra("fp_unlock_priority", false) && MiuiKeyguardUtils.isGxzwSensor() && !KeyguardViewMediator.this.mOccluded && !MiuiGxzwManager.getInstance().supportFodInBouncer() && KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser()) && KeyguardViewMediator.this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(KeyguardUpdateMonitor.getCurrentUser())) {
                    resetLockScreen = true;
                }
                KeyguardViewMediator.this.mHandler.post(new Runnable() {
                    public void run() {
                        if (resetLockScreen) {
                            KeyguardViewMediator.this.resetStateLocked();
                        } else {
                            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.dismiss();
                        }
                    }
                });
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mShowing;
    /* access modifiers changed from: private */
    public boolean mShuttingDown;
    private boolean mSimLockedOrMissing;
    private SmartCoverHelper mSmartCoverHelper;
    private boolean mStartedWakingUp;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSystemReady;
    /* access modifiers changed from: private */
    public TrustManager mTrustManager;
    private int mTrustedSoundId;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    /* access modifiers changed from: private */
    public int mUiSoundsStreamType;
    /* access modifiers changed from: private */
    public boolean mUnlockByFace = false;
    /* access modifiers changed from: private */
    public boolean mUnlockByFingerPrint = false;
    private int mUnlockSoundId;
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitching(int userId) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                KeyguardViewMediator.this.resetStateLocked();
                KeyguardViewMediator.this.adjustStatusBarLocked();
            }
        }

        public void onUserSwitchComplete(int userId) {
            if (userId != 0) {
                UserInfo info = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(userId);
                if (info == null) {
                    return;
                }
                if (info.isGuest() || (info.flags & 512) == 512) {
                    KeyguardViewMediator.this.dismiss(null);
                }
            }
        }

        public void onUserInfoChanged(int userId) {
        }

        public void onPhoneStateChanged(int phoneState) {
            synchronized (KeyguardViewMediator.this) {
                if (phoneState == 0) {
                    try {
                        if (!KeyguardViewMediator.this.mDeviceInteractive && KeyguardViewMediator.this.mExternallyEnabled) {
                            Log.d("KeyguardViewMediator", "screen is off and call ended, let's make sure the keyguard is showing");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }

        public void onClockVisibilityChanged() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        public void onDeviceProvisioned() {
            KeyguardViewMediator.this.sendUserPresentBroadcast();
            synchronized (KeyguardViewMediator.this) {
                if (KeyguardViewMediator.this.mustNotUnlockCurrentUser()) {
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
            Log.d("KeyguardViewMediator", "onSimStateChanged(subId=" + subId + ", slotId=" + slotId + ",state=" + simState + ")");
            KeyguardViewMediator.this.handleSimSecureStateChanged();
            switch (AnonymousClass29.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[simState.ordinal()]) {
                case 1:
                case 2:
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else {
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                        if (simState == IccCardConstants.State.ABSENT && (slotId != MiuiSettings.VirtualSim.getVirtualSimSlotId(KeyguardViewMediator.this.mContext) || 2 == MiuiSettings.VirtualSim.getVirtualSimStatus(KeyguardViewMediator.this.mContext))) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                onSimAbsentLocked();
                            } else {
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                    }
                    break;
                case 3:
                case 4:
                    synchronized (KeyguardViewMediator.this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            KeyguardViewMediator.this.mUpdateMonitor.stopFaceUnlock();
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                    }
                    break;
                case 5:
                    synchronized (KeyguardViewMediator.this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        onSimAbsentLocked();
                    }
                    break;
                case 6:
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.mShowing) {
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        boolean unused = KeyguardViewMediator.this.mLockWhenSimRemoved = true;
                    }
                    break;
                default:
                    Log.v("KeyguardViewMediator", "Unspecific state: " + simState);
                    break;
            }
            if (simState != IccCardConstants.State.READY) {
                KeyguardViewMediator.this.mUpdateMonitor.setSimStateEarlyReady(slotId, false);
            }
        }

        private void onSimAbsentLocked() {
            if (KeyguardViewMediator.this.isSecure() && KeyguardViewMediator.this.mLockWhenSimRemoved && !KeyguardViewMediator.this.mShuttingDown) {
                boolean unused = KeyguardViewMediator.this.mLockWhenSimRemoved = false;
                MetricsLogger.action(KeyguardViewMediator.this.mContext, 496, KeyguardViewMediator.this.mShowing);
                if (!KeyguardViewMediator.this.mShowing) {
                    Log.i("KeyguardViewMediator", "SIM removed, showing keyguard");
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        public void onFingerprintAuthFailed() {
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(currentUser)) {
                DevicePolicyManagerCompat.reportFailedFingerprintAttempt(KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager(), currentUser);
            }
        }

        public void onFingerprintAuthenticated(int userId) {
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(userId)) {
                DevicePolicyManagerCompat.reportSuccessfulFingerprintAttempt(KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager(), userId);
                boolean unused = KeyguardViewMediator.this.mUnlockByFingerPrint = true;
            }
        }

        public void onHasLockscreenWallpaperChanged(boolean hasLockscreenWallpaper) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.notifyHasLockscreenWallpaperChanged(hasLockscreenWallpaper);
            }
        }

        public void onKeyguardBouncerChanged(boolean bouncer) {
            if (KeyguardViewMediator.this.isShowingAndNotOccluded()) {
                KeyguardViewMediator.this.disableFullScreenGesture(!bouncer);
            }
        }

        public void onFaceAuthenticated() {
            boolean unused = KeyguardViewMediator.this.mUnlockByFace = true;
        }
    };
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        public void keyguardDone(boolean strongAuth, int targetUserId) {
            if (targetUserId == ActivityManager.getCurrentUser()) {
                KeyguardViewMediator.this.tryKeyguardDone();
                if (strongAuth) {
                    KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
                }
            }
        }

        public void keyguardDoneDrawing() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDoneDrawing");
            KeyguardViewMediator.this.mHandler.sendEmptyMessage(8);
            Trace.endSection();
        }

        public void setNeedsInput(boolean needsInput) {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.setNeedsInput(needsInput);
        }

        public void keyguardDonePending(boolean strongAuth, int targetUserId) {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDonePending");
            if (targetUserId != ActivityManager.getCurrentUser()) {
                Trace.endSection();
                return;
            }
            boolean unused = KeyguardViewMediator.this.mKeyguardDonePending = true;
            boolean unused2 = KeyguardViewMediator.this.mHideAnimationRun = true;
            boolean unused3 = KeyguardViewMediator.this.mHideAnimationRunning = true;
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(KeyguardViewMediator.this.mHideAnimationFinishedRunnable);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(13, 1000);
            if (strongAuth) {
                KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
            }
            Trace.endSection();
        }

        public void keyguardGone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardGone");
            KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
            Trace.endSection();
        }

        public void readyForKeyguardDone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#readyForKeyguardDone");
            if (KeyguardViewMediator.this.mKeyguardDonePending) {
                boolean unused = KeyguardViewMediator.this.mKeyguardDonePending = false;
                KeyguardViewMediator.this.tryKeyguardDone();
            }
            if (MiuiKeyguardUtils.isScreenTurnOnDelayed()) {
                KeyguardViewMediator.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        Log.d("KeyguardViewMediator", "unblock screen on when screen turned on delayed");
                        KeyguardViewMediator.this.unblockScreenOn();
                        MiuiKeyguardUtils.setScreenTurnOnDelayed(false);
                    }
                }, Math.max(0, 50 - (System.currentTimeMillis() - FaceUnlockManager.sStageInFaceUnlockTime)));
            }
            Trace.endSection();
        }

        public void resetKeyguard() {
            KeyguardViewMediator.this.resetStateLocked();
        }

        public void onBouncerVisiblityChanged(boolean shown) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.adjustStatusBarLocked(shown);
            }
        }

        public int getBouncerPromptReason() {
            int currentUser = ActivityManager.getCurrentUser();
            boolean trust = TrustManagerCompat.isTrustUsuallyManaged(KeyguardViewMediator.this.mTrustManager, currentUser);
            boolean fingerprint = KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(currentUser);
            boolean faceUnlock = KeyguardViewMediator.this.mUpdateMonitor.shouldListenForFaceUnlock();
            boolean bleUnlock = KeyguardViewMediator.this.mBleUnlockHelper.isUnlockWithBlePossible();
            boolean any = trust || fingerprint || faceUnlock || bleUnlock;
            KeyguardUpdateMonitor.StrongAuthTracker strongAuthTracker = KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker();
            int strongAuth = strongAuthTracker.getStrongAuthForUser(currentUser);
            Log.i("KeyguardViewMediator", "getBouncerPromptReason trust = " + trust + " fingerprint = " + fingerprint + " faceUnlock = " + faceUnlock + " bleUnlock = " + bleUnlock + " strongAuth = " + strongAuth);
            if (any && !strongAuthTracker.hasUserAuthenticatedSinceBoot()) {
                return 1;
            }
            if (any && (KeyguardViewMediator.STRONG_AUTH_REQUIRED_AFTER_TIMEOUT & strongAuth) != 0) {
                return 2;
            }
            if (any && (strongAuth & 2) != 0) {
                return 3;
            }
            if (trust && (strongAuth & 4) != 0) {
                return 4;
            }
            if (!any || (strongAuth & 8) == 0) {
                return 0;
            }
            return 5;
        }
    };
    private long mWaitFwTotalTime = 0;
    private boolean mWaitingUntilKeyguardVisible = false;
    private boolean mWakeAndUnlocking;
    /* access modifiers changed from: private */
    public Sensor mWakeupAndSleepSensor = null;
    /* access modifiers changed from: private */
    public SensorEventListener mWakeupAndSleepSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (event.values != null && event.values[0] == 1.0f && KeyguardViewMediator.this.mDisplay.getState() != 2) {
                AnalyticsHelper.record("keyguard_screenon_by_pick_up");
                KeyguardViewMediator.this.mPM.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:PICK_UP");
                boolean unused = KeyguardViewMediator.this.mWakeupByPickUp = true;
            } else if (event.values == null) {
            } else {
                if ((event.values[0] == 2.0f || event.values[0] == 0.0f) && KeyguardViewMediator.this.mWakeupByPickUp && KeyguardViewMediator.this.isShowingAndNotOccluded() && KeyguardViewMediator.this.mDisplay.getState() == 2) {
                    AnalyticsHelper.record("keyguard_sleep_by_put_down");
                    Slog.i("KeyguardViewMediator", "keyguard_screen_off_reason:put down");
                    KeyguardViewMediator.this.mPM.goToSleep(SystemClock.uptimeMillis());
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    /* access modifiers changed from: private */
    public boolean mWakeupByPickUp = false;
    private KeyguardWallpaperHelper mWallpaperHelper;
    private WorkLockActivityController mWorkLockController;

    /* renamed from: com.android.systemui.keyguard.KeyguardViewMediator$29  reason: invalid class name */
    static /* synthetic */ class AnonymousClass29 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NOT_READY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.ABSENT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PERM_DISABLED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        private StartKeyguardExitAnimParams(long startTime2, long fadeoutDuration2) {
            this.startTime = startTime2;
            this.fadeoutDuration = fadeoutDuration2;
        }
    }

    public void handleSimSecureStateChanged() {
        int size = this.mKeyguardStateCallbacks.size();
        boolean simPinSecure = this.mUpdateMonitor.isSimPinSecure();
        for (int i = size - 1; i >= 0; i--) {
            try {
                this.mKeyguardStateCallbacks.get(i).onSimSecureStateChanged(simPinSecure);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(i);
                }
            }
        }
    }

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    /* access modifiers changed from: package-private */
    public boolean mustNotUnlockCurrentUser() {
        return (UserManagerCompat.isSplitSystemUser() || com.android.systemui.proxy.UserManager.isDeviceInDemoMode(this.mContext)) && KeyguardUpdateMonitor.getCurrentUser() == 0;
    }

    private void setupLocked() {
        this.mPM = (PowerManager) this.mContext.getSystemService("power");
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(this.mContext);
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        this.mShowKeyguardWakeLock = this.mPM.newWakeLock(1, "show keyguard");
        this.mShowKeyguardWakeLock.setReferenceCounted(false);
        this.mBleUnlockHelper = new MiuiBleUnlockHelper(this.mContext, this);
        this.mSmartCoverHelper = new SmartCoverHelper(this.mContext, this);
        this.mWallpaperHelper = new KeyguardWallpaperHelper(this.mContext);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        filter.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mContext.registerReceiverAsUser(this.mShowUnlockScreenReceiver, UserHandle.ALL, new IntentFilter("xiaomi.intent.action.SHOW_SECURE_KEYGUARD"), null, null);
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUpdateMonitor.setKeyguardViewMediator(this);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        KeyguardUpdateMonitor.setCurrentUser(ActivityManager.getCurrentUser());
        if (this.mContext.getResources().getBoolean(R.bool.config_enableKeyguardService)) {
            setShowingLocked(!shouldWaitForProvisioning() && !this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()), true);
        }
        this.mStatusBarKeyguardViewManager = SystemUIFactory.getInstance().createStatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver cr = this.mContext.getContentResolver();
        this.mDeviceInteractive = this.mPM.isInteractive();
        this.mLockSounds = new SoundPool(1, 1, 0);
        String soundPath = Settings.Global.getString(cr, "lock_sound");
        if (soundPath != null) {
            this.mLockSoundId = this.mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || this.mLockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load lock sound from " + soundPath);
        }
        String soundPath2 = Settings.Global.getString(cr, "unlock_sound");
        if (soundPath2 != null) {
            this.mUnlockSoundId = this.mLockSounds.load(soundPath2, 1);
        }
        if (soundPath2 == null || this.mUnlockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load unlock sound from " + soundPath2);
        }
        String soundPath3 = Settings.Global.getString(cr, "trusted_sound");
        if (soundPath3 != null) {
            this.mTrustedSoundId = this.mLockSounds.load(soundPath3, 1);
        }
        if (soundPath3 == null || this.mTrustedSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load trusted sound from " + soundPath3);
        }
        this.mLockSoundVolume = (float) Math.pow(10.0d, (double) (((float) this.mContext.getResources().getInteger(17694799)) / 20.0f));
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, 17432664);
        this.mWorkLockController = new WorkLockActivityController(this.mContext);
        BoostFrameworkHelper.initBoostFramework();
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.init(this.mContext);
            MiuiGxzwManager.getInstance().setKeyguardViewMediator(this);
        }
        if (MiuiKeyguardUtils.hasNavigationBar()) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, this.mFullScreenGestureObserver);
            this.mFullScreenGestureObserver.onChange(false);
        }
        if (this.mUpdateMonitor.shouldListenForFaceUnlock()) {
            this.mFaceUnlockManager.runOnFaceUnlockWorkerThread(this.mMigrateFilesRunnable);
        }
    }

    public void start() {
        synchronized (this) {
            setupLocked();
        }
        putComponent(KeyguardViewMediator.class, this);
    }

    public void onSystemReady() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "onSystemReady");
            this.mSystemReady = true;
            doKeyguardLocked(null);
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
        }
        maybeSendUserPresentBroadcast();
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0062 A[Catch:{ RemoteException -> 0x006f }] */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0082 A[Catch:{ RemoteException -> 0x006f }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onStartedGoingToSleep(int r12) {
        /*
            r11 = this;
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "onStartedGoingToSleep("
            r1.append(r2)
            r1.append(r12)
            java.lang.String r2 = ")"
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.d(r0, r1)
            com.android.systemui.statusbar.phone.FingerprintUnlockController r0 = r11.mFingerprintUnlockController
            r0.resetCancelingPendingLock()
            com.android.keyguard.faceunlock.FaceUnlockController r0 = r11.mFaceUnlockController
            r0.resetFaceUnlockMode()
            monitor-enter(r11)
            r0 = 0
            r11.mDeviceInteractive = r0     // Catch:{ all -> 0x00d3 }
            r1 = 1
            r11.mGoingToSleep = r1     // Catch:{ all -> 0x00d3 }
            r11.mWakeupByPickUp = r0     // Catch:{ all -> 0x00d3 }
            r11.mUnlockByFingerPrint = r0     // Catch:{ all -> 0x00d3 }
            r11.mUnlockByFace = r0     // Catch:{ all -> 0x00d3 }
            r11.mReadyForKeyEvent = r0     // Catch:{ all -> 0x00d3 }
            r11.mSendKeyEventScreenOn = r0     // Catch:{ all -> 0x00d3 }
            r11.resetFingerprintUnlockState()     // Catch:{ all -> 0x00d3 }
            int r2 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()     // Catch:{ all -> 0x00d3 }
            com.android.internal.widget.LockPatternUtils r3 = r11.mLockPatternUtils     // Catch:{ all -> 0x00d3 }
            boolean r3 = r3.getPowerButtonInstantlyLocks(r2)     // Catch:{ all -> 0x00d3 }
            if (r3 != 0) goto L_0x0050
            com.android.internal.widget.LockPatternUtils r3 = r11.mLockPatternUtils     // Catch:{ all -> 0x00d3 }
            boolean r3 = r3.isSecure(r2)     // Catch:{ all -> 0x00d3 }
            if (r3 != 0) goto L_0x004e
            goto L_0x0050
        L_0x004e:
            r3 = r0
            goto L_0x0051
        L_0x0050:
            r3 = r1
        L_0x0051:
            int r4 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()     // Catch:{ all -> 0x00d3 }
            long r4 = r11.getLockTimeout(r4)     // Catch:{ all -> 0x00d3 }
            r11.mLockLater = r0     // Catch:{ all -> 0x00d3 }
            com.android.internal.policy.IKeyguardExitCallback r6 = r11.mExitSecureCallback     // Catch:{ all -> 0x00d3 }
            r7 = 0
            r9 = 3
            if (r6 == 0) goto L_0x0082
            java.lang.String r6 = "KeyguardViewMediator"
            java.lang.String r10 = "pending exit secure callback cancelled"
            android.util.Log.d(r6, r10)     // Catch:{ all -> 0x00d3 }
            com.android.internal.policy.IKeyguardExitCallback r6 = r11.mExitSecureCallback     // Catch:{ RemoteException -> 0x006f }
            r6.onKeyguardExitResult(r0)     // Catch:{ RemoteException -> 0x006f }
            goto L_0x0077
        L_0x006f:
            r0 = move-exception
            java.lang.String r6 = "KeyguardViewMediator"
            java.lang.String r10 = "Failed to call onKeyguardExitResult(false)"
            android.util.Slog.w(r6, r10, r0)     // Catch:{ all -> 0x00d3 }
        L_0x0077:
            r0 = 0
            r11.mExitSecureCallback = r0     // Catch:{ all -> 0x00d3 }
            boolean r0 = r11.mExternallyEnabled     // Catch:{ all -> 0x00d3 }
            if (r0 != 0) goto L_0x00ae
            r11.hideLocked()     // Catch:{ all -> 0x00d3 }
            goto L_0x00ae
        L_0x0082:
            boolean r0 = r11.mShowing     // Catch:{ all -> 0x00d3 }
            if (r0 == 0) goto L_0x0093
            boolean r0 = r11.mHiding     // Catch:{ all -> 0x00d3 }
            if (r0 != 0) goto L_0x0093
            android.os.Handler r0 = r11.mHandler     // Catch:{ all -> 0x00d3 }
            r6 = 7
            r0.removeMessages(r6)     // Catch:{ all -> 0x00d3 }
            r11.mPendingReset = r1     // Catch:{ all -> 0x00d3 }
            goto L_0x00ae
        L_0x0093:
            if (r12 != r9) goto L_0x0099
            int r0 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r0 > 0) goto L_0x009e
        L_0x0099:
            r0 = 2
            if (r12 != r0) goto L_0x00a4
            if (r3 != 0) goto L_0x00a4
        L_0x009e:
            r11.doKeyguardLaterLocked(r4)     // Catch:{ all -> 0x00d3 }
            r11.mLockLater = r1     // Catch:{ all -> 0x00d3 }
            goto L_0x00ae
        L_0x00a4:
            com.android.internal.widget.LockPatternUtils r0 = r11.mLockPatternUtils     // Catch:{ all -> 0x00d3 }
            boolean r0 = r0.isLockScreenDisabled(r2)     // Catch:{ all -> 0x00d3 }
            if (r0 != 0) goto L_0x00ae
            r11.mPendingLock = r1     // Catch:{ all -> 0x00d3 }
        L_0x00ae:
            boolean r0 = r11.mPendingLock     // Catch:{ all -> 0x00d3 }
            if (r0 == 0) goto L_0x00b7
            if (r12 == r9) goto L_0x00b7
            r11.playSounds(r1)     // Catch:{ all -> 0x00d3 }
        L_0x00b7:
            monitor-exit(r11)     // Catch:{ all -> 0x00d3 }
            android.content.Context r0 = r11.mContext
            com.android.keyguard.KeyguardUpdateMonitor r0 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r0)
            r0.dispatchStartedGoingToSleep(r12)
            sScreenOnDelay = r7
            r11.notifyStartedGoingToSleep()
            com.android.systemui.recents.events.RecentsEventBus r0 = com.android.systemui.recents.events.RecentsEventBus.getDefault()
            com.android.systemui.events.ScreenOffEvent r1 = new com.android.systemui.events.ScreenOffEvent
            r1.<init>()
            r0.post(r1)
            return
        L_0x00d3:
            r0 = move-exception
            monitor-exit(r11)     // Catch:{ all -> 0x00d3 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.onStartedGoingToSleep(int):void");
    }

    public void cancelPendingLock() {
        synchronized (this) {
            if (this.mPendingLock) {
                this.mPendingLock = false;
                playSounds(false);
                resetAppLock();
            }
        }
    }

    private void resetAppLock() {
        SecurityManager securityManager = (SecurityManager) this.mContext.getSystemService("security");
        if (securityManager != null) {
            securityManager.removeAccessControlPassAsUser("*", -1);
        }
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        Log.d("KeyguardViewMediator", "onFinishedGoingToSleep(" + why + ")");
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            notifyFinishedGoingToSleep();
            if (cameraGestureTriggered) {
                Log.i("KeyguardViewMediator", "Camera gesture was triggered, preventing Keyguard locking.");
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
                this.mPendingLock = false;
                this.mPendingReset = false;
            }
            if (this.mPendingReset) {
                resetStateLocked();
                this.mPendingReset = false;
            }
            if (this.mPendingLock) {
                doKeyguardLocked(null);
                this.mPendingLock = false;
            }
            if (!this.mLockLater && !cameraGestureTriggered) {
                doKeyguardForChildProfilesLocked();
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchFinishedGoingToSleep(why);
    }

    private long getLockTimeout(int userId) {
        return 0;
    }

    public boolean isFaceUnlockInited() {
        return this.mFaceUnlockManager.hasInit();
    }

    public boolean shouldListenForFaceUnlock() {
        return this.mUpdateMonitor.shouldListenForFaceUnlock();
    }

    private void doKeyguardLaterLocked() {
        long timeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
        if (timeout == 0) {
            doKeyguardLocked(null);
        } else {
            doKeyguardLaterLocked(timeout);
        }
    }

    private void doKeyguardLaterLocked(long timeout) {
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        intent.addFlags(268435456);
        PendingIntent sender = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + timeout, sender);
        Log.d("KeyguardViewMediator", "setting alarm to turn off keyguard, seq = " + this.mDelayedShowingSequence);
        doKeyguardLaterForChildProfilesLocked();
    }

    private void doKeyguardLaterForChildProfilesLocked() {
        for (int profileId : UserManagerCompat.getEnabledProfileIds(UserManager.get(this.mContext), UserHandle.myUserId())) {
            if (LockPatternUtilsCompat.isSeparateProfileChallengeEnabled(this.mLockPatternUtils, profileId)) {
                long userTimeout = getLockTimeout(profileId);
                if (userTimeout == 0) {
                    doKeyguardForChildProfilesLocked();
                } else {
                    Intent lockIntent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
                    lockIntent.putExtra("seq", this.mDelayedProfileShowingSequence);
                    lockIntent.putExtra("android.intent.extra.USER_ID", profileId);
                    lockIntent.addFlags(268435456);
                    this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + userTimeout, PendingIntent.getBroadcast(this.mContext, 0, lockIntent, 268435456));
                }
            }
        }
    }

    private void doKeyguardForChildProfilesLocked() {
        for (int profileId : UserManagerCompat.getEnabledProfileIds(UserManager.get(this.mContext), UserHandle.myUserId())) {
            if (LockPatternUtilsCompat.isSeparateProfileChallengeEnabled(this.mLockPatternUtils, profileId)) {
                lockProfile(profileId);
            }
        }
    }

    private void cancelDoKeyguardLaterLocked() {
        this.mDelayedShowingSequence++;
    }

    private void cancelDoKeyguardForChildProfilesLocked() {
        this.mDelayedProfileShowingSequence++;
    }

    public void onStartedWakingUp() {
        Trace.beginSection("KeyguardViewMediator#onStartedWakingUp");
        synchronized (this) {
            this.mDeviceInteractive = true;
            cancelDoKeyguardLaterLocked();
            cancelDoKeyguardForChildProfilesLocked();
            Log.d("KeyguardViewMediator", "onStartedWakingUp, seq = " + this.mDelayedShowingSequence);
            notifyStartedWakingUp();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedWakingUp();
        maybeSendUserPresentBroadcast();
        RecentsEventBus.getDefault().post(new ScreenOnEvent());
        Trace.endSection();
    }

    public void onStartedWakingUp(String reason) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "onStartedWakingUp, reason = " + reason);
            this.mStartedWakingUp = true;
            if (!MiuiKeyguardUtils.isSupportScreenOnDelayed(this.mContext) || !shouldListenForFaceUnlock() || !isFaceUnlockInited() || this.mUpdateMonitor.isStayScreenFaceUnlockSuccess()) {
                MiuiKeyguardUtils.setScreenTurnOnDelayed(false);
                sScreenOnDelay = 0;
            } else {
                MiuiKeyguardUtils.setScreenTurnOnDelayed(true);
                Log.d("face_unlock", "face unlock when screen on delayed");
                sScreenOnDelay = 550;
            }
        }
    }

    public void onFinishedWakingUp() {
        Log.d("KeyguardViewMediator", "onFinishedWakingUp");
        synchronized (this) {
            this.mStartedWakingUp = false;
        }
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
        Trace.beginSection("KeyguardViewMediator#onScreenTurningOn");
        this.mScreeningOn = true;
        notifyScreenOn(callback);
        Trace.endSection();
    }

    public void onScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#onScreenTurnedOn");
        this.mScreeningOn = false;
        notifyScreenTurnedOn();
        this.mUpdateMonitor.dispatchScreenTurnedOn();
        Trace.endSection();
    }

    public void onScreenTurnedOff() {
        notifyScreenTurnedOff();
        this.mUpdateMonitor.dispatchScreenTurnedOff();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            sendUserPresentBroadcast();
        } else if (this.mSystemReady && shouldWaitForProvisioning()) {
            LockPatternUtilsCompat.userPresent(this.mLockPatternUtils, KeyguardUpdateMonitor.getCurrentUser());
        }
    }

    public void onDreamingStarted() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStarted();
        synchronized (this) {
            if (this.mDeviceInteractive && this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                doKeyguardLaterLocked();
            }
        }
    }

    public void onDreamingStopped() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStopped();
        synchronized (this) {
            if (this.mDeviceInteractive) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00a3, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setKeyguardEnabled(boolean r5) {
        /*
            r4 = this;
            monitor-enter(r4)
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x00a4 }
            r1.<init>()     // Catch:{ all -> 0x00a4 }
            java.lang.String r2 = "setKeyguardEnabled("
            r1.append(r2)     // Catch:{ all -> 0x00a4 }
            r1.append(r5)     // Catch:{ all -> 0x00a4 }
            java.lang.String r2 = ")"
            r1.append(r2)     // Catch:{ all -> 0x00a4 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x00a4 }
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00a4 }
            r4.mExternallyEnabled = r5     // Catch:{ all -> 0x00a4 }
            r0 = 1
            if (r5 != 0) goto L_0x0042
            boolean r1 = r4.mShowing     // Catch:{ all -> 0x00a4 }
            if (r1 == 0) goto L_0x0042
            com.android.internal.policy.IKeyguardExitCallback r1 = r4.mExitSecureCallback     // Catch:{ all -> 0x00a4 }
            if (r1 == 0) goto L_0x0032
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "in process of verifyUnlock request, ignoring"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00a4 }
            monitor-exit(r4)     // Catch:{ all -> 0x00a4 }
            return
        L_0x0032:
            java.lang.String r1 = "KeyguardViewMediator"
            java.lang.String r2 = "remembering to reshow, hiding keyguard, disabling status bar expansion"
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x00a4 }
            r4.mNeedToReshowWhenReenabled = r0     // Catch:{ all -> 0x00a4 }
            r4.updateInputRestrictedLocked()     // Catch:{ all -> 0x00a4 }
            r4.hideLocked()     // Catch:{ all -> 0x00a4 }
            goto L_0x00a2
        L_0x0042:
            if (r5 == 0) goto L_0x00a2
            boolean r1 = r4.mNeedToReshowWhenReenabled     // Catch:{ all -> 0x00a4 }
            if (r1 == 0) goto L_0x00a2
            java.lang.String r1 = "KeyguardViewMediator"
            java.lang.String r2 = "previously hidden, reshowing, reenabling status bar expansion"
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x00a4 }
            r1 = 0
            r4.mNeedToReshowWhenReenabled = r1     // Catch:{ all -> 0x00a4 }
            r4.updateInputRestrictedLocked()     // Catch:{ all -> 0x00a4 }
            com.android.internal.policy.IKeyguardExitCallback r2 = r4.mExitSecureCallback     // Catch:{ all -> 0x00a4 }
            r3 = 0
            if (r2 == 0) goto L_0x0075
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r2 = "onKeyguardExitResult(false), resetting"
            android.util.Log.d(r0, r2)     // Catch:{ all -> 0x00a4 }
            com.android.internal.policy.IKeyguardExitCallback r0 = r4.mExitSecureCallback     // Catch:{ RemoteException -> 0x0067 }
            r0.onKeyguardExitResult(r1)     // Catch:{ RemoteException -> 0x0067 }
            goto L_0x006f
        L_0x0067:
            r0 = move-exception
            java.lang.String r1 = "KeyguardViewMediator"
            java.lang.String r2 = "Failed to call onKeyguardExitResult(false)"
            android.util.Slog.w(r1, r2, r0)     // Catch:{ all -> 0x00a4 }
        L_0x006f:
            r4.mExitSecureCallback = r3     // Catch:{ all -> 0x00a4 }
            r4.resetStateLocked()     // Catch:{ all -> 0x00a4 }
            goto L_0x00a2
        L_0x0075:
            r4.showLocked(r3)     // Catch:{ all -> 0x00a4 }
            r4.mWaitingUntilKeyguardVisible = r0     // Catch:{ all -> 0x00a4 }
            android.os.Handler r0 = r4.mHandler     // Catch:{ all -> 0x00a4 }
            r1 = 8
            r2 = 2000(0x7d0, double:9.88E-321)
            r0.sendEmptyMessageDelayed(r1, r2)     // Catch:{ all -> 0x00a4 }
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "waiting until mWaitingUntilKeyguardVisible is false"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00a4 }
        L_0x008a:
            boolean r0 = r4.mWaitingUntilKeyguardVisible     // Catch:{ all -> 0x00a4 }
            if (r0 == 0) goto L_0x009b
            r4.wait()     // Catch:{ InterruptedException -> 0x0092 }
        L_0x0091:
            goto L_0x008a
        L_0x0092:
            r0 = move-exception
            java.lang.Thread r1 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x00a4 }
            r1.interrupt()     // Catch:{ all -> 0x00a4 }
            goto L_0x0091
        L_0x009b:
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "done waiting for mWaitingUntilKeyguardVisible"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00a4 }
        L_0x00a2:
            monitor-exit(r4)     // Catch:{ all -> 0x00a4 }
            return
        L_0x00a4:
            r0 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x00a4 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.setKeyguardEnabled(boolean):void");
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        Trace.beginSection("KeyguardViewMediator#verifyUnlock");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "verifyUnlock");
            if (shouldWaitForProvisioning()) {
                Log.d("KeyguardViewMediator", "ignoring because device isn't provisioned");
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
            } else if (this.mExternallyEnabled) {
                Log.w("KeyguardViewMediator", "verifyUnlock called when not externally disabled");
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e2) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e2);
                }
            } else if (this.mExitSecureCallback != null) {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e3) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e3);
                }
            } else if (!isSecure()) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
                try {
                    callback.onKeyguardExitResult(true);
                } catch (RemoteException e4) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e4);
                }
            } else {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e5) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e5);
                }
            }
        }
        Trace.endSection();
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public boolean isHiding() {
        return this.mHiding;
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean isShowingAndOccluded() {
        return this.mShowing && this.mOccluded;
    }

    public boolean isStartedWakingUp() {
        return this.mStartedWakingUp;
    }

    public boolean isScreeningOn() {
        return this.mScreeningOn;
    }

    public void setOccluded(boolean isOccluded, boolean animate) {
        Trace.beginSection("KeyguardViewMediator#setOccluded");
        Slog.i("KeyguardViewMediator", "setOccluded " + isOccluded);
        this.mHandler.removeMessages(9);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, isOccluded, animate));
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleSetOccluded(boolean isOccluded, boolean animate) {
        String str;
        Trace.beginSection("KeyguardViewMediator#handleSetOccluded");
        synchronized (this) {
            if (this.mHiding && isOccluded) {
                startKeyguardExitAnimation(0, 0);
            }
            if (this.mOccluded != isOccluded) {
                this.mOccluded = isOccluded;
                this.mUpdateMonitor.setKeyguardShowingAndOccluded(this.mShowing, this.mOccluded);
                if (Build.VERSION.SDK_INT < 26) {
                    updateActivityLockScreenState(this.mShowing);
                }
                this.mSmartCoverHelper.refreshSmartCover();
                if (isOccluded) {
                    this.mUpdateMonitor.stopFaceUnlock();
                }
                this.mStatusBarKeyguardViewManager.setOccluded(isOccluded, animate && this.mDeviceInteractive);
                if (!isOccluded && this.mDeviceInteractive) {
                    this.mUpdateMonitor.startFaceUnlock();
                    this.mBleUnlockHelper.verifyBLEDeviceRssi();
                }
                adjustStatusBarLocked();
                Context context = this.mContext;
                if (this.mOccluded) {
                    str = "Wallpaper_Covered";
                } else {
                    str = "Wallpaper_Uncovered";
                }
                LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(context, str);
            }
            if (this.mShowing) {
                disableFullScreenGesture(!this.mOccluded);
            }
        }
        Trace.endSection();
    }

    public void doKeyguardTimeout(Bundle options) {
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, options));
    }

    public boolean isInputRestricted() {
        return this.mShowing || this.mNeedToReshowWhenReenabled;
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
        }
    }

    private void updateInputRestrictedLocked() {
        boolean inputRestricted = isInputRestricted();
        if (this.mInputRestricted != inputRestricted) {
            this.mInputRestricted = inputRestricted;
            for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
                IKeyguardStateCallbackCompat callback = this.mKeyguardStateCallbacks.get(i);
                try {
                    callback.onInputRestrictedStateChanged(inputRestricted);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(callback);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void doKeyguardLocked(Bundle options) {
        if (!this.mExternallyEnabled) {
            Slog.w("KeyguardViewMediator", "doKeyguard: not showing because externally disabled");
        } else if (this.mStatusBarKeyguardViewManager.isShowing()) {
            Slog.w("KeyguardViewMediator", "doKeyguard: not showing because it is already showing");
            resetStateLocked();
        } else {
            if (!mustNotUnlockCurrentUser() || !this.mUpdateMonitor.isDeviceProvisioned()) {
                boolean forceShow = true;
                this.mSimLockedOrMissing = this.mUpdateMonitor.isSimPinSecure() || ((SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.ABSENT)) || SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PERM_DISABLED))) && (SystemProperties.getBoolean("keyguard.no_require_sim", false) ^ true));
                if (this.mSimLockedOrMissing || !shouldWaitForProvisioning()) {
                    boolean isSecure = isSecure();
                    boolean isLockScreenDisabled = this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) || (!isSecure && MiuiKeyguardUtils.showMXTelcelLockScreen(this.mContext));
                    if (options == null || !options.getBoolean("force_show", false)) {
                        forceShow = false;
                    }
                    if (isLockScreenDisabled && !this.mSimLockedOrMissing && !forceShow) {
                        Slog.w("KeyguardViewMediator", "doKeyguard: not showing because lockscreen is off");
                        return;
                    } else if (this.mLockPatternUtils.checkVoldPassword(KeyguardUpdateMonitor.getCurrentUser())) {
                        Slog.w("KeyguardViewMediator", "Not showing lock screen since just decrypted");
                        setShowingLocked(false);
                        hideLocked();
                        this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
                        return;
                    } else if (this.mHideLockForLid && !this.mSimLockedOrMissing && !isSecure) {
                        Slog.w("KeyguardViewMediator", "Not showing lock screen since in smart cover mode");
                        if (this.mShowing) {
                            handleHide();
                        }
                        return;
                    }
                } else {
                    Slog.w("KeyguardViewMediator", "doKeyguard: not showing because device isn't provisioned and the sim is not locked or missing");
                    return;
                }
            }
            if (this.mFingerprintUnlockController.isCancelingPendingLock()) {
                Slog.w("KeyguardViewMediator", "doKeyguard: not showing because canceling pending lock");
                return;
            }
            Log.d("KeyguardViewMediator", "doKeyguard: showing the lock screen");
            showLocked(options);
        }
    }

    public void setHideLockForLid(boolean hideLockForLid) {
        this.mHideLockForLid = hideLockForLid;
    }

    public boolean isSimLockedOrMissing() {
        return this.mSimLockedOrMissing;
    }

    /* access modifiers changed from: private */
    public void lockProfile(int userId) {
        TrustManagerCompat.setDeviceLockedForUser(this.mTrustManager, userId, true);
    }

    /* access modifiers changed from: private */
    public boolean shouldWaitForProvisioning() {
        return !this.mUpdateMonitor.isDeviceProvisioned() && !isSecure();
    }

    /* access modifiers changed from: private */
    public void handleDismiss(IKeyguardDismissCallback callback) {
        if (this.mShowing) {
            if (callback != null) {
                this.mDismissCallbackRegistry.addCallback(callback);
            }
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
        } else if (callback != null) {
            new DismissCallbackWrapper(callback).notifyDismissError();
        }
    }

    public void dismiss(IKeyguardDismissCallback callback) {
        this.mHandler.obtainMessage(11, callback).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void resetStateLocked() {
        Log.e("KeyguardViewMediator", "resetStateLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
    }

    private void notifyStartedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyStartedGoingToSleep");
        this.mHandler.sendEmptyMessage(17);
    }

    private void notifyFinishedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyFinishedGoingToSleep");
        this.mHandler.sendEmptyMessage(5);
    }

    private void notifyStartedWakingUp() {
        Log.d("KeyguardViewMediator", "notifyStartedWakingUp");
        this.mHandler.sendEmptyMessage(14);
    }

    private void notifyScreenOn(IKeyguardDrawnCallback callback) {
        Slog.w("KeyguardViewMediator", "notifyScreenOn");
        if (this.mFingerprintUnlockController.isCancelingPendingLock()) {
            synchronized (this) {
                if (this.mFpAuthTime != 0) {
                    this.mWaitFwTotalTime = System.currentTimeMillis() - this.mFpAuthTime;
                }
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(6, callback));
    }

    private void notifyScreenTurnedOn() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOn");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(15));
    }

    private void notifyScreenTurnedOff() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOff");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(16));
    }

    private void showLocked(Bundle options) {
        Trace.beginSection("KeyguardViewMediator#showLocked aqcuiring mShowKeyguardWakeLock");
        Log.d("KeyguardViewMediator", "showLocked");
        this.mShowKeyguardWakeLock.acquire();
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(this.mContext);
        if (this.mUpdateMonitor.shouldListenForFaceUnlock()) {
            this.mFaceUnlockManager.runOnFaceUnlockWorkerThread(this.mInitAllRunnable);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, options));
        this.mUpdateMonitor.setKeyguardHide(false);
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public boolean migrateKeyguardFiles() {
        try {
            File keyguardFolder = new File("/data/user_de/0/com.android.keyguard/files/");
            if (!keyguardFolder.exists()) {
                Log.i("KeyguardViewMediator", "old files not exist");
                return true;
            }
            File[] files = keyguardFolder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return !"miuisdk".equals(name) && !"model".equals(name);
                }
            });
            if (files != null) {
                if (files.length != 0) {
                    for (int i = 0; i < files.length; i++) {
                        FileUtils.copyFile(files[i], new File("/data/user_de/0/com.android.systemui/files/" + files[i].getName()));
                        Log.i("KeyguardViewMediator", "is migrating " + files[i]);
                        files[i].delete();
                    }
                    deleteFile(new File("/data/user_de/0/com.android.keyguard/"));
                    return true;
                }
            }
            Log.i("KeyguardViewMediator", "old files dir " + keyguardFolder + " has no file to migrate");
            return true;
        } catch (Exception e) {
            Log.d("KeyguardViewMediator", "migrateKeyguardFiles fail", e);
            return false;
        }
    }

    private void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File deleteFile : files) {
                deleteFile(deleteFile);
            }
            file.delete();
        }
    }

    /* access modifiers changed from: private */
    public void sendReEntryFaceUnlockNotification() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.keyguard.settings.MiuiFaceDataInput"));
        KeyguardCompatibilityHelperForO.sendNotification(this.mContext, this.mContext.getString(R.string.face_unlock_reenter_title), this.mContext.getString(R.string.face_unlock_reenter_content), R.drawable.placeholder, intent, true, "com.android.settings");
    }

    private void hideLocked() {
        Trace.beginSection("KeyguardViewMediator#hideLocked");
        Log.d("KeyguardViewMediator", "hideLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        Trace.endSection();
    }

    public boolean isSecure() {
        return isSecure(KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean isSecure(int userId) {
        return this.mLockPatternUtils.isSecure(userId) || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure();
    }

    public void setSwitchingUser(boolean switching) {
        Trace.beginSection("KeyguardViewMediator#setSwitchingUser");
        this.mHandler.removeMessages(18);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, switching, 0));
        Trace.endSection();
    }

    public void setCurrentUser(int newUserId) {
        KeyguardUpdateMonitor.setCurrentUser(newUserId);
        Class<?> cls = getClass();
        PanelBar.LOG((Class) cls, "setCurrentUser=" + newUserId);
        if (newUserId != 0) {
            MiuiKeyguardUtils.setUserAuthenticatedSinceBootSecond();
        }
        synchronized (this) {
            notifyTrustedChangedLocked(this.mUpdateMonitor.getUserHasTrust(newUserId));
        }
    }

    public void keyguardDone() {
        Trace.beginSection("KeyguardViewMediator#keyguardDone");
        Log.d("KeyguardViewMediator", "keyguardDone()");
        userActivity();
        EventLog.writeEvent(70000, 2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7));
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void tryKeyguardDone() {
        Log.d("KeyguardViewMediator", "tryKeyguardDone mKeyguardDonePending = " + this.mKeyguardDonePending + " mHideAnimationRun = " + this.mHideAnimationRun + " mHideAnimationRunning = " + this.mHideAnimationRunning);
        if (!this.mKeyguardDonePending && this.mHideAnimationRun && !this.mHideAnimationRunning) {
            handleKeyguardDone();
        } else if (!this.mHideAnimationRun) {
            this.mHideAnimationRun = true;
            this.mHideAnimationRunning = true;
            this.mStatusBarKeyguardViewManager.startPreHideAnimation(this.mHideAnimationFinishedRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void handleKeyguardDone() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDone");
        final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        this.mUiOffloadThread.submit(new Runnable() {
            public void run() {
                if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(currentUser)) {
                    DevicePolicyManagerCompat.reportKeyguardDismissed(KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager(), currentUser);
                }
            }
        });
        Log.d("KeyguardViewMediator", "handleKeyguardDone");
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        this.mUpdateMonitor.clearFailedUnlockAttempts();
        this.mUpdateMonitor.clearFingerprintRecognized();
        this.mUpdateMonitor.resetAllFingerprintLockout();
        this.mBleUnlockHelper.unregisterUnlockListener();
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setUnlockLockout(false);
        }
        if (this.mGoingToSleep) {
            this.mFingerprintUnlockController.resetMode();
            this.mFaceUnlockController.resetFaceUnlockMode();
            Log.i("KeyguardViewMediator", "Device is going to sleep, aborting keyguardDone");
            return;
        }
        if (this.mExitSecureCallback != null) {
            try {
                this.mExitSecureCallback.onKeyguardExitResult(true);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult()", e);
            }
            this.mExitSecureCallback = null;
            this.mExternallyEnabled = true;
            this.mNeedToReshowWhenReenabled = false;
            updateInputRestricted();
        }
        handleHide();
        if (MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext)) {
            Log.d("face_unlock", "keyguard dismiss time=" + (System.currentTimeMillis() - FaceUnlockManager.sStageInFaceUnlockTime));
            Log.d("face_unlock", "face unlock time=" + (System.currentTimeMillis() - KeyguardUpdateMonitor.sScreenTurnedOnTime));
        }
        this.mUiOffloadThread.submit(new Runnable() {
            public void run() {
                MiuiKeyguardUtils.recordKeyguardSettingsStatistics(KeyguardViewMediator.this.mContext);
                LockScreenMagazineAnalytics.recordLockScreenMagazineAd(KeyguardViewMediator.this.mContext);
            }
        });
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                final int currentUserId = KeyguardUpdateMonitor.getCurrentUser();
                final UserHandle currentUser = new UserHandle(currentUserId);
                final UserManager um = (UserManager) this.mContext.getSystemService("user");
                this.mUiOffloadThread.submit(new Runnable() {
                    public void run() {
                        for (int profileId : UserManagerCompat.getProfileIdsWithDisabled(um, currentUser.getIdentifier())) {
                            KeyguardViewMediator.this.mContext.sendBroadcastAsUser(KeyguardViewMediator.USER_PRESENT_INTENT, UserHandleCompat.of(profileId));
                        }
                        LockPatternUtilsCompat.userPresent(KeyguardViewMediator.this.mLockPatternUtils, currentUserId);
                    }
                });
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleKeyguardDoneDrawing() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDoneDrawing");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing");
            if (this.mWaitingUntilKeyguardVisible) {
                Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(8);
            }
        }
        Trace.endSection();
    }

    private void playSounds(boolean locked) {
        playSound(locked ? this.mLockSoundId : this.mUnlockSoundId);
    }

    private void playSound(final int soundId) {
        if (soundId != 0 && Settings.System.getInt(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1) == 1) {
            this.mLockSounds.stop(this.mLockSoundStreamId);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                if (this.mAudioManager != null) {
                    this.mUiSoundsStreamType = this.mAudioManager.getUiSoundsStreamType();
                } else {
                    return;
                }
            }
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    if (!KeyguardViewMediator.this.mAudioManager.isStreamMute(KeyguardViewMediator.this.mUiSoundsStreamType)) {
                        int id = KeyguardViewMediator.this.mLockSounds.play(soundId, KeyguardViewMediator.this.mLockSoundVolume, KeyguardViewMediator.this.mLockSoundVolume, 1, 0, 1.0f);
                        synchronized (this) {
                            int unused = KeyguardViewMediator.this.mLockSoundStreamId = id;
                        }
                    }
                }
            });
        }
    }

    private void updateActivityLockScreenState(boolean showing) {
        try {
            ActivityManagerCompat.setLockScreenShown(showing, this.mOccluded);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /* access modifiers changed from: private */
    public void handleShow(Bundle options) {
        Trace.beginSection("KeyguardViewMediator#handleShow");
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (this.mLockPatternUtils.isSecure(currentUser)) {
            DevicePolicyManagerCompat.reportKeyguardSecured(this.mLockPatternUtils.getDevicePolicyManager(), currentUser);
        }
        synchronized (this) {
            if (!this.mSystemReady) {
                Log.d("KeyguardViewMediator", "ignoring handleShow because system is not ready.");
                this.mUpdateMonitor.setKeyguardHide(true);
                return;
            }
            Log.d("KeyguardViewMediator", "handleShow");
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.show(options);
            this.mHiding = false;
            this.mWakeAndUnlocking = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            adjustStatusBarLocked();
            userActivity();
            if (MiuiKeyguardUtils.isGxzwSensor()) {
                MiuiGxzwManager.getInstance().onKeyguardShow();
            }
            this.mShowKeyguardWakeLock.release();
            this.mUpdateMonitor.setKeyguardHide(false);
            this.mKeyguardDisplayManager.show();
            disableFullScreenGesture(!this.mOccluded);
            registerWakeupAndSleepSensor();
            resetAppLock();
            Trace.endSection();
        }
    }

    /* access modifiers changed from: private */
    public void keyguardGoingAway() {
        final int flags;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            if (!MiuiGxzwManager.getInstance().isFodFastUnlock() || MiuiGxzwManager.getInstance().getGxzwUnlockMode() != 1) {
                flags = 8;
            } else {
                flags = 2;
            }
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    MiuiSettings.System.putBooleanForUser(KeyguardViewMediator.this.mContext.getContentResolver(), "is_fingerprint_unlock", true, -2);
                }
            });
        } else {
            flags = 2;
        }
        if (!MiuiKeyguardUtils.isGxzwSensor() || !MiuiGxzwManager.getInstance().isFodFastUnlock() || MiuiGxzwManager.getInstance().getGxzwUnlockMode() != 1) {
            try {
                ActivityManagerCompat.keyguardGoingAway(flags);
                Log.d("KeyguardViewMediator", "call fw keyguardGoingAway: flags = " + flags);
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
        } else {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    try {
                        ActivityManagerCompat.keyguardGoingAway(flags);
                        Log.d("KeyguardViewMediator", "call fw keyguardGoingAway: flags = " + flags);
                    } catch (RemoteException e) {
                        Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0065, code lost:
        unregisterWakeupAndSleepSensor();
        r4.mUpdateMonitor.unregisterSeneorsForKeyguard();
        android.os.Trace.endSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0070, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleHide() {
        /*
            r4 = this;
            java.lang.String r0 = "KeyguardViewMediator#handleHide"
            android.os.Trace.beginSection(r0)
            com.android.keyguard.KeyguardUpdateMonitor r0 = r4.mUpdateMonitor
            r1 = 1
            r0.setKeyguardHide(r1)
            monitor-enter(r4)
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r2 = "handleHide"
            android.util.Log.d(r0, r2)     // Catch:{ all -> 0x0071 }
            boolean r0 = r4.mustNotUnlockCurrentUser()     // Catch:{ all -> 0x0071 }
            if (r0 == 0) goto L_0x0022
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "Split system user, quit unlocking."
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x0071 }
            monitor-exit(r4)     // Catch:{ all -> 0x0071 }
            return
        L_0x0022:
            r4.mHiding = r1     // Catch:{ all -> 0x0071 }
            com.android.keyguard.KeyguardUpdateMonitor r0 = r4.mUpdateMonitor     // Catch:{ all -> 0x0071 }
            r1 = 0
            r0.setFaceUnlockStarted(r1)     // Catch:{ all -> 0x0071 }
            com.android.keyguard.faceunlock.FaceUnlockManager r0 = r4.mFaceUnlockManager     // Catch:{ all -> 0x0071 }
            com.android.systemui.keyguard.KeyguardViewMediator$17 r1 = new com.android.systemui.keyguard.KeyguardViewMediator$17     // Catch:{ all -> 0x0071 }
            r1.<init>()     // Catch:{ all -> 0x0071 }
            r0.runOnFaceUnlockWorkerThread(r1)     // Catch:{ all -> 0x0071 }
            boolean r0 = r4.mShowing     // Catch:{ all -> 0x0071 }
            if (r0 == 0) goto L_0x0042
            boolean r0 = r4.mOccluded     // Catch:{ all -> 0x0071 }
            if (r0 != 0) goto L_0x0042
            java.lang.Runnable r0 = r4.mKeyguardGoingAwayRunnable     // Catch:{ all -> 0x0071 }
            r0.run()     // Catch:{ all -> 0x0071 }
            goto L_0x0057
        L_0x0042:
            long r0 = android.os.SystemClock.uptimeMillis()     // Catch:{ all -> 0x0071 }
            android.view.animation.Animation r2 = r4.mHideAnimation     // Catch:{ all -> 0x0071 }
            long r2 = r2.getStartOffset()     // Catch:{ all -> 0x0071 }
            long r0 = r0 + r2
            android.view.animation.Animation r2 = r4.mHideAnimation     // Catch:{ all -> 0x0071 }
            long r2 = r2.getDuration()     // Catch:{ all -> 0x0071 }
            r4.handleStartKeyguardExitAnimation(r0, r2)     // Catch:{ all -> 0x0071 }
        L_0x0057:
            boolean r0 = com.android.keyguard.MiuiKeyguardUtils.isGxzwSensor()     // Catch:{ all -> 0x0071 }
            if (r0 == 0) goto L_0x0064
            com.android.keyguard.fod.MiuiGxzwManager r0 = com.android.keyguard.fod.MiuiGxzwManager.getInstance()     // Catch:{ all -> 0x0071 }
            r0.onKeyguardHide()     // Catch:{ all -> 0x0071 }
        L_0x0064:
            monitor-exit(r4)     // Catch:{ all -> 0x0071 }
            r4.unregisterWakeupAndSleepSensor()
            com.android.keyguard.KeyguardUpdateMonitor r0 = r4.mUpdateMonitor
            r0.unregisterSeneorsForKeyguard()
            android.os.Trace.endSection()
            return
        L_0x0071:
            r0 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x0071 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.handleHide():void");
    }

    /* access modifiers changed from: private */
    public void handleStartKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        Trace.beginSection("KeyguardViewMediator#handleStartKeyguardExitAnimation");
        Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation startTime=" + startTime + " fadeoutDuration=" + fadeoutDuration);
        synchronized (this) {
            if (this.mHiding) {
                this.mHiding = false;
                if (this.mWakeAndUnlocking && this.mDrawnCallback != null) {
                    this.mStatusBarKeyguardViewManager.getViewRootImpl().setReportNextDraw();
                    notifyDrawn(this.mDrawnCallback);
                    this.mDrawnCallback = null;
                }
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(this.mPhoneState)) {
                    playSounds(false);
                }
                this.mWakeAndUnlocking = false;
                setShowingLocked(false);
                this.mDismissCallbackRegistry.notifyDismissSucceeded();
                this.mStatusBarKeyguardViewManager.hide(startTime, fadeoutDuration);
                resetKeyguardDonePendingLocked();
                this.mHideAnimationRun = false;
                adjustStatusBarLocked();
                sendUserPresentBroadcast();
                this.mUpdateMonitor.setKeyguardGoingAway(false);
                this.mUiOffloadThread.submit(new Runnable() {
                    public void run() {
                        MiuiKeyguardUtils.setUserAuthenticatedSinceBoot();
                    }
                });
                if (this.mOccluded && ((this.mUnlockByFingerPrint || this.mUnlockByFace) && !MiuiKeyguardUtils.isTopActivitySystemApp(this.mContext))) {
                    this.mUnlockByFingerPrint = false;
                    this.mUnlockByFace = false;
                    if (this.mReadyForKeyEvent) {
                        sendKeyEvent();
                        this.mReadyForKeyEvent = false;
                        this.mSendKeyEventScreenOn = false;
                    } else {
                        this.mSendKeyEventScreenOn = true;
                    }
                }
                unregisterLargeAreaTouchSensor();
                this.mUpdateMonitor.setFaceUnlockMode(0);
                if (MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext)) {
                    Slog.i("face_unlock", "keyguard exit time=" + (System.currentTimeMillis() - KeyguardUpdateMonitor.sScreenTurnedOnTime));
                }
                printFingerprintUnlockInfo(false);
                LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Device_Unlock");
                Trace.endSection();
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendKeyEvent() {
        if (!MiuiKeyguardUtils.isGxzwSensor()) {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    try {
                        long now = SystemClock.uptimeMillis();
                        KeyEvent up = new KeyEvent(now, now, 0, 3, 0, 0, -1, 0, 8, 257);
                        KeyEvent keyEvent = new KeyEvent(now, now, 1, 3, 0, 0, -1, 0, 8, 257);
                        InputManager.getInstance().injectInputEvent(up, 0);
                        InputManager.getInstance().injectInputEvent(keyEvent, 0);
                        Log.d("miui_keyguard", "send keyEvent Home");
                    } catch (Exception e) {
                        Log.e("miui_keyguard", "send keyEvent Home fail:" + e.toString());
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void adjustStatusBarLocked() {
        adjustStatusBarLocked(false);
    }

    /* access modifiers changed from: private */
    public void adjustStatusBarLocked(boolean forceHideHomeClockBack) {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        if (this.mStatusBarManager == null) {
            Log.w("KeyguardViewMediator", "Could not get status bar manager");
            return;
        }
        int flags = 0;
        if (this.mShowing) {
            flags = 0 | 16777216;
        }
        if (forceHideHomeClockBack || isShowingAndNotOccluded()) {
            flags = flags | 2097152 | 8388608 | 4194304;
        }
        Log.d("KeyguardViewMediator", "adjustStatusBarLocked: mShowing=" + this.mShowing + " mOccluded=" + this.mOccluded + " isSecure=" + isSecure() + " force=" + forceHideHomeClockBack + " --> flags=0x" + Integer.toHexString(flags));
        this.mStatusBarManager.disable(flags);
    }

    /* access modifiers changed from: private */
    public void handleReset() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleReset");
            this.mStatusBarKeyguardViewManager.reset(true);
            this.mFingerprintUnlockController.resetMode();
        }
    }

    /* access modifiers changed from: private */
    public void handleVerifyUnlock() {
        Trace.beginSection("KeyguardViewMediator#handleVerifyUnlock");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleVerifyUnlock");
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyStartedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyStartedGoingToSleep");
            this.mUpdateMonitor.stopFaceUnlock();
            this.mStatusBarKeyguardViewManager.onStartedGoingToSleep();
        }
    }

    /* access modifiers changed from: private */
    public void handleNotifyFinishedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyFinishedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onFinishedGoingToSleep();
            unregisterLargeAreaTouchSensor();
        }
    }

    /* access modifiers changed from: private */
    public void handleNotifyStartedWakingUp() {
        Trace.beginSection("KeyguardViewMediator#handleMotifyStartedWakingUp");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyWakingUp");
            this.mUpdateMonitor.startFaceUnlock();
            this.mStatusBarKeyguardViewManager.onStartedWakingUp();
            registerLargeAreaTouchSensor();
        }
        this.mStartedWakingUp = false;
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurningOn(IKeyguardDrawnCallback callback) {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurningOn");
        synchronized (this) {
            Slog.w("KeyguardViewMediator", "handleNotifyScreenTurningOn");
            this.mUpdateMonitor.startFaceUnlock();
            this.mStatusBarKeyguardViewManager.onScreenTurningOn();
            AnalyticsHelper.recordScreenOn(WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper());
            if (callback != null) {
                if (this.mWakeAndUnlocking) {
                    this.mDrawnCallback = callback;
                } else if (MiuiKeyguardUtils.isScreenTurnOnDelayed()) {
                    notifyDrawnWhenScreenOn(callback);
                } else {
                    notifyDrawn(callback);
                }
            }
            this.mReadyForKeyEvent = true;
            if (this.mSendKeyEventScreenOn) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        KeyguardViewMediator.this.sendKeyEvent();
                        boolean unused = KeyguardViewMediator.this.mReadyForKeyEvent = false;
                        boolean unused2 = KeyguardViewMediator.this.mSendKeyEventScreenOn = false;
                    }
                });
            }
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurnedOn");
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionEnd(5);
        }
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOn");
            this.mSmartCoverHelper.onScreenTurnedOn();
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn();
            KeyguardUpdateMonitor.sWakeupByNotification = false;
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurnedOff() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOff");
            this.mStatusBarKeyguardViewManager.onScreenTurnedOff();
            this.mDrawnCallback = null;
            this.mWakeAndUnlocking = false;
        }
    }

    private void notifyDrawn(IKeyguardDrawnCallback callback) {
        Trace.beginSection("KeyguardViewMediator#notifyDrawn");
        Slog.w("KeyguardViewMediator", "notifyDrawn");
        if (this.mFingerprintUnlockController.isCancelingPendingLock()) {
            printFingerprintUnlockInfo(true);
        }
        try {
            callback.onDrawn();
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Exception calling onDrawn():", e);
        }
        Trace.endSection();
    }

    private void notifyDrawnWhenScreenOn(IKeyguardDrawnCallback callback) {
        Trace.beginSection("KeyguardViewMediator#notifyDrawn");
        screenTurnedOnCallback(callback);
        Trace.endSection();
    }

    private static void screenTurnedOnCallback(IKeyguardDrawnCallback callback) {
        Slog.w("KeyguardViewMediator", "screenTurnedOnCallback: sScreenOnDelay = " + sScreenOnDelay);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.android.internal.policy.IKeyguardDrawnCallback");
            data.writeLong(sScreenOnDelay);
            callback.asBinder().transact(255, data, reply, 1);
            reply.readException();
        } catch (RemoteException e) {
            Log.e("MiuiKeyguardUtils", "something wrong when delayed turn on screen");
            e.printStackTrace();
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
            throw th;
        }
        data.recycle();
        reply.recycle();
    }

    /* access modifiers changed from: private */
    public void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(13);
    }

    public void onBootCompleted() {
        this.mUpdateMonitor.dispatchBootCompleted();
        synchronized (this) {
            this.mBootCompleted = true;
            if (this.mBootSendUserPresent) {
                sendUserPresentBroadcast();
            }
        }
    }

    public void onWakeAndUnlocking() {
        Trace.beginSection("KeyguardViewMediator#onWakeAndUnlocking");
        this.mWakeAndUnlocking = true;
        keyguardDone();
        Trace.endSection();
    }

    public void preHideKeyguard() {
        keyguardGoingAway();
        this.mStatusBarKeyguardViewManager.setKeyguardTransparent();
    }

    public StatusBarKeyguardViewManager registerStatusBar(StatusBar statusBar, ViewGroup container, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController, FaceUnlockController faceUnlockController) {
        this.mFingerprintUnlockController = fingerprintUnlockController;
        this.mFaceUnlockController = faceUnlockController;
        this.mStatusBarKeyguardViewManager.registerStatusBar(statusBar, container, scrimController, fingerprintUnlockController, faceUnlockController, this.mDismissCallbackRegistry);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        Trace.beginSection("KeyguardViewMediator#startKeyguardExitAnimation");
        synchronized (this) {
            if (this.mKeyguardGoingAwayTime != 0) {
                this.mWaitFwTotalTime = System.currentTimeMillis() - this.mKeyguardGoingAwayTime;
            }
        }
        this.mHandler.removeMessages(19);
        Handler handler = this.mHandler;
        StartKeyguardExitAnimParams startKeyguardExitAnimParams = new StartKeyguardExitAnimParams(startTime, fadeoutDuration);
        this.mHandler.sendMessage(handler.obtainMessage(12, startKeyguardExitAnimParams));
        Trace.endSection();
    }

    public void onShortPowerPressedGoHome() {
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mSystemReady: ");
        pw.println(this.mSystemReady);
        pw.print("  mBootCompleted: ");
        pw.println(this.mBootCompleted);
        pw.print("  mBootSendUserPresent: ");
        pw.println(this.mBootSendUserPresent);
        pw.print("  mExternallyEnabled: ");
        pw.println(this.mExternallyEnabled);
        pw.print("  mShuttingDown: ");
        pw.println(this.mShuttingDown);
        pw.print("  mNeedToReshowWhenReenabled: ");
        pw.println(this.mNeedToReshowWhenReenabled);
        pw.print("  mShowing: ");
        pw.println(this.mShowing);
        pw.print("  mInputRestricted: ");
        pw.println(this.mInputRestricted);
        pw.print("  mOccluded: ");
        pw.println(this.mOccluded);
        pw.print("  mDelayedShowingSequence: ");
        pw.println(this.mDelayedShowingSequence);
        pw.print("  mExitSecureCallback: ");
        pw.println(this.mExitSecureCallback);
        pw.print("  mDeviceInteractive: ");
        pw.println(this.mDeviceInteractive);
        pw.print("  mStartedWakingUp: ");
        pw.println(this.mStartedWakingUp);
        pw.print("  mScreeningOn: ");
        pw.println(this.mScreeningOn);
        pw.print("  mGoingToSleep: ");
        pw.println(this.mGoingToSleep);
        pw.print("  mHiding: ");
        pw.println(this.mHiding);
        pw.print("  mWaitingUntilKeyguardVisible: ");
        pw.println(this.mWaitingUntilKeyguardVisible);
        pw.print("  mKeyguardDonePending: ");
        pw.println(this.mKeyguardDonePending);
        pw.print("  mHideAnimationRun: ");
        pw.println(this.mHideAnimationRun);
        pw.print("  mPendingReset: ");
        pw.println(this.mPendingReset);
        pw.print("  mPendingLock: ");
        pw.println(this.mPendingLock);
        pw.print("  mWakeAndUnlocking: ");
        pw.println(this.mWakeAndUnlocking);
        pw.print("  mDrawnCallback: ");
        pw.println(this.mDrawnCallback);
        pw.print("  mHideLockForLid: ");
        pw.println(this.mHideLockForLid);
        if (this.mLockPatternUtils != null) {
            pw.print("  isLockScreenDisabled: ");
            pw.println(this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()));
        }
    }

    private void setShowingLocked(boolean showing) {
        setShowingLocked(showing, false);
    }

    private void setShowingLocked(boolean showing, boolean forceCallbacks) {
        if (showing != this.mShowing || forceCallbacks) {
            this.mShowing = showing;
            this.mUpdateMonitor.setKeyguardShowingAndOccluded(this.mShowing, this.mOccluded);
            this.mUpdateMonitor.updateShowingState(this.mShowing);
            for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
                IKeyguardStateCallbackCompat callback = this.mKeyguardStateCallbacks.get(i);
                try {
                    callback.onShowingStateChanged(showing);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(callback);
                    }
                }
            }
            updateInputRestrictedLocked();
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    KeyguardViewMediator.this.mTrustManager.reportKeyguardShowingChanged();
                }
            });
            updateActivityLockScreenState(showing);
        }
    }

    private void notifyTrustedChangedLocked(boolean trusted) {
        for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
            try {
                this.mKeyguardStateCallbacks.get(i).onTrustedChanged(trusted);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call notifyTrustedChangedLocked", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(i);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyHasLockscreenWallpaperChanged(boolean hasLockscreenWallpaper) {
        for (int i = this.mKeyguardStateCallbacks.size() - 1; i >= 0; i--) {
            try {
                this.mKeyguardStateCallbacks.get(i).onHasLockscreenWallpaperChanged(hasLockscreenWallpaper);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onHasLockscreenWallpaperChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(i);
                }
            }
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallbackCompat callback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(callback);
            try {
                callback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                callback.onShowingStateChanged(this.mShowing);
                callback.onInputRestrictedStateChanged(this.mInputRestricted);
                callback.onTrustedChanged(this.mUpdateMonitor.getUserHasTrust(KeyguardUpdateMonitor.getCurrentUser()));
                callback.onHasLockscreenWallpaperChanged(this.mUpdateMonitor.hasLockscreenWallpaper());
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call to IKeyguardStateCallback", e);
            }
        }
    }

    public void unblockScreenOn() {
        Iterator<IKeyguardStateCallbackCompat> it = this.mKeyguardStateCallbacks.iterator();
        while (it.hasNext()) {
            IKeyguardStateCallbackCompat callback = it.next();
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken("com.android.internal.policy.IKeyguardStateCallback");
                callback.asBinder().transact(255, data, reply, 1);
                reply.readException();
            } catch (RemoteException e) {
                Log.e("MiuiKeyguardUtils", "something wrong when unblock screen on");
                e.printStackTrace();
            } catch (Throwable th) {
                data.recycle();
                reply.recycle();
                throw th;
            }
            data.recycle();
            reply.recycle();
        }
    }

    /* access modifiers changed from: private */
    public void disableFullScreenGesture(boolean disable) {
        if (this.mIsFullScreenGestureOpened) {
            Utils.updateFsgState(this.mContext, "typefrom_keyguard", disable);
        }
    }

    private boolean isSupportPickup() {
        boolean z = false;
        if (this.mSensorManager != null && isTriggerSensorEnabled()) {
            this.mWakeupAndSleepSensor = this.mSensorManager.getDefaultSensor(33171036, true);
            if (this.mWakeupAndSleepSensor != null && ("oem7 Pick Up Gesture".equalsIgnoreCase(this.mWakeupAndSleepSensor.getName()) || "pickup  Wakeup".equalsIgnoreCase(this.mWakeupAndSleepSensor.getName()))) {
                return true;
            }
            if (MiuiKeyguardUtils.isSupportPickupByMTK(this.mContext)) {
                this.mWakeupAndSleepSensor = this.mSensorManager.getDefaultSensor(22, true);
                if (this.mWakeupAndSleepSensor != null) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    private void registerWakeupAndSleepSensor() {
        if (isSupportPickup()) {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    if (KeyguardViewMediator.this.mWakeupAndSleepSensor != null) {
                        Slog.i("KeyguardViewMediator", "register pickup sensor");
                        KeyguardViewMediator.this.mSensorManager.registerListener(KeyguardViewMediator.this.mWakeupAndSleepSensorListener, KeyguardViewMediator.this.mWakeupAndSleepSensor, 3);
                    }
                }
            });
        }
    }

    private void unregisterWakeupAndSleepSensor() {
        if (this.mSensorManager != null && this.mWakeupAndSleepSensor != null) {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    Slog.i("KeyguardViewMediator", "unregister pickup sensor");
                    Sensor unused = KeyguardViewMediator.this.mWakeupAndSleepSensor = null;
                    KeyguardViewMediator.this.mSensorManager.unregisterListener(KeyguardViewMediator.this.mWakeupAndSleepSensorListener);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public boolean shouldRegisterLargeAreaSensor() {
        return "polaris".equals(miui.os.Build.DEVICE) && this.mSensorManager != null && this.mLargeAreaTouchSensor == null && !this.mHiding && this.mShowing;
    }

    private void registerLargeAreaTouchSensor() {
        if (shouldRegisterLargeAreaSensor()) {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    if (KeyguardViewMediator.this.shouldRegisterLargeAreaSensor()) {
                        Sensor unused = KeyguardViewMediator.this.mLargeAreaTouchSensor = KeyguardViewMediator.this.mSensorManager.getDefaultSensor(33171031);
                        KeyguardViewMediator.this.mSensorManager.registerListener(KeyguardViewMediator.this.mLargeAreaTouchSensorListener, KeyguardViewMediator.this.mLargeAreaTouchSensor, 3);
                    }
                }
            });
        }
    }

    private void unregisterLargeAreaTouchSensor() {
        if ("polaris".equals(miui.os.Build.DEVICE)) {
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    if (KeyguardViewMediator.this.mSensorManager != null && KeyguardViewMediator.this.mLargeAreaTouchSensor != null) {
                        KeyguardViewMediator.this.mSensorManager.unregisterListener(KeyguardViewMediator.this.mLargeAreaTouchSensorListener);
                        Sensor unused = KeyguardViewMediator.this.mLargeAreaTouchSensor = null;
                    }
                }
            });
        }
    }

    private boolean isTriggerSensorEnabled() {
        return MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), "pick_up_gesture_wakeup_mode", false, KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean isGoingToSleep() {
        return this.mGoingToSleep;
    }

    public void recordFingerprintUnlockState() {
        synchronized (this) {
            if (this.mFpAuthTime == 0) {
                this.mFpAuthTime = System.currentTimeMillis();
                this.mKeyguardGoingAwayTime = 0;
                this.mWaitFwTotalTime = 0;
            }
        }
    }

    private void resetFingerprintUnlockState() {
        synchronized (this) {
            this.mFpAuthTime = 0;
            this.mKeyguardGoingAwayTime = 0;
            this.mWaitFwTotalTime = 0;
        }
    }

    private void printFingerprintUnlockInfo(boolean waitScreenOn) {
        String fwReason = waitScreenOn ? "wait fw call onScreenTurningOn = " : "wait fw call startKeyguardExitAnimation = ";
        synchronized (this) {
            if (this.mFpAuthTime != 0) {
                long now = System.currentTimeMillis();
                StringBuilder buffer = new StringBuilder();
                buffer.append("fingerprint unlock time: ");
                buffer.append(fwReason + this.mWaitFwTotalTime + " ms ");
                buffer.append("total = " + (now - this.mFpAuthTime) + " ms");
                Slog.i("miui_keyguard_fingerprint", buffer.toString());
                resetFingerprintUnlockState();
            }
        }
    }
}
