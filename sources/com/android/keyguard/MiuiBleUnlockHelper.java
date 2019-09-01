package com.android.keyguard;

import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.security.MiuiLockPatternUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.R;
import com.android.systemui.keyguard.KeyguardViewMediator;
import miui.bluetooth.ble.MiBleProfile;
import miui.bluetooth.ble.MiBleUnlockProfile;

public class MiuiBleUnlockHelper {
    private MiBleUnlockProfile.OnUnlockStateChangeListener mBleListener = new MiBleUnlockProfile.OnUnlockStateChangeListener() {
        public void onUnlocked(byte state) {
            Slog.i("MiuiBleUnlockHelper", "mBleListener state: " + state);
            if (state == 2) {
                MiuiBleUnlockHelper.this.mUpdateMonitor.setBLEUnlockState(BLEUnlockState.SUCCEED);
            } else if (state == 1) {
                MiuiBleUnlockHelper.this.mUpdateMonitor.setBLEUnlockState(BLEUnlockState.FAILED);
            } else {
                MiuiBleUnlockHelper.this.mUpdateMonitor.setBLEUnlockState(BLEUnlockState.FAILED);
            }
            MiuiBleUnlockHelper.this.setBLEStatusBarIcon(state);
        }
    };
    private Context mContext;
    private final BroadcastReceiver mGlobalBluetoothBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("MiuiBleUnlockHelper", "ble action name: " + intent.getAction());
            if ("com.miui.keyguard.bluetoothdeviceunlock.disable".equals(intent.getAction()) || "com.xiaomi.hm.health.ACTION_DEVICE_UNBIND_APPLICATION".equals(intent.getAction())) {
                MiuiBleUnlockHelper.this.disconnectBleDeviceIfNecessary();
            } else if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
                int btState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10);
                if (btState == 12) {
                    MiuiBleUnlockHelper.this.connectBLEDevice();
                } else if (btState == 13 || btState == 10) {
                    MiuiBleUnlockHelper.this.disconnectBleDeviceIfNecessary();
                }
            } else {
                MiuiBleUnlockHelper.this.connectBLEDevice();
            }
        }
    };
    private MiuiLockPatternUtils mLockPatternUtils;
    private LockPatternUtilsWrapper mLockPatternUtilsWrapper;
    private MiBleProfile.IProfileStateChangeCallback mStateChangeCallback = new MiBleProfile.IProfileStateChangeCallback() {
        public void onState(int state) {
            Log.d("MiuiBleUnlockHelper", "Ble state change onState: " + state);
            if (state != 4) {
                MiuiBleUnlockHelper.this.unregisterUnlockListener();
            } else if (MiuiBleUnlockHelper.this.mUnlockProfile != null && MiuiBleUnlockHelper.this.mUpdateMonitor.isScreenOn() && MiuiBleUnlockHelper.this.mViewMediator.isShowingAndNotOccluded()) {
                MiuiBleUnlockHelper.this.registerUnlockListener();
            }
        }
    };
    private StatusBarManager mStatusBarManager;
    /* access modifiers changed from: private */
    public MiBleUnlockProfile mUnlockProfile;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mUpdateMonitor;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStartedWakingUp() {
            MiuiBleUnlockHelper.this.verifyBLEDeviceRssi();
        }

        public void onFinishedGoingToSleep(int why) {
            MiuiBleUnlockHelper.this.unregisterUnlockListener();
        }
    };
    /* access modifiers changed from: private */
    public KeyguardViewMediator mViewMediator;

    public enum BLEUnlockState {
        FAILED,
        SUCCEED,
        PROCESSING
    }

    public MiuiBleUnlockHelper(Context context, KeyguardViewMediator viewMediator) {
        this.mContext = context;
        this.mViewMediator = viewMediator;
        this.mLockPatternUtils = new MiuiLockPatternUtils(context);
        this.mLockPatternUtilsWrapper = new LockPatternUtilsWrapper(this.mLockPatternUtils);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
        registerBleUnlockReceiver();
    }

    private void registerBleUnlockReceiver() {
        IntentFilter globalBluetoothIntentFilter = new IntentFilter();
        globalBluetoothIntentFilter.addAction("com.miui.keyguard.bluetoothdeviceunlock");
        globalBluetoothIntentFilter.addAction("com.miui.keyguard.bluetoothdeviceunlock.disable");
        globalBluetoothIntentFilter.addAction("com.xiaomi.hm.health.ACTION_DEVICE_UNBIND_APPLICATION");
        globalBluetoothIntentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mGlobalBluetoothBroadcastReceiver, UserHandle.ALL, globalBluetoothIntentFilter, null, null);
    }

    public void verifyBLEDeviceRssi() {
        if (!this.mViewMediator.isShowingAndNotOccluded() || this.mViewMediator.isHiding() || !this.mUpdateMonitor.isDeviceInteractive() || MiuiKeyguardUtils.isDozing()) {
            Log.d("MiuiBleUnlockHelper", "verifyBLEDeviceRssi isShowingAndNotOccluded = " + this.mViewMediator.isShowingAndNotOccluded() + " isHiding = " + this.mViewMediator.isHiding() + " isDeviceInteractive = " + this.mUpdateMonitor.isDeviceInteractive());
        }
        if (this.mViewMediator.isShowingAndNotOccluded() && !this.mViewMediator.isHiding() && KeyguardUpdateMonitor.isOwnerUser() && this.mUpdateMonitor.isDeviceInteractive() && this.mUpdateMonitor.getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot() && this.mLockPatternUtilsWrapper.isSecure() && isUnlockWithBlePossible()) {
            if (this.mUnlockProfile != null) {
                registerUnlockListener();
                return;
            }
            Log.d("MiuiBleUnlockHelper", "connectBLEDevice...");
            connectBLEDevice();
        }
    }

    public boolean isUnlockWithBlePossible() {
        return this.mLockPatternUtils.getBluetoothUnlockEnabled() && !TextUtils.isEmpty(this.mLockPatternUtils.getBluetoothAddressToUnlock()) && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    /* access modifiers changed from: private */
    public void connectBLEDevice() {
        if (this.mLockPatternUtilsWrapper.isSecure() && KeyguardUpdateMonitor.isOwnerUser() && this.mUpdateMonitor.getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot() && isUnlockWithBlePossible()) {
            this.mUpdateMonitor.setBLEUnlockState(BLEUnlockState.FAILED);
            try {
                if (this.mUnlockProfile != null) {
                    this.mUnlockProfile.disconnect();
                }
            } catch (Exception e) {
                Log.e("MiuiBleUnlockHelper", e.getMessage(), e);
            }
            this.mUnlockProfile = new MiBleUnlockProfile(this.mContext, this.mLockPatternUtils.getBluetoothAddressToUnlock(), this.mStateChangeCallback);
            this.mUnlockProfile.connect();
        }
    }

    /* access modifiers changed from: private */
    public void disconnectBleDeviceIfNecessary() {
        try {
            if (this.mUnlockProfile != null) {
                unregisterUnlockListener();
                this.mUnlockProfile.disconnect();
                this.mUnlockProfile = null;
            }
        } catch (Exception e) {
            Log.e("MiuiBleUnlockHelper", e.getMessage(), e);
        }
    }

    /* access modifiers changed from: private */
    public void registerUnlockListener() {
        if (this.mUnlockProfile != null) {
            this.mUnlockProfile.registerUnlockListener(this.mBleListener);
            setBLEStatusBarIcon(0);
        }
    }

    public void unregisterUnlockListener() {
        if (this.mUnlockProfile != null) {
            this.mUnlockProfile.unregisterUnlockListener();
            this.mStatusBarManager.removeIcon("ble_unlock_mode");
        }
        this.mUpdateMonitor.setBLEUnlockState(BLEUnlockState.FAILED);
    }

    /* access modifiers changed from: private */
    public void setBLEStatusBarIcon(int state) {
        int iconResId;
        if (state == 0) {
            iconResId = R.drawable.ble_unlock_statusbar_icon_unverified;
        } else if (state == 2) {
            iconResId = R.drawable.ble_unlock_statusbar_icon_verified_near;
        } else {
            iconResId = R.drawable.ble_unlock_statusbar_icon_verified_far;
        }
        this.mStatusBarManager.setIcon("ble_unlock_mode", iconResId, 0, null);
    }
}
