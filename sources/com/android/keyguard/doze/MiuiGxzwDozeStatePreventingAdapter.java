package com.android.keyguard.doze;

import android.app.ActivityManager;
import android.content.Context;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.systemui.doze.DozeHost;

public class MiuiGxzwDozeStatePreventingAdapter extends DozeMachine.Service.Delegate {
    private int mAodBrightness = MiuiGxzwUtils.getAodInitBrightness();
    private final Context mContext;
    private boolean mFingerprintPress;
    private final DozeHost mHost;

    private MiuiGxzwDozeStatePreventingAdapter(DozeMachine.Service wrappedService, DozeHost host, Context context) {
        super(wrappedService);
        this.mHost = host;
        this.mContext = context;
    }

    public void requestState(DozeMachine.State requestedState) {
        boolean aodClockDisable = MiuiKeyguardUtils.isAodClockDisable(this.mContext);
        if (requestedState == DozeMachine.State.DOZE_AOD) {
            this.mHost.setAodClockVisibility(!aodClockDisable);
            super.requestState(requestedState);
        } else if (requestedState == DozeMachine.State.DOZE || requestedState == DozeMachine.State.DOZE_AOD_PAUSING || requestedState == DozeMachine.State.DOZE_AOD_PAUSED) {
            this.mHost.setAodClockVisibility(!aodClockDisable);
            if (!MiuiGxzwManager.getInstance().isShowFingerprintIcon()) {
                super.requestState(requestedState);
            }
        } else if (requestedState == DozeMachine.State.FINISH && !KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockWithFingerprintPossible(ActivityManager.getCurrentUser())) {
            super.requestState(requestedState);
        }
    }

    public void finish() {
        super.finish();
        this.mAodBrightness = MiuiGxzwUtils.getAodInitBrightness();
        this.mFingerprintPress = false;
    }

    public void setDozeScreenBrightness(int brightness) {
        this.mAodBrightness = brightness;
        if (!MiuiGxzwUtils.supportLowBrightnessFod() || !this.mFingerprintPress) {
            super.setDozeScreenBrightness(brightness);
        }
    }

    public void fingerprintPressed(boolean pressed) {
        super.fingerprintPressed(pressed);
        this.mFingerprintPress = pressed;
        if (MiuiGxzwUtils.supportLowBrightnessFod()) {
            if (pressed) {
                super.setDozeScreenBrightness(MiuiGxzwUtils.getAod2OnBrightness());
            } else {
                super.setDozeScreenBrightness(this.mAodBrightness);
            }
        }
    }

    public static DozeMachine.Service wrapIfNeeded(DozeMachine.Service inner, DozeHost host, Context context) {
        return MiuiKeyguardUtils.isGxzwSensor() ? new MiuiGxzwDozeStatePreventingAdapter(inner, host, context) : inner;
    }
}
