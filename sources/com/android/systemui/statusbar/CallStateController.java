package com.android.systemui.statusbar;

public interface CallStateController {
    int getCallState(int i);

    boolean isMsim();

    void setCallState(int i, int i2);

    void setSimCount(int i);
}
