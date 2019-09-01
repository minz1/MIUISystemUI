package com.android.keyguard;

import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import java.util.List;
import java.util.Objects;

public class LockPatternUtilsWrapper {
    private final LockPatternUtils mUtils;

    public LockPatternUtilsWrapper(LockPatternUtils utils) {
        this.mUtils = utils;
    }

    public int getKeyguardStoredPasswordQuality() {
        return this.mUtils.getKeyguardStoredPasswordQuality(getCurrentUserId());
    }

    public int getActivePasswordQuality() {
        return getActivePasswordQuality(getCurrentUserId());
    }

    public int getActivePasswordQuality(int userId) {
        return this.mUtils.getActivePasswordQuality(userId);
    }

    public boolean isSecure() {
        return isSecure(getCurrentUserId());
    }

    public boolean isSecure(int userId) {
        return this.mUtils.isSecure(userId);
    }

    public boolean checkPattern(List<LockPatternView.Cell> pattern, int userId) throws KeyguardRequestThrottledException {
        try {
            return this.mUtils.checkPattern(pattern, userId);
        } catch (LockPatternUtils.RequestThrottledException e) {
            String simpleName = LockPatternUtilsWrapper.class.getSimpleName();
            Log.e(simpleName, "message:" + e.getMessage() + "; timeout:" + e.getTimeoutMs(), e);
            throw new KeyguardRequestThrottledException(e.getTimeoutMs());
        }
    }

    public boolean checkPattern(List<LockPatternView.Cell> pattern, int userId, OnCheckForUsersCallback callback) throws KeyguardRequestThrottledException {
        try {
            LockPatternUtils lockPatternUtils = this.mUtils;
            Objects.requireNonNull(callback);
            return lockPatternUtils.checkPattern(pattern, userId, new LockPatternUtils.CheckCredentialProgressCallback() {
                public final void onEarlyMatched() {
                    OnCheckForUsersCallback.this.onEarlyMatched();
                }
            });
        } catch (LockPatternUtils.RequestThrottledException e) {
            String simpleName = LockPatternUtilsWrapper.class.getSimpleName();
            Log.e(simpleName, "message:" + e.getMessage() + "; timeout:" + e.getTimeoutMs(), e);
            throw new KeyguardRequestThrottledException(e.getTimeoutMs());
        }
    }

    public boolean checkPassword(String password, int userId) throws KeyguardRequestThrottledException {
        try {
            return this.mUtils.checkPassword(password, userId);
        } catch (LockPatternUtils.RequestThrottledException e) {
            String simpleName = LockPatternUtilsWrapper.class.getSimpleName();
            Log.e(simpleName, "message:" + e.getMessage() + "; timeout:" + e.getTimeoutMs(), e);
            throw new KeyguardRequestThrottledException(e.getTimeoutMs());
        }
    }

    public boolean checkPassword(String password, int userId, OnCheckForUsersCallback callback) throws KeyguardRequestThrottledException {
        try {
            LockPatternUtils lockPatternUtils = this.mUtils;
            Objects.requireNonNull(callback);
            return lockPatternUtils.checkPassword(password, userId, new LockPatternUtils.CheckCredentialProgressCallback() {
                public final void onEarlyMatched() {
                    OnCheckForUsersCallback.this.onEarlyMatched();
                }
            });
        } catch (LockPatternUtils.RequestThrottledException e) {
            String simpleName = LockPatternUtilsWrapper.class.getSimpleName();
            Log.e(simpleName, "message:" + e.getMessage() + "; timeout:" + e.getTimeoutMs(), e);
            throw new KeyguardRequestThrottledException(e.getTimeoutMs());
        }
    }

    public boolean isOwnerInfoEnabled() {
        return this.mUtils.isOwnerInfoEnabled(getCurrentUserId());
    }

    private int getCurrentUserId() {
        return KeyguardUpdateMonitor.getCurrentUser();
    }
}
