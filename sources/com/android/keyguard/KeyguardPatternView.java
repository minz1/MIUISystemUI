package com.android.keyguard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.MiuiLockPatternView;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.android.systemui.R;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KeyguardPatternView extends MiuiKeyguardPasswordView implements KeyguardSecurityView, AppearAnimationCreator<MiuiLockPatternView.CellState> {
    /* access modifiers changed from: private */
    public boolean mAppearAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    /* access modifiers changed from: private */
    public Runnable mCancelPatternRunnable;
    private CountDownTimer mCountdownTimer;
    /* access modifiers changed from: private */
    public boolean mDisappearAnimatePending;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    /* access modifiers changed from: private */
    public Runnable mDisappearFinishRunnable;
    private int mDisappearYTranslation;
    private long mLastPokeTime;
    /* access modifiers changed from: private */
    public MiuiLockPatternView mLockPatternView;
    /* access modifiers changed from: private */
    public AsyncTask<?, ?, ?> mPendingLockCheck;
    private Rect mTempRect;

    private class UnlockPatternListener implements MiuiLockPatternView.OnPatternListener {
        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> list) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            KeyguardPatternView.this.mLockPatternView.disableInput();
            if (KeyguardPatternView.this.mPendingLockCheck != null) {
                KeyguardPatternView.this.mPendingLockCheck.cancel(false);
            }
            final int userId = KeyguardUpdateMonitor.getCurrentUser();
            if (pattern.size() < 4) {
                KeyguardPatternView.this.mLockPatternView.enableInput();
                onPatternChecked(userId, false, 0, false);
                return;
            }
            if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(3);
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(4);
            }
            AsyncTask unused = KeyguardPatternView.this.mPendingLockCheck = LockPatternChecker.checkPatternForUsers(new LockPatternUtilsWrapper(KeyguardPatternView.this.mLockPatternUtils), KeyguardPatternView.this.mLockPatternUtils, pattern, KeyguardPatternView.this.mContext, new OnCheckForUsersCallback() {
                public void onEarlyMatched() {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(3);
                    }
                    UnlockPatternListener.this.onPatternChecked(userId, true, 0, true);
                }

                public void onChecked(boolean matched, int userIdMatched, int timeoutMs) {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(4);
                    }
                    KeyguardPatternView.this.mLockPatternView.enableInput();
                    AsyncTask unused = KeyguardPatternView.this.mPendingLockCheck = null;
                    if (MiuiKeyguardUtils.needPasswordCheck(matched, userIdMatched)) {
                        UnlockPatternListener.this.onPatternChecked(userIdMatched, matched, timeoutMs, true);
                    }
                }
            });
            if (pattern.size() > 2) {
                KeyguardPatternView.this.mCallback.userActivity();
            }
        }

        /* access modifiers changed from: private */
        public void onPatternChecked(int userId, boolean matched, int timeoutMs, boolean isValidPattern) {
            if (!matched) {
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(MiuiLockPatternView.DisplayMode.Wrong);
                if (isValidPattern) {
                    KeyguardPatternView.this.mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                    if (timeoutMs > 0) {
                        KeyguardPatternView.this.mKeyguardUpdateMonitor.stopFaceUnlock();
                        KeyguardPatternView.this.handleAttemptLockout(KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs));
                    }
                }
                KeyguardPatternView.this.mVibrator.vibrate(150);
                if (timeoutMs == 0) {
                    KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 1500);
                }
            } else if (KeyguardPatternView.this.allowUnlock(userId)) {
                KeyguardPatternView.this.switchUser(userId);
                KeyguardCompatibilityHelperForP.sanitizePassword(KeyguardPatternView.this.mLockPatternUtils);
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(userId, true, 0);
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(MiuiLockPatternView.DisplayMode.Correct);
                KeyguardPatternView.this.mCallback.dismiss(true, userId);
                AnalyticsHelper.recordUnlockWay("pw");
            }
        }
    }

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public KeyguardPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCountdownTimer = null;
        this.mLastPokeTime = -7000;
        this.mCancelPatternRunnable = new Runnable() {
            public void run() {
                KeyguardPatternView.this.mLockPatternView.clearPattern();
            }
        };
        this.mTempRect = new Rect();
        AppearAnimationUtils appearAnimationUtils = new AppearAnimationUtils(context, 220, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, 17563662));
        this.mAppearAnimationUtils = appearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils = new DisappearAnimationUtils(context, 125, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtils = disappearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils2 = new DisappearAnimationUtils(context, 187, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtilsLocked = disappearAnimationUtils2;
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = this.mLockPatternUtils == null ? new LockPatternUtils(this.mContext) : this.mLockPatternUtils;
        this.mLockPatternView = (MiuiLockPatternView) findViewById(R.id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        setPositionForFod();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        long elapsed = SystemClock.elapsedRealtime() - this.mLastPokeTime;
        if (result && elapsed > 6900) {
            this.mLastPokeTime = SystemClock.elapsedRealtime();
        }
        boolean z = false;
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(this.mLockPatternView, this.mTempRect);
        ev.offsetLocation((float) this.mTempRect.left, (float) this.mTempRect.top);
        if (this.mLockPatternView.dispatchTouchEvent(ev) || result) {
            z = true;
        }
        boolean result2 = z;
        ev.offsetLocation((float) (-this.mTempRect.left), (float) (-this.mTempRect.top));
        return result2;
    }

    public void reset() {
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        }
    }

    /* access modifiers changed from: protected */
    public void handleWrongPassword() {
        this.mLockPatternView.setDisplayMode(MiuiLockPatternView.DisplayMode.Wrong);
        this.mVibrator.vibrate(150);
        this.mLockPatternView.postDelayed(this.mCancelPatternRunnable, 1500);
    }

    /* access modifiers changed from: private */
    public void handleAttemptLockout(long elapsedRealtimeDeadline) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mCallback.handleAttemptLockout(elapsedRealtimeDeadline - elapsedRealtime);
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setEnabled(false);
        AnonymousClass2 r4 = new CountDownTimer(((long) Math.ceil(((double) (elapsedRealtimeDeadline - elapsedRealtime)) / 1000.0d)) * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                KeyguardPatternView.this.mLockPatternView.setEnabled(true);
            }
        };
        this.mCountdownTimer = r4.start();
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
                return resources.getQuantityString(R.plurals.input_pattern_after_timeout_msg, (int) TimeUnit.MILLISECONDS.toHours(timeout), new Object[]{Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeout))});
            case 3:
                return resources.getString(R.string.kg_prompt_reason_device_admin);
            case 4:
                return resources.getString(R.string.kg_prompt_reason_user_request);
            default:
                return resources.getString(R.string.kg_prompt_reason_timeout_pattern);
        }
    }

    public void showMessage(String title, String message, int color) {
        this.mKeyguardBouncerMessageView.showMessage(title, message, color);
    }

    public void applyHintAnimation(long offset) {
        this.mKeyguardBouncerMessageView.applyHintAnimation(offset);
    }

    public void startAppearAnimation() {
        this.mKeyguardBouncerMessageView.setVisibility(0);
        enableClipping(false);
        setAlpha(1.0f);
        if (this.mKeyguardUpdateMonitor.getBLEUnlockState() != MiuiBleUnlockHelper.BLEUnlockState.SUCCEED) {
            this.mAppearAnimating = true;
            this.mDisappearAnimatePending = false;
            setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
            AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
            this.mAppearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
                public void run() {
                    KeyguardPatternView.this.enableClipping(true);
                    boolean unused = KeyguardPatternView.this.mAppearAnimating = false;
                    if (KeyguardPatternView.this.mDisappearAnimatePending) {
                        boolean unused2 = KeyguardPatternView.this.mDisappearAnimatePending = false;
                        KeyguardPatternView.this.startDisappearAnimation(KeyguardPatternView.this.mDisappearFinishRunnable);
                    }
                }
            }, this);
        }
    }

    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        float durationMultiplier;
        DisappearAnimationUtils disappearAnimationUtils;
        if (this.mAppearAnimating) {
            this.mDisappearAnimatePending = true;
            this.mDisappearFinishRunnable = finishRunnable;
            return true;
        }
        this.mKeyguardBouncerMessageView.setVisibility(4);
        this.mFaceUnlockView.setVisibility(4);
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            durationMultiplier = 1.5f;
        } else {
            durationMultiplier = 1.0f;
        }
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, (long) (300.0f * durationMultiplier), -this.mDisappearAnimationUtils.getStartTranslation(), this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            public void run() {
                Log.d("SecurityPatternView", "startDisappearAnimation finish");
                KeyguardPatternView.this.enableClipping(true);
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        }, this);
        return true;
    }

    /* access modifiers changed from: private */
    public void enableClipping(boolean enable) {
        setClipChildren(enable);
        setClipToPadding(enable);
    }

    public void createAnimation(MiuiLockPatternView.CellState animatedCell, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable finishListener) {
        this.mLockPatternView.startCellStateAnimation(animatedCell, 1.0f, appearing ? 1.0f : 0.0f, appearing ? translationY : 0.0f, appearing ? 0.0f : translationY, appearing ? 0.0f : 1.0f, 1.0f, delay, duration, interpolator, finishListener);
        if (finishListener != null) {
            this.mAppearAnimationUtils.createAnimation((View) this.mEmergencyCarrierArea, delay, duration, translationY, appearing, interpolator, (Runnable) null);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationFontScaleChanged() {
        int textSize = getResources().getDimensionPixelSize(R.dimen.miui_keyguard_view_eca_text_size);
        this.mEmergencyButton.setTextSize(0, (float) textSize);
        this.mBackButton.setTextSize(0, (float) textSize);
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationOrientationChanged() {
        LinearLayout.LayoutParams messageLayoutParams = (LinearLayout.LayoutParams) this.mKeyguardBouncerMessageView.getLayoutParams();
        messageLayoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_bouncer_message_view_margin_top);
        this.mKeyguardBouncerMessageView.setLayoutParams(messageLayoutParams);
    }

    private void setPositionForFod() {
        if (MiuiKeyguardUtils.isGxzwSensor() && MiuiGxzwManager.getInstance().supportFodInBouncer()) {
            Display display = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0);
            Point point = new Point();
            display.getRealSize(point);
            int screenHeight = Math.max(point.x, point.y);
            int containerHeight = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pattern_layout_height);
            int patternHeight = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pattern_view_pattern_view_height_width);
            int margin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pattern_view_pattern_view_margin_bottom);
            int itemHieght = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pattern_view_eca_height);
            int ecaTopMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pattern_view_eca_fod_top_margin);
            MiuiGxzwUtils.caculateGxzwIconSize(getContext());
            int fodCenterY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
            View container = findViewById(R.id.container);
            LinearLayout.LayoutParams containerLayoutParams = (LinearLayout.LayoutParams) container.getLayoutParams();
            containerLayoutParams.bottomMargin = (screenHeight - (((containerHeight - patternHeight) - margin) - (itemHieght / 2))) - fodCenterY;
            containerLayoutParams.height = containerHeight + ecaTopMargin;
            container.setLayoutParams(containerLayoutParams);
            View eca = findViewById(R.id.keyguard_selector_fade_container);
            LinearLayout.LayoutParams ecaLayoutParams = (LinearLayout.LayoutParams) eca.getLayoutParams();
            ecaLayoutParams.topMargin = ecaTopMargin;
            eca.setLayoutParams(ecaLayoutParams);
        }
    }
}
