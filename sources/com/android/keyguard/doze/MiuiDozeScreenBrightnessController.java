package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;
import com.android.systemui.doze.DozeHost;

public class MiuiDozeScreenBrightnessController implements DozeMachine.Part {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = DozeService.DEBUG;
    public static final String TAG = MiuiDozeScreenBrightnessController.class.getSimpleName();
    /* access modifiers changed from: private */
    public final Context mContext;
    private DozeTriggers mDozeTriggers;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public DozeHost mHost;
    private DozeHost.Callback mHostCallback = new DozeHost.Callback() {
        public void onFodInAodStateChanged(boolean on) {
            if (!on && !MiuiKeyguardUtils.isAodClockDisable(MiuiDozeScreenBrightnessController.this.mContext)) {
                MiuiDozeScreenBrightnessController.this.checkToScreenOff(MiuiDozeScreenBrightnessController.this.mLight);
            }
        }

        public void onFingerprintPressed(boolean pressed) {
        }

        public void onPowerSaveChanged(boolean active) {
        }

        public void onAodAnimate(boolean show) {
        }
    };
    /* access modifiers changed from: private */
    public boolean mLight = true;
    private Sensor mLightSensor;
    private final AlarmTimeout mListenLightSensorTimeout;
    /* access modifiers changed from: private */
    public final DozeMachine mMachine;
    /* access modifiers changed from: private */
    public final AlarmTimeout mOffTimeout;
    private boolean mRegistered;
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            String str = MiuiDozeScreenBrightnessController.TAG;
            Log.i(str, "sensor event:" + event.sensor.getType() + ", value:" + event.values[0]);
            if (33171029 == event.sensor.getType()) {
                int value = (int) event.values[0];
                DozeMachine.State state = MiuiDozeScreenBrightnessController.this.mMachine.getState();
                boolean paused = state == DozeMachine.State.DOZE_AOD_PAUSED;
                boolean pausing = state == DozeMachine.State.DOZE_AOD_PAUSING;
                boolean aod = state == DozeMachine.State.DOZE_AOD;
                if (value == 2 || value == 1) {
                    boolean unused = MiuiDozeScreenBrightnessController.this.mLight = false;
                    if (aod) {
                        if (MiuiDozeScreenBrightnessController.DEBUG) {
                            Log.i(MiuiDozeScreenBrightnessController.TAG, "Prox NEAR, pausing AOD   ");
                        }
                        MiuiDozeScreenBrightnessController.this.mService.requestState(DozeMachine.State.DOZE_AOD_PAUSING);
                    }
                } else if (paused || pausing) {
                    if (MiuiDozeScreenBrightnessController.DEBUG) {
                        Log.i(MiuiDozeScreenBrightnessController.TAG, "Prox FAR, unpausing AOD");
                    }
                    MiuiDozeScreenBrightnessController.this.mService.requestState(DozeMachine.State.DOZE_AOD);
                }
                if (value == 3) {
                    boolean unused2 = MiuiDozeScreenBrightnessController.this.mLight = false;
                    MiuiDozeScreenBrightnessController.this.mHost.setAodDimmingScrim(0.5f);
                    MiuiDozeScreenBrightnessController.this.mService.setDozeScreenBrightness(1);
                    MiuiDozeScreenBrightnessController.this.mOffTimeout.schedule(300000, 1);
                } else {
                    MiuiDozeScreenBrightnessController.this.mOffTimeout.cancel();
                }
                if (value == 5) {
                    boolean unused3 = MiuiDozeScreenBrightnessController.this.mLight = true;
                    MiuiDozeScreenBrightnessController.this.checkToScreenOff(MiuiDozeScreenBrightnessController.this.mLight);
                    MiuiDozeScreenBrightnessController.this.mHost.setAodDimmingScrim(0.0f);
                    MiuiDozeScreenBrightnessController.this.mService.setDozeScreenBrightness(1);
                } else if (value == 4) {
                    boolean unused4 = MiuiDozeScreenBrightnessController.this.mLight = true;
                    MiuiDozeScreenBrightnessController.this.checkToScreenOff(MiuiDozeScreenBrightnessController.this.mLight);
                    MiuiDozeScreenBrightnessController.this.mHost.setAodDimmingScrim(0.0f);
                    MiuiDozeScreenBrightnessController.this.mService.setDozeScreenBrightness(PowerManager.BRIGHTNESS_ON);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorManager mSensorManager;
    /* access modifiers changed from: private */
    public DozeMachine.Service mService;

    public MiuiDozeScreenBrightnessController(Handler handler, DozeMachine machine, AlarmManager alarmManager, DozeMachine.Service service, DozeHost host, SensorManager sensorManager, DozeTriggers dozeTriggers, Context context) {
        this.mContext = context;
        this.mHandler = handler;
        this.mMachine = machine;
        this.mService = service;
        this.mHost = host;
        this.mSensorManager = sensorManager;
        this.mDozeTriggers = dozeTriggers;
        this.mOffTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiDozeScreenBrightnessController.this.onOffTimeout();
            }
        }, "OffAlarmTimeout", handler);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(33171029, true);
        this.mListenLightSensorTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiDozeScreenBrightnessController.this.onListenLightSensorTimeout();
            }
        }, "onListenLightSensorTimeout", handler);
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        switch (newState) {
            case INITIALIZED:
                this.mHost.setAodDimmingScrim(0.0f);
                this.mHost.addCallback(this.mHostCallback);
                return;
            case DOZE_AOD:
                if (!this.mRegistered) {
                    this.mListenLightSensorTimeout.schedule(10000, 1);
                    return;
                }
                return;
            case DOZE:
            case FINISH:
                this.mOffTimeout.cancel();
                this.mListenLightSensorTimeout.cancel();
                this.mHost.removeCallback(this.mHostCallback);
                if (this.mLightSensor != null && this.mRegistered) {
                    this.mSensorManager.unregisterListener(this.mSensorEventListener);
                    this.mRegistered = false;
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: private */
    public void onListenLightSensorTimeout() {
        if (this.mLightSensor != null && !this.mRegistered) {
            this.mRegistered = this.mSensorManager.registerListener(this.mSensorEventListener, this.mLightSensor, 3, 0, this.mHandler);
        }
    }

    /* access modifiers changed from: private */
    public void onOffTimeout() {
        checkToScreenOff(this.mLight);
    }

    /* access modifiers changed from: private */
    public void checkToScreenOff(boolean light) {
        boolean dark = !light;
        DozeMachine.State state = this.mMachine.getState();
        boolean aod = false;
        boolean paused = state == DozeMachine.State.DOZE_AOD_PAUSED;
        boolean pausing = state == DozeMachine.State.DOZE_AOD_PAUSING;
        if (state == DozeMachine.State.DOZE_AOD) {
            aod = true;
        }
        if (light && (paused || pausing)) {
            if (DEBUG) {
                Log.i(TAG, "Brightness Light, unpausing AOD");
            }
            this.mService.requestState(DozeMachine.State.DOZE_AOD);
        } else if (dark && aod) {
            if (DEBUG) {
                Log.i(TAG, "Brightness Dark, pausing AOD");
            }
            this.mService.requestState(DozeMachine.State.DOZE_AOD_PAUSING);
        }
    }
}
