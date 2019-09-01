package com.android.keyguard;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.security.MiuiLockPatternUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.PasswordTextView;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.android.systemui.R;

public class KeyguardPINView extends KeyguardPinBasedInputView implements PasswordTextView.TextChangeListener {
    /* access modifiers changed from: private */
    public boolean mAppearAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private ViewGroup mContainer;
    /* access modifiers changed from: private */
    public boolean mDisappearAnimatePending;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    /* access modifiers changed from: private */
    public Runnable mDisappearFinishRunnable;
    private int mDisappearYTranslation;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private int mPasswordLength;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private View[][] mViews;

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context);
        Context context2 = context;
        DisappearAnimationUtils disappearAnimationUtils = new DisappearAnimationUtils(context2, 125, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtils = disappearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils2 = new DisappearAnimationUtils(context2, 187, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtilsLocked = disappearAnimationUtils2;
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mPasswordLength = (int) new MiuiLockPatternUtils(context).getLockPasswordLength(KeyguardUpdateMonitor.getCurrentUser());
        if (this.mPasswordLength < 4) {
            this.mPasswordLength = 4;
            Log.e("KeyguardPINView", "get password length = " + this.mPasswordLength);
        }
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        super.resetState();
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R.id.pinEntry;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPasswordEntry.removeTextChangedListener();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPasswordEntry.addTextChangedListener(this);
        this.mContainer = (ViewGroup) findViewById(R.id.container);
        this.mRow0 = (ViewGroup) findViewById(R.id.row0);
        this.mRow1 = (ViewGroup) findViewById(R.id.row1);
        this.mRow2 = (ViewGroup) findViewById(R.id.row2);
        this.mRow3 = (ViewGroup) findViewById(R.id.row3);
        this.mViews = new View[][]{new View[]{this.mRow0, null, null}, new View[]{findViewById(R.id.key1), findViewById(R.id.key2), findViewById(R.id.key3)}, new View[]{findViewById(R.id.key4), findViewById(R.id.key5), findViewById(R.id.key6)}, new View[]{findViewById(R.id.key7), findViewById(R.id.key8), findViewById(R.id.key9)}, new View[]{null, findViewById(R.id.key0), null}, new View[]{findViewById(R.id.emergency_call_button), null, findViewById(R.id.delete_button), findViewById(R.id.back_button)}};
        setPositionForFod();
    }

    public void startAppearAnimation() {
        this.mKeyguardBouncerMessageView.setVisibility(0);
        setAlpha(1.0f);
        if (this.mKeyguardUpdateMonitor.getBLEUnlockState() != MiuiBleUnlockHelper.BLEUnlockState.SUCCEED) {
            this.mAppearAnimating = true;
            this.mDisappearAnimatePending = false;
            setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
            AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
            this.mAppearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
                public void run() {
                    boolean unused = KeyguardPINView.this.mAppearAnimating = false;
                    if (KeyguardPINView.this.mDisappearAnimatePending) {
                        boolean unused2 = KeyguardPINView.this.mDisappearAnimatePending = false;
                        KeyguardPINView.this.startDisappearAnimation(KeyguardPINView.this.mDisappearFinishRunnable);
                    }
                }
            });
        }
    }

    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        DisappearAnimationUtils disappearAnimationUtils;
        if (this.mAppearAnimating) {
            this.mDisappearAnimatePending = true;
            this.mDisappearFinishRunnable = finishRunnable;
            return true;
        }
        this.mKeyguardBouncerMessageView.setVisibility(4);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 280, (float) this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            public void run() {
                Log.d("KeyguardPINView", "startDisappearAnimation finish");
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        });
        return true;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onTextChanged(int count) {
        if (count == this.mPasswordLength) {
            verifyPasswordAndUnlock();
        }
        if (count == 0) {
            this.mBackButton.setVisibility(0);
            this.mDeleteButton.setVisibility(8);
            return;
        }
        this.mBackButton.setVisibility(8);
        this.mDeleteButton.setVisibility(0);
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationFontScaleChanged() {
        int textSize = getResources().getDimensionPixelSize(R.dimen.miui_keyguard_view_eca_text_size);
        this.mEmergencyButton.setTextSize(0, (float) textSize);
        this.mBackButton.setTextSize(0, (float) textSize);
        this.mDeleteButton.setTextSize(0, (float) textSize);
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationOrientationChanged() {
        LinearLayout.LayoutParams containerLayoutParams = (LinearLayout.LayoutParams) this.mContainer.getLayoutParams();
        containerLayoutParams.height = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_rows_layout_height);
        this.mContainer.setLayoutParams(containerLayoutParams);
        LinearLayout.LayoutParams messageLayoutParams = (LinearLayout.LayoutParams) this.mKeyguardBouncerMessageView.getLayoutParams();
        messageLayoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_bouncer_message_view_margin_top);
        this.mKeyguardBouncerMessageView.setLayoutParams(messageLayoutParams);
    }

    /* access modifiers changed from: protected */
    public void handleWrongPassword() {
        TranslateAnimation translateAnimation = new TranslateAnimation(1, -0.1f, 1, 0.1f, 1, 0.0f, 1, 0.0f);
        translateAnimation.setDuration(30);
        translateAnimation.setRepeatCount(3);
        translateAnimation.setRepeatMode(2);
        this.mPasswordEntry.startAnimation(translateAnimation);
    }

    private void setPositionForFod() {
        if (MiuiKeyguardUtils.isGxzwSensor() && MiuiGxzwManager.getInstance().supportFodInBouncer()) {
            DisplayManager displayManager = (DisplayManager) getContext().getSystemService("display");
            Display display = displayManager.getDisplay(0);
            Point point = new Point();
            display.getRealSize(point);
            int screenHeight = Math.max(point.x, point.y);
            int height = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_rows_layout_height);
            int row0Margin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_row0_margin_bottom);
            int row1Row2Row3Margin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_row1_row2_row3_margin_bottom);
            int row4Margin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_row4_margin_bottom);
            int row4MarginFod = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_row4_margin_bottom_fod);
            int row5Margin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_row5_margin_bottom);
            MiuiGxzwUtils.caculateGxzwIconSize(getContext());
            int fodCenterY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
            LinearLayout.LayoutParams containerLayoutParams = (LinearLayout.LayoutParams) this.mContainer.getLayoutParams();
            containerLayoutParams.bottomMargin = ((screenHeight - ((((((height - row0Margin) - (row1Row2Row3Margin * 3)) - row4Margin) - row5Margin) / 6) / 2)) - row5Margin) - fodCenterY;
            containerLayoutParams.height = height + row4MarginFod;
            this.mContainer.setLayoutParams(containerLayoutParams);
            View row4 = findViewById(R.id.row4);
            LinearLayout.LayoutParams row4LayoutParams = (LinearLayout.LayoutParams) row4.getLayoutParams();
            DisplayManager displayManager2 = displayManager;
            row4LayoutParams.bottomMargin = row4Margin + row4MarginFod;
            row4.setLayoutParams(row4LayoutParams);
        }
    }
}
