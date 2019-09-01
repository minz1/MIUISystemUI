package com.android.keyguard;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.systemui.R;
import java.util.concurrent.TimeUnit;

public abstract class KeyguardAbsKeyInputView extends MiuiKeyguardPasswordView implements EmergencyButton.EmergencyButtonCallback, KeyguardSecurityView {
    private CountDownTimer mCountdownTimer;
    private boolean mDismissing;
    protected boolean mEnableHaptics;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected SecurityMessageDisplay mSecurityMessageDisplay;

    /* access modifiers changed from: protected */
    public abstract String getPasswordText();

    /* access modifiers changed from: protected */
    public abstract int getPasswordTextViewId();

    /* access modifiers changed from: protected */
    public abstract void resetPasswordText(boolean z, boolean z2);

    /* access modifiers changed from: protected */
    public abstract void resetState();

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryEnabled(boolean z);

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryInputEnabled(boolean z);

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCountdownTimer = null;
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mEnableHaptics = this.mLockPatternUtils.isTactileFeedbackEnabled();
    }

    public void reset() {
        this.mDismissing = false;
        resetPasswordText(false, false);
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
    }

    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        if (!this.mDismissing) {
            String entry = getPasswordText();
            setPasswordEntryInputEnabled(false);
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
            }
            final int userId = KeyguardUpdateMonitor.getCurrentUser();
            if (entry.length() <= 3) {
                setPasswordEntryInputEnabled(true);
                onPasswordChecked(userId, false, 0, false);
                return;
            }
            if (LatencyTracker.isEnabled(this.mContext)) {
                LatencyTracker.getInstance(this.mContext).onActionStart(3);
                LatencyTracker.getInstance(this.mContext).onActionStart(4);
            }
            this.mPendingLockCheck = LockPatternChecker.checkPasswordForUsers(new LockPatternUtilsWrapper(this.mLockPatternUtils), this.mLockPatternUtils, entry, this.mContext, new OnCheckForUsersCallback() {
                public void onEarlyMatched() {
                    if (LatencyTracker.isEnabled(KeyguardAbsKeyInputView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardAbsKeyInputView.this.mContext).onActionEnd(3);
                    }
                    KeyguardAbsKeyInputView.this.onPasswordChecked(userId, true, 0, true);
                }

                public void onChecked(boolean matched, int userIdMatched, int timeoutMs) {
                    if (LatencyTracker.isEnabled(KeyguardAbsKeyInputView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardAbsKeyInputView.this.mContext).onActionEnd(4);
                    }
                    KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                    KeyguardAbsKeyInputView.this.mPendingLockCheck = null;
                    if (MiuiKeyguardUtils.needPasswordCheck(matched, userIdMatched)) {
                        KeyguardAbsKeyInputView.this.onPasswordChecked(userIdMatched, matched, timeoutMs, true);
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void onPasswordChecked(int userId, boolean matched, int timeoutMs, boolean isValidPassword) {
        if (!matched) {
            if (isValidPassword) {
                this.mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                if (timeoutMs > 0) {
                    this.mKeyguardUpdateMonitor.stopFaceUnlock();
                    handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs));
                }
            }
            handleWrongPassword();
            this.mVibrator.vibrate(150);
        } else if (!allowUnlock(userId)) {
            resetPasswordText(true, false);
            return;
        } else {
            switchUser(userId);
            KeyguardCompatibilityHelperForP.sanitizePassword(this.mLockPatternUtils);
            this.mCallback.reportUnlockAttempt(userId, true, 0);
            this.mDismissing = true;
            this.mCallback.dismiss(true, userId);
            AnalyticsHelper.recordUnlockWay("pw");
        }
        resetPasswordText(true, !matched);
    }

    /* access modifiers changed from: protected */
    public void handleAttemptLockout(long elapsedRealtimeDeadline) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mCallback.handleAttemptLockout(elapsedRealtimeDeadline - elapsedRealtime);
        setPasswordEntryEnabled(false);
        AnonymousClass2 r4 = new CountDownTimer(((long) Math.ceil(((double) (elapsedRealtimeDeadline - elapsedRealtime)) / 1000.0d)) * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                KeyguardAbsKeyInputView.this.resetState();
            }
        };
        this.mCountdownTimer = r4.start();
    }

    /* access modifiers changed from: protected */
    public void onUserInput() {
        if (this.mCallback != null) {
            this.mCallback.userActivity();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        onUserInput();
        return false;
    }

    public boolean needsInput() {
        return false;
    }

    public void onPause() {
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
            this.mPendingLockCheck = null;
        }
    }

    public void onResume(int reason) {
        reset();
    }

    public void showPromptReason(int reason) {
        if (reason != 0) {
            String promptReasonString = getPromptReasonString(reason);
            if (!TextUtils.isEmpty(promptReasonString)) {
                this.mKeyguardBouncerMessageView.showMessage(this.mContext.getResources().getString(R.string.input_password_hint_text), promptReasonString);
            }
        }
    }

    /* access modifiers changed from: protected */
    public String getPromptReasonString(int reason) {
        Resources resources = this.mContext.getResources();
        switch (reason) {
            case 0:
                return "";
            case 1:
                return resources.getString(R.string.input_password_after_boot_msg);
            case 2:
                long timeout = getRequiredStrongAuthTimeout();
                return resources.getQuantityString(R.plurals.input_password_after_timeout_msg, (int) TimeUnit.MILLISECONDS.toHours(timeout), new Object[]{Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeout))});
            case 3:
                return resources.getString(R.string.kg_prompt_reason_device_admin);
            case 4:
                return resources.getString(R.string.kg_prompt_reason_user_request);
            default:
                return resources.getString(R.string.kg_prompt_reason_timeout_password);
        }
    }

    public void showMessage(String title, String message, int color) {
        this.mKeyguardBouncerMessageView.showMessage(title, message, color);
    }

    public void applyHintAnimation(long offset) {
        this.mKeyguardBouncerMessageView.applyHintAnimation(offset);
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        this.mFaceUnlockView.setVisibility(4);
        return false;
    }
}
