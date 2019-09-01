package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.os.Handler;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;

public class MiuiDozeBrightnessTimeoutAdapter extends DozeMachine.Service.Delegate {
    private int mLastBrightness;
    private final AlarmTimeout mSetDozeScreenBrightnessTimeout;

    public MiuiDozeBrightnessTimeoutAdapter(DozeMachine.Service wrappedService, AlarmManager alarmManager, Handler handler) {
        super(wrappedService);
        this.mSetDozeScreenBrightnessTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiDozeBrightnessTimeoutAdapter.this.onSetDozeScreenBrightnessTimeout();
            }
        }, "SetDozeScreenBrightnessTimeout", handler);
    }

    /* access modifiers changed from: private */
    public void onSetDozeScreenBrightnessTimeout() {
        super.setDozeScreenBrightness(this.mLastBrightness);
    }

    public void finish() {
        super.finish();
        this.mSetDozeScreenBrightnessTimeout.cancel();
    }

    public void setDozeScreenBrightness(int brightness) {
        if (brightness != this.mLastBrightness) {
            this.mSetDozeScreenBrightnessTimeout.schedule(10000, 2);
            this.mLastBrightness = brightness;
        }
    }
}
