package com.android.keyguard;

public interface ViewMediatorCallback {
    int getBouncerPromptReason();

    void keyguardDone(boolean z, int i);

    void keyguardDoneDrawing();

    void keyguardDonePending(boolean z, int i);

    void keyguardGone();

    void onBouncerVisiblityChanged(boolean z);

    void readyForKeyguardDone();

    void resetKeyguard();

    void setNeedsInput(boolean z);

    void userActivity();
}
