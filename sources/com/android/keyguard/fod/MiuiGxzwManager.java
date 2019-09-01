package com.android.keyguard.fod;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.plugins.R;
import com.android.systemui.statusbar.phone.NotificationPanelView;

public class MiuiGxzwManager extends Binder {
    private static MiuiGxzwManager sService;
    /* access modifiers changed from: private */
    public final boolean SUPPORT_FOD_IN_BOUNCER;
    private int mAuthFingerprintId;
    /* access modifiers changed from: private */
    public boolean mBouncer;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public Context mContext;
    private PowerManager.WakeLock mDrawWakeLock;
    private final boolean mEnableFastUnlock;
    /* access modifiers changed from: private */
    public boolean mFingerprintLockout;
    private int mGxzwUnlockMode;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIgnoreFocusChange;
    private Runnable mIgnoreFocusRunnable;
    private IntentFilter mIntentFilter;
    private boolean mKeyguardAuthen;
    private boolean mKeyguardShow;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback;
    /* access modifiers changed from: private */
    public KeyguardViewMediator mKeyguardViewMediator;
    /* access modifiers changed from: private */
    public MiuiGxzwIconView mMiuiGxzwIconView;
    /* access modifiers changed from: private */
    public MiuiGxzwOverlayView mMiuiGxzwOverlayView;
    private NotificationPanelView mNotificationPanelView;
    private KeyguardSecurityModel.SecurityMode mSecurityMode;
    private boolean mShouldShowGxzwIconInKeyguard;
    private boolean mShowLockoutView;
    /* access modifiers changed from: private */
    public boolean mShowed;
    private volatile boolean mShowingChargeAnimationWindow;

    public static void init(Context context) {
        try {
            sService = new MiuiGxzwManager(context);
            ServiceManager.addService("android.app.fod.ICallback", sService);
            Log.d("MiuiGxzwManager", "add MiuiGxzwManager successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MiuiGxzwManager", "add MiuiGxzwManager fail");
        }
    }

    public static MiuiGxzwManager getInstance() {
        return sService;
    }

    public MiuiGxzwManager(Context context) {
        this.SUPPORT_FOD_IN_BOUNCER = !"equuleus".equals(Build.DEVICE) && !"ursa".equals(Build.DEVICE);
        this.mShowed = false;
        this.mShouldShowGxzwIconInKeyguard = true;
        this.mKeyguardAuthen = false;
        this.mBouncer = false;
        this.mShowingChargeAnimationWindow = false;
        this.mGxzwUnlockMode = 0;
        this.mIgnoreFocusChange = false;
        this.mAuthFingerprintId = 0;
        this.mSecurityMode = KeyguardSecurityModel.SecurityMode.None;
        this.mShowLockoutView = false;
        this.mFingerprintLockout = false;
        this.mIgnoreFocusRunnable = new Runnable() {
            public void run() {
                boolean unused = MiuiGxzwManager.this.mIgnoreFocusChange = false;
                MiuiGxzwManager.this.updateGxzwState();
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                boolean z = true;
                switch (msg.what) {
                    case 1001:
                        MiuiGxzwManager.this.setKeyguardAuthen(KeyguardUpdateMonitor.getInstance(MiuiGxzwManager.this.mContext).isFingerprintDetectionRunning());
                        MiuiGxzwManager miuiGxzwManager = MiuiGxzwManager.this;
                        if (msg.arg1 != 1) {
                            z = false;
                        }
                        miuiGxzwManager.showGxzwView(z);
                        MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(false);
                        MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(false);
                        return;
                    case 1002:
                        MiuiGxzwManager.this.dismissGxzwView();
                        MiuiGxzwManager.this.setKeyguardAuthen(false);
                        MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(false);
                        MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(false);
                        return;
                    case 1003:
                        if (!MiuiGxzwManager.this.getKeyguardAuthen()) {
                            MiuiGxzwManager.this.setKeyguardAuthen(false);
                            MiuiGxzwManager.this.dismissGxzwView();
                            return;
                        }
                        return;
                    case 1004:
                        if (!MiuiGxzwManager.this.getKeyguardAuthen()) {
                            MiuiGxzwManager.this.setKeyguardAuthen(false);
                            MiuiGxzwManager.this.dismissGxzwView();
                            MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(false);
                            MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(false);
                            return;
                        }
                        return;
                    case 1005:
                        MiuiGxzwManager.this.setKeyguardAuthen(false);
                        MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(true);
                        MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(true);
                        MiuiGxzwManager.this.showGxzwView(false);
                        return;
                    case 1006:
                        if (msg.arg1 == 5 && MiuiGxzwManager.this.mShowed && !MiuiGxzwManager.this.getKeyguardAuthen()) {
                            MiuiGxzwManager.this.dismissGxzwView();
                            MiuiGxzwManager.this.setKeyguardAuthen(false);
                            MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(false);
                            MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(false);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            private final int FINGERPRINT_ERROR_LOCKOUT = 7;
            private final int FINGERPRINT_ERROR_LOCKOUT_PERMANENT_FOR_O = 9;
            private Runnable mDelayRunnable = new Runnable() {
                public void run() {
                    boolean unused = MiuiGxzwManager.this.mFingerprintLockout = false;
                    if (MiuiGxzwManager.this.SUPPORT_FOD_IN_BOUNCER) {
                        MiuiGxzwManager.this.updateGxzwState();
                    }
                }
            };

            public void onKeyguardBouncerChanged(boolean bouncer) {
                super.onKeyguardBouncerChanged(bouncer);
                Log.d("MiuiGxzwManager", "onKeyguardBouncerChanged: bouncer = " + bouncer);
                boolean unused = MiuiGxzwManager.this.mBouncer = bouncer;
                MiuiGxzwManager.this.updateGxzwState();
                if (MiuiGxzwManager.this.SUPPORT_FOD_IN_BOUNCER && KeyguardUpdateMonitor.getInstance(MiuiGxzwManager.this.mContext).isDeviceInteractive()) {
                    MiuiGxzwManager.this.mMiuiGxzwIconView.refreshIcon();
                }
            }

            public void onScreenTurnedOn() {
                super.onScreenTurnedOn();
                Log.d("MiuiGxzwManager", "onScreenTurnedOn");
                MiuiGxzwManager.this.mMiuiGxzwOverlayView.onScreenTurnedOn();
                MiuiGxzwManager.this.mMiuiGxzwIconView.onScreenTurnedOn();
            }

            public void onStartedGoingToSleep(int why) {
                super.onStartedGoingToSleep(why);
                Log.d("MiuiGxzwManager", "onStartedGoingToSleep");
                MiuiGxzwManager.this.ignoreFocusChangeForWhile();
                MiuiGxzwManager.this.mMiuiGxzwOverlayView.onStartedGoingToSleep();
                MiuiGxzwManager.this.mMiuiGxzwIconView.onStartedGoingToSleep();
            }

            public void onFingerprintError(int msgId, String errString) {
                super.onFingerprintError(msgId, errString);
                Log.d("MiuiGxzwManager", "onFingerprintError: msgId = " + msgId + ", errString = " + errString);
                if ((msgId == 7 || msgId == 9) && !MiuiGxzwManager.this.mShowed) {
                    MiuiGxzwManager.this.showGxzwInKeyguardWhenLockout();
                }
                if (msgId == 7 || msgId == 9) {
                    MiuiGxzwManager.this.mHandler.removeCallbacks(this.mDelayRunnable);
                    boolean unused = MiuiGxzwManager.this.mFingerprintLockout = true;
                    if (MiuiGxzwManager.this.SUPPORT_FOD_IN_BOUNCER) {
                        MiuiGxzwManager.this.updateGxzwState();
                    }
                }
            }

            public void onFingerprintRunningStateChanged(boolean running) {
                super.onFingerprintRunningStateChanged(running);
                if (running) {
                    MiuiGxzwManager.this.mHandler.removeCallbacks(this.mDelayRunnable);
                    MiuiGxzwManager.this.mHandler.postDelayed(this.mDelayRunnable, 200);
                }
            }

            public void onFingerprintLockoutReset() {
                super.onFingerprintLockoutReset();
                MiuiGxzwManager.this.mHandler.removeCallbacks(this.mDelayRunnable);
                MiuiGxzwManager.this.mHandler.postDelayed(this.mDelayRunnable, 200);
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                    MiuiGxzwManager.this.dismissGxzwView();
                    MiuiGxzwManager.this.mMiuiGxzwIconView.setEnrolling(false);
                    MiuiGxzwManager.this.mMiuiGxzwOverlayView.setEnrolling(false);
                }
            }
        };
        this.mContext = context;
        this.mMiuiGxzwOverlayView = new MiuiGxzwOverlayView(this.mContext);
        this.mMiuiGxzwIconView = new MiuiGxzwIconView(this.mContext);
        this.mMiuiGxzwIconView.setCollectGxzwListener(this.mMiuiGxzwOverlayView);
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        monitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mDrawWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(128, "gxzw");
        int id = this.mContext.getResources().getIdentifier("config_displayBlanksAfterDoze", "bool", "android");
        this.mEnableFastUnlock = id > 0 && !this.mContext.getResources().getBoolean(id);
        if (monitor.isFingerprintDetectionRunning()) {
            dealCallback(1, 0);
        }
    }

    public void setKeyguardViewMediator(KeyguardViewMediator kvw) {
        this.mKeyguardViewMediator = kvw;
    }

    public void startDozing() {
        Log.i("MiuiGxzwManager", "startDozing");
        this.mMiuiGxzwOverlayView.startDozing();
        this.mMiuiGxzwIconView.startDozing();
    }

    public void stopDozing() {
        Log.i("MiuiGxzwManager", "stopDozing");
        ignoreFocusChangeForWhile();
        this.mMiuiGxzwOverlayView.stopDozing();
        this.mMiuiGxzwIconView.stopDozing();
    }

    public void onKeyguardShow() {
        Log.d("MiuiGxzwManager", "onKeyguardShow");
        this.mKeyguardShow = true;
        setGxzwUnlockMode(0);
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isFingerprintDetectionRunning() && !this.mShowed) {
            this.mHandler.removeMessages(1001);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1001, 0, 0));
        }
    }

    public void onKeyguardHide() {
        Log.d("MiuiGxzwManager", "onKeyguardHide");
        this.mKeyguardShow = false;
        dismissGxzwView();
        this.mShouldShowGxzwIconInKeyguard = true;
    }

    public void dismissGxzwIconView(boolean dismiss) {
        Log.i("MiuiGxzwManager", "dismissGxzwIconView: dismiss = " + dismiss);
        if (this.mShouldShowGxzwIconInKeyguard != (!dismiss) && this.mKeyguardShow) {
            this.mShouldShowGxzwIconInKeyguard = !dismiss;
            if (this.mShowed && getKeyguardAuthen()) {
                if (dismiss) {
                    this.mMiuiGxzwIconView.dismiss();
                } else {
                    this.mMiuiGxzwIconView.show(false);
                }
            }
        }
    }

    public boolean isBouncer() {
        return this.mBouncer;
    }

    public synchronized void resetGxzwUnlockMode() {
        setGxzwUnlockMode(0);
    }

    private synchronized void setGxzwUnlockMode(int mode) {
        this.mGxzwUnlockMode = mode;
    }

    private synchronized void setGxzwAuthFingerprintID(int fingerID) {
        this.mAuthFingerprintId = fingerID;
    }

    public synchronized int getGxzwUnlockMode() {
        return this.mGxzwUnlockMode;
    }

    public synchronized int getGxzwAuthFingerprintID() {
        return this.mAuthFingerprintId;
    }

    public synchronized boolean isUnlockByGxzw() {
        boolean z;
        z = true;
        if (!(this.mGxzwUnlockMode == 1 || this.mGxzwUnlockMode == 2)) {
            z = false;
        }
        return z;
    }

    public boolean isFodFastUnlock() {
        return this.mEnableFastUnlock && isUnlockByGxzw() && !isBouncer() && this.mKeyguardViewMediator.isShowingAndNotOccluded();
    }

    public boolean isShowFingerprintIcon() {
        return KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockWithFingerprintPossible(ActivityManager.getCurrentUser()) && this.mMiuiGxzwIconView.isShowFingerprintIcon();
    }

    public void showGxzwInKeyguardWhenLockout() {
        if (!this.mShowed && this.mKeyguardShow) {
            setKeyguardAuthen(true);
            showGxzwView(false);
            this.mMiuiGxzwIconView.setEnrolling(false);
            this.mMiuiGxzwOverlayView.setEnrolling(false);
        }
    }

    public void requestDrawWackLock(long timeout) {
        this.mDrawWakeLock.acquire(timeout);
    }

    public void requestDrawWackLock() {
        this.mDrawWakeLock.acquire();
    }

    public void releaseDrawWackLock() {
        this.mDrawWakeLock.release();
    }

    public void setUnlockLockout(boolean lockout) {
        this.mMiuiGxzwIconView.setUnlockLockout(lockout);
    }

    public boolean isIgnoreFocusChange() {
        return this.mIgnoreFocusChange;
    }

    public boolean isShouldShowGxzwIconInKeyguard() {
        return this.mShouldShowGxzwIconInKeyguard;
    }

    public void setNotificationPanelView(NotificationPanelView view) {
        this.mNotificationPanelView = view;
    }

    public void updateGxzwState() {
        if (this.mNotificationPanelView != null) {
            this.mNotificationPanelView.updateGxzwState();
        }
    }

    public boolean supportFodInBouncer() {
        return this.SUPPORT_FOD_IN_BOUNCER;
    }

    public boolean isShowFodInBouncer() {
        boolean z = false;
        if (!this.SUPPORT_FOD_IN_BOUNCER) {
            return false;
        }
        boolean unlockingAllowed = KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockingWithFingerprintAllowed(KeyguardUpdateMonitor.getCurrentUser());
        if ((this.mSecurityMode == KeyguardSecurityModel.SecurityMode.Pattern || this.mSecurityMode == KeyguardSecurityModel.SecurityMode.PIN || this.mSecurityMode == KeyguardSecurityModel.SecurityMode.Password) && !this.mShowLockoutView && unlockingAllowed && !this.mFingerprintLockout) {
            z = true;
        }
        return z;
    }

    public void setSecurityMode(KeyguardSecurityModel.SecurityMode securityMode) {
        this.mSecurityMode = securityMode;
        updateGxzwState();
    }

    public void setShowLockoutView(boolean show) {
        this.mShowLockoutView = show;
        updateGxzwState();
    }

    private int dealCallback(int cmd, int param) {
        Log.i("MiuiGxzwManager", "dealCallback, cmd: " + cmd + " param: " + param);
        switch (cmd) {
            case 1:
                this.mHandler.removeMessages(1001);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1001, param, 0));
                break;
            case 2:
                this.mHandler.removeMessages(1002);
                this.mHandler.sendEmptyMessage(1002);
                break;
            case 3:
                processVendorSucess(param);
                break;
            case 4:
                this.mHandler.removeMessages(1006);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1006, param, 0));
                break;
            case 5:
                this.mHandler.removeMessages(1003);
                this.mHandler.sendEmptyMessage(1003);
                break;
            default:
                switch (cmd) {
                    case R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle /*101*/:
                        this.mHandler.removeMessages(1005);
                        this.mHandler.sendEmptyMessage(1005);
                        break;
                    case R.styleable.AppCompatTheme_textAppearanceSearchResultTitle /*102*/:
                        this.mHandler.removeMessages(1004);
                        this.mHandler.sendEmptyMessage(1004);
                        break;
                }
        }
        return 1;
    }

    private void processVendorSucess(int param) {
        if (param != 0) {
            if (getKeyguardAuthen()) {
                int userId = MiuiKeyguardUtils.getAuthUserId(this.mContext, param);
                boolean unlockingAllowed = KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockingWithFingerprintAllowed(userId);
                if (unlockingAllowed && KeyguardUpdateMonitor.getCurrentUser() != userId && !MiuiKeyguardUtils.canSwitchUser(this.mContext, userId)) {
                    unlockingAllowed = false;
                }
                if (unlockingAllowed) {
                    if (!MiuiKeyguardUtils.isDozing()) {
                        setGxzwUnlockMode(2);
                    } else {
                        setGxzwUnlockMode(1);
                        this.mContext.sendBroadcast(new Intent("com.miui.keyguard.face_unlock_succeed"));
                    }
                    fastUnlockIfNeed();
                    setGxzwAuthFingerprintID(param);
                    if (!this.mEnableFastUnlock || !MiuiKeyguardUtils.isDozing()) {
                        this.mHandler.removeMessages(1002);
                        this.mHandler.sendEmptyMessage(1002);
                    }
                }
            } else {
                this.mHandler.removeMessages(1002);
                this.mHandler.sendEmptyMessage(1002);
            }
        }
    }

    private void fastUnlockIfNeed() {
        if (isFodFastUnlock()) {
            ((PowerManager) this.mContext.getSystemService("power")).wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
            this.mHandler.post(new Runnable() {
                public void run() {
                    MiuiGxzwManager.this.mKeyguardViewMediator.preHideKeyguard();
                    MiuiGxzwManager.this.mMiuiGxzwOverlayView.dismiss();
                }
            });
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1) {
            return super.onTransact(code, data, reply, flags);
        }
        data.enforceInterface("android.app.fod.ICallback");
        int result = dealCallback(data.readInt(), data.readInt());
        reply.writeNoException();
        reply.writeInt(result);
        return true;
    }

    /* access modifiers changed from: private */
    public void showGxzwView(boolean lightIcon) {
        Log.i("MiuiGxzwManager", "showGxzwView: lightIcon = " + lightIcon + ", mShowed = " + this.mShowed + ", mShouldShowGxzwIconInKeyguard = " + this.mShouldShowGxzwIconInKeyguard + ", keyguardAuthen = " + getKeyguardAuthen());
        if (!this.mShowed) {
            MiuiGxzwUtils.caculateGxzwIconSize(this.mContext);
            this.mMiuiGxzwOverlayView.show();
            if (this.mShouldShowGxzwIconInKeyguard || !getKeyguardAuthen()) {
                this.mMiuiGxzwIconView.show(lightIcon);
            }
            this.mShowed = true;
            this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
        }
    }

    /* access modifiers changed from: private */
    public void dismissGxzwView() {
        Log.i("MiuiGxzwManager", "dismissGxzwView: mShowed = " + this.mShowed);
        if (this.mShowed) {
            this.mMiuiGxzwIconView.dismiss();
            this.mMiuiGxzwOverlayView.dismiss();
            this.mShowed = false;
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    /* access modifiers changed from: private */
    public synchronized void setKeyguardAuthen(boolean keyguardAuthen) {
        boolean old = this.mKeyguardAuthen;
        this.mKeyguardAuthen = keyguardAuthen;
        if (old != keyguardAuthen) {
            this.mMiuiGxzwOverlayView.onKeyguardAuthen(keyguardAuthen);
            this.mMiuiGxzwIconView.onKeyguardAuthen(keyguardAuthen);
        }
    }

    /* access modifiers changed from: private */
    public synchronized boolean getKeyguardAuthen() {
        return this.mKeyguardAuthen;
    }

    /* access modifiers changed from: private */
    public void ignoreFocusChangeForWhile() {
        this.mIgnoreFocusChange = true;
        this.mHandler.removeCallbacks(this.mIgnoreFocusRunnable);
        this.mHandler.postDelayed(this.mIgnoreFocusRunnable, 1000);
    }

    public boolean isShowingChargeAnimationWindow() {
        return this.mShowingChargeAnimationWindow;
    }

    public void setShowingChargeAnimationWindow(boolean showingChargeAnimationWindow) {
        if (this.mShowingChargeAnimationWindow != showingChargeAnimationWindow) {
            this.mShowingChargeAnimationWindow = showingChargeAnimationWindow;
            updateGxzwState();
        }
    }
}
