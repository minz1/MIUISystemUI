package com.android.systemui.pip.phone;

public abstract class PipTouchGesture {
    /* access modifiers changed from: package-private */
    public void onDown(PipTouchState touchState) {
    }

    /* access modifiers changed from: package-private */
    public boolean onMove(PipTouchState touchState) {
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean onUp(PipTouchState touchState) {
        return false;
    }
}
