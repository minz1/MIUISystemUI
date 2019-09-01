package com.android.keyguard;

public class KeyguardRequestThrottledException extends Exception {
    private static final long serialVersionUID = 1;
    private int mTimeoutMs;

    public KeyguardRequestThrottledException(int timeoutMs) {
        this.mTimeoutMs = timeoutMs;
    }

    public int getTimeoutMs() {
        return this.mTimeoutMs;
    }
}
