package com.android.keyguard.charge;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.charge.rapid.IRapidAnimationListener;
import com.android.keyguard.charge.rapid.RapidChargeView;
import com.android.keyguard.charge.rapid.WirelessRapidChargeView;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.events.ScreenOffEvent;
import com.android.systemui.events.ScreenOnEvent;
import com.android.systemui.recents.events.RecentsEventBus;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import miui.os.Build;

public class MiuiChargeController implements IRapidAnimationListener {
    private final boolean SUPPORT_NEW_ANIMATION = MiuiKeyguardUtils.supportNewChargeAnimation();
    /* access modifiers changed from: private */
    public MiuiKeyguardWirelessChargingView mBatteryChargingView;
    /* access modifiers changed from: private */
    public int mChargeDeviceType;
    private int mChargeType;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public final Runnable mDismissRunnable = new Runnable() {
        public void run() {
            if (MiuiChargeController.this.mScreenOn) {
                if (MiuiChargeController.this.mWirelessChargeView != null) {
                    MiuiChargeController.this.mWindowManager.removeView(MiuiChargeController.this.mWirelessChargeView);
                }
                View unused = MiuiChargeController.this.mWirelessChargeView = null;
            } else if (MiuiChargeController.this.mWirelessChargeView != null) {
                MiuiChargeController.this.mWirelessChargeView.setVisibility(4);
            }
            boolean unused2 = MiuiChargeController.this.mJustWirelessCharging = false;
        }
    };
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public boolean mJustWirelessCharging;
    private KeyguardManager mKeyguardManager;
    private MiuiWirelessChargeSlowlyView mMiuiWirelessChargeSlowlyView;
    /* access modifiers changed from: private */
    public boolean mNeedRepositionDevice = false;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mRapidChargeAnimationShowing = false;
    /* access modifiers changed from: private */
    public RapidChargeView mRapidChargeView;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor.BatteryStatus mSavedBatteryStatus;
    /* access modifiers changed from: private */
    public final Runnable mScreenOffRunnable = new Runnable() {
        public void run() {
            if (!MiuiChargeController.this.mNeedRepositionDevice) {
                Slog.i("MiuiChargeController", "keyguard_screen_off_reason:wireless charge");
                MiuiChargeController.this.mPowerManager.goToSleep(SystemClock.uptimeMillis());
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mScreenOn = false;
    /* access modifiers changed from: private */
    public PowerManager.WakeLock mScreenOnWakeLock;
    private final Runnable mShowSlowlyRunnable = new Runnable() {
        public void run() {
            MiuiChargeController.this.showMissedTip(true);
        }
    };
    /* access modifiers changed from: private */
    public boolean mStartingDissmissWirelessAlphaAnim = false;
    private boolean mStateInitialized;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;
    /* access modifiers changed from: private */
    public ValueAnimator mWirelessChargeAnimator;
    private List<WirelessChargeCallback> mWirelessChargeCallbackList = new ArrayList();
    /* access modifiers changed from: private */
    public int mWirelessChargeState;
    /* access modifiers changed from: private */
    public View mWirelessChargeView;
    private boolean mWirelessCharging = false;
    private boolean mWirelessOnline = false;
    /* access modifiers changed from: private */
    public boolean mWirelessRapidChargeAnimationShowing = false;
    /* access modifiers changed from: private */
    public WirelessRapidChargeView mWirelessRapidChargeView;
    private boolean pendingRapidAnimation;
    private boolean pendingWirelessRapidAnimation;

    public interface WirelessChargeCallback {
        void onNeedRepositionDevice(boolean z);
    }

    public MiuiChargeController(Context context) {
        Log.i("MiuiChargeController", "MiuiChargeController: ");
        this.mContext = context;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        this.mWirelessChargeState = -1;
        this.mChargeDeviceType = -1;
        this.mStateInitialized = false;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("miui.intent.action.ACTION_WIRELESS_TX_TYPE");
        intentFilter.addAction("miui.intent.action.ACTION_HVDCP_TYPE");
        intentFilter.addAction("miui.intent.action.ACTION_WIRELESS_POSITION");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.setPriority(1001);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Intent intent2 = intent;
                if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                    int status = intent2.getIntExtra("status", 1);
                    int plugged = intent2.getIntExtra("plugged", 0);
                    int level = intent2.getIntExtra("level", 0);
                    int health = intent2.getIntExtra("health", 1);
                    MiuiChargeController miuiChargeController = MiuiChargeController.this;
                    KeyguardUpdateMonitor.BatteryStatus batteryStatus = new KeyguardUpdateMonitor.BatteryStatus(status, level, plugged, health, 0, 0, 0);
                    KeyguardUpdateMonitor.BatteryStatus unused = miuiChargeController.mSavedBatteryStatus = batteryStatus;
                    MiuiChargeController.this.checkBatteryStatus(MiuiChargeController.this.mSavedBatteryStatus);
                } else if ("miui.intent.action.ACTION_WIRELESS_TX_TYPE".equals(intent.getAction())) {
                    int unused2 = MiuiChargeController.this.mChargeDeviceType = intent2.getIntExtra("miui.intent.extra.wireless_tx_type", -1);
                    Log.i("MiuiChargeController", "onReceive: wirelessChargeType " + MiuiChargeController.this.mChargeDeviceType);
                    MiuiChargeController.this.checkBatteryStatus(MiuiChargeController.this.mSavedBatteryStatus);
                } else if ("miui.intent.action.ACTION_HVDCP_TYPE".equals(intent.getAction())) {
                    int unused3 = MiuiChargeController.this.mChargeDeviceType = intent2.getIntExtra("miui.intent.extra.hvdcp_type", -1);
                    Log.i("MiuiChargeController", "onReceive: mChargeDeviceType " + MiuiChargeController.this.mChargeDeviceType);
                    MiuiChargeController.this.checkBatteryStatus(MiuiChargeController.this.mSavedBatteryStatus);
                } else if ("miui.intent.action.ACTION_WIRELESS_POSITION".equals(intent.getAction())) {
                    int unused4 = MiuiChargeController.this.mWirelessChargeState = intent2.getIntExtra("miui.intent.extra.wireless_position", -1);
                    Log.i("MiuiChargeController", "onReceive: mWirelessChargeState " + MiuiChargeController.this.mWirelessChargeState);
                    if (MiuiChargeController.this.mWirelessChargeState == 0) {
                        MiuiChargeController.this.setNeedRepositionDevice(true);
                        MiuiChargeController.this.showMissedTip(true);
                    } else if (MiuiChargeController.this.mWirelessChargeState == 1) {
                        MiuiChargeController.this.setNeedRepositionDevice(false);
                        MiuiChargeController.this.showMissedTip(false);
                    }
                } else if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                    MiuiChargeController.this.mHandler.removeCallbacks(MiuiChargeController.this.mScreenOffRunnable);
                    MiuiChargeController.this.dismissRapidChargeAnimation("USER_PRESENT");
                    MiuiChargeController.this.dismissWirelessRapidChargeAnimation("USER_PRESENT");
                }
            }
        }, intentFilter);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mScreenOnWakeLock = this.mPowerManager.newWakeLock(10, "wireless_charge");
        this.mScreenOnWakeLock.setReferenceCounted(false);
        RecentsEventBus.getDefault().register(this);
        this.mChargeType = -1;
    }

    public final void onBusEvent(ScreenOffEvent event) {
        onStartedGoingToSleep();
    }

    public final void onBusEvent(ScreenOnEvent event) {
        onStartedWakingUp();
    }

    public void registerWirelessChargeCallback(WirelessChargeCallback callback) {
        synchronized (this.mWirelessChargeCallbackList) {
            this.mWirelessChargeCallbackList.add(callback);
        }
        callback.onNeedRepositionDevice(this.mNeedRepositionDevice);
    }

    public void unregisterWirelessChargeCallback(WirelessChargeCallback callback) {
        synchronized (this.mWirelessChargeCallbackList) {
            this.mWirelessChargeCallbackList.remove(callback);
        }
    }

    public boolean isNeedRepositionDevice() {
        return this.mNeedRepositionDevice;
    }

    /* access modifiers changed from: private */
    public void checkBatteryStatus(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
        if (batteryStatus != null) {
            int plugged = batteryStatus.plugged;
            int status = batteryStatus.status;
            boolean z = false;
            boolean wirelessOnline = plugged == 4;
            int chargeType = checkChargeState(batteryStatus);
            boolean isWirelessCarMode = ChargeUtils.isWirelessCarMode(this.mChargeDeviceType);
            boolean isRapidCharge = false;
            boolean isSuperCharge = false;
            if (chargeType == 10) {
                isSuperCharge = ChargeUtils.isWirelessSuperRapidCharge(this.mChargeDeviceType);
            } else if (chargeType == 11) {
                isRapidCharge = ChargeUtils.isRapidCharge(this.mChargeDeviceType);
                isSuperCharge = ChargeUtils.isSuperRapidCharge(this.mChargeDeviceType);
            } else {
                this.mChargeDeviceType = -1;
            }
            Log.i("MiuiChargeController", "checkBatteryStatus: chargeType " + chargeType + " status " + status + " plugged " + plugged + " isRapidCharge " + isRapidCharge + " isSuperCharge " + isSuperCharge + " isCarMode " + isWirelessCarMode + " mChargeDeviceType " + this.mChargeDeviceType);
            if (this.mStateInitialized) {
                dealWithAnimationShow(chargeType, wirelessOnline);
                dealWithBadlyCharge(wirelessOnline, chargeType);
            }
            if (this.mRapidChargeView != null && this.mRapidChargeAnimationShowing) {
                this.mRapidChargeView.setProgress((float) batteryStatus.level);
                this.mRapidChargeView.setChargeState(isRapidCharge, isSuperCharge);
            }
            if (this.mWirelessRapidChargeView != null && this.mWirelessRapidChargeAnimationShowing) {
                this.mWirelessRapidChargeView.setProgress((float) batteryStatus.level);
                this.mWirelessRapidChargeView.setChargeState(isSuperCharge, isWirelessCarMode);
            }
            this.mWirelessOnline = wirelessOnline;
            if (chargeType == 10) {
                z = true;
            }
            this.mWirelessCharging = z;
            this.mChargeType = chargeType;
            this.mStateInitialized = true;
        }
    }

    private int checkChargeState(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
        int plugged = batteryStatus.plugged;
        int status = batteryStatus.status;
        boolean normalOnline = false;
        boolean wirelessOnline = plugged == 4;
        if (plugged == 1 || plugged == 2) {
            normalOnline = true;
        }
        if (status == 2 || status == 5) {
            if (wirelessOnline) {
                return 10;
            }
            if (normalOnline) {
                return 11;
            }
        }
        return -1;
    }

    private void dealWithAnimationShow(int chargeType, boolean wirelessOnline) {
        if (this.SUPPORT_NEW_ANIMATION) {
            if (this.mChargeType != chargeType) {
                if (chargeType == 11) {
                    showRapidChargeAnimation();
                } else if (chargeType == 10) {
                    showWirelessRapidChargeAnimation();
                } else {
                    this.pendingRapidAnimation = false;
                    this.pendingWirelessRapidAnimation = false;
                    this.mHandler.removeCallbacks(this.mScreenOffRunnable);
                    dismissRapidChargeAnimation("dealWithAnimationShow");
                    dismissWirelessRapidChargeAnimation("dealWithAnimationShow");
                }
            }
        } else if (!this.mWirelessOnline && !this.mJustWirelessCharging && wirelessOnline) {
            showWirelessChargeAnimation(this.mSavedBatteryStatus.level, this.mScreenOn);
        }
    }

    private void dealWithBadlyCharge(boolean wirelessOnline, int chargeType) {
        if (this.mWirelessOnline && !wirelessOnline) {
            int status = this.mSavedBatteryStatus.status;
            int plugged = this.mSavedBatteryStatus.plugged;
            showToast((status == 2 && (plugged == 1 || plugged == 2)) ? R.string.wireless_change_to_ac_charging : R.string.wireless_charge_stop);
            setNeedRepositionDevice(false);
            startDismissWirelessAlphaAnim();
            this.mHandler.removeCallbacks(this.mScreenOffRunnable);
        }
        if (chargeType == 11) {
            setNeedRepositionDevice(false);
            showMissedTip(false);
        }
        if (!this.mWirelessCharging && chargeType == 10 && "polaris".equals(Build.DEVICE)) {
            checkWirelessChargeEfficiency();
        }
    }

    private void notifyNeedRepositionDevice(boolean needRepositionDevice) {
        synchronized (this.mWirelessChargeCallbackList) {
            for (WirelessChargeCallback cb : this.mWirelessChargeCallbackList) {
                cb.onNeedRepositionDevice(needRepositionDevice);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setNeedRepositionDevice(boolean needRepositionDevice) {
        this.mNeedRepositionDevice = needRepositionDevice;
        notifyNeedRepositionDevice(needRepositionDevice);
    }

    private void prepareWirelessChargeView(boolean fromScreenOff) {
        if (this.mWirelessChargeView == null) {
            this.mWirelessChargeView = View.inflate(this.mContext, R.layout.keyguard_wireless_charging_layout, null);
            this.mWirelessChargeView.setSystemUiVisibility(4864);
            this.mBatteryChargingView = (MiuiKeyguardWirelessChargingView) this.mWirelessChargeView.findViewById(R.id.battery_charging_num);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2009, 84083968, -2);
            layoutParams.windowAnimations = 0;
            layoutParams.setTitle("wireless_charge");
            this.mWirelessChargeView.setAlpha(0.0f);
            this.mWindowManager.addView(this.mWirelessChargeView, layoutParams);
        }
    }

    private void showWirelessChargeAnimation(int batteryLevel, boolean screenOn) {
        Log.d("MiuiChargeController", "showWirelessChargeAnimation");
        this.mJustWirelessCharging = true;
        prepareWirelessChargeView(false);
        this.mWirelessChargeView.setAlpha(1.0f);
        this.mWirelessChargeView.setVisibility(0);
        this.mWirelessChargeAnimator = ValueAnimator.ofInt(new int[]{0, 9240});
        this.mWirelessChargeAnimator.setDuration((long) 9240);
        this.mWirelessChargeAnimator.setInterpolator(new LinearInterpolator());
        if (!screenOn && !this.mNeedRepositionDevice) {
            this.mHandler.postDelayed(this.mScreenOffRunnable, (long) (9240 - 200));
        }
        this.mBatteryChargingView.setScreenStateWhenStartAnim(screenOn);
        this.mWirelessChargeAnimator.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                Log.i("MiuiChargeController", "mScreenOnWakeLock onAnimationStart: acquire");
                MiuiChargeController.this.mScreenOnWakeLock.acquire();
            }

            public void onAnimationRepeat(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = MiuiChargeController.this.mWirelessChargeAnimator = null;
                Log.i("MiuiChargeController", "mScreenOnWakeLock onAnimationEnd: release");
                MiuiChargeController.this.mScreenOnWakeLock.release();
                MiuiChargeController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                MiuiChargeController.this.startDismissWirelessAlphaAnim();
            }

            public void onAnimationCancel(Animator animation) {
            }
        });
        this.mWirelessChargeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiChargeController.this.mBatteryChargingView.setTime(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        this.mWirelessChargeAnimator.start();
        this.mWirelessChargeView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MiuiChargeController.this.startDismissWirelessAlphaAnim();
                MiuiChargeController.this.mHandler.removeCallbacks(MiuiChargeController.this.mScreenOffRunnable);
            }
        });
        this.mBatteryChargingView.setChargingProgress(batteryLevel);
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive()) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:WIRELESS_CHARGE");
        }
    }

    /* access modifiers changed from: private */
    public void startDismissWirelessAlphaAnim() {
        if (this.mJustWirelessCharging && !this.mStartingDissmissWirelessAlphaAnim && this.mWirelessChargeView != null) {
            if (this.mWirelessChargeAnimator != null && this.mWirelessChargeAnimator.isStarted()) {
                this.mWirelessChargeAnimator.cancel();
            }
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this.mWirelessChargeView, "alpha", new float[]{1.0f, 0.0f}).setDuration(200);
            alphaAnimator.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    boolean unused = MiuiChargeController.this.mStartingDissmissWirelessAlphaAnim = false;
                    MiuiChargeController.this.mHandler.post(MiuiChargeController.this.mDismissRunnable);
                }

                public void onAnimationCancel(Animator animation) {
                }
            });
            this.mStartingDissmissWirelessAlphaAnim = true;
            alphaAnimator.start();
        }
    }

    public void onRapidAnimationStart(int type) {
        Log.i("MiuiChargeController", "onRapidAnimationStart: " + type);
    }

    public void onRapidAnimationEnd(int type) {
        Log.i("MiuiChargeController", "onRapidAnimationEnd: " + type);
        Log.i("MiuiChargeController", "mScreenOnWakeLock onRapidAnimationEnd: release");
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        this.mScreenOnWakeLock.release();
    }

    public void onRapidAnimationDismiss(int type) {
        Log.i("MiuiChargeController", "onRapidAnimationDismiss: ");
        if (type == 11) {
            this.mRapidChargeAnimationShowing = false;
        } else if (type == 10) {
            this.mWirelessRapidChargeAnimationShowing = false;
        }
        if (!this.SUPPORT_NEW_ANIMATION) {
            return;
        }
        if (this.pendingRapidAnimation) {
            this.pendingRapidAnimation = false;
            showRapidChargeAnimation();
        } else if (this.pendingWirelessRapidAnimation) {
            this.pendingWirelessRapidAnimation = false;
            showWirelessRapidChargeAnimation();
        }
    }

    private void showRapidChargeAnimation() {
        Log.i("MiuiChargeController", "showRapidChargeAnimation: ");
        if (this.SUPPORT_NEW_ANIMATION) {
            if (!this.mKeyguardManager.isKeyguardLocked()) {
                Log.i("MiuiChargeController", "showRapidChargeAnimation: isKeyguardLocked false");
                return;
            }
            this.mHandler.removeCallbacks(this.mScreenOffRunnable);
            if (this.mWirelessRapidChargeAnimationShowing) {
                this.pendingRapidAnimation = true;
                dismissWirelessRapidChargeAnimation("showRapidChargeAnimation");
            } else if (!this.mRapidChargeAnimationShowing) {
                prepareRapidChargeView();
                this.mRapidChargeAnimationShowing = true;
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().setShowingChargeAnimationWindow(true);
                }
                this.mRapidChargeView.zoomLarge();
                if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive()) {
                    this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:RAPID_CHARGE");
                }
                Log.i("MiuiChargeController", "mScreenOnWakeLock showRapidChargeAnimation: acquire");
                this.mScreenOnWakeLock.acquire(20000);
                Log.i("MiuiChargeController", "showRapidChargeAnimation: mScreenOn " + this.mScreenOn);
                if (!this.mScreenOn && !this.mNeedRepositionDevice) {
                    this.mHandler.postDelayed(this.mScreenOffRunnable, 20000);
                } else if (!this.mNeedRepositionDevice) {
                    Log.i("MiuiChargeController", "showRapidChargeAnimation: mKeyguardMediator.isShowing()");
                    this.mHandler.postDelayed(this.mScreenOffRunnable, 20000);
                }
            }
        }
    }

    private void prepareRapidChargeView() {
        if (this.SUPPORT_NEW_ANIMATION) {
            if (this.mRapidChargeView == null) {
                this.mRapidChargeView = new RapidChargeView(this.mContext);
                this.mRapidChargeView.setScreenOn(this.mScreenOn);
                this.mRapidChargeView.setRapidAnimationListener(this);
                this.mRapidChargeView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        MiuiChargeController.this.dismissRapidChargeAnimation("onClick");
                        MiuiChargeController.this.mHandler.removeCallbacks(MiuiChargeController.this.mScreenOffRunnable);
                    }
                });
            }
            this.mRapidChargeView.setScreenOn(this.mScreenOn);
            this.mRapidChargeView.addToWindow("prepareRapidChargeView");
        }
    }

    /* access modifiers changed from: private */
    public void dismissRapidChargeAnimation(String reason) {
        Log.i("MiuiChargeController", "dismissRapidChargeAnimation: " + reason);
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setShowingChargeAnimationWindow(false);
        }
        if (this.SUPPORT_NEW_ANIMATION && this.mRapidChargeAnimationShowing) {
            if (this.mRapidChargeView != null) {
                RapidChargeView rapidChargeView = this.mRapidChargeView;
                rapidChargeView.startDismiss("dismissRapidChargeAnimation reason " + reason);
            }
            this.mRapidChargeAnimationShowing = false;
        }
    }

    private void showWirelessRapidChargeAnimation() {
        Log.i("MiuiChargeController", "showWirelessRapidChargeAnimation: ");
        if (this.SUPPORT_NEW_ANIMATION) {
            this.mHandler.removeCallbacks(this.mScreenOffRunnable);
            if (this.mRapidChargeAnimationShowing) {
                this.pendingWirelessRapidAnimation = true;
                dismissRapidChargeAnimation("showWirelessRapidChargeAnimation");
            } else if (!this.mWirelessRapidChargeAnimationShowing) {
                prepareWirelessRapidChargeView();
                this.mWirelessRapidChargeAnimationShowing = true;
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().setShowingChargeAnimationWindow(true);
                }
                this.mWirelessRapidChargeView.zoomLarge(this.mScreenOn);
                if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive()) {
                    this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:WIRELESS_RAPID_CHARGE");
                }
                Log.i("MiuiChargeController", "mScreenOnWakeLock showWirelessRapidChargeAnimation: acquire");
                this.mScreenOnWakeLock.acquire(20000);
                Log.i("MiuiChargeController", "showWirelessRapidChargeAnimation: mScreenOn " + this.mScreenOn);
                if (!this.mScreenOn && !this.mNeedRepositionDevice) {
                    this.mHandler.postDelayed(this.mScreenOffRunnable, 20000);
                } else if (this.mKeyguardManager.isKeyguardLocked() && !this.mNeedRepositionDevice) {
                    Log.i("MiuiChargeController", "showWirelessRapidChargeAnimation: mKeyguardMediator.isShowing()");
                    this.mHandler.postDelayed(this.mScreenOffRunnable, 20000);
                }
            }
        }
    }

    private void prepareWirelessRapidChargeView() {
        if (this.SUPPORT_NEW_ANIMATION) {
            if (this.mWirelessRapidChargeView == null) {
                this.mWirelessRapidChargeView = new WirelessRapidChargeView(this.mContext);
                this.mWirelessRapidChargeView.setScreenOn(this.mScreenOn);
                this.mWirelessRapidChargeView.setRapidAnimationListener(this);
                this.mWirelessRapidChargeView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        MiuiChargeController.this.dismissWirelessRapidChargeAnimation("onClick");
                        MiuiChargeController.this.mHandler.removeCallbacks(MiuiChargeController.this.mScreenOffRunnable);
                    }
                });
            }
            this.mWirelessRapidChargeView.setScreenOn(this.mScreenOn);
            this.mWirelessRapidChargeView.addToWindow("prepareWirelessRapidChargeView");
        }
    }

    /* access modifiers changed from: private */
    public void dismissWirelessRapidChargeAnimation(String reason) {
        Log.i("MiuiChargeController", "dismissWirelessRapidChargeAnimation: " + reason);
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setShowingChargeAnimationWindow(false);
        }
        if (this.SUPPORT_NEW_ANIMATION && this.mWirelessRapidChargeAnimationShowing) {
            if (this.mWirelessRapidChargeView != null) {
                WirelessRapidChargeView wirelessRapidChargeView = this.mWirelessRapidChargeView;
                wirelessRapidChargeView.startDismiss("dismissWirelessRapidChargeAnimation for " + reason);
            }
            this.mWirelessRapidChargeAnimationShowing = false;
        }
    }

    private void showToast(int res) {
        Util.showSystemOverlayToast(this.mContext, res, 1);
    }

    private void checkWirelessChargeEfficiency() {
        new AsyncTask<Void, Void, Integer>() {
            /* access modifiers changed from: protected */
            public Integer doInBackground(Void... params) {
                int value = -1;
                try {
                    FileReader reader = new FileReader("/sys/class/power_supply/wireless/signal_strength");
                    value = reader.read();
                    reader.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return Integer.valueOf(value);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Integer value) {
                Log.i("MiuiChargeController", "checkWirelessChargeEfficiency: value = " + value);
                if (value.intValue() == 48) {
                    MiuiChargeController.this.checkIfShowWirelessChargeSlowly();
                    MiuiChargeController.this.setNeedRepositionDevice(true);
                } else if (value.intValue() == 49) {
                    MiuiChargeController.this.showMissedTip(false);
                } else if (value.intValue() != 50) {
                    Log.e("MiuiChargeController", "impossible value=" + value);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void checkIfShowWirelessChargeSlowly() {
        new AsyncTask<Void, Void, Boolean>() {
            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... params) {
                boolean z = false;
                SharedPreferences sp = MiuiChargeController.this.mContext.getSharedPreferences("wireless_charge", 0);
                if ("polaris".equals(Build.DEVICE) && sp.getBoolean("show_dialog", true)) {
                    z = true;
                }
                return Boolean.valueOf(z);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean show) {
                if (show.booleanValue()) {
                    MiuiChargeController.this.showWirelessChargeSlowly();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void showWirelessChargeSlowly() {
        this.mHandler.postDelayed(this.mShowSlowlyRunnable, 2000);
    }

    private void onStartedGoingToSleep() {
        this.mScreenOn = false;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setShowingChargeAnimationWindow(false);
        }
        if (this.SUPPORT_NEW_ANIMATION) {
            showMissedTip(false);
            prepareRapidChargeView();
            prepareWirelessRapidChargeView();
            dismissRapidChargeAnimation("screen off");
            dismissWirelessRapidChargeAnimation("screen off");
        } else {
            prepareWirelessChargeView(true);
            startDismissWirelessAlphaAnim();
        }
        this.mHandler.removeCallbacks(this.mScreenOffRunnable);
    }

    private void onStartedWakingUp() {
        this.mScreenOn = true;
        if (this.mRapidChargeView != null) {
            this.mRapidChargeView.setScreenOn(true);
        }
        if (this.mWirelessRapidChargeView != null) {
            this.mWirelessRapidChargeView.setScreenOn(true);
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                if (MiuiChargeController.this.mWirelessChargeView != null && !MiuiChargeController.this.mJustWirelessCharging) {
                    MiuiChargeController.this.mWindowManager.removeView(MiuiChargeController.this.mWirelessChargeView);
                    View unused = MiuiChargeController.this.mWirelessChargeView = null;
                }
                if (MiuiChargeController.this.mRapidChargeView != null && !MiuiChargeController.this.mRapidChargeAnimationShowing) {
                    MiuiChargeController.this.mRapidChargeView.removeFromWindow("onStartedWakingUp");
                }
                if (MiuiChargeController.this.mWirelessRapidChargeView != null && !MiuiChargeController.this.mWirelessRapidChargeAnimationShowing) {
                    MiuiChargeController.this.mWirelessRapidChargeView.removeFromWindow("onStartedWakingUp");
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void showMissedTip(boolean show) {
        if (show) {
            if (this.mMiuiWirelessChargeSlowlyView == null) {
                this.mMiuiWirelessChargeSlowlyView = new MiuiWirelessChargeSlowlyView(this.mContext, !this.SUPPORT_NEW_ANIMATION);
            }
            this.mMiuiWirelessChargeSlowlyView.show();
            return;
        }
        this.mHandler.removeCallbacks(this.mShowSlowlyRunnable);
        if (this.mMiuiWirelessChargeSlowlyView != null) {
            this.mMiuiWirelessChargeSlowlyView.dismiss();
        }
    }
}
