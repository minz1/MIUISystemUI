package com.android.keyguard.util;

import android.hardware.HardwareBuffer;
import android.hardware.Sensor;
import android.hardware.SensorAdditionalInfo;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.MemoryFile;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.util.List;

public class AsyncSensorManager extends SensorManager {
    private static final HandlerThread sHandlerThread = new HandlerThread("async_sensor");
    @VisibleForTesting
    final Handler mHandler;
    private final SensorManager mInner;
    private final List<Sensor> mSensorCache;

    public AsyncSensorManager(SensorManager inner) {
        this.mInner = inner;
        if (!sHandlerThread.isAlive()) {
            sHandlerThread.start();
        }
        this.mHandler = new Handler(sHandlerThread.getLooper());
        this.mSensorCache = this.mInner.getSensorList(-1);
    }

    /* access modifiers changed from: protected */
    public List<Sensor> getFullSensorList() {
        return this.mSensorCache;
    }

    /* access modifiers changed from: protected */
    public List<Sensor> getFullDynamicSensorList() {
        return this.mInner.getDynamicSensorList(-1);
    }

    /* access modifiers changed from: protected */
    public boolean registerListenerImpl(SensorEventListener listener, Sensor sensor, int delayUs, Handler handler, int maxReportLatencyUs, int reservedFlags) {
        Handler handler2 = this.mHandler;
        $$Lambda$AsyncSensorManager$kryoSbYo50vfdTOLeg72arTfK5w r1 = new Runnable(listener, sensor, delayUs, maxReportLatencyUs, handler) {
            private final /* synthetic */ SensorEventListener f$1;
            private final /* synthetic */ Sensor f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ int f$4;
            private final /* synthetic */ Handler f$5;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
            }

            public final void run() {
                AsyncSensorManager.lambda$registerListenerImpl$0(AsyncSensorManager.this, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
            }
        };
        handler2.post(r1);
        return true;
    }

    public static /* synthetic */ void lambda$registerListenerImpl$0(AsyncSensorManager asyncSensorManager, SensorEventListener listener, Sensor sensor, int delayUs, int maxReportLatencyUs, Handler handler) {
        if (!asyncSensorManager.mInner.registerListener(listener, sensor, delayUs, maxReportLatencyUs, handler)) {
            Log.e("AsyncSensorManager", "Registering " + listener + " for " + sensor + " failed.");
        }
    }

    /* access modifiers changed from: protected */
    public boolean flushImpl(SensorEventListener listener) {
        return this.mInner.flush(listener);
    }

    /* access modifiers changed from: protected */
    public SensorDirectChannel createDirectChannelImpl(MemoryFile memoryFile, HardwareBuffer hardwareBuffer) {
        throw new UnsupportedOperationException("not implemented");
    }

    /* access modifiers changed from: protected */
    public void destroyDirectChannelImpl(SensorDirectChannel channel) {
        throw new UnsupportedOperationException("not implemented");
    }

    /* access modifiers changed from: protected */
    public int configureDirectChannelImpl(SensorDirectChannel channel, Sensor s, int rate) {
        throw new UnsupportedOperationException("not implemented");
    }

    /* access modifiers changed from: protected */
    public void registerDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback callback, Handler handler) {
        this.mHandler.post(new Runnable(callback, handler) {
            private final /* synthetic */ SensorManager.DynamicSensorCallback f$1;
            private final /* synthetic */ Handler f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                AsyncSensorManager.this.mInner.registerDynamicSensorCallback(this.f$1, this.f$2);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void unregisterDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback callback) {
        this.mHandler.post(new Runnable(callback) {
            private final /* synthetic */ SensorManager.DynamicSensorCallback f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                AsyncSensorManager.this.mInner.unregisterDynamicSensorCallback(this.f$1);
            }
        });
    }

    /* access modifiers changed from: protected */
    public boolean requestTriggerSensorImpl(TriggerEventListener listener, Sensor sensor) {
        this.mHandler.post(new Runnable(listener, sensor) {
            private final /* synthetic */ TriggerEventListener f$1;
            private final /* synthetic */ Sensor f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                AsyncSensorManager.lambda$requestTriggerSensorImpl$3(AsyncSensorManager.this, this.f$1, this.f$2);
            }
        });
        return true;
    }

    public static /* synthetic */ void lambda$requestTriggerSensorImpl$3(AsyncSensorManager asyncSensorManager, TriggerEventListener listener, Sensor sensor) {
        if (!asyncSensorManager.mInner.requestTriggerSensor(listener, sensor)) {
            Log.e("AsyncSensorManager", "Requesting " + listener + " for " + sensor + " failed.");
        }
    }

    /* access modifiers changed from: protected */
    public boolean cancelTriggerSensorImpl(TriggerEventListener listener, Sensor sensor, boolean disable) {
        Preconditions.checkArgument(disable);
        this.mHandler.post(new Runnable(listener, sensor) {
            private final /* synthetic */ TriggerEventListener f$1;
            private final /* synthetic */ Sensor f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                AsyncSensorManager.lambda$cancelTriggerSensorImpl$4(AsyncSensorManager.this, this.f$1, this.f$2);
            }
        });
        return true;
    }

    public static /* synthetic */ void lambda$cancelTriggerSensorImpl$4(AsyncSensorManager asyncSensorManager, TriggerEventListener listener, Sensor sensor) {
        if (!asyncSensorManager.mInner.cancelTriggerSensor(listener, sensor)) {
            Log.e("AsyncSensorManager", "Canceling " + listener + " for " + sensor + " failed.");
        }
    }

    /* access modifiers changed from: protected */
    public boolean initDataInjectionImpl(boolean enable) {
        throw new UnsupportedOperationException("not implemented");
    }

    /* access modifiers changed from: protected */
    public boolean injectSensorDataImpl(Sensor sensor, float[] values, int accuracy, long timestamp) {
        throw new UnsupportedOperationException("not implemented");
    }

    /* access modifiers changed from: protected */
    public boolean setOperationParameterImpl(SensorAdditionalInfo parameter) {
        this.mHandler.post(new Runnable(parameter) {
            private final /* synthetic */ SensorAdditionalInfo f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                AsyncSensorManager.this.mInner.setOperationParameter(this.f$1);
            }
        });
        return true;
    }

    /* access modifiers changed from: protected */
    public void unregisterListenerImpl(SensorEventListener listener, Sensor sensor) {
        this.mHandler.post(new Runnable(sensor, listener) {
            private final /* synthetic */ Sensor f$1;
            private final /* synthetic */ SensorEventListener f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                AsyncSensorManager.lambda$unregisterListenerImpl$6(AsyncSensorManager.this, this.f$1, this.f$2);
            }
        });
    }

    public static /* synthetic */ void lambda$unregisterListenerImpl$6(AsyncSensorManager asyncSensorManager, Sensor sensor, SensorEventListener listener) {
        if (sensor == null) {
            asyncSensorManager.mInner.unregisterListener(listener);
        } else {
            asyncSensorManager.mInner.unregisterListener(listener, sensor);
        }
    }
}
