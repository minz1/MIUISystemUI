package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.systemui.R;

public class KeyguardSecurityContainer extends FrameLayout implements KeyguardSecurityView {
    /* access modifiers changed from: private */
    public KeyguardSecurityCallback mCallback;
    private KeyguardSecurityModel.SecurityMode mCurrentSecuritySelection;
    /* access modifiers changed from: private */
    public View mFogetPasswordMethod;
    /* access modifiers changed from: private */
    public View mFogetPasswordSuggestion;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public View mLockoutView;
    private KeyguardSecurityCallback mNullCallback;
    /* access modifiers changed from: private */
    public SecurityCallback mSecurityCallback;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public interface SecurityCallback {
        boolean dismiss(boolean z, int i);

        void finish(boolean z, int i);

        void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z);

        void reset();

        void userActivity();
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardSecurityContainer(Context context) {
        this(context, null, 0);
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs, int defStyle) {
        super(new ContextThemeWrapper(context, 16974120), attrs, defStyle);
        this.mCurrentSecuritySelection = KeyguardSecurityModel.SecurityMode.Invalid;
        this.mCallback = new KeyguardSecurityCallback() {
            public void userActivity() {
                if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                    KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
                }
            }

            public void dismiss(boolean authenticated, int targetId) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(authenticated, targetId);
            }

            public void reportUnlockAttempt(int userId, boolean success, int timeoutMs) {
                KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
                if (success) {
                    monitor.clearFailedUnlockAttempts();
                    KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(userId);
                    return;
                }
                KeyguardSecurityContainer.this.reportFailedUnlockAttempt(userId, timeoutMs);
            }

            public void handleAttemptLockout(long deadline) {
                KeyguardSecurityContainer.this.showLockoutView(deadline);
            }

            public void reset() {
                KeyguardSecurityContainer.this.mSecurityCallback.reset();
                LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(KeyguardSecurityContainer.this.mContext, "Wallpaper_Uncovered");
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() {
            public void userActivity() {
            }

            public void reportUnlockAttempt(int userId, boolean success, int timeoutMs) {
            }

            public void dismiss(boolean securityVerified, int targetUserId) {
            }

            public void reset() {
            }

            public void handleAttemptLockout(long deadline) {
            }
        };
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void setSecurityCallback(SecurityCallback callback) {
        this.mSecurityCallback = callback;
    }

    public boolean onBackPressed() {
        if (!(this.mFogetPasswordSuggestion == null || this.mFogetPasswordMethod == null)) {
            if (this.mFogetPasswordSuggestion.getVisibility() == 0) {
                this.mFogetPasswordSuggestion.setVisibility(4);
                this.mFogetPasswordMethod.setVisibility(0);
                return true;
            } else if (this.mFogetPasswordMethod.getVisibility() == 0) {
                this.mFogetPasswordMethod.setVisibility(4);
                setLockoutViewVisible(0);
                return true;
            }
        }
        return false;
    }

    public void onResume(int reason) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onResume(reason);
        }
    }

    public void onPause() {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onPause();
        }
    }

    public void startAppearAnimation() {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).startAppearAnimation();
        }
    }

    public boolean startDisappearAnimation(Runnable onFinishRunnable) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            return getSecurityView(this.mCurrentSecuritySelection).startDisappearAnimation(onFinishRunnable);
        }
        return false;
    }

    public CharSequence getCurrentSecurityModeContentDescription() {
        View v = (View) getSecurityView(this.mCurrentSecuritySelection);
        if (v != null) {
            return v.getContentDescription();
        }
        return "";
    }

    /* JADX WARNING: type inference failed for: r5v6, types: [android.view.View] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.keyguard.KeyguardSecurityView getSecurityView(com.android.keyguard.KeyguardSecurityModel.SecurityMode r10) {
        /*
            r9 = this;
            int r0 = r9.getSecurityViewIdForMode(r10)
            r1 = 0
            com.android.keyguard.KeyguardSecurityViewFlipper r2 = r9.mSecurityViewFlipper
            int r2 = r2.getChildCount()
            r3 = 0
            r4 = r3
        L_0x000d:
            if (r4 >= r2) goto L_0x0028
            com.android.keyguard.KeyguardSecurityViewFlipper r5 = r9.mSecurityViewFlipper
            android.view.View r5 = r5.getChildAt(r4)
            int r5 = r5.getId()
            if (r5 != r0) goto L_0x0025
            com.android.keyguard.KeyguardSecurityViewFlipper r5 = r9.mSecurityViewFlipper
            android.view.View r5 = r5.getChildAt(r4)
            r1 = r5
            com.android.keyguard.KeyguardSecurityView r1 = (com.android.keyguard.KeyguardSecurityView) r1
            goto L_0x0028
        L_0x0025:
            int r4 = r4 + 1
            goto L_0x000d
        L_0x0028:
            int r4 = r9.getLayoutIdFor(r10)
            if (r1 != 0) goto L_0x005d
            if (r4 == 0) goto L_0x005d
            android.content.Context r5 = r9.mContext
            android.view.LayoutInflater r5 = android.view.LayoutInflater.from(r5)
            java.lang.String r6 = "KeyguardSecurityView"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "inflating id = "
            r7.append(r8)
            r7.append(r4)
            java.lang.String r7 = r7.toString()
            android.util.Log.v(r6, r7)
            com.android.keyguard.KeyguardSecurityViewFlipper r6 = r9.mSecurityViewFlipper
            android.view.View r3 = r5.inflate(r4, r6, r3)
            com.android.keyguard.KeyguardSecurityViewFlipper r6 = r9.mSecurityViewFlipper
            r6.addView(r3)
            r9.updateSecurityView(r3)
            r1 = r3
            com.android.keyguard.KeyguardSecurityView r1 = (com.android.keyguard.KeyguardSecurityView) r1
        L_0x005d:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSecurityContainer.getSecurityView(com.android.keyguard.KeyguardSecurityModel$SecurityMode):com.android.keyguard.KeyguardSecurityView");
    }

    private void updateSecurityView(View view) {
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView ksv = (KeyguardSecurityView) view;
            ksv.setKeyguardCallback(this.mCallback);
            ksv.setLockPatternUtils(this.mLockPatternUtils);
            return;
        }
        Log.w("KeyguardSecurityView", "View " + view + " is not a KeyguardSecurityView");
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(R.id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityModel.setLockPatternUtils(utils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    private void showDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(title).setMessage(message).setCancelable(false).setNeutralButton(R.string.ok, null).create();
        if (!(this.mContext instanceof Activity)) {
            dialog.getWindow().setType(2009);
        }
        dialog.show();
    }

    private void showAlmostAtWipeDialog(int attempts, int remaining, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_wipe, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
            case 2:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_erase_profile, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
            case 3:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_erase_user, new Object[]{Integer.valueOf(attempts), Integer.valueOf(remaining)});
                break;
        }
        showDialog(null, message);
    }

    private void showWipeDialog(int attempts, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_wiping, new Object[]{Integer.valueOf(attempts)});
                break;
            case 2:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_erasing_profile, new Object[]{Integer.valueOf(attempts)});
                break;
            case 3:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_erasing_user, new Object[]{Integer.valueOf(attempts)});
                break;
        }
        showDialog(null, message);
    }

    /* access modifiers changed from: private */
    public void reportFailedUnlockAttempt(int userId, int timeoutMs) {
        int remainingBeforeWipe;
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int failedAttempts = monitor.getFailedUnlockAttempts(userId) + 1;
        Log.d("KeyguardSecurityView", "reportFailedPatternAttempt: #" + failedAttempts);
        DevicePolicyManager dpm = this.mLockPatternUtils.getDevicePolicyManager();
        int failedAttemptsBeforeWipe = dpm.getMaximumFailedPasswordsForWipe(null, userId);
        if (failedAttemptsBeforeWipe > 0) {
            remainingBeforeWipe = failedAttemptsBeforeWipe - failedAttempts;
        } else {
            remainingBeforeWipe = Integer.MAX_VALUE;
        }
        if (remainingBeforeWipe < 5) {
            int expiringUser = dpm.getProfileWithMinimumFailedPasswordsForWipe(userId);
            int userType = 1;
            if (expiringUser == userId) {
                if (expiringUser != 0) {
                    userType = 3;
                }
            } else if (expiringUser != -10000) {
                userType = 2;
            }
            if (remainingBeforeWipe > 0) {
                showAlmostAtWipeDialog(failedAttempts, remainingBeforeWipe, userType);
            } else {
                Slog.i("KeyguardSecurityView", "Too many unlock attempts; user " + expiringUser + " will be wiped!");
                showWipeDialog(failedAttempts, userType);
            }
        }
        monitor.reportFailedStrongAuthUnlockAttempt(userId);
        this.mLockPatternUtils.reportFailedPasswordAttempt(userId);
        if (timeoutMs > 0) {
            LockPatternUtilsCompat.reportPasswordLockout(this.mLockPatternUtils, timeoutMs, userId);
            showLockoutView((long) timeoutMs);
            if (MiuiKeyguardUtils.isGxzwSensor()) {
                MiuiGxzwManager.getInstance().setUnlockLockout(true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void showLockoutView(long timeoutMs) {
        if (this.mLockoutView == null) {
            loadLockoutView();
        } else if (this.mLockoutView.getVisibility() == 0) {
            return;
        }
        this.mLockoutView.setVisibility(0);
        Animation inAnimation = new AlphaAnimation(0.0f, 1.0f);
        inAnimation.setDuration(500);
        this.mLockoutView.startAnimation(inAnimation);
        final TextView textView = (TextView) this.mLockoutView.findViewById(R.id.phone_locked_timeout_id);
        AnonymousClass1 r1 = new CountDownTimer(((long) Math.ceil(((double) timeoutMs) / 1000.0d)) * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                KeyguardSecurityContainer.this.updateCountDown(textView, (long) ((int) Math.round(((double) millisUntilFinished) / 1000.0d)));
            }

            public void onFinish() {
                KeyguardSecurityContainer.this.hideLockoutView();
            }
        };
        r1.start();
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setShowLockoutView(true);
        }
    }

    /* access modifiers changed from: private */
    public void updateCountDown(TextView countDownView, long leftSeconds) {
        if (leftSeconds <= 60) {
            countDownView.setText(getResources().getQuantityString(R.plurals.phone_locked_timeout_seconds_string, (int) leftSeconds, new Object[]{Long.valueOf(leftSeconds)}));
            return;
        }
        countDownView.setText(getResources().getQuantityString(R.plurals.phone_locked_timeout_minutes_string, ((int) leftSeconds) / 60, new Object[]{Long.valueOf(leftSeconds / 60)}));
    }

    /* access modifiers changed from: protected */
    public void hideLockoutView() {
        hideLockoutView(true);
    }

    /* access modifiers changed from: protected */
    public void hideLockoutView(boolean animated) {
        if (animated) {
            Animation outAnimation = AnimationUtils.loadAnimation(this.mContext, 17432577);
            outAnimation.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    KeyguardSecurityContainer.this.mLockoutView.setVisibility(4);
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
            this.mLockoutView.startAnimation(outAnimation);
        } else {
            this.mLockoutView.clearAnimation();
            this.mLockoutView.setVisibility(4);
        }
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setShowLockoutView(false);
        }
    }

    private void loadLockoutView() {
        View lockviewWrapper = View.inflate(getContext(), R.layout.miui_unlockscreen_lockout, null);
        ((ViewGroup) getParent()).addView(lockviewWrapper);
        this.mLockoutView = lockviewWrapper.findViewById(R.id.unlockscreen_lockout_id);
        this.mFogetPasswordMethod = this.mLockoutView.findViewById(R.id.forget_password_hint_container);
        this.mFogetPasswordSuggestion = this.mLockoutView.findViewById(R.id.forget_password_suggesstion);
        ((Button) this.mLockoutView.findViewById(R.id.foget_password)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardSecurityContainer.this.setLockoutViewVisible(4);
                KeyguardSecurityContainer.this.mFogetPasswordMethod.setVisibility(0);
                ((TextView) KeyguardSecurityContainer.this.mFogetPasswordMethod.findViewById(R.id.forget_password_method_content)).setText(Html.fromHtml(KeyguardSecurityContainer.this.getResources().getString(R.string.phone_locked_foget_password_method_content)));
            }
        });
        this.mLockoutView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 0) {
                    KeyguardSecurityContainer.this.mCallback.userActivity();
                }
                return true;
            }
        });
        this.mFogetPasswordMethod.findViewById(R.id.forget_password_method_next).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardSecurityContainer.this.mFogetPasswordMethod.setVisibility(4);
                KeyguardSecurityContainer.this.setLockoutViewVisible(4);
                KeyguardSecurityContainer.this.mFogetPasswordSuggestion.setVisibility(0);
                ((TextView) KeyguardSecurityContainer.this.mFogetPasswordSuggestion.findViewById(R.id.forget_password_suggesstion_one)).setText(Html.fromHtml(KeyguardSecurityContainer.this.getResources().getString(R.string.phone_locked_forget_password_suggesstion_one_content), new Html.ImageGetter() {
                    public Drawable getDrawable(String source) {
                        if (source == null) {
                            return null;
                        }
                        Drawable bitmapDrawable = KeyguardSecurityContainer.this.getResources().getDrawable(R.drawable.miui_keyguard_forget_password_mi);
                        bitmapDrawable.setBounds(0, 0, bitmapDrawable.getIntrinsicWidth(), bitmapDrawable.getIntrinsicHeight());
                        return bitmapDrawable;
                    }
                }, null));
                ((TextView) KeyguardSecurityContainer.this.mFogetPasswordSuggestion.findViewById(R.id.forget_password_suggesstion_two)).setText(Html.fromHtml(KeyguardSecurityContainer.this.getResources().getString(R.string.phone_locked_forget_password_suggesstion_two_content)));
            }
        });
        this.mFogetPasswordMethod.findViewById(R.id.forget_password_method_back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardSecurityContainer.this.mFogetPasswordMethod.setVisibility(4);
                KeyguardSecurityContainer.this.setLockoutViewVisible(0);
            }
        });
        this.mFogetPasswordSuggestion.findViewById(R.id.forget_password_suggesstion_ok).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardSecurityContainer.this.mFogetPasswordSuggestion.setVisibility(4);
                KeyguardSecurityContainer.this.mFogetPasswordMethod.setVisibility(4);
                KeyguardSecurityContainer.this.setLockoutViewVisible(0);
            }
        });
    }

    /* access modifiers changed from: private */
    public void setLockoutViewVisible(int visible) {
        this.mLockoutView.findViewById(R.id.phone_locked_textview).setVisibility(visible);
        this.mLockoutView.findViewById(R.id.phone_locked_timeout_id).setVisibility(visible);
        this.mLockoutView.findViewById(R.id.foget_password).setVisibility(visible);
    }

    /* access modifiers changed from: package-private */
    public void showPrimarySecurityScreen(boolean turningOff) {
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(turningOff=" + turningOff + ")");
        showSecurityScreen(securityMode);
    }

    /* access modifiers changed from: package-private */
    public boolean showNextSecurityScreenOrFinish(boolean authenticated, int targetUserId) {
        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish(" + authenticated + ")");
        boolean finish = false;
        boolean strongAuth = false;
        if (!this.mUpdateMonitor.getUserCanSkipBouncer(targetUserId)) {
            if (KeyguardSecurityModel.SecurityMode.None != this.mCurrentSecuritySelection) {
                if (authenticated) {
                    switch (this.mCurrentSecuritySelection) {
                        case Pattern:
                        case PIN:
                        case Password:
                            strongAuth = true;
                            finish = true;
                            break;
                        case SimPin:
                        case SimPuk:
                            KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode(targetUserId);
                            if (securityMode == KeyguardSecurityModel.SecurityMode.None && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
                                finish = true;
                                break;
                            } else {
                                showSecurityScreen(securityMode);
                                break;
                            }
                            break;
                        default:
                            Log.v("KeyguardSecurityView", "Bad security screen " + this.mCurrentSecuritySelection + ", fail safe");
                            showPrimarySecurityScreen(false);
                            break;
                    }
                }
            } else {
                KeyguardSecurityModel.SecurityMode securityMode2 = this.mSecurityModel.getSecurityMode(targetUserId);
                if (KeyguardSecurityModel.SecurityMode.None == securityMode2) {
                    finish = true;
                } else {
                    showSecurityScreen(securityMode2);
                }
            }
        } else {
            finish = true;
        }
        if (finish) {
            this.mSecurityCallback.finish(strongAuth, targetUserId);
        }
        return finish;
    }

    private void showSecurityScreen(KeyguardSecurityModel.SecurityMode securityMode) {
        Log.d("KeyguardSecurityView", "showSecurityScreen(" + securityMode + ")");
        if (securityMode != this.mCurrentSecuritySelection) {
            KeyguardSecurityView oldView = getSecurityView(this.mCurrentSecuritySelection);
            KeyguardSecurityView newView = getSecurityView(securityMode);
            if (oldView != null) {
                oldView.onPause();
                oldView.setKeyguardCallback(this.mNullCallback);
            }
            if (securityMode != KeyguardSecurityModel.SecurityMode.None) {
                newView.onResume(2);
                newView.setKeyguardCallback(this.mCallback);
            }
            int childCount = this.mSecurityViewFlipper.getChildCount();
            int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= childCount) {
                    break;
                } else if (this.mSecurityViewFlipper.getChildAt(i).getId() == securityViewIdForMode) {
                    this.mSecurityViewFlipper.setDisplayedChild(i);
                    break;
                } else {
                    i++;
                }
            }
            this.mCurrentSecuritySelection = securityMode;
            SecurityCallback securityCallback = this.mSecurityCallback;
            if (securityMode != KeyguardSecurityModel.SecurityMode.None && newView.needsInput()) {
                z = true;
            }
            securityCallback.onSecurityModeChanged(securityMode, z);
        }
    }

    private int getSecurityViewIdForMode(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return R.id.keyguard_pattern_view;
            case PIN:
                return R.id.keyguard_pin_view;
            case Password:
                return R.id.keyguard_password_view;
            case SimPin:
                return R.id.keyguard_sim_pin_view;
            case SimPuk:
                return R.id.keyguard_sim_puk_view;
            default:
                return 0;
        }
    }

    /* access modifiers changed from: protected */
    public int getLayoutIdFor(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return R.layout.keyguard_pattern_view;
            case PIN:
                return R.layout.keyguard_pin_view;
            case Password:
                return R.layout.keyguard_password_view;
            case SimPin:
                return R.layout.keyguard_sim_pin_view;
            case SimPuk:
                return R.layout.keyguard_sim_puk_view;
            default:
                return 0;
        }
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mCurrentSecuritySelection;
    }

    public boolean needsInput() {
        return this.mSecurityViewFlipper.needsInput();
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mSecurityViewFlipper.setKeyguardCallback(callback);
    }

    public void showPromptReason(int reason) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            if (reason != 0) {
                Log.i("KeyguardSecurityView", "Strong auth required, reason: " + reason);
            }
            getSecurityView(this.mCurrentSecuritySelection).showPromptReason(reason);
        }
    }

    public void showMessage(String title, String message, int color) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).showMessage(title, message, color);
        }
    }

    public void applyHintAnimation(long offset) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).applyHintAnimation(offset);
        }
    }
}
