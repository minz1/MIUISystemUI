package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.android.systemui.R;
import miui.view.MiuiKeyBoardView;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements KeyguardSecurityView {
    /* access modifiers changed from: private */
    public boolean mAppearAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    /* access modifiers changed from: private */
    public boolean mDisappearAnimatePending;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    /* access modifiers changed from: private */
    public Runnable mDisappearFinishRunnable;
    private final int mDisappearYTranslation;
    private Space mEmptySpace;
    private Interpolator mFastOutLinearInInterpolator;
    private MiuiKeyBoardView mKeyboardView;
    private ViewGroup mKeyboardViewLayout;
    private Interpolator mLinearOutSlowInInterpolator;
    /* access modifiers changed from: private */
    public EditText mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context);
        DisappearAnimationUtils disappearAnimationUtils = new DisappearAnimationUtils(context, 125, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtils = disappearAnimationUtils;
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R.id.passwordEntry;
    }

    public boolean needsInput() {
        return false;
    }

    public void onResume(int reason) {
        super.onResume(reason);
        post(new Runnable() {
            public void run() {
                if (KeyguardPasswordView.this.isShown() && KeyguardPasswordView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                }
            }
        });
        this.mPasswordEntry.setHint(R.string.input_password_hint_text);
    }

    public void onPause() {
        super.onPause();
    }

    public void reset() {
        super.reset();
        this.mPasswordEntry.requestFocus();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPasswordEntry = (EditText) findViewById(getPasswordTextViewId());
        this.mPasswordEntryDisabler = new TextViewInputDisabler(this.mPasswordEntry);
        this.mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        this.mPasswordEntry.setInputType(0);
        this.mPasswordEntry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardPasswordView.this.mCallback.userActivity();
            }
        });
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.requestFocus();
        this.mKeyboardView = findViewById(R.id.mixed_password_keyboard_view);
        this.mKeyboardView.addKeyboardListener(new MiuiKeyBoardView.OnKeyboardActionListener() {
            public void onText(CharSequence text) {
                if (TextUtils.isEmpty(KeyguardPasswordView.this.mPasswordEntry.getText().toString())) {
                    KeyguardPasswordView.this.mPasswordEntry.setHint(R.string.input_password_hint_text);
                }
                KeyguardPasswordView.this.mCallback.userActivity();
                KeyguardPasswordView.this.mPasswordEntry.append(text);
            }

            public void onKeyBoardDelete() {
                KeyguardPasswordView.this.mCallback.userActivity();
                Editable editable = KeyguardPasswordView.this.mPasswordEntry.getText();
                if (!TextUtils.isEmpty(editable.toString())) {
                    editable.delete(editable.length() - 1, editable.length());
                }
            }

            public void onKeyBoardOK() {
                KeyguardPasswordView.this.mCallback.userActivity();
                KeyguardPasswordView.this.verifyPasswordAndUnlock();
            }
        });
        this.mEmptySpace = (Space) findViewById(R.id.empty_space);
        this.mEmptySpace.setVisibility(8);
        this.mKeyboardViewLayout = (ViewGroup) findViewById(R.id.mixed_password_keyboard_view_layout);
        setPositionForFod();
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    /* access modifiers changed from: protected */
    public void resetPasswordText(boolean animate, boolean announce) {
        this.mPasswordEntry.setText("");
        if (announce) {
            this.mPasswordEntry.setHint(R.string.wrong_password);
        }
    }

    /* access modifiers changed from: protected */
    public String getPasswordText() {
        return this.mPasswordEntry.getText().toString();
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryInputEnabled(boolean enabled) {
        this.mPasswordEntryDisabler.setInputEnabled(enabled);
    }

    public void startAppearAnimation() {
        Animator transAnim;
        RenderNodeAnimator scaleY;
        this.mKeyguardBouncerMessageView.setVisibility(0);
        setAlpha(1.0f);
        if (this.mKeyguardUpdateMonitor.getBLEUnlockState() != MiuiBleUnlockHelper.BLEUnlockState.SUCCEED) {
            this.mAppearAnimating = true;
            this.mDisappearAnimatePending = false;
            setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
            AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
            if (this.mKeyboardViewLayout.isHardwareAccelerated()) {
                this.mKeyboardViewLayout.setTranslationY((float) this.mKeyboardViewLayout.getHeight());
                Animator translationY = new RenderNodeAnimator(1, 0.0f);
                translationY.setTarget(this.mKeyboardViewLayout);
                transAnim = translationY;
                this.mKeyboardViewLayout.setScaleY(2.0f);
                scaleY = new RenderNodeAnimator(4, 1.0f);
                scaleY.setTarget(this.mKeyboardViewLayout);
            } else {
                transAnim = ObjectAnimator.ofFloat(this.mKeyboardViewLayout, View.TRANSLATION_Y, new float[]{(float) this.mKeyboardViewLayout.getHeight(), 0.0f});
                scaleY = ObjectAnimator.ofFloat(this.mKeyboardViewLayout, View.SCALE_Y, new float[]{2.0f, 1.0f});
            }
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(new Animator[]{transAnim, scaleY});
            animatorSet.setDuration(500);
            animatorSet.setInterpolator(this.mLinearOutSlowInInterpolator);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    boolean unused = KeyguardPasswordView.this.mAppearAnimating = false;
                    if (KeyguardPasswordView.this.mDisappearAnimatePending) {
                        boolean unused2 = KeyguardPasswordView.this.mDisappearAnimatePending = false;
                        KeyguardPasswordView.this.startDisappearAnimation(KeyguardPasswordView.this.mDisappearFinishRunnable);
                    }
                }
            });
            animatorSet.start();
        }
    }

    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        Animator transAnim;
        RenderNodeAnimator alpha;
        if (this.mAppearAnimating) {
            this.mDisappearAnimatePending = true;
            this.mDisappearFinishRunnable = finishRunnable;
            return true;
        }
        this.mKeyguardBouncerMessageView.setVisibility(4);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 280, (float) this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyboardViewLayout.isHardwareAccelerated()) {
            Animator translationY = new RenderNodeAnimator(1, (float) this.mDisappearYTranslation);
            translationY.setTarget(this.mKeyboardViewLayout);
            transAnim = translationY;
            alpha = new RenderNodeAnimator(11, 0.0f);
            alpha.setTarget(this);
        } else {
            transAnim = ObjectAnimator.ofFloat(this.mKeyboardViewLayout, View.TRANSLATION_Y, new float[]{this.mKeyboardViewLayout.getTranslationY(), (float) this.mDisappearYTranslation});
            alpha = ObjectAnimator.ofFloat(this, View.ALPHA, new float[]{1.0f, 0.0f});
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{transAnim, alpha});
        animatorSet.setDuration(100);
        animatorSet.setInterpolator(this.mFastOutLinearInInterpolator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Log.d("KeyguardPasswordView", "startDisappearAnimation finish");
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        });
        animatorSet.start();
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationFontScaleChanged() {
        int textSize = getResources().getDimensionPixelSize(R.dimen.miui_keyguard_view_eca_text_size);
        this.mEmergencyButton.setTextSize(0, (float) textSize);
        this.mBackButton.setTextSize(0, (float) textSize);
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationOrientationChanged() {
        LinearLayout.LayoutParams entryLayoutParams = (LinearLayout.LayoutParams) this.mPasswordEntry.getLayoutParams();
        entryLayoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_password_margin_bottom);
        this.mPasswordEntry.setLayoutParams(entryLayoutParams);
        LinearLayout.LayoutParams messageLayoutParams = (LinearLayout.LayoutParams) this.mKeyguardBouncerMessageView.getLayoutParams();
        messageLayoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_bouncer_message_view_margin_top);
        this.mKeyguardBouncerMessageView.setLayoutParams(messageLayoutParams);
        LinearLayout.LayoutParams keyboardContainerLayoutParams = (LinearLayout.LayoutParams) this.mKeyboardViewLayout.getLayoutParams();
        keyboardContainerLayoutParams.height = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_layout_height);
        this.mKeyboardViewLayout.setLayoutParams(keyboardContainerLayoutParams);
        LinearLayout.LayoutParams keyboardLayoutParams = (LinearLayout.LayoutParams) this.mKeyboardView.getLayoutParams();
        keyboardLayoutParams.height = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_key_board_view_height);
        this.mKeyboardView.setLayoutParams(keyboardLayoutParams);
    }

    private void setPositionForFod() {
        if (MiuiKeyguardUtils.isGxzwSensor() && MiuiGxzwManager.getInstance().supportFodInBouncer()) {
            DisplayManager displayManager = (DisplayManager) getContext().getSystemService("display");
            Display display = displayManager.getDisplay(0);
            Point point = new Point();
            display.getRealSize(point);
            int screenHeight = Math.max(point.x, point.y);
            int KeyboardHeight = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_key_board_view_height);
            int marginTop = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_eca_fod_margin_top);
            int marginBottom = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_eca_margin_bottom);
            int padingTop = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_layout_padingTop);
            int passwordEntryMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_password_margin_bottom);
            int entryFodMargin = getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_password_entry_fod_margin);
            MiuiGxzwUtils.caculateGxzwIconSize(getContext());
            LinearLayout.LayoutParams containerLayoutParams = (LinearLayout.LayoutParams) this.mKeyboardViewLayout.getLayoutParams();
            DisplayManager displayManager2 = displayManager;
            containerLayoutParams.bottomMargin = ((screenHeight - (((((((getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_password_view_layout_height) + marginTop) + marginBottom) - KeyboardHeight) - padingTop) - marginTop) - marginBottom) / 2)) - marginBottom) - (MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2));
            containerLayoutParams.height = containerLayoutParams.height + marginTop + marginBottom;
            this.mKeyboardViewLayout.setLayoutParams(containerLayoutParams);
            View eca = findViewById(R.id.keyguard_selector_fade_container);
            LinearLayout.LayoutParams layoutParams = containerLayoutParams;
            LinearLayout.LayoutParams ecaLayoutParams = (LinearLayout.LayoutParams) eca.getLayoutParams();
            ecaLayoutParams.topMargin = marginTop;
            eca.setLayoutParams(ecaLayoutParams);
            View view = eca;
            View passwordEntry = findViewById(R.id.passwordEntry);
            LinearLayout.LayoutParams layoutParams2 = ecaLayoutParams;
            LinearLayout.LayoutParams passwordEntryLayoutParams = (LinearLayout.LayoutParams) passwordEntry.getLayoutParams();
            Display display2 = display;
            passwordEntryLayoutParams.bottomMargin = ((passwordEntryMargin + entryFodMargin) - marginTop) - marginBottom;
            passwordEntry.setLayoutParams(passwordEntryLayoutParams);
            this.mEmptySpace.setVisibility(0);
        }
    }
}
