package com.android.systemui.doze;

public interface DozeHost {

    public interface Callback {
        void onAodAnimate(boolean z);

        void onFingerprintPressed(boolean z);

        void onFodInAodStateChanged(boolean z);

        void onPowerSaveChanged(boolean z);
    }

    public interface PulseCallback {
        void onPulseFinished();

        void onPulseStarted();
    }

    void addCallback(Callback callback);

    void dozeTimeTick();

    void extendPulse();

    void fireAnimateState();

    void fireAodState(boolean z);

    void fireFingerprintPressed(boolean z);

    boolean isAnimateShowing();

    boolean isBlockingDoze();

    boolean isDozing();

    boolean isPowerSaveActive();

    boolean isProvisioned();

    boolean isPulsingBlocked();

    void pulseWhileDozing(PulseCallback pulseCallback, int i);

    void removeCallback(Callback callback);

    void setAnimateWakeup(boolean z);

    void setAodClockVisibility(boolean z);

    void setAodDimmingScrim(float f);

    void setDozeScreenBrightness(int i);

    void setNotificationAnimate(boolean z);

    void setSunImage(int i);

    void startDozing();

    void stopDozing();
}
