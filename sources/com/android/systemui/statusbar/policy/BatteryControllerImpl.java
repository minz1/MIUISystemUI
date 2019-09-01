package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatteryControllerImpl extends BroadcastReceiver implements BatteryController {
    private static final boolean DEBUG = Log.isLoggable("BatteryController", 3);
    ContentObserver mBatteryExtremeSaveModeChangeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            BatteryControllerImpl batteryControllerImpl = BatteryControllerImpl.this;
            boolean z = true;
            if (Settings.Secure.getIntForUser(BatteryControllerImpl.this.mContext.getContentResolver(), "EXTREME_POWER_MODE_ENABLE", 0, -2) != 1) {
                z = false;
            }
            batteryControllerImpl.mIsExtremePowerSaveMode = z;
            BatteryControllerImpl.this.fireExtremePowerSaveChanged();
        }
    };
    ContentObserver mBatterySaveModeChangeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            BatteryControllerImpl batteryControllerImpl = BatteryControllerImpl.this;
            boolean z = true;
            if (Settings.System.getIntForUser(BatteryControllerImpl.this.mContext.getContentResolver(), "POWER_SAVE_MODE_OPEN", 0, -2) != 1) {
                z = false;
            }
            batteryControllerImpl.mIsPowerSaveMode = z;
            BatteryControllerImpl.this.firePowerSaveChanged();
        }
    };
    /* access modifiers changed from: private */
    public int mBatteryStyle = 1;
    ContentObserver mBatteryStyleChangeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int unused = BatteryControllerImpl.this.mBatteryStyle = Settings.System.getIntForUser(BatteryControllerImpl.this.mContext.getContentResolver(), "battery_indicator_style", 1, -2);
            synchronized (BatteryControllerImpl.this.mChangeCallbacks) {
                int N = BatteryControllerImpl.this.mChangeCallbacks.size();
                for (int i = 0; i < N; i++) {
                    ((BatteryController.BatteryStateChangeCallback) BatteryControllerImpl.this.mChangeCallbacks.get(i)).onBatteryStyleChanged(BatteryControllerImpl.this.mBatteryStyle);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final ArrayList<BatteryController.BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    protected boolean mCharged;
    protected boolean mCharging;
    /* access modifiers changed from: private */
    public final Context mContext;
    private boolean mDemoMode;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private boolean mHasReceivedBattery = false;
    protected boolean mIsExtremePowerSaveMode;
    protected boolean mIsPowerSaveMode;
    protected int mLevel;
    protected boolean mPluggedIn;
    private final PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mTestmode = false;

    public BatteryControllerImpl(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        registerReceiver();
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("battery_indicator_style"), false, this.mBatteryStyleChangeObserver, -1);
        this.mBatteryStyleChangeObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("POWER_SAVE_MODE_OPEN"), false, this.mBatterySaveModeChangeObserver, -1);
        this.mBatterySaveModeChangeObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("EXTREME_POWER_MODE_ENABLE"), false, this.mBatteryExtremeSaveModeChangeObserver, -1);
        this.mBatteryExtremeSaveModeChangeObserver.onChange(false);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        this.mContext.registerReceiver(this, filter);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel=");
        pw.println(this.mLevel);
        pw.print("  mPluggedIn=");
        pw.println(this.mPluggedIn);
        pw.print("  mCharging=");
        pw.println(this.mCharging);
        pw.print("  mCharged=");
        pw.println(this.mCharged);
        pw.print("  mPowerSave=");
        pw.println(this.mIsPowerSaveMode);
        pw.print("  mExtremePowerSave=");
        pw.println(this.mIsExtremePowerSaveMode);
    }

    public void addCallback(BatteryController.BatteryStateChangeCallback cb) {
        synchronized (this.mChangeCallbacks) {
            this.mChangeCallbacks.add(cb);
        }
        cb.onBatteryStyleChanged(this.mBatteryStyle);
        cb.onPowerSaveChanged(this.mIsPowerSaveMode);
        cb.onExtremePowerSaveChanged(this.mIsExtremePowerSaveMode);
        if (this.mHasReceivedBattery) {
            cb.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
        }
    }

    public void removeCallback(BatteryController.BatteryStateChangeCallback cb) {
        synchronized (this.mChangeCallbacks) {
            this.mChangeCallbacks.remove(cb);
        }
    }

    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        boolean z = true;
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            if (!this.mTestmode || intent.getBooleanExtra("testmode", false)) {
                this.mHasReceivedBattery = true;
                this.mLevel = intent.getIntExtra("level", 0);
                this.mPluggedIn = intent.getIntExtra("plugged", 0) != 0;
                int status = intent.getIntExtra("status", 1);
                this.mCharged = status == 5;
                if ((!this.mCharged && status != 2) || !this.mPluggedIn) {
                    z = false;
                }
                this.mCharging = z;
                fireBatteryLevelChanged();
            }
        } else if ("android.intent.action.USER_SWITCHED".equals(action)) {
            this.mBatteryStyleChangeObserver.onChange(false);
            this.mBatterySaveModeChangeObserver.onChange(false);
            this.mBatteryExtremeSaveModeChangeObserver.onChange(false);
        } else if (action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
            this.mTestmode = true;
            this.mHandler.post(new Runnable() {
                int curLevel = 0;
                Intent dummy = new Intent("android.intent.action.BATTERY_CHANGED");
                int incr = 1;
                int saveLevel = BatteryControllerImpl.this.mLevel;
                boolean savePlugged = BatteryControllerImpl.this.mPluggedIn;

                public void run() {
                    int i = 0;
                    if (this.curLevel < 0) {
                        boolean unused = BatteryControllerImpl.this.mTestmode = false;
                        this.dummy.putExtra("level", this.saveLevel);
                        this.dummy.putExtra("plugged", this.savePlugged);
                        this.dummy.putExtra("testmode", false);
                    } else {
                        this.dummy.putExtra("level", this.curLevel);
                        Intent intent = this.dummy;
                        if (this.incr > 0) {
                            i = 1;
                        }
                        intent.putExtra("plugged", i);
                        this.dummy.putExtra("testmode", true);
                    }
                    context.sendBroadcast(this.dummy);
                    if (BatteryControllerImpl.this.mTestmode) {
                        this.curLevel += this.incr;
                        if (this.curLevel == 100) {
                            this.incr *= -1;
                        }
                        BatteryControllerImpl.this.mHandler.postDelayed(this, 200);
                    }
                }
            });
        }
    }

    public boolean isPowerSave() {
        return this.mIsPowerSaveMode;
    }

    public boolean isExtremePowerSave() {
        return this.mIsExtremePowerSaveMode;
    }

    /* access modifiers changed from: protected */
    public void fireBatteryLevelChanged() {
        synchronized (this.mChangeCallbacks) {
            int N = this.mChangeCallbacks.size();
            for (int i = 0; i < N; i++) {
                this.mChangeCallbacks.get(i).onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
            }
        }
    }

    /* access modifiers changed from: private */
    public void firePowerSaveChanged() {
        synchronized (this.mChangeCallbacks) {
            int N = this.mChangeCallbacks.size();
            for (int i = 0; i < N; i++) {
                this.mChangeCallbacks.get(i).onPowerSaveChanged(this.mIsPowerSaveMode);
            }
        }
    }

    /* access modifiers changed from: private */
    public void fireExtremePowerSaveChanged() {
        synchronized (this.mChangeCallbacks) {
            int N = this.mChangeCallbacks.size();
            for (int i = 0; i < N; i++) {
                this.mChangeCallbacks.get(i).onExtremePowerSaveChanged(this.mIsExtremePowerSaveMode);
            }
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            this.mContext.unregisterReceiver(this);
            this.mLevel = 100;
            this.mPluggedIn = false;
            this.mCharging = false;
            fireBatteryLevelChanged();
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            registerReceiver();
            this.mBatteryStyleChangeObserver.onChange(false);
            this.mBatterySaveModeChangeObserver.onChange(false);
            this.mBatteryExtremeSaveModeChangeObserver.onChange(false);
        } else if (this.mDemoMode && command.equals("battery")) {
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                this.mLevel = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                this.mPluggedIn = Boolean.parseBoolean(plugged);
            }
            fireBatteryLevelChanged();
        }
    }
}
