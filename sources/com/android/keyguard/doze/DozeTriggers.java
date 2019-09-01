package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.phone.DozeParameters;
import com.android.keyguard.util.Assert;
import com.android.keyguard.util.wakelock.WakeLock;
import com.android.systemui.doze.DozeHost;
import java.io.PrintWriter;

public class DozeTriggers implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    private final boolean mAllowPulseTriggers;
    private final TriggerReceiver mBroadcastReceiver = new TriggerReceiver();
    private final AmbientDisplayConfiguration mConfig;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final DozeHost mDozeHost;
    private final DozeParameters mDozeParameters;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private DozeHost.Callback mHostCallback = new DozeHost.Callback() {
        public void onFodInAodStateChanged(final boolean on) {
            DozeTriggers.this.mHandler.post(DozeTriggers.this.mWakeLock.wrap((Runnable) new Runnable() {
                public void run() {
                    DozeMachine.State state = DozeTriggers.this.mMachine.getState();
                    boolean doze = false;
                    boolean paused = state == DozeMachine.State.DOZE_AOD_PAUSED;
                    boolean pausing = state == DozeMachine.State.DOZE_AOD_PAUSING;
                    boolean dozeAod = state == DozeMachine.State.DOZE_AOD;
                    if (state == DozeMachine.State.DOZE) {
                        doze = true;
                    }
                    if (on) {
                        if (paused || pausing || doze) {
                            DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
                        }
                    } else if ((dozeAod || paused || pausing) && MiuiKeyguardUtils.isAodClockDisable(DozeTriggers.this.mContext)) {
                        DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE);
                    }
                }
            }));
        }

        public void onFingerprintPressed(boolean pressed) {
            if (pressed) {
                DozeTriggers.this.mService.fingerprintPressed(pressed);
                DozeTriggers.this.mMachine.requestPulse(15);
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE_PULSING);
                return;
            }
            DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
            DozeTriggers.this.mService.fingerprintPressed(pressed);
        }

        public void onPowerSaveChanged(boolean active) {
            if (active) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
        }

        public void onAodAnimate(final boolean show) {
            DozeTriggers.this.mHandler.post(DozeTriggers.this.mWakeLock.wrap((Runnable) new Runnable() {
                public void run() {
                    DozeMachine.State state = DozeTriggers.this.mMachine.getState();
                    boolean doze = false;
                    boolean paused = state == DozeMachine.State.DOZE_AOD_PAUSED;
                    boolean pausing = state == DozeMachine.State.DOZE_AOD_PAUSING;
                    boolean dozeAod = state == DozeMachine.State.DOZE_AOD;
                    if (state == DozeMachine.State.DOZE) {
                        doze = true;
                    }
                    if (show) {
                        if (paused || pausing || doze) {
                            DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
                        }
                    } else if (!MiuiKeyguardUtils.isAodClockDisable(DozeTriggers.this.mContext)) {
                    } else {
                        if (dozeAod || paused || pausing) {
                            DozeTriggers.this.mMachine.requestState(DozeMachine.State.DOZE);
                        }
                    }
                }
            }));
        }
    };
    /* access modifiers changed from: private */
    public final DozeMachine mMachine;
    private long mNotificationPulseTime;
    private boolean mPulsePending;
    private final SensorManager mSensorManager;
    /* access modifiers changed from: private */
    public DozeMachine.Service mService;
    private final UiModeManager mUiModeManager;
    /* access modifiers changed from: private */
    public final WakeLock mWakeLock;

    private class TriggerReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private TriggerReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.doze.pulse".equals(intent.getAction())) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "Received pulse intent");
                }
                DozeTriggers.this.requestPulse(0);
            }
            if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intent.getAction())) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
        }

        public void register(Context context) {
            if (!this.mRegistered) {
                IntentFilter filter = new IntentFilter("com.android.systemui.doze.pulse");
                filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
                context.registerReceiver(this, filter);
                this.mRegistered = true;
            }
        }

        public void unregister(Context context) {
            if (this.mRegistered) {
                context.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }

    public DozeTriggers(Context context, DozeMachine machine, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration config, DozeParameters dozeParameters, SensorManager sensorManager, Handler handler, WakeLock wakeLock, boolean allowPulseTriggers, DozeMachine.Service service) {
        this.mContext = context;
        this.mMachine = machine;
        this.mDozeHost = dozeHost;
        this.mConfig = config;
        this.mDozeParameters = dozeParameters;
        this.mSensorManager = sensorManager;
        this.mHandler = handler;
        this.mWakeLock = wakeLock;
        this.mAllowPulseTriggers = allowPulseTriggers;
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        this.mService = service;
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        switch (newState) {
            case INITIALIZED:
                this.mBroadcastReceiver.register(this.mContext);
                this.mDozeHost.addCallback(this.mHostCallback);
                checkTriggersAtInit();
                return;
            case FINISH:
                this.mBroadcastReceiver.unregister(this.mContext);
                this.mDozeHost.removeCallback(this.mHostCallback);
                return;
            default:
                return;
        }
    }

    private void checkTriggersAtInit() {
        if (this.mUiModeManager.getCurrentModeType() == 3 || this.mDozeHost.isPowerSaveActive() || this.mDozeHost.isBlockingDoze() || !this.mDozeHost.isProvisioned()) {
            this.mService.requestState(DozeMachine.State.FINISH);
        }
    }

    /* access modifiers changed from: private */
    public void requestPulse(int reason) {
        Assert.isMainThread();
        this.mDozeHost.extendPulse();
        if (this.mPulsePending || !this.mAllowPulseTriggers || !canPulse()) {
            if (this.mAllowPulseTriggers) {
                DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
            }
            return;
        }
        continuePulseRequest(reason);
    }

    private boolean canPulse() {
        return this.mMachine.getState() == DozeMachine.State.DOZE || this.mMachine.getState() == DozeMachine.State.DOZE_AOD;
    }

    private void continuePulseRequest(int reason) {
        this.mPulsePending = false;
        if (this.mDozeHost.isPulsingBlocked() || !canPulse()) {
            DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
        } else {
            this.mMachine.requestPulse(reason);
        }
    }

    public void dump(PrintWriter pw) {
        pw.print(" notificationPulseTime=");
        pw.println(Formatter.formatShortElapsedTime(this.mContext, this.mNotificationPulseTime));
        pw.print(" pulsePending=");
        pw.println(this.mPulsePending);
        pw.println("DozeSensors:");
    }
}
