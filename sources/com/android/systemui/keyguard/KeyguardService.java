package com.android.systemui.keyguard;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.policy.IKeyguardStateCallbackCompat;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.systemui.Application;
import com.android.systemui.Dependency;

public class KeyguardService extends Service {
    private final IKeyguardService.Stub mBinder = new IKeyguardService.Stub() {
        public void addStateMonitorCallback(IKeyguardStateCallback callback) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.addStateMonitorCallback(new IKeyguardStateCallbackCompat(callback));
        }

        public void verifyUnlock(IKeyguardExitCallback callback) {
            Trace.beginSection("KeyguardService.mBinder#verifyUnlock");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.verifyUnlock(callback);
            Trace.endSection();
        }

        public void setOccluded(boolean isOccluded, boolean animate) {
            Trace.beginSection("KeyguardService.mBinder#setOccluded");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setOccluded(isOccluded, animate);
            Trace.endSection();
        }

        public void dismiss(IKeyguardDismissCallback callback, CharSequence c) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.dismiss(callback);
        }

        public void onDreamingStarted() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onDreamingStarted();
        }

        public void onDreamingStopped() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onDreamingStopped();
        }

        public void onStartedGoingToSleep(int reason) {
            KeyguardService.this.checkPermission();
            MiuiKeyguardUtils.setScreenTurnOnDelayed(false);
            KeyguardService.this.mKeyguardViewMediator.onStartedGoingToSleep(reason);
        }

        public void onFinishedGoingToSleep(int reason, boolean cameraGestureTriggered) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onFinishedGoingToSleep(reason, cameraGestureTriggered);
        }

        public void onStartedWakingUp() {
            Trace.beginSection("KeyguardService.mBinder#onStartedWakingUp");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onStartedWakingUp();
            Trace.endSection();
            if (Dependency.getHost() != null) {
                Dependency.getHost().stopDozing();
            }
        }

        public void onStartedWakingUp(String reason) {
            KeyguardService.this.checkPermission();
            if ("android.policy:POWER".equalsIgnoreCase(reason) || "android.policy:SLIDE".equalsIgnoreCase(reason)) {
                KeyguardService.this.mKeyguardViewMediator.onStartedWakingUp(reason);
            }
        }

        public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
            Trace.beginSection("KeyguardService.mBinder#onScreenTurningOn");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurningOn(callback);
            Trace.endSection();
        }

        public void onScreenTurnedOn() {
            Trace.beginSection("KeyguardService.mBinder#onScreenTurnedOn");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurnedOn();
            Trace.endSection();
        }

        public void onScreenTurnedOff() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurnedOff();
        }

        public void setKeyguardEnabled(boolean enabled) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setKeyguardEnabled(enabled);
        }

        public void onSystemReady() {
            Trace.beginSection("KeyguardService.mBinder#onSystemReady");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onSystemReady();
            Trace.endSection();
        }

        public void doKeyguardTimeout(Bundle options) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.doKeyguardTimeout(options);
        }

        public void setSwitchingUser(boolean switching) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setSwitchingUser(switching);
        }

        public void setCurrentUser(int userId) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setCurrentUser(userId);
        }

        public void onBootCompleted() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onBootCompleted();
        }

        public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
            Trace.beginSection("KeyguardService.mBinder#startKeyguardExitAnimation");
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.startKeyguardExitAnimation(startTime, fadeoutDuration);
            Trace.endSection();
        }

        public void onShortPowerPressedGoHome() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onShortPowerPressedGoHome();
        }

        public void OnDoubleClickHome() {
        }

        public void onFinishedWakingUp() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onFinishedWakingUp();
        }

        public void onScreenTurningOff() {
            KeyguardService.this.checkPermission();
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 255) {
                return KeyguardService.super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface("com.android.internal.policy.IKeyguardService");
            onStartedWakingUp(data.readString());
            reply.writeNoException();
            return true;
        }
    };
    /* access modifiers changed from: private */
    public KeyguardViewMediator mKeyguardViewMediator;

    public void onCreate() {
        ((Application) getApplication()).getSystemUIApplication().startServicesIfNeeded();
        this.mKeyguardViewMediator = (KeyguardViewMediator) ((Application) getApplication()).getSystemUIApplication().getComponent(KeyguardViewMediator.class);
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    /* access modifiers changed from: package-private */
    public void checkPermission() {
        if (Binder.getCallingUid() != 1000 && getBaseContext().checkCallingOrSelfPermission("android.permission.CONTROL_KEYGUARD") != 0) {
            Log.w("KeyguardService", "Caller needs permission 'android.permission.CONTROL_KEYGUARD' to call " + Debug.getCaller());
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + "android.permission.CONTROL_KEYGUARD");
        }
    }
}
