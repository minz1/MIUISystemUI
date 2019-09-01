package com.android.keyguard.faceunlock;

public interface FaceUnlockCallback {
    void onFaceAuthFailed(boolean z);

    void onFaceAuthenticated();

    void onFaceHelp(int i);

    void onFaceLocked();

    void onFaceStart();

    void onFaceStop();

    void restartFaceUnlock();

    void unblockScreenOn();
}
