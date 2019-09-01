package com.android.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.UserSwitchObserverCompat;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerCompat;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ComponentInfo;
import android.content.pm.ResolveInfoCompat;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManagerCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.TextureView;
import android.view.WindowManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.PhoneSignalController;
import com.android.keyguard.charge.MiuiChargeController;
import com.android.keyguard.faceunlock.FaceUnlockCallback;
import com.android.keyguard.faceunlock.FaceUnlockManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.phone.PanelBar;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.content.res.ThemeResources;
import miui.maml.ScreenContext;
import miui.maml.ScreenElementRoot;
import miui.maml.elements.AdvancedSlider;
import miui.maml.elements.ScreenElement;
import miui.maml.elements.ScreenElementFactory;
import miui.maml.util.ZipResourceLoader;
import miui.os.Build;
import miui.util.ProximitySensorWrapper;
import org.w3c.dom.Element;

public class KeyguardUpdateMonitor implements TrustManager.TrustListener {
    private static final ComponentName FALLBACK_HOME_COMPONENT = new ComponentName("com.android.settings", "com.android.settings.FallbackHome");
    private static int sCurrentUser;
    private static List<String> sGlobalDeviceFaceJiajiaSupport = new ArrayList();
    private static List<String> sGlobalDeviceSenseTimeSupport = new ArrayList();
    private static List<String> sGlobalRegionFaceJiajiaSupport = new ArrayList();
    private static List<String> sGlobalRegionSenseTimeSupport = new ArrayList();
    private static KeyguardUpdateMonitor sInstance;
    public static long sScreenTurnedOnTime;
    /* access modifiers changed from: private */
    public static int sSecondUser;
    /* access modifiers changed from: private */
    public static String sVideo24WallpaperThumnailName;
    public static boolean sWakeupByNotification;
    private Sensor mAccelerometerSensor = null;
    private AlarmManager mAlarmManager;
    private FingerprintManager.AuthenticationCallback mAuthenticationCallback = new FingerprintManager.AuthenticationCallback() {
        public void onAuthenticationFailed() {
            Slog.w("miui_keyguard_fingerprint", "onAuthenticationFailed");
            KeyguardUpdateMonitor.this.handleFingerprintAuthFailed();
        }

        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            Trace.beginSection("KeyguardUpdateMonitor#onAuthenticationSucceeded");
            Slog.i("miui_keyguard_fingerprint", "onAuthenticationSucceeded");
            KeyguardUpdateMonitor.this.handleFingerprintAuthenticated(MiuiKeyguardUtils.getAuthUserId(KeyguardUpdateMonitor.this.mContext, result.getFingerprint().getFingerId()));
            Trace.endSection();
        }

        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            KeyguardUpdateMonitor.this.handleFingerprintHelp(helpMsgId, helpString.toString());
        }

        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            KeyguardUpdateMonitor.this.handleFingerprintError(errMsgId, TextUtils.isEmpty(errString) ? "" : errString.toString());
        }

        public void onAuthenticationAcquired(int acquireInfo) {
            KeyguardUpdateMonitor.this.handleFingerprintAcquired(acquireInfo);
        }
    };
    private MiuiBleUnlockHelper.BLEUnlockState mBLEUnlockState;
    private BatteryStatus mBatteryStatus;
    private boolean mBootCompleted;
    private int mBottomRegionHeight;
    private boolean mBouncer;
    private final BroadcastReceiver mBroadcastAllReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(317, intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()), 0));
            } else if ("com.android.facelock.FACE_UNLOCK_STARTED".equals(action)) {
                Trace.beginSection("KeyguardUpdateMonitor.mBroadcastAllReceiver#onReceive ACTION_FACE_UNLOCK_STARTED");
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 1, getSendingUserId()));
                Trace.endSection();
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 0, getSendingUserId()));
            } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(334);
            } else if ("face_unlock_release".equals(action)) {
                if (MiuiKeyguardUtils.isSupportFaceUnlock(KeyguardUpdateMonitor.this.mContext)) {
                    KeyguardUpdateMonitor.this.mFaceUnlockManager.deleteFeature();
                    KeyguardUpdateMonitor.this.mFaceUnlockManager.release(false);
                    Settings.Secure.putIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlock_has_feature", 0, KeyguardUpdateMonitor.getCurrentUser());
                }
            } else if ("miui.intent.action.MIUI_REGION_CHANGED".equals(action)) {
                String unused = KeyguardUpdateMonitor.this.mCurrentRegion = Build.getRegion();
                FaceUnlockManager unused2 = KeyguardUpdateMonitor.this.mFaceUnlockManager = FaceUnlockManager.getInstance(context);
                if (Build.IS_INTERNATIONAL_BUILD && !MiuiKeyguardUtils.isSupportFaceUnlock(KeyguardUpdateMonitor.this.mContext) && KeyguardUpdateMonitor.this.hasFaceUnlockData()) {
                    Settings.Secure.putIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlock_has_feature", 0, KeyguardUpdateMonitor.getCurrentUser());
                    KeyguardUpdateMonitor.this.mFaceUnlockManager.deleteFeature();
                    KeyguardUpdateMonitor.this.mFaceUnlockManager.release();
                }
                if (Build.IS_INTERNATIONAL_BUILD && MiuiKeyguardUtils.isSupportFaceUnlock(KeyguardUpdateMonitor.this.mContext)) {
                    KeyguardUpdateMonitor.this.registerFaceUnlockContentObserver();
                }
                KeyguardUpdateMonitor.this.handleRegionChanged();
            }
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Intent intent2 = intent;
            String action = intent.getAction();
            Log.d("KeyguardUpdateMonitor", "received broadcast " + action);
            int i = -1;
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int status = intent2.getIntExtra("status", 1);
                int plugged = intent2.getIntExtra("plugged", 0);
                int level = intent2.getIntExtra("level", 0);
                int health = intent2.getIntExtra("health", 1);
                int voltage = intent2.getIntExtra("voltage", 0);
                int temperature = intent2.getIntExtra("temperature", 0);
                int maxChargingMicroAmp = intent2.getIntExtra("max_charging_current", -1);
                int maxChargingMicroVolt = intent2.getIntExtra("max_charging_voltage", -1);
                if (maxChargingMicroVolt <= 0) {
                    maxChargingMicroVolt = 5000000;
                }
                int maxChargingMicroVolt2 = maxChargingMicroVolt;
                if (maxChargingMicroAmp > 0) {
                    i = (maxChargingMicroAmp / 1000) * (maxChargingMicroVolt2 / 1000);
                }
                int maxChargingMicroWatt = i;
                Handler access$1800 = KeyguardUpdateMonitor.this.mHandler;
                int i2 = status;
                BatteryStatus batteryStatus = r7;
                int i3 = plugged;
                int i4 = maxChargingMicroVolt2;
                int i5 = maxChargingMicroAmp;
                BatteryStatus batteryStatus2 = new BatteryStatus(status, level, plugged, health, maxChargingMicroWatt, voltage, temperature);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(access$1800.obtainMessage(302, batteryStatus));
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                SimData args = SimData.fromIntent(intent);
                Log.v("KeyguardUpdateMonitor", "action " + action + " state: " + intent2.getStringExtra("ss") + " slotId: " + args.slotId + " subid: " + args.subId);
                KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, args.subId, args.slotId, args.simState).sendToTarget();
            } else if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(305, intent2.getIntExtra("android.media.EXTRA_RINGER_MODE", -1), 0));
            } else if ("android.intent.action.PHONE_STATE".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(306, intent2.getStringExtra("state")));
            } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(329);
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                KeyguardUpdateMonitor.this.dispatchBootCompleted();
            } else if ("android.intent.action.SERVICE_STATE".equals(action)) {
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                int subId = intent2.getIntExtra("subscription", -1);
                Log.v("KeyguardUpdateMonitor", "action " + action + " serviceState=" + serviceState + " subId=" + subId);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(330, subId, 0, serviceState));
            } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(500);
            }
        }
    };
    /* access modifiers changed from: private */
    public final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = Lists.newArrayList();
    private int mClockRegionHeight;
    private int mClockRegionMarginTop;
    private int mClockRegionWidth;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public String mCurrentRegion = Build.getRegion();
    private boolean mDeviceInteractive;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned;
    private ContentObserver mDeviceProvisionedObserver;
    private DisplayClientState mDisplayClientState = new DisplayClientState();
    /* access modifiers changed from: private */
    public boolean mFaceUnlockApplyLock;
    ContentObserver mFaceUnlockApplyObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
            boolean z = true;
            if (Settings.Secure.getIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlcok_apply_for_lock", 1, KeyguardUpdateMonitor.getCurrentUser()) == 0) {
                z = false;
            }
            boolean unused = keyguardUpdateMonitor.mFaceUnlockApplyLock = z;
        }
    };
    /* access modifiers changed from: private */
    public final FaceUnlockCallback mFaceUnlockCallback = new FaceUnlockCallback() {
        public void onFaceAuthenticated() {
            PanelBar.LOG((Class) getClass(), "onSuccess");
            int i = 0;
            boolean unused = KeyguardUpdateMonitor.this.mFaceUnlockStarted = false;
            if (!MiuiKeyguardUtils.isSupportScreenOnDelayed(KeyguardUpdateMonitor.this.mContext) || KeyguardUpdateMonitor.this.mScreenOn || MiuiKeyguardUtils.isScreenTurnOnDelayed()) {
                KeyguardUpdateMonitor.this.mUserFaceAuthenticated.put(0, true);
                while (true) {
                    int i2 = i;
                    if (i2 < KeyguardUpdateMonitor.this.mCallbacks.size()) {
                        KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i2)).get();
                        if (cb != null) {
                            cb.onFaceAuthenticated();
                        }
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
            } else {
                Log.e("face_unlock", "face unlock returned because screen turn off");
            }
        }

        public void onFaceHelp(int helpStringId) {
            Class<?> cls = getClass();
            PanelBar.LOG((Class) cls, "onHelp=" + KeyguardUpdateMonitor.this.mContext.getResources().getString(helpStringId));
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFaceHelp(helpStringId);
                }
            }
        }

        public void onFaceAuthFailed(boolean hasFace) {
            Class<?> cls = getClass();
            PanelBar.LOG((Class) cls, "onFailed  hasFace=" + hasFace);
            int i = 0;
            boolean unused = KeyguardUpdateMonitor.this.mFaceUnlockStarted = false;
            while (true) {
                int i2 = i;
                if (i2 < KeyguardUpdateMonitor.this.mCallbacks.size()) {
                    KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i2)).get();
                    if (cb != null) {
                        cb.onFaceAuthFailed(hasFace);
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }

        public void onFaceStart() {
            PanelBar.LOG((Class) getClass(), "onStart");
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFaceStart();
                }
            }
        }

        public void onFaceStop() {
            PanelBar.LOG((Class) getClass(), "onStop");
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onFaceStop();
                }
            }
        }

        public void onFaceLocked() {
            PanelBar.LOG((Class) getClass(), "onFaceLocked");
            int i = 0;
            boolean unused = KeyguardUpdateMonitor.this.mFaceUnlockStarted = false;
            while (true) {
                int i2 = i;
                if (i2 < KeyguardUpdateMonitor.this.mCallbacks.size()) {
                    KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i2)).get();
                    if (cb != null) {
                        cb.onFaceLocked();
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }

        public void unblockScreenOn() {
            PanelBar.LOG((Class) getClass(), "unblockScreenOn");
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.unblockScreenOn();
                }
            }
        }

        public void restartFaceUnlock() {
            PanelBar.LOG((Class) getClass(), "resstartFaceUnlock");
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.restartFaceUnlock();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mFaceUnlockCheckByNotificationScreenOn;
    ContentObserver mFaceUnlockCheckByNotificationScreenOnObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
            boolean z = false;
            if (Settings.Secure.getIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlock_by_notification_screen_on", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
                z = true;
            }
            boolean unused = keyguardUpdateMonitor.mFaceUnlockCheckByNotificationScreenOn = z;
        }
    };
    ContentObserver mFaceUnlockFeatureObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
            boolean z = false;
            if (Settings.Secure.getIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlock_has_feature", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
                z = true;
            }
            boolean unused = keyguardUpdateMonitor.mHasFaceUnlockData = z;
        }
    };
    /* access modifiers changed from: private */
    public FaceUnlockManager mFaceUnlockManager;
    private int mFaceUnlockMode = 0;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockStarted;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockSuccessStayScreen;
    ContentObserver mFaceUnlockSuccessStayScreenObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
            boolean z = false;
            if (Settings.Secure.getIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "face_unlock_success_stay_screen", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
                z = true;
            }
            boolean unused = keyguardUpdateMonitor.mFaceUnlockSuccessStayScreen = z;
        }
    };
    private SparseIntArray mFailedAttempts = new SparseIntArray();
    private CancellationSignal mFingerprintCancelSignal;
    private int mFingerprintMode = 0;
    private int mFingerprintRunningState = 0;
    private FingerprintManager mFpm;
    ContentObserver mFullScreenGestureObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            MiuiKeyguardUtils.setIsFullScreenGestureOpened(MiuiSettings.Global.getBoolean(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "force_fsg_nav_bar"));
        }
    };
    private boolean mGoingToSleep;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 500) {
                switch (i) {
                    case 301:
                        KeyguardUpdateMonitor.this.handleTimeUpdate();
                        return;
                    case 302:
                        KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) msg.obj);
                        return;
                    default:
                        switch (i) {
                            case 304:
                                KeyguardUpdateMonitor.this.handleSimStateChange(msg.arg1, msg.arg2, (IccCardConstants.State) msg.obj);
                                return;
                            case 305:
                                KeyguardUpdateMonitor.this.handleRingerModeChange(msg.arg1);
                                return;
                            case 306:
                                KeyguardUpdateMonitor.this.handlePhoneStateChanged((String) msg.obj);
                                return;
                            default:
                                switch (i) {
                                    case 308:
                                        KeyguardUpdateMonitor.this.handleDeviceProvisioned();
                                        return;
                                    case 309:
                                        KeyguardUpdateMonitor.this.handleDevicePolicyManagerStateChanged();
                                        return;
                                    case 310:
                                        KeyguardUpdateMonitor.this.handleUserSwitching(msg.arg1, (IRemoteCallback) msg.obj);
                                        return;
                                    default:
                                        switch (i) {
                                            case 312:
                                                KeyguardUpdateMonitor.this.handleKeyguardReset();
                                                return;
                                            case 313:
                                                KeyguardUpdateMonitor.this.handleBootCompleted();
                                                return;
                                            case 314:
                                                KeyguardUpdateMonitor.this.handleUserSwitchComplete(msg.arg1);
                                                return;
                                            default:
                                                switch (i) {
                                                    case 317:
                                                        KeyguardUpdateMonitor.this.handleUserInfoChanged(msg.arg1);
                                                        return;
                                                    case 318:
                                                        KeyguardUpdateMonitor.this.handleReportEmergencyCallAction();
                                                        return;
                                                    case 319:
                                                        Trace.beginSection("KeyguardUpdateMonitor#handler MSG_STARTED_WAKING_UP");
                                                        KeyguardUpdateMonitor.this.handleStartedWakingUp();
                                                        Trace.endSection();
                                                        return;
                                                    case 320:
                                                        KeyguardUpdateMonitor.this.handleFinishedGoingToSleep(msg.arg1);
                                                        return;
                                                    case 321:
                                                        KeyguardUpdateMonitor.this.handleStartedGoingToSleep(msg.arg1);
                                                        return;
                                                    case 322:
                                                        KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(msg.arg1);
                                                        return;
                                                    default:
                                                        switch (i) {
                                                            case 327:
                                                                Trace.beginSection("KeyguardUpdateMonitor#handler MSG_FACE_UNLOCK_STATE_CHANGED");
                                                                KeyguardUpdateMonitor.this.handleFaceUnlockStateChanged(msg.arg1 != 0, msg.arg2);
                                                                Trace.endSection();
                                                                return;
                                                            case 328:
                                                                KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                                                                return;
                                                            case 329:
                                                                KeyguardUpdateMonitor.this.handleAirplaneModeChanged();
                                                                return;
                                                            case 330:
                                                                KeyguardUpdateMonitor.this.handleServiceStateChange(msg.arg1, (ServiceState) msg.obj);
                                                                return;
                                                            case 331:
                                                                KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                                                                return;
                                                            case 332:
                                                                Trace.beginSection("KeyguardUpdateMonitor#handler MSG_SCREEN_TURNED_ON");
                                                                KeyguardUpdateMonitor.this.handleScreenTurnedOff();
                                                                Trace.endSection();
                                                                return;
                                                            case 333:
                                                                KeyguardUpdateMonitor.this.handleDreamingStateChanged(msg.arg1);
                                                                return;
                                                            case 334:
                                                                KeyguardUpdateMonitor.this.handleUserUnlocked();
                                                                break;
                                                            case 335:
                                                                KeyguardUpdateMonitor.this.handleShowingStateChange(msg.arg1);
                                                                return;
                                                            default:
                                                                super.handleMessage(msg);
                                                                return;
                                                        }
                                                }
                                        }
                                }
                        }
                }
            }
            KeyguardUpdateMonitor.this.handleLocaleChanged();
        }
    };
    /* access modifiers changed from: private */
    public int mHardwareUnavailableRetryCount = 0;
    /* access modifiers changed from: private */
    public boolean mHasFaceUnlockData;
    private boolean mHasLockscreenWallpaper;
    private boolean mIsFingerprintPermanentlyLockout;
    private boolean mIsFingerprintTemporarilyLockout;
    /* access modifiers changed from: private */
    public boolean mIsInSuspectMode = false;
    private boolean mIsLockScreenMagazinePkgExist = true;
    private boolean mIsPsensorDisabled = false;
    private boolean mIsSupportLockScreenMagazineLeft;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardHide = false;
    private boolean mKeyguardIsVisible;
    private KeyguardViewMediator mKeyguardMediator;
    private boolean mKeyguardOccluded;
    private boolean mKeyguardShowing;
    private boolean mKeyguardShowingAndOccluded;
    private final SensorEventListener mLightAndAccSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            boolean z = true;
            if (5 == event.sensor.getType()) {
                float unused = KeyguardUpdateMonitor.this.mLigthLux = event.values[0];
            } else if (1 == event.sensor.getType()) {
                float unused2 = KeyguardUpdateMonitor.this.mOrientationZ = event.values[2];
            }
            if (KeyguardUpdateMonitor.this.mLigthLux >= 3.0f || KeyguardUpdateMonitor.this.mOrientationZ >= 2.0f) {
                z = false;
            }
            boolean isInSuspectMode = z;
            if (isInSuspectMode != KeyguardUpdateMonitor.this.mIsInSuspectMode) {
                boolean unused3 = KeyguardUpdateMonitor.this.mIsInSuspectMode = isInSuspectMode;
                if (KeyguardUpdateMonitor.this.mSensorsChangeCallback != null) {
                    KeyguardUpdateMonitor.this.mSensorsChangeCallback.onChange(isInSuspectMode);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private boolean mLightClock = false;
    private boolean mLightKeyguardWallpaperGxzw = false;
    private boolean mLightLockScreenMagazineGlobalPre = false;
    private boolean mLightLockScreenMagazinePreSetting = false;
    private Sensor mLightSensor = null;
    private boolean mLightWallpaperBottom = false;
    private boolean mLightWallpaperStatusBar = false;
    /* access modifiers changed from: private */
    public float mLigthLux = 10.0f;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    private LockScreenMagazineWallpaperInfo mLockScreenMagazineWallpaperInfo = new LockScreenMagazineWallpaperInfo();
    private final FingerprintManager.LockoutResetCallback mLockoutResetCallback = new FingerprintManager.LockoutResetCallback() {
        public void onLockoutReset() {
            KeyguardUpdateMonitor.this.handleFingerprintLockoutReset();
            KeyguardUpdateMonitor.this.resetAllFingerprintLockout();
        }
    };
    private int mMagazinePreGlobalDesHeight;
    private int mMagazinePreGlobalDesMarginTop;
    private int mMagazinePreSettingRegionMarginEnd;
    private int mMagazinePreSettingRegionMarginTop;
    private int mMagazinePreSettingRegionWidth;
    private MiuiChargeController mMiuiChargeController;
    private boolean mNeedsSlowUnlockTransition;
    /* access modifiers changed from: private */
    public float mOrientationZ = 2.0f;
    PhoneSignalController.PhoneSignalChangeCallback mPhoneSignalChangeCallback = new PhoneSignalController.PhoneSignalChangeCallback() {
        public void onSignalChange(boolean signalAvailable) {
            for (int i = 0; i < KeyguardUpdateMonitor.this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) ((WeakReference) KeyguardUpdateMonitor.this.mCallbacks.get(i)).get();
                if (cb != null) {
                    cb.onPhoneSignalChanged(signalAvailable);
                }
            }
        }
    };
    private PhoneSignalController mPhoneSignalController;
    private int mPhoneState;
    private ProximitySensorWrapper mProximitySensorWrapper = null;
    private Runnable mRetryFingerprintAuthentication = new Runnable() {
        public void run() {
            Log.w("KeyguardUpdateMonitor", "Retrying fingerprint after HW unavailable, attempt " + KeyguardUpdateMonitor.this.mHardwareUnavailableRetryCount);
            KeyguardUpdateMonitor.this.updateFingerprintListeningState();
        }
    };
    private int mRingMode;
    /* access modifiers changed from: private */
    public boolean mScreenOn;
    private ContentObserver mSecondUserProviderObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            synchronized (KeyguardUpdateMonitor.class) {
                int unused = KeyguardUpdateMonitor.sSecondUser = Settings.Secure.getIntForUser(KeyguardUpdateMonitor.this.mContext.getContentResolver(), "second_user_id", -10000, 0);
            }
        }
    };
    private final ProximitySensorWrapper.ProximitySensorChangeListener mSensorListener = new ProximitySensorWrapper.ProximitySensorChangeListener() {
        public void onSensorChanged(boolean tooClose) {
            if (KeyguardUpdateMonitor.this.mSensorsChangeCallback != null) {
                KeyguardUpdateMonitor.this.mSensorsChangeCallback.onChange(tooClose);
            }
        }
    };
    private SensorManager mSensorManager = null;
    /* access modifiers changed from: private */
    public SensorsChangeCallback mSensorsChangeCallback = null;
    HashMap<Integer, ServiceState> mServiceStates = new HashMap<>();
    HashMap<Integer, SimData> mSimDatas = new HashMap<>();
    HashMap<Integer, Boolean> mSimStateEarlyReadyStatus = new HashMap<>();
    private int mStatusBarHeight;
    private final BroadcastReceiver mStrongAuthTimeoutReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT".equals(intent.getAction())) {
                int userId = intent.getIntExtra("com.android.systemui.USER_ID", -1);
                LockPatternUtilsCompat.requireStrongAuth(KeyguardUpdateMonitor.this.mLockPatternUtils, KeyguardViewMediator.STRONG_AUTH_REQUIRED_AFTER_TIMEOUT, userId);
                KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
            }
        }
    };
    private final StrongAuthTracker mStrongAuthTracker;
    private List<SubscriptionInfo> mSubscriptionInfo;
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        public void onSubscriptionsChanged() {
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(328);
        }
    };
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    private final BroadcastReceiver mTimeTickReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
        }
    };
    private TrustManager mTrustManager;
    /* access modifiers changed from: private */
    public SparseBooleanArray mUserFaceAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
    private SparseBooleanArray mUserFingerprintAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
    private UserManager mUserManager;
    private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();
    private int mWallpaperBlurColor = -1;
    /* access modifiers changed from: private */
    public ArrayList<WallpaperChangeCallback> mWallpaperChangeCallbacks = Lists.newArrayList();

    public static class BatteryStatus {
        public final int health;
        public final int level;
        public final int maxChargingWattage;
        public final int plugged;
        public final int status;
        public final int temperature;
        public final int voltage;

        public BatteryStatus(int status2, int level2, int plugged2, int health2, int maxChargingWattage2, int voltage2, int temperature2) {
            this.status = status2;
            this.level = level2;
            this.plugged = plugged2;
            this.health = health2;
            this.maxChargingWattage = maxChargingWattage2;
            this.voltage = voltage2;
            this.temperature = temperature2;
        }

        public boolean isPluggedIn() {
            return this.plugged == 1 || this.plugged == 2 || this.plugged == 4;
        }

        public boolean isCharged() {
            return this.status == 5 || this.level >= 100;
        }

        public boolean isBatteryLow() {
            return this.level < 20;
        }

        public final int getChargingSpeed(int slowThreshold, int fastThreshold) {
            if (this.maxChargingWattage <= 0) {
                return -1;
            }
            if (this.maxChargingWattage < slowThreshold) {
                return 0;
            }
            if (this.maxChargingWattage > fastThreshold) {
                return 2;
            }
            return 1;
        }
    }

    static class DisplayClientState {
        DisplayClientState() {
        }
    }

    private static class LockscreenElementFactory extends ScreenElementFactory {
        LockscreenElementFactory() {
        }

        public ScreenElement createInstance(Element ele, ScreenElementRoot root) {
            return ele.getTagName().equalsIgnoreCase("Unlocker") ? new AdvancedSlider(ele, root) : KeyguardUpdateMonitor.super.createInstance(ele, root);
        }
    }

    public interface SensorsChangeCallback {
        void onChange(boolean z);
    }

    private static class SimData {
        public IccCardConstants.State simState;
        public int slotId;
        public int subId;

        SimData(IccCardConstants.State state, int slot, int id) {
            this.simState = state;
            this.slotId = slot;
            this.subId = id;
        }

        static SimData fromIntent(Intent intent) {
            IccCardConstants.State absentReason;
            IccCardConstants.State state;
            IccCardConstants.State state2;
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                String stateExtra = intent.getStringExtra("ss");
                int slotId2 = intent.getIntExtra("slot", 0);
                int subId2 = intent.getIntExtra("subscription", -1);
                if ("ABSENT".equals(stateExtra)) {
                    if ("PERM_DISABLED".equals(intent.getStringExtra("reason"))) {
                        state2 = IccCardConstants.State.PERM_DISABLED;
                    } else {
                        state2 = IccCardConstants.State.ABSENT;
                    }
                    absentReason = state2;
                } else if ("READY".equals(stateExtra)) {
                    absentReason = IccCardConstants.State.READY;
                } else if ("LOCKED".equals(stateExtra)) {
                    String lockedReason = intent.getStringExtra("reason");
                    if ("PIN".equals(lockedReason)) {
                        state = IccCardConstants.State.PIN_REQUIRED;
                    } else if ("PUK".equals(lockedReason)) {
                        state = IccCardConstants.State.PUK_REQUIRED;
                    } else {
                        state = IccCardConstants.State.UNKNOWN;
                    }
                    absentReason = state;
                } else if ("NETWORK".equals(stateExtra)) {
                    absentReason = IccCardConstants.State.NETWORK_LOCKED;
                } else if ("CARD_IO_ERROR".equals(stateExtra)) {
                    absentReason = IccCardConstants.State.CARD_IO_ERROR;
                } else if ("LOADED".equals(stateExtra) || "IMSI".equals(stateExtra)) {
                    absentReason = IccCardConstants.State.READY;
                } else {
                    absentReason = IccCardConstants.State.UNKNOWN;
                }
                return new SimData(absentReason, slotId2, subId2);
            }
            throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
        }

        public String toString() {
            return "SimData{state=" + this.simState + ",slotId=" + this.slotId + ",subId=" + this.subId + "}";
        }
    }

    public class StrongAuthTracker extends AbstractStrongAuthTracker {
        public StrongAuthTracker(Context context) {
            super(context);
        }

        public boolean isUnlockingWithFingerprintAllowed() {
            return isFingerprintAllowedForUser(KeyguardUpdateMonitor.getCurrentUser());
        }

        public boolean isUnlockingWithFingerprintAllowed(int userId) {
            return isFingerprintAllowedForUser(userId);
        }

        public boolean hasUserAuthenticatedSinceBoot(int userId) {
            return (getStrongAuthForUser(userId) & 1) == 0;
        }

        public boolean hasUserAuthenticatedSinceBoot() {
            return (getStrongAuthForUser(KeyguardUpdateMonitor.getCurrentUser()) & 1) == 0;
        }

        public boolean hasOwnerUserAuthenticatedSinceBoot() {
            return (getStrongAuthForUser(0) & 1) == 0;
        }

        public void onStrongAuthRequiredChanged(int userId) {
            KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
        }

        public int getStrongAuthForUser(int userId) {
            return super.getStrongAuthForUser(userId) & -3;
        }
    }

    public interface WallpaperChangeCallback {
        void onWallpaperChange(boolean z);
    }

    public void setLockScreenMagazineWallpaperInfo(LockScreenMagazineWallpaperInfo lockScreenMagazineWallpaperInfo) {
        this.mLockScreenMagazineWallpaperInfo = lockScreenMagazineWallpaperInfo;
    }

    public LockScreenMagazineWallpaperInfo getLockScreenMagazineWallpaperInfo() {
        return this.mLockScreenMagazineWallpaperInfo;
    }

    public void setLockScreenMagazinePkgExist(boolean isExist) {
        this.mIsLockScreenMagazinePkgExist = isExist;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0016, code lost:
        updateFingerprintListeningState();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001a, code lost:
        r2.mHandler.postAtFrontOfQueue(new com.android.keyguard.KeyguardUpdateMonitor.AnonymousClass1(r2));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0024, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0014, code lost:
        if (android.os.Looper.myLooper() != r2.mHandler.getLooper()) goto L_0x001a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setKeyguardHide(boolean r3) {
        /*
            r2 = this;
            monitor-enter(r2)
            boolean r0 = r2.mKeyguardHide     // Catch:{ all -> 0x0025 }
            if (r0 != r3) goto L_0x0007
            monitor-exit(r2)     // Catch:{ all -> 0x0025 }
            return
        L_0x0007:
            r2.mKeyguardHide = r3     // Catch:{ all -> 0x0025 }
            monitor-exit(r2)     // Catch:{ all -> 0x0025 }
            android.os.Looper r0 = android.os.Looper.myLooper()
            android.os.Handler r1 = r2.mHandler
            android.os.Looper r1 = r1.getLooper()
            if (r0 != r1) goto L_0x001a
            r2.updateFingerprintListeningState()
            goto L_0x0024
        L_0x001a:
            android.os.Handler r0 = r2.mHandler
            com.android.keyguard.KeyguardUpdateMonitor$1 r1 = new com.android.keyguard.KeyguardUpdateMonitor$1
            r1.<init>()
            r0.postAtFrontOfQueue(r1)
        L_0x0024:
            return
        L_0x0025:
            r0 = move-exception
            monitor-exit(r2)     // Catch:{ all -> 0x0025 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardUpdateMonitor.setKeyguardHide(boolean):void");
    }

    private boolean isKeyguardHide() {
        boolean z;
        synchronized (this) {
            z = this.mKeyguardHide;
        }
        return z;
    }

    public boolean isLockScreenMagazinePkgExist() {
        return this.mIsLockScreenMagazinePkgExist;
    }

    public static synchronized void setCurrentUser(int currentUser) {
        synchronized (KeyguardUpdateMonitor.class) {
            sCurrentUser = currentUser;
        }
    }

    public static synchronized int getCurrentUser() {
        int i;
        synchronized (KeyguardUpdateMonitor.class) {
            i = sCurrentUser;
        }
        return i;
    }

    public static synchronized int getSecondUser() {
        int i;
        synchronized (KeyguardUpdateMonitor.class) {
            i = sSecondUser;
        }
        return i;
    }

    public void setKeyguardShowingAndOccluded(boolean isShowing, boolean occluded) {
        this.mKeyguardShowingAndOccluded = isShowing && occluded;
        this.mKeyguardOccluded = occluded;
        this.mKeyguardShowing = isShowing;
        updateFingerprintListeningState();
    }

    public boolean isKeyguardShowing() {
        return this.mKeyguardShowing;
    }

    public static synchronized boolean isOwnerUser() {
        boolean z;
        synchronized (KeyguardUpdateMonitor.class) {
            z = sCurrentUser == 0;
        }
        return z;
    }

    public void onTrustChanged(boolean enabled, int userId, int flags) {
    }

    /* access modifiers changed from: protected */
    public void handleSimSubscriptionInfoChanged() {
        Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged()");
        List<SubscriptionInfo> preSubscriptionInfos = getSubscriptionInfo(false);
        List<SubscriptionInfo> subscriptionInfos = getSubscriptionInfo(true);
        ArrayList<SubscriptionInfo> changedSubscriptions = new ArrayList<>();
        for (int i = 0; i < subscriptionInfos.size(); i++) {
            SubscriptionInfo info = subscriptionInfos.get(i);
            if (refreshSimState(info.getSubscriptionId(), info.getSimSlotIndex())) {
                changedSubscriptions.add(info);
            }
        }
        if (preSubscriptionInfos.isEmpty() != 0 && !subscriptionInfos.isEmpty() && changedSubscriptions.isEmpty()) {
            this.mKeyguardMediator.handleSimSecureStateChanged();
        }
        for (int i2 = 0; i2 < changedSubscriptions.size(); i2++) {
            SimData data = this.mSimDatas.get(Integer.valueOf(changedSubscriptions.get(i2).getSimSlotIndex()));
            for (int j = 0; j < this.mCallbacks.size(); j++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
                if (cb != null) {
                    cb.onSimStateChanged(data.subId, data.slotId, data.simState);
                }
            }
        }
        for (int j2 = 0; j2 < this.mCallbacks.size(); j2++) {
            KeyguardUpdateMonitorCallback cb2 = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j2).get();
            if (cb2 != null) {
                cb2.onRefreshCarrierInfo();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleAirplaneModeChanged() {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onAirplaneModeChanged();
            }
        }
    }

    public List<SubscriptionInfo> getSubscriptionInfo(boolean forceReload) {
        List<SubscriptionInfo> sil = this.mSubscriptionInfo;
        if (sil == null || forceReload) {
            sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (sil == null) {
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = sil;
        }
        return this.mSubscriptionInfo;
    }

    public void onTrustManagedChanged(boolean managed, int userId) {
    }

    public void onTrustError(CharSequence message) {
    }

    public void setKeyguardGoingAway(boolean goingAway) {
        this.mKeyguardGoingAway = goingAway;
    }

    private void onFingerprintAuthenticated(int userId) {
        Trace.beginSection("KeyGuardUpdateMonitor#onFingerPrintAuthenticated");
        this.mUserFingerprintAuthenticated.put(userId, true);
        this.mFingerprintCancelSignal = null;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAuthenticated(userId);
            }
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthFailed() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAuthFailed();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAcquired(int acquireInfo) {
        if (acquireInfo == 0) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onFingerprintAcquired();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthenticated(int authUserId) {
        Trace.beginSection("KeyGuardUpdateMonitor#handlerFingerPrintAuthenticated");
        try {
            int userId = ActivityManagerCompat.getService().getCurrentUser().id;
            Log.d("KeyguardUpdateMonitor", "userId: " + userId + ";currentuserId=" + getCurrentUser() + ";authUserId=" + authUserId);
            if (userId != authUserId) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint authenticated for wrong user: " + authUserId);
            }
            if (isFingerprintDisabled(authUserId)) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint disabled by DPM for userId: " + authUserId);
                return;
            }
            onFingerprintAuthenticated(authUserId);
            setFingerprintRunningState(0);
            Trace.endSection();
        } catch (RemoteException e) {
            Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
        } finally {
            setFingerprintRunningState(0);
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintHelp(int msgId, String helpString) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintHelp(msgId, helpString);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintError(int msgId, String errString) {
        int i = 0;
        if (msgId == 5 && this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(0);
            updateFingerprintListeningState();
        } else {
            setFingerprintRunningState(0);
        }
        if (msgId == 1 && this.mHardwareUnavailableRetryCount < 3) {
            this.mHardwareUnavailableRetryCount++;
            this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
            this.mHandler.postDelayed(this.mRetryFingerprintAuthentication, 3000);
        }
        if (msgId == 9) {
            LockPatternUtilsCompat.requireStrongAuth(this.mLockPatternUtils, 8, getCurrentUser());
            this.mIsFingerprintPermanentlyLockout = true;
        }
        if (msgId == 7) {
            this.mIsFingerprintTemporarilyLockout = true;
        }
        while (true) {
            int i2 = i;
            if (i2 < this.mCallbacks.size()) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i2).get();
                if (cb != null) {
                    cb.onFingerprintError(msgId, errString);
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintLockoutReset() {
        updateFingerprintListeningState();
        if (this.mKeyguardIsVisible) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onFingerprintLockoutReset();
                }
            }
        }
    }

    private void setFingerprintRunningState(int fingerprintRunningState) {
        boolean isRunning = false;
        boolean wasRunning = this.mFingerprintRunningState == 1;
        if (fingerprintRunningState == 1) {
            isRunning = true;
        }
        this.mFingerprintRunningState = fingerprintRunningState;
        if (wasRunning != isRunning) {
            notifyFingerprintRunningStateChanged();
        }
    }

    private void notifyFingerprintRunningStateChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceUnlockStateChanged(boolean running, int userId) {
        this.mUserFaceUnlockRunning.put(userId, running);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFaceUnlockStateChanged(running, userId);
            }
        }
    }

    public boolean isFaceUnlockRunning(int userId) {
        return this.mUserFaceUnlockRunning.get(userId);
    }

    public boolean isFingerprintDetectionRunning() {
        return this.mFingerprintRunningState == 1;
    }

    private boolean isTrustDisabled(int userId) {
        return isSimPinSecure();
    }

    private boolean isFingerprintDisabled(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        return !(dpm == null || (dpm.getKeyguardDisabledFeatures(null, userId) & 32) == 0) || isSimPinSecure();
    }

    public boolean getUserCanSkipBouncer(int userId) {
        return getUserHasTrust(userId) || ((this.mUserFingerprintAuthenticated.get(userId) || (this.mUserFaceAuthenticated.get(userId) && !isSimPinSecure())) && isUnlockingWithFingerprintAllowed(userId));
    }

    public boolean getUserHasTrust(int userId) {
        return !isTrustDisabled(userId) && this.mUserHasTrust.get(userId);
    }

    public boolean getUserTrustIsManaged(int userId) {
        return this.mUserTrustIsManaged.get(userId) && !isTrustDisabled(userId);
    }

    public boolean isUnlockingWithFingerprintAllowed() {
        return this.mStrongAuthTracker.isUnlockingWithFingerprintAllowed();
    }

    public boolean isUnlockingWithFingerprintAllowed(int userId) {
        return this.mStrongAuthTracker.isUnlockingWithFingerprintAllowed(userId);
    }

    public boolean needsSlowUnlockTransition() {
        return this.mNeedsSlowUnlockTransition;
    }

    public StrongAuthTracker getStrongAuthTracker() {
        return this.mStrongAuthTracker;
    }

    public void reportSuccessfulStrongAuthUnlockAttempt() {
        scheduleStrongAuthTimeout();
        if (this.mFpm != null) {
            this.mFpm.resetTimeout(null);
        }
    }

    private void scheduleStrongAuthTimeout() {
        if (Build.VERSION.SDK_INT <= 25) {
            long when = SystemClock.elapsedRealtime() + DevicePolicyManagerCompat.getRequiredStrongAuthTimeout((DevicePolicyManager) this.mContext.getSystemService("device_policy"), null, sCurrentUser);
            Intent intent = new Intent("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
            intent.putExtra("com.android.systemui.USER_ID", sCurrentUser);
            this.mAlarmManager.set(3, when, PendingIntent.getBroadcast(this.mContext, sCurrentUser, intent, 268435456));
            notifyStrongAuthStateChanged(sCurrentUser);
        }
    }

    /* access modifiers changed from: private */
    public void notifyStrongAuthStateChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStrongAuthStateChanged(userId);
            }
        }
    }

    public boolean isScreenOn() {
        return this.mScreenOn;
    }

    /* access modifiers changed from: private */
    public void registerFaceUnlockContentObserver() {
        if (MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext)) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("face_unlock_has_feature"), false, this.mFaceUnlockFeatureObserver, 0);
            this.mFaceUnlockFeatureObserver.onChange(false);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("face_unlcok_apply_for_lock"), false, this.mFaceUnlockApplyObserver, 0);
            this.mFaceUnlockApplyObserver.onChange(false);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("face_unlock_success_stay_screen"), false, this.mFaceUnlockSuccessStayScreenObserver, 0);
            this.mFaceUnlockSuccessStayScreenObserver.onChange(false);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("face_unlock_by_notification_screen_on"), false, this.mFaceUnlockCheckByNotificationScreenOnObserver, 0);
            this.mFaceUnlockCheckByNotificationScreenOnObserver.onChange(false);
        }
    }

    public boolean hasFaceUnlockData() {
        if ("ursa".equals(miui.os.Build.DEVICE)) {
            return this.mHasFaceUnlockData;
        }
        return this.mFaceUnlockManager.hasEnrolledFaces();
    }

    public boolean faceUnlockApplyLock() {
        return this.mFaceUnlockApplyLock;
    }

    public boolean shouldListenForFaceUnlock() {
        return MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext) && hasFaceUnlockData() && this.mFaceUnlockApplyLock && isOwnerUser();
    }

    public boolean isStayScreenFaceUnlockSuccess() {
        return this.mFaceUnlockSuccessStayScreen;
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    /* access modifiers changed from: protected */
    public void handleStartedWakingUp() {
        Trace.beginSection("KeyguardUpdateMonitor#handleStartedWakingUp");
        updateFingerprintListeningState();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStartedWakingUp();
            }
        }
        Trace.endSection();
    }

    /* access modifiers changed from: protected */
    public void handleStartedGoingToSleep(int arg1) {
        clearFingerprintRecognized();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStartedGoingToSleep(arg1);
            }
        }
        this.mGoingToSleep = true;
        updateFingerprintListeningState();
    }

    /* access modifiers changed from: protected */
    public void handleFinishedGoingToSleep(int arg1) {
        this.mGoingToSleep = false;
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFinishedGoingToSleep(arg1);
            }
        }
        updateFingerprintListeningState();
    }

    /* access modifiers changed from: private */
    public void handleScreenTurnedOn() {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOn();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleScreenTurnedOff() {
        this.mHardwareUnavailableRetryCount = 0;
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOff();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleDreamingStateChanged(int dreamStart) {
        int count = this.mCallbacks.size();
        boolean showingDream = true;
        if (dreamStart != 1) {
            showingDream = false;
        }
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDreamingStateChanged(showingDream);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleUserInfoChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserInfoChanged(userId);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleUserUnlocked() {
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserUnlocked();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleRegionChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onRegionChanged();
            }
        }
    }

    public String getCurrentRegion() {
        return this.mCurrentRegion;
    }

    private KeyguardUpdateMonitor(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mDeviceProvisioned = isDeviceProvisionedInSettingsDb();
        this.mStrongAuthTracker = new StrongAuthTracker(context);
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(context);
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        BatteryStatus batteryStatus = new BatteryStatus(1, 100, 0, 0, 0, 0, 0);
        this.mBatteryStatus = batteryStatus;
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction("android.intent.action.TIME_TICK");
        timeFilter.addAction("android.intent.action.TIME_SET");
        timeFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        context.registerReceiverAsUser(this.mTimeTickReceiver, UserHandle.ALL, timeFilter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, filter, null, this.mHandler);
        IntentFilter bootCompleteFilter = new IntentFilter();
        bootCompleteFilter.setPriority(1000);
        bootCompleteFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBroadcastReceiver, bootCompleteFilter, null, this.mHandler);
        IntentFilter allUserFilter = new IntentFilter();
        allUserFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        allUserFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STARTED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STOPPED");
        allUserFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        allUserFilter.addAction("android.intent.action.USER_UNLOCKED");
        allUserFilter.addAction("face_unlock_release");
        allUserFilter.addAction("miui.intent.action.MIUI_REGION_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastAllReceiver, UserHandle.ALL, allUserFilter, null, this.mHandler);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        try {
            ActivityManagerCompat.registerUserSwitchObserver(new UserSwitchObserverCompat() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(310, newUserId, 0, reply));
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, newUserId, 0));
                }
            }, "KeyguardUpdateMonitor");
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
        if (Build.VERSION.SDK_INT <= 25) {
            IntentFilter strongAuthTimeoutFilter = new IntentFilter();
            strongAuthTimeoutFilter.addAction("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
            context.registerReceiver(this.mStrongAuthTimeoutReceiver, strongAuthTimeoutFilter, "com.android.systemui.permission.SELF", null);
        }
        this.mTrustManager = (TrustManager) context.getSystemService("trust");
        this.mTrustManager.registerTrustListener(this);
        this.mLockPatternUtils = new LockPatternUtils(context);
        LockPatternUtilsCompat.registerStrongAuthTracker(this.mLockPatternUtils, this.mStrongAuthTracker);
        this.mStatusBarHeight = this.mContext.getResources().getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        updateFingerprintListeningState();
        if (this.mFpm != null) {
            this.mFpm.addLockoutResetCallback(this.mLockoutResetCallback);
        }
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("second_user_id"), false, this.mSecondUserProviderObserver, 0);
        this.mSecondUserProviderObserver.onChange(false);
        updateKeyguardWallpaperLightDimens();
        updateWallpaper(true);
        IntentFilter wallpaperChangeIntentFilter = new IntentFilter();
        wallpaperChangeIntentFilter.addAction("com.miui.keyguard.setwallpaper");
        context.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean succeed = intent.getBooleanExtra("set_lock_wallpaper_result", true);
                Log.d("KeyguardUpdateMonitor", "set_lock_wallpaper_result:" + succeed);
                if (succeed) {
                    String video24WallpaperThumnailName = intent.getStringExtra("video24_wallpaper");
                    if (!TextUtils.isEmpty(video24WallpaperThumnailName)) {
                        String unused = KeyguardUpdateMonitor.sVideo24WallpaperThumnailName = video24WallpaperThumnailName;
                    }
                    KeyguardUpdateMonitor.this.updateWallpaper(succeed);
                }
            }
        }, UserHandle.ALL, wallpaperChangeIntentFilter, null, null);
        registerFaceUnlockContentObserver();
        if (MiuiKeyguardUtils.hasNavigationBar()) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, this.mFullScreenGestureObserver);
            this.mFullScreenGestureObserver.onChange(false);
        }
        if (MiuiKeyguardUtils.supportWirelessCharge() || MiuiKeyguardUtils.supportNewChargeAnimation()) {
            this.mMiuiChargeController = new MiuiChargeController(this.mContext);
        }
        this.mIsPsensorDisabled = MiuiKeyguardUtils.isPsensorDisabled(this.mContext);
        if (this.mIsPsensorDisabled) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }
        if (miui.os.Build.IS_CM_CUSTOMIZATION_TEST) {
            this.mPhoneSignalController = new PhoneSignalController(this.mContext);
        }
    }

    /* access modifiers changed from: private */
    public void updateWallpaper(final boolean succeed) {
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... params) {
                if (succeed) {
                    ThemeResources.clearLockWallpaperCache();
                    KeyguardUpdateMonitor.this.processKeyguardWallpaper();
                }
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                Iterator it = KeyguardUpdateMonitor.this.mWallpaperChangeCallbacks.iterator();
                while (it.hasNext()) {
                    ((WallpaperChangeCallback) it.next()).onWallpaperChange(succeed);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void registerWallpaperChangeCallback(WallpaperChangeCallback callback) {
        if (!this.mWallpaperChangeCallbacks.contains(callback)) {
            this.mWallpaperChangeCallbacks.add(callback);
            callback.onWallpaperChange(false);
        }
    }

    public void unregisterWallpaperChangeCallback(WallpaperChangeCallback callback) {
        this.mWallpaperChangeCallbacks.remove(callback);
    }

    public void registerPhoneSignalChangeCallback() {
        if (this.mPhoneSignalController != null) {
            this.mPhoneSignalController.registerPhoneSignalChangeCallback(this.mPhoneSignalChangeCallback);
        }
    }

    public void unRegisterPhoneSignalChangeCallback() {
        if (this.mPhoneSignalController != null) {
            this.mPhoneSignalController.removePhoneSignalChangeCallback(this.mPhoneSignalChangeCallback);
        }
    }

    public static String getVideo24WallpaperThumnailName() {
        return sVideo24WallpaperThumnailName;
    }

    public void updateKeyguardWallpaperLightDimens() {
        Resources resources = this.mContext.getResources();
        this.mClockRegionWidth = resources.getDimensionPixelOffset(R.dimen.wallpaper_clock_region_width);
        this.mClockRegionHeight = resources.getDimensionPixelOffset(R.dimen.wallpaper_clock_region_height);
        this.mClockRegionMarginTop = resources.getDimensionPixelOffset(R.dimen.wallpaper_clock_region_margin_top);
        this.mBottomRegionHeight = resources.getDimensionPixelOffset(R.dimen.wallpaper_bottom_region_height);
        this.mMagazinePreSettingRegionWidth = resources.getDimensionPixelOffset(R.dimen.wallpaper_magazine_pre_setting_region_width);
        this.mMagazinePreSettingRegionMarginTop = resources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_setting_margin_top);
        this.mMagazinePreSettingRegionMarginEnd = resources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_setting_margin_end);
        this.mMagazinePreGlobalDesHeight = resources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_des_height);
        this.mMagazinePreGlobalDesMarginTop = resources.getDimensionPixelOffset(R.dimen.lock_screen_magazine_pre_global_des_margin_top);
    }

    /* access modifiers changed from: private */
    public void processKeyguardWallpaper() {
        Bitmap lockScreenMagazineGlobalPreBitmap;
        int globalPreHeight;
        Drawable wallpaperDrawable = KeyguardWallpaperUtils.getLockWallpaperPreview(this.mContext);
        if (wallpaperDrawable != null) {
            Resources resources = this.mContext.getResources();
            Bitmap wallpaperBmp = ((BitmapDrawable) wallpaperDrawable).getBitmap();
            int originalWidth = wallpaperBmp.getWidth();
            int originalHeight = wallpaperBmp.getHeight();
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            int screenWidth = dm.widthPixels;
            int screenHeigth = dm.heightPixels;
            float scaleWidth = (((float) originalWidth) * 1.0f) / ((float) screenWidth);
            float scaleHeight = (((float) originalHeight) * 1.0f) / ((float) screenHeigth);
            int statusBarHeight = (int) (((float) this.mStatusBarHeight) * scaleHeight);
            int statusBarHeight2 = statusBarHeight;
            Bitmap statusbarBitmap = createBitmapSafely(wallpaperBmp, 0, 0, originalWidth, statusBarHeight);
            int width = (int) (((float) this.mClockRegionWidth) * scaleWidth);
            int height = (int) (((float) this.mClockRegionHeight) * scaleHeight);
            int x = (originalWidth / 2) - (width / 2);
            Drawable drawable = wallpaperDrawable;
            Bitmap statusbarBitmap2 = statusbarBitmap;
            int i = width;
            int i2 = height;
            Bitmap clockBitmap = createBitmapSafely(wallpaperBmp, x >= 0 ? x : 0, ((int) (((float) this.mClockRegionMarginTop) * scaleHeight)) + statusBarHeight2, width, height);
            int bottomHeight = (int) (((float) this.mBottomRegionHeight) * scaleHeight);
            int i3 = bottomHeight;
            Resources resources2 = resources;
            Bitmap clockBitmap2 = clockBitmap;
            Bitmap bottomBitmap = createBitmapSafely(wallpaperBmp, 0, originalHeight - bottomHeight, originalWidth, bottomHeight);
            int preSettingWidth = (int) (((float) this.mMagazinePreSettingRegionWidth) * scaleWidth);
            int preSettingHeight = (int) (((float) this.mMagazinePreSettingRegionWidth) * scaleHeight);
            int preSettingMarginTop = (int) (((float) this.mMagazinePreSettingRegionMarginTop) * scaleHeight);
            Bitmap bottomBitmap2 = bottomBitmap;
            int preSettingMarginEnd = (int) (((float) this.mMagazinePreSettingRegionMarginEnd) * scaleWidth);
            int x2 = screenHeigth;
            int y = screenWidth;
            DisplayMetrics displayMetrics = dm;
            WindowManager windowManager = wm;
            int i4 = originalHeight;
            Bitmap lockScreenMagazinePreSettingBitmap = createBitmapSafely(wallpaperBmp, (originalWidth - preSettingWidth) - preSettingMarginEnd, preSettingMarginTop, preSettingWidth, preSettingHeight);
            int globalPreHeight2 = (int) (((float) this.mMagazinePreGlobalDesHeight) * scaleHeight);
            int i5 = preSettingMarginTop;
            int preSettingMarginTop2 = (int) (((float) this.mMagazinePreGlobalDesMarginTop) * scaleHeight);
            int i6 = preSettingHeight;
            Bitmap bottomBitmap3 = bottomBitmap2;
            int i7 = preSettingMarginEnd;
            Bitmap lockScreenMagazineGlobalPreBitmap2 = createBitmapSafely(wallpaperBmp, 0, preSettingMarginTop2, originalWidth, globalPreHeight2);
            if (statusbarBitmap2 != null) {
                this.mLightWallpaperStatusBar = MiuiKeyguardUtils.getBitmapColorMode(statusbarBitmap2, 3) != 0;
                statusbarBitmap2.recycle();
            }
            if (clockBitmap2 != null) {
                this.mLightClock = MiuiKeyguardUtils.getBitmapColorMode(clockBitmap2, 3) != 0;
                clockBitmap2.recycle();
            }
            if (bottomBitmap3 != null) {
                this.mLightWallpaperBottom = MiuiKeyguardUtils.getBitmapColorMode(bottomBitmap3, 3) != 0;
                bottomBitmap3.recycle();
            }
            if (MiuiKeyguardUtils.isGxzwSensor()) {
                MiuiGxzwUtils.caculateGxzwIconSize(this.mContext);
                int i8 = globalPreHeight2;
                Resources resources3 = resources2;
                int margin = resources3.getDimensionPixelOffset(R.dimen.wallpaper_gxzw_region_margin);
                int y2 = (int) (((float) (MiuiGxzwUtils.GXZW_ICON_Y - margin)) * scaleHeight);
                int width2 = (int) (((float) (MiuiGxzwUtils.GXZW_ICON_WIDTH + (margin * 2))) * scaleWidth);
                int height2 = (int) (((float) (MiuiGxzwUtils.GXZW_ICON_HEIGHT + (margin * 2))) * scaleHeight);
                Resources resources4 = resources3;
                globalPreHeight = 3;
                lockScreenMagazineGlobalPreBitmap = lockScreenMagazineGlobalPreBitmap2;
                int i9 = y2;
                int width3 = width2;
                int height3 = height2;
                Bitmap gxzw = createBitmapSafely(wallpaperBmp, (int) (((float) (MiuiGxzwUtils.GXZW_ICON_X - margin)) * scaleWidth), y2, width2, height2);
                if (gxzw != null) {
                    this.mLightKeyguardWallpaperGxzw = MiuiKeyguardUtils.getBitmapColorMode(gxzw, 3) != 0;
                    gxzw.recycle();
                }
                int i10 = width3;
                int i11 = height3;
            } else {
                lockScreenMagazineGlobalPreBitmap = lockScreenMagazineGlobalPreBitmap2;
                int i12 = globalPreHeight2;
                Resources resources5 = resources2;
                globalPreHeight = 3;
                int i13 = preSettingMarginTop2;
            }
            if (lockScreenMagazinePreSettingBitmap != null) {
                this.mLightLockScreenMagazinePreSetting = MiuiKeyguardUtils.getBitmapColorMode(lockScreenMagazinePreSettingBitmap, globalPreHeight) != 0;
                lockScreenMagazinePreSettingBitmap.recycle();
            }
            Bitmap lockScreenMagazineGlobalPreBitmap3 = lockScreenMagazineGlobalPreBitmap;
            if (lockScreenMagazineGlobalPreBitmap3 != null) {
                this.mLightLockScreenMagazineGlobalPre = MiuiKeyguardUtils.getBitmapColorMode(lockScreenMagazineGlobalPreBitmap3, globalPreHeight) != 0;
                lockScreenMagazineGlobalPreBitmap3.recycle();
            }
            updateWallpaperBlurColor();
            return;
        }
    }

    private Bitmap createBitmapSafely(Bitmap scr, int x, int y, int width, int height) {
        try {
            return Bitmap.createBitmap(scr, x, y, width, height);
        } catch (IllegalArgumentException e) {
            Log.e("KeyguardUpdateMonitor", "createBitmapSafely: illegal argument: bitmapWidth = " + scr.getWidth() + ", bitmapHeight = " + scr.getHeight() + ", x = " + x + ", y = " + y + ", width = " + width + ", height = " + height);
            return null;
        }
    }

    public void updateWallpaperBlurColor() {
        int maskColor;
        Drawable wallpaperDrawable = KeyguardWallpaperUtils.getLockWallpaperPreview(this.mContext);
        int bgMask = this.mContext.getResources().getColor(R.color.wallpaper_des_text_dark_color);
        Bitmap awesomeLock = getAwesomeLockScreen(this.mContext, wallpaperDrawable);
        if (awesomeLock != null) {
            maskColor = MiuiKeyguardUtils.getFastBlurColor(this.mContext, awesomeLock);
        } else {
            maskColor = MiuiKeyguardUtils.getFastBlurColor(this.mContext, wallpaperDrawable);
        }
        if (awesomeLock != null) {
            awesomeLock.recycle();
        }
        this.mWallpaperBlurColor = maskColor == -1 ? bgMask : MiuiKeyguardUtils.addTwoColor(maskColor, bgMask);
    }

    private Bitmap getAwesomeLockScreen(Context context, Drawable wallpaper) {
        Bitmap bitmap;
        if (!new File("/data/system/theme/lockscreen").exists()) {
            return null;
        }
        ZipResourceLoader resourceLoader = new ZipResourceLoader("/data/system/theme/lockscreen", "advance/");
        try {
            resourceLoader.setLocal(context.getResources().getConfiguration().locale);
            ScreenElementRoot root = new ScreenElementRoot(new ScreenContext(context, resourceLoader, new LockscreenElementFactory()));
            if (!root.load()) {
                return null;
            }
            root.init();
            Display display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            if (wallpaper == null || !(wallpaper instanceof BitmapDrawable)) {
                bitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = ((BitmapDrawable) wallpaper).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
            }
            Canvas canvas = new Canvas(bitmap);
            root.tick(SystemClock.elapsedRealtime());
            root.render(canvas);
            root.finish();
            return bitmap;
        } catch (Exception e) {
            Log.e("KeyguardUpdateMonitor", "get awesone lock screen fail", e);
            return null;
        }
    }

    public boolean isLightWallpaperStatusBar() {
        return this.mLightWallpaperStatusBar;
    }

    public boolean isLightWallpaperBottom() {
        return this.mLightWallpaperBottom;
    }

    public boolean isLightClock() {
        return this.mLightClock;
    }

    public boolean isLightWallpaperGxzw() {
        return this.mLightKeyguardWallpaperGxzw;
    }

    public boolean isLightLockScreenMagazinePreSettings() {
        return this.mLightLockScreenMagazinePreSetting;
    }

    public boolean isLightLockScreenMagazineGlobalPre() {
        return this.mLightLockScreenMagazineGlobalPre;
    }

    public int getWallpaperBlurColor() {
        return this.mWallpaperBlurColor;
    }

    public boolean isPasswordStatusEnableFaceUnlock() {
        return this.mKeyguardMediator.getViewMediatorCallback().getBouncerPromptReason() == 0;
    }

    private boolean canFaceUnlockWhenOccluded() {
        return ("perseus".equals(miui.os.Build.DEVICE) && !MiuiKeyguardUtils.isTopActivitySystemApp(this.mContext) && !MiuiKeyguardUtils.isSCSlideNotOpenCamera(this.mContext)) || (MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext) && this.mBouncer);
    }

    private boolean isSlideCoverOpened() {
        return !"perseus".equals(miui.os.Build.DEVICE) || Settings.System.getIntForUser(this.mContext.getContentResolver(), "sc_status", 0, -2) == 0;
    }

    /* access modifiers changed from: private */
    public boolean shouldStartFaceUnlock(boolean startFromLiftingCamera) {
        return ((this.mFaceUnlockCheckByNotificationScreenOn && sWakeupByNotification) || !sWakeupByNotification) && shouldListenForFaceUnlock() && getStrongAuthTracker().hasUserAuthenticatedSinceBoot() && !this.mSwitchingUser && (this.mKeyguardMediator.isStartedWakingUp() || this.mKeyguardMediator.isScreeningOn() || this.mKeyguardIsVisible) && (((this.mKeyguardOccluded && canFaceUnlockWhenOccluded()) || !this.mKeyguardOccluded) && !this.mKeyguardMediator.isHiding() && !isFingerprintUnlock() && !MiuiKeyguardUtils.isDozing() && isSlideCoverOpened() && !isSimPinSecure() && !isFaceUnlock() && isPasswordStatusEnableFaceUnlock() && (!MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext) || (MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext) && startFromLiftingCamera)));
    }

    public void startFaceUnlock() {
        startFaceUnlock(false);
    }

    public void startFaceUnlock(final boolean startFromLiftingCamera) {
        if (shouldStartFaceUnlock(startFromLiftingCamera) && !this.mFaceUnlockStarted) {
            Slog.i("face_unlock", "start face unlock ");
            sScreenTurnedOnTime = System.currentTimeMillis();
            this.mFaceUnlockStarted = true;
            this.mFaceUnlockManager.runOnFaceUnlockWorkerThread(new Runnable() {
                public void run() {
                    if (!KeyguardUpdateMonitor.this.mFaceUnlockManager.hasInit() || !KeyguardUpdateMonitor.this.shouldStartFaceUnlock(startFromLiftingCamera)) {
                        boolean unused = KeyguardUpdateMonitor.this.mFaceUnlockStarted = false;
                        KeyguardUpdateMonitor.this.printLog("in");
                        return;
                    }
                    TextureView faceUnlockView = new TextureView(KeyguardUpdateMonitor.this.mContext);
                    faceUnlockView.setSurfaceTexture(new SurfaceTexture(0));
                    KeyguardUpdateMonitor.this.mFaceUnlockManager.startFaceUnlock(faceUnlockView, KeyguardUpdateMonitor.this.mFaceUnlockCallback);
                }
            });
        } else if (isFaceUnlockApplyLock() && !MiuiKeyguardUtils.isDozing() && !this.mFaceUnlockStarted && !isFingerprintUnlock()) {
            printLog("out");
        }
    }

    /* access modifiers changed from: private */
    public void printLog(String param) {
        Slog.e("face_unlock", "keyguard update monitor, start face unlock  " + param + ";sWakeupByNotification=" + sWakeupByNotification + ";mFaceUnlockCheckByNotificationScreenOn=" + this.mFaceUnlockCheckByNotificationScreenOn + ";shouldListenForFaceUnlock=" + shouldListenForFaceUnlock() + ";isSupportFaceUnlock=" + MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext) + ";isOwnerUser=" + isOwnerUser() + ";mSwitchingUser=" + this.mSwitchingUser + ";isKeyguardHiding=" + this.mKeyguardMediator.isHiding() + ";mFaceUnlockStarted=" + this.mFaceUnlockStarted + ";isStartedWakingUp=" + this.mKeyguardMediator.isStartedWakingUp() + ";isScreeningOn=" + this.mKeyguardMediator.isScreeningOn() + ";mKeyguardIsVisible=" + this.mKeyguardIsVisible + ";faceunlock=" + isFaceUnlock());
    }

    public void stopFaceUnlock() {
        setFaceUnlockStarted(false);
        this.mFaceUnlockManager.runOnFaceUnlockWorkerThread(new Runnable() {
            public void run() {
                KeyguardUpdateMonitor.this.mFaceUnlockManager.stopFaceUnlock();
            }
        });
    }

    public void setFaceUnlockStarted(boolean faceUnlockStarted) {
        this.mFaceUnlockStarted = faceUnlockStarted;
    }

    public boolean isFaceUnlockStarted() {
        return this.mFaceUnlockStarted;
    }

    public boolean isFaceUnlockApplyLock() {
        return hasFaceUnlockData() && this.mFaceUnlockApplyLock;
    }

    public void updateFingerprintListeningState() {
        this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
        boolean shouldListenForFingerprint = shouldListenForFingerprint();
        if (this.mFingerprintRunningState == 1 && !shouldListenForFingerprint) {
            stopListeningForFingerprint();
        } else if (this.mFingerprintRunningState != 1 && shouldListenForFingerprint) {
            startListeningForFingerprint();
        }
    }

    public boolean shouldListenForFingerprint() {
        boolean shouldListen = (this.mKeyguardIsVisible || !this.mDeviceInteractive || ((this.mBouncer && !this.mKeyguardGoingAway) || this.mGoingToSleep || this.mKeyguardShowingAndOccluded)) && !this.mSwitchingUser && !isFingerprintDisabled(getCurrentUser()) && (!isKeyguardHide() || this.mGoingToSleep) && !isFingerprintUnlock() && !isFaceUnlock();
        if (!shouldListen || !this.mKeyguardShowingAndOccluded || (this.mBouncer && !this.mKeyguardGoingAway)) {
            return shouldListen;
        }
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            return true ^ MiuiKeyguardUtils.isTopActivityNeedFingerprint(this.mContext);
        }
        return true ^ MiuiKeyguardUtils.isTopActivitySystemApp(this.mContext);
    }

    private void startListeningForFingerprint() {
        if (this.mFingerprintRunningState == 2) {
            setFingerprintRunningState(3);
        } else if (this.mFingerprintRunningState != 3) {
            Log.v("KeyguardUpdateMonitor", "startListeningForFingerprint()");
            int userId = ActivityManager.getCurrentUser();
            if (isUnlockWithFingerprintPossible(userId)) {
                if (this.mFingerprintCancelSignal != null) {
                    this.mFingerprintCancelSignal.cancel();
                }
                this.mFingerprintCancelSignal = new CancellationSignal();
                this.mFpm.authenticate(null, this.mFingerprintCancelSignal, 0, this.mAuthenticationCallback, null, userId);
                setFingerprintRunningState(1);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleLocaleChanged() {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    public boolean isUnlockWithFingerprintPossible(int userId) {
        return this.mFpm != null && this.mFpm.isHardwareDetected() && !isFingerprintDisabled(userId) && this.mFpm.getEnrolledFingerprints(userId).size() > 0;
    }

    private void stopListeningForFingerprint() {
        Log.v("KeyguardUpdateMonitor", "stopListeningForFingerprint()");
        if (this.mFingerprintRunningState == 1) {
            if (this.mFingerprintCancelSignal != null) {
                this.mFingerprintCancelSignal.cancel();
            }
            this.mFingerprintCancelSignal = null;
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(2);
        }
    }

    /* access modifiers changed from: private */
    public boolean isDeviceProvisionedInSettingsDb() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private void watchForDeviceProvisioning() {
        this.mDeviceProvisionedObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                boolean unused = KeyguardUpdateMonitor.this.mDeviceProvisioned = KeyguardUpdateMonitor.this.isDeviceProvisionedInSettingsDb();
                if (KeyguardUpdateMonitor.this.mDeviceProvisioned) {
                    MiuiKeyguardUtils.setUserAuthenticatedSinceBoot();
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(308);
                }
                Log.d("KeyguardUpdateMonitor", "DEVICE_PROVISIONED state = " + KeyguardUpdateMonitor.this.mDeviceProvisioned);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean provisioned = isDeviceProvisionedInSettingsDb();
        if (provisioned != this.mDeviceProvisioned) {
            this.mDeviceProvisioned = provisioned;
            if (this.mDeviceProvisioned) {
                this.mHandler.sendEmptyMessage(308);
            }
        }
    }

    public void setHasLockscreenWallpaper(boolean hasLockscreenWallpaper) {
        if (hasLockscreenWallpaper != this.mHasLockscreenWallpaper) {
            this.mHasLockscreenWallpaper = hasLockscreenWallpaper;
            for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onHasLockscreenWallpaperChanged(hasLockscreenWallpaper);
                }
            }
        }
    }

    public boolean hasLockscreenWallpaper() {
        return this.mHasLockscreenWallpaper;
    }

    /* access modifiers changed from: protected */
    public void handleDevicePolicyManagerStateChanged() {
        updateFingerprintListeningState();
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDevicePolicyManagerStateChanged();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitching(int userId, IRemoteCallback reply) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserSwitching(userId);
            }
        }
        try {
            reply.sendResult(null);
        } catch (RemoteException e) {
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitchComplete(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserSwitchComplete(userId);
            }
        }
    }

    public void dispatchBootCompleted() {
        this.mHandler.sendEmptyMessage(313);
    }

    /* access modifiers changed from: protected */
    public void handleBootCompleted() {
        if (!this.mBootCompleted) {
            this.mBootCompleted = true;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onBootCompleted();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleDeviceProvisioned() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDeviceProvisioned();
            }
        }
        if (this.mDeviceProvisionedObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
            this.mDeviceProvisionedObserver = null;
        }
    }

    /* access modifiers changed from: protected */
    public void handlePhoneStateChanged(String newState) {
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged(" + newState + ")");
        int i = 0;
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(newState)) {
            this.mPhoneState = 0;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(newState)) {
            this.mPhoneState = 2;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(newState)) {
            this.mPhoneState = 1;
        }
        while (true) {
            int i2 = i;
            if (i2 < this.mCallbacks.size()) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i2).get();
                if (cb != null) {
                    cb.onPhoneStateChanged(this.mPhoneState);
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleRingerModeChange(int mode) {
        Log.d("KeyguardUpdateMonitor", "handleRingerModeChange(" + mode + ")");
        this.mRingMode = mode;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onRingerModeChanged(mode);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleTimeUpdate() {
        Log.d("KeyguardUpdateMonitor", "handleTimeUpdate");
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTimeChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleBatteryUpdate(BatteryStatus status) {
        Log.d("KeyguardUpdateMonitor", "handleBatteryUpdate");
        boolean batteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, status);
        this.mBatteryStatus = status;
        if (batteryUpdateInteresting) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onRefreshBatteryInfo(status);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleSimStateChange(int subId, int slotId, IccCardConstants.State state) {
        boolean changed;
        boolean simStateChanged;
        Log.d("KeyguardUpdateMonitor", "handleSimStateChange(subId=" + subId + ", slotId=" + slotId + ", state=" + state + ")");
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w("KeyguardUpdateMonitor", "invalid subId in handleSimStateChange()");
            return;
        }
        SimData data = this.mSimDatas.get(Integer.valueOf(slotId));
        if (data == null) {
            this.mSimDatas.put(Integer.valueOf(slotId), new SimData(state, slotId, subId));
            changed = true;
        } else {
            if (data.simState == state || isEarlyReportSimUnlocked(state, data.simState, slotId)) {
                simStateChanged = false;
            } else {
                simStateChanged = true;
                data.simState = state;
            }
            boolean changed2 = (!simStateChanged && data.subId == subId && data.slotId == slotId) ? false : true;
            data.subId = subId;
            data.slotId = slotId;
            changed = changed2;
        }
        if (changed && state != IccCardConstants.State.UNKNOWN) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onSimStateChanged(subId, slotId, state);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleServiceStateChange(int subId, ServiceState serviceState) {
        Log.d("KeyguardUpdateMonitor", "handleServiceStateChange(subId=" + subId + ", serviceState=" + serviceState);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w("KeyguardUpdateMonitor", "invalid subId in handleServiceStateChange()");
            return;
        }
        this.mServiceStates.put(Integer.valueOf(subId), serviceState);
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
                cb.onServiceStateChanged(subId, serviceState);
            }
        }
    }

    public void onKeyguardVisibilityChanged(boolean showing) {
        Log.d("KeyguardUpdateMonitor", "onKeyguardVisibilityChanged(" + showing + ")");
        this.mKeyguardIsVisible = showing;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardVisibilityChangedRaw(showing);
            }
        }
        updateFingerprintListeningState();
    }

    /* access modifiers changed from: private */
    public void handleKeyguardReset() {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardReset");
        updateFingerprintListeningState();
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
    }

    private boolean resolveNeedsSlowUnlockTransition() {
        if (isUserUnlocked()) {
            return false;
        }
        ComponentInfo ci = ResolveInfoCompat.getComponentInfo(this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 0));
        return FALLBACK_HOME_COMPONENT.equals(new ComponentName(ci.packageName, ci.name));
    }

    public boolean isUserUnlocked() {
        return UserManagerCompat.isUserUnlocked(this.mUserManager, getCurrentUser());
    }

    /* access modifiers changed from: private */
    public void handleKeyguardBouncerChanged(int bouncer) {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardBouncerChanged(" + bouncer + ")");
        boolean isBouncer = true;
        if (bouncer != 1) {
            isBouncer = false;
        }
        this.mBouncer = isBouncer;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardBouncerChanged(isBouncer);
            }
        }
        updateFingerprintListeningState();
    }

    /* access modifiers changed from: private */
    public void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onEmergencyCallAction();
            }
        }
    }

    private static boolean isBatteryUpdateInteresting(BatteryStatus old, BatteryStatus current) {
        boolean nowPluggedIn = current.isPluggedIn();
        boolean wasPluggedIn = old.isPluggedIn();
        boolean stateChangedWhilePluggedIn = wasPluggedIn && nowPluggedIn && old.status != current.status;
        if (wasPluggedIn != nowPluggedIn || stateChangedWhilePluggedIn) {
            return true;
        }
        if (nowPluggedIn && old.level != current.level) {
            return true;
        }
        if (nowPluggedIn || !current.isBatteryLow() || current.level == old.level) {
            return nowPluggedIn && current.maxChargingWattage != old.maxChargingWattage;
        }
        return true;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** unregister callback for " + callback);
        for (int i = this.mCallbacks.size() + -1; i >= 0; i--) {
            if (this.mCallbacks.get(i).get() == callback) {
                this.mCallbacks.remove(i);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** register callback for " + callback);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == callback) {
                Log.e("KeyguardUpdateMonitor", "Object tried to add another callback", new Exception("Called by"));
                return;
            }
        }
        this.mCallbacks.add(new WeakReference(callback));
        removeCallback(null);
        sendUpdates(callback);
    }

    public boolean isSwitchingUser() {
        return this.mSwitchingUser;
    }

    public void setSwitchingUser(boolean switching) {
        this.mSwitchingUser = switching;
        updateFingerprintListeningState();
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback callback) {
        callback.onRefreshBatteryInfo(this.mBatteryStatus);
        callback.onTimeChanged();
        callback.onRingerModeChanged(this.mRingMode);
        callback.onPhoneStateChanged(this.mPhoneState);
        callback.onRefreshCarrierInfo();
        callback.onAirplaneModeChanged();
        callback.onClockVisibilityChanged();
        for (Map.Entry<Integer, SimData> data : this.mSimDatas.entrySet()) {
            SimData state = data.getValue();
            if (state.simState != IccCardConstants.State.UNKNOWN) {
                callback.onSimStateChanged(state.subId, state.slotId, state.simState);
            }
        }
    }

    public void sendKeyguardReset() {
        this.mHandler.obtainMessage(312).sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean showingBouncer) {
        Log.d("KeyguardUpdateMonitor", "sendKeyguardBouncerChanged(" + showingBouncer + ")");
        Message message = this.mHandler.obtainMessage(322);
        message.arg1 = showingBouncer;
        message.sendToTarget();
    }

    public void reportSimUnlocked(int subId) {
        Log.v("KeyguardUpdateMonitor", "reportSimUnlocked(subId=" + subId + ")");
        int slotId = SubscriptionManagerCompat.getSlotIndex(subId);
        handleSimStateChange(subId, slotId, IccCardConstants.State.READY);
        setSimStateEarlyReady(slotId, true);
    }

    public void setSimStateEarlyReady(int slotId, boolean isEarly) {
        this.mSimStateEarlyReadyStatus.put(Integer.valueOf(slotId), Boolean.valueOf(isEarly));
    }

    private boolean isSimStateEarlyReady(int slotId) {
        return this.mSimStateEarlyReadyStatus.get(Integer.valueOf(slotId)).booleanValue();
    }

    public void reportEmergencyCallAction(boolean bypassHandler) {
        if (!bypassHandler) {
            this.mHandler.obtainMessage(318).sendToTarget();
        } else {
            handleReportEmergencyCallAction();
        }
    }

    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisioned;
    }

    public void clearFailedUnlockAttempts() {
        this.mFailedAttempts.delete(sCurrentUser);
    }

    public int getFailedUnlockAttempts(int userId) {
        return this.mFailedAttempts.get(userId, 0);
    }

    public void reportFailedStrongAuthUnlockAttempt(int userId) {
        this.mFailedAttempts.put(userId, getFailedUnlockAttempts(userId) + 1);
    }

    public void clearFingerprintRecognized() {
        this.mUserFingerprintAuthenticated.clear();
        this.mUserFaceAuthenticated.clear();
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    public boolean isSimPinSecure() {
        for (SubscriptionInfo info : getSubscriptionInfo(false)) {
            if (isSimPinSecure(getSimState(info.getSimSlotIndex()))) {
                return true;
            }
        }
        return false;
    }

    public IccCardConstants.State getSimState(int slotId) {
        if (this.mSimDatas.containsKey(Integer.valueOf(slotId))) {
            return this.mSimDatas.get(Integer.valueOf(slotId)).simState;
        }
        return IccCardConstants.State.UNKNOWN;
    }

    public boolean isOOS() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        boolean ret = true;
        for (int phoneId = 0; phoneId < phoneCount; phoneId++) {
            int[] subId = SubscriptionManager.getSubId(phoneId);
            if (subId != null && subId.length >= 1) {
                Log.d("KeyguardUpdateMonitor", "slot id:" + phoneId + " subId:" + subId[0]);
                ServiceState state = this.mServiceStates.get(Integer.valueOf(subId[0]));
                if (state != null) {
                    if (state.isEmergencyOnly()) {
                        ret = false;
                    }
                    if (!(state.getVoiceRegState() == 1 || state.getVoiceRegState() == 3)) {
                        ret = false;
                    }
                    Log.d("KeyguardUpdateMonitor", "is emergency: " + state.isEmergencyOnly());
                    Log.d("KeyguardUpdateMonitor", "voice state: " + state.getVoiceRegState());
                } else {
                    Log.d("KeyguardUpdateMonitor", "state is NULL");
                }
            }
        }
        Log.d("KeyguardUpdateMonitor", "is Emergency supported: " + ret);
        return ret;
    }

    public void setFingerprintMode(int mode) {
        this.mFingerprintMode = mode;
        updateFingerprintListeningState();
    }

    private boolean isFingerprintUnlock() {
        return this.mFingerprintMode == 5 || this.mFingerprintMode == 1 || this.mFingerprintMode == 2 || this.mFingerprintMode == 6;
    }

    public void setFaceUnlockMode(int mode) {
        this.mFaceUnlockMode = mode;
    }

    public boolean isFaceUnlock() {
        return this.mFaceUnlockMode == 5 || this.mFingerprintMode == 1 || this.mFaceUnlockMode == 2;
    }

    private boolean refreshSimState(int subId, int slotId) {
        IllegalArgumentException ex;
        boolean simStateChanged;
        try {
            ex = IccCardConstants.State.intToState(TelephonyManager.from(this.mContext).getSimState(slotId));
        } catch (IllegalArgumentException e) {
            Log.w("KeyguardUpdateMonitor", "Unknown sim state: " + simState);
            ex = IccCardConstants.State.UNKNOWN;
        }
        SimData data = this.mSimDatas.get(Integer.valueOf(slotId));
        if (data == null) {
            this.mSimDatas.put(Integer.valueOf(slotId), new SimData(ex, slotId, subId));
            return true;
        }
        boolean changed = false;
        if (data.simState == ex || isEarlyReportSimUnlocked(ex, data.simState, slotId)) {
            simStateChanged = false;
        } else {
            simStateChanged = true;
            data.simState = ex;
        }
        if (simStateChanged || data.subId != subId) {
            changed = true;
        }
        return changed;
    }

    private boolean isEarlyReportSimUnlocked(IccCardConstants.State newState, IccCardConstants.State oldState, int slotId) {
        return (newState == IccCardConstants.State.PIN_REQUIRED || newState == IccCardConstants.State.PUK_REQUIRED) && oldState == IccCardConstants.State.READY && isSimStateEarlyReady(slotId);
    }

    public static boolean isSimPinSecure(IccCardConstants.State state) {
        IccCardConstants.State simState = state;
        return simState == IccCardConstants.State.PIN_REQUIRED || simState == IccCardConstants.State.PUK_REQUIRED || simState == IccCardConstants.State.PERM_DISABLED;
    }

    public void dispatchStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchStartedGoingToSleep(int why) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(321, why, 0));
    }

    public void dispatchFinishedGoingToSleep(int why) {
        synchronized (this) {
            this.mDeviceInteractive = false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(320, why, 0));
    }

    public void dispatchScreenTurnedOn() {
        synchronized (this) {
            this.mScreenOn = true;
        }
        this.mHandler.sendEmptyMessage(331);
    }

    public void dispatchScreenTurnedOff() {
        synchronized (this) {
            this.mScreenOn = false;
        }
        this.mHandler.sendEmptyMessage(332);
    }

    public void dispatchDreamingStarted() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(333, 1, 0));
    }

    public void dispatchDreamingStopped() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(333, 0, 0));
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public int getNextSubIdForState(IccCardConstants.State state) {
        List<SubscriptionInfo> list = getSubscriptionInfo(false);
        int resultId = -1;
        int bestSlotId = Integer.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            int id = list.get(i).getSubscriptionId();
            int slotId = SubscriptionManagerCompat.getSlotIndex(id);
            if (state == getSimState(slotId) && bestSlotId > slotId) {
                resultId = id;
                bestSlotId = slotId;
            }
        }
        return resultId;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId) {
        List<SubscriptionInfo> list = getSubscriptionInfo(false);
        for (int i = 0; i < list.size(); i++) {
            SubscriptionInfo info = list.get(i);
            if (subId == info.getSubscriptionId()) {
                return info;
            }
        }
        return null;
    }

    public void setBLEUnlockState(MiuiBleUnlockHelper.BLEUnlockState state) {
        this.mBLEUnlockState = state;
        if (state == MiuiBleUnlockHelper.BLEUnlockState.SUCCEED) {
            Intent intent = new Intent("miui_keyguard_ble_unlock_succeed");
            intent.setPackage(this.mContext.getPackageName());
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    public MiuiBleUnlockHelper.BLEUnlockState getBLEUnlockState() {
        return this.mBLEUnlockState;
    }

    public void registerWirelessChargeCallback(MiuiChargeController.WirelessChargeCallback callback) {
        if (this.mMiuiChargeController != null) {
            this.mMiuiChargeController.registerWirelessChargeCallback(callback);
        }
    }

    public void unregisterWirelessChargeCallback(MiuiChargeController.WirelessChargeCallback callback) {
        if (this.mMiuiChargeController != null) {
            this.mMiuiChargeController.unregisterWirelessChargeCallback(callback);
        }
    }

    public boolean isNeedRepositionDevice() {
        if (this.mMiuiChargeController != null) {
            return this.mMiuiChargeController.isNeedRepositionDevice();
        }
        return false;
    }

    public void resetAllFingerprintLockout() {
        this.mIsFingerprintPermanentlyLockout = false;
        this.mIsFingerprintTemporarilyLockout = false;
    }

    public boolean isFingerprintTemporarilyLockout() {
        return this.mIsFingerprintTemporarilyLockout;
    }

    public void registerSeneorsForKeyguard(SensorsChangeCallback callback) {
        if (this.mKeyguardIsVisible && !isFingerprintUnlock()) {
            if (this.mIsPsensorDisabled) {
                if (this.mSensorManager != null) {
                    this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
                    this.mSensorManager.registerListener(this.mLightAndAccSensorListener, this.mLightSensor, 3);
                    this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
                    this.mSensorManager.registerListener(this.mLightAndAccSensorListener, this.mAccelerometerSensor, 3);
                    this.mSensorsChangeCallback = callback;
                }
            } else if (this.mProximitySensorWrapper == null) {
                this.mProximitySensorWrapper = new ProximitySensorWrapper(this.mContext);
                this.mProximitySensorWrapper.registerListener(this.mSensorListener);
                this.mSensorsChangeCallback = callback;
            }
        }
    }

    public void unregisterSeneorsForKeyguard() {
        if (this.mIsPsensorDisabled) {
            if (this.mSensorManager != null) {
                this.mSensorManager.unregisterListener(this.mLightAndAccSensorListener);
                this.mIsInSuspectMode = false;
                this.mSensorsChangeCallback = null;
            }
        } else if (this.mProximitySensorWrapper != null) {
            this.mProximitySensorWrapper.unregisterAllListeners();
            this.mProximitySensorWrapper = null;
            this.mSensorsChangeCallback = null;
        }
    }

    public boolean isPsensorDisabled() {
        return this.mIsPsensorDisabled;
    }

    public void setKeyguardViewMediator(KeyguardViewMediator mediator) {
        this.mKeyguardMediator = mediator;
    }

    public void updateShowingState(boolean showing) {
        Message message = this.mHandler.obtainMessage(335);
        message.arg1 = showing;
        message.sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleShowingStateChange(int showing) {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                boolean z = true;
                if (showing != 1) {
                    z = false;
                }
                cb.updateShowingStatus(z);
            }
        }
    }

    public boolean isSupportLockScreenMagazineLeft() {
        return this.mIsSupportLockScreenMagazineLeft && LockScreenMagazineUtils.supportLockScreenMagazineRegion(this.mContext);
    }

    public void setSupportLockScreenMagazineLeft(boolean isSupport) {
        this.mIsSupportLockScreenMagazineLeft = isSupport;
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onLockScreenMagazineStatusChanged();
            }
        }
    }

    public void handleBottomAreaButtonClicked(boolean isClickAnimating) {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onBottomAreaButtonClicked(isClickAnimating);
            }
        }
    }

    public void handleLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = (KeyguardUpdateMonitorCallback) this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onLockScreenMagazinePreViewVisibilityChanged(visible);
            }
        }
    }

    public int getPhoneState() {
        return this.mPhoneState;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyguardUpdateMonitor state:");
        pw.println("  SIM States:");
        Iterator<SimData> it = this.mSimDatas.values().iterator();
        while (it.hasNext()) {
            pw.println("    " + it.next().toString());
        }
        pw.println("  Service states:");
        for (Integer intValue : this.mServiceStates.keySet()) {
            int subId = intValue.intValue();
            pw.println("    " + subId + "=" + this.mServiceStates.get(Integer.valueOf(subId)));
        }
        if (this.mFpm != null && this.mFpm.isHardwareDetected()) {
            int userId = ActivityManager.getCurrentUser();
            int strongAuthFlags = this.mStrongAuthTracker.getStrongAuthForUser(userId);
            pw.println("  Fingerprint state (user=" + userId + ")");
            StringBuilder sb = new StringBuilder();
            sb.append("    allowed=");
            sb.append(isUnlockingWithFingerprintAllowed());
            pw.println(sb.toString());
            pw.println("    auth'd=" + this.mUserFingerprintAuthenticated.get(userId));
            pw.println("    authSinceBoot=" + getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
            pw.println("    disabled(DPM)=" + isFingerprintDisabled(userId));
            pw.println("    possible=" + isUnlockWithFingerprintPossible(userId));
            pw.println("    strongAuthFlags=" + Integer.toHexString(strongAuthFlags));
            pw.println("    trustManaged=" + getUserTrustIsManaged(userId));
        }
        pw.println("    supportFaceUnlock=" + MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext));
        pw.println("    hasFaceUnlockData=" + hasFaceUnlockData());
    }
}
